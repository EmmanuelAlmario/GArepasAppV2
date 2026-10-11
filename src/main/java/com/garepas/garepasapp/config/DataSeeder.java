package com.garepas.garepasapp.config;

import com.garepas.garepasapp.dto.request.DetalleVentaRequest;
import com.garepas.garepasapp.dto.request.GastoRequest;
import com.garepas.garepasapp.dto.request.ProduccionRequest;
import com.garepas.garepasapp.dto.request.VentaRequest;
import com.garepas.garepasapp.entity.DetalleProduccion;
import com.garepas.garepasapp.entity.DetalleReceta;
import com.garepas.garepasapp.entity.DetalleVenta;
import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.entity.Jornada;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Produccion;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.entity.Usuario;
import com.garepas.garepasapp.entity.Venta;
import com.garepas.garepasapp.enums.CategoriaGasto;
import com.garepas.garepasapp.enums.EstadoProduccion;
import com.garepas.garepasapp.enums.Rol;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.repository.GastoRepository;
import com.garepas.garepasapp.repository.InsumoRepository;
import com.garepas.garepasapp.repository.JornadaRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.ProduccionRepository;
import com.garepas.garepasapp.repository.RecetaRepository;
import com.garepas.garepasapp.repository.UsuarioRepository;
import com.garepas.garepasapp.repository.VentaRepository;
import com.garepas.garepasapp.service.GastoService;
import com.garepas.garepasapp.service.JornadaService;
import com.garepas.garepasapp.service.ProduccionService;
import com.garepas.garepasapp.service.VentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.garepas.garepasapp.enums.UnidadMedida.GRAMO;
import static com.garepas.garepasapp.enums.UnidadMedida.KILOGRAMO;
import static com.garepas.garepasapp.enums.UnidadMedida.LITRO;
import static com.garepas.garepasapp.enums.UnidadMedida.MILILITRO;
import static com.garepas.garepasapp.enums.UnidadMedida.UNIDAD;

/**
 * Datos de prueba para desarrollo y demos.
 *
 * Solo se activa con el perfil `dev` (@Profile("dev")): en produccion la
 * clase no se registra. Además exige app.seed.demo-data=true y que la base
 * todavia no tenga productos, para no duplicar nada en un redeploy.
 *
 * Estrategia:
 * - Historial retroactivo (últimos 9 días): entidades creadas directamente
 *   con fechas pasadas, replicando manualmente los mismos efectos que aplican
 *   los servicios (descuento de stock, snapshot de precio por gramo y gasto
 *   automático de materia prima). Así los reportes por rango (hoy / 7 días /
 *   mes) muestran una serie real.
 * - Día de hoy: usa la lógica de negocio real (JornadaService,
 *   ProduccionService, VentaService, GastoService), con una jornada abierta,
 *   ventas asociadas a ella y una producción PENDIENTE de ejemplo.
 */
@Component
@Order(2)
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final ProduccionRepository produccionRepository;
    private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;
    private final JornadaRepository jornadaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final JornadaService jornadaService;
    private final ProduccionService produccionService;
    private final VentaService ventaService;
    private final GastoService gastoService;

    @Value("${app.seed.demo-data:false}")
    private boolean demoData;

    @Value("${app.seed.demo-cajero-password}")
    private String cajeroPassword;

    /** Insumos de trabajo, con el stock actualizado en memoria durante el sembrado. */
    private Map<String, Insumo> insumos;
    /** Recetas de trabajo por nombre. */
    private Map<String, Receta> recetas;
    /** Productos de trabajo por nombre. */
    private Map<String, Producto> productos;

    @Override
    public void run(String... args) {
        if (!demoData) {
            return;
        }
        if (productoRepository.count() > 0) {
            log.info("Datos de prueba omitidos: la base ya contiene productos.");
            return;
        }
        log.info("Sembrando datos de prueba (insumos, recetas, productos, jornadas, ventas y gastos)...");
        // Una sola transaccion: las entidades quedan gestionadas durante todo
        // el sembrado (sin choques de @Version entre saves sueltos) y si algo
        // falla la base queda vacia para reintentar en el proximo arranque.
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> sembrar());
        log.info("Datos de prueba de desarrollo listos (perfil dev).");
    }

    private void sembrar() {
        crearUsuarioCajero();
        crearInsumos();
        crearRecetas();
        crearProductos();
        sembrarHistorial();
        sembrarHoy();
    }

    /* ============================ Catálogo ============================ */

    private void crearUsuarioCajero() {
        if (usuarioRepository.findByUsernameIgnoreCase("cajero1").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("cajero1")
                    .password(passwordEncoder.encode(cajeroPassword))
                    .rol(Rol.VENTAS)
                    .activo(true)
                    .build());
        }
    }

    /**
     * Insumos con stock y precio expresados en unidad base (g / ml / u),
     * igual que lo haría el controlador convirtiendo la presentación con
     * UnidadUtil. La unidad guardada es la de presentación (kg, L, u...).
     */
    private void crearInsumos() {
        insumos = new LinkedHashMap<>();
        insumos.put("Harina de maiz precocida", insumo("Harina de maiz precocida",
                "Moliendas", "Diana", KILOGRAMO, "40000", "5000", "4.5", true));
        insumos.put("Queso campesino", insumo("Queso campesino",
                "Lacteos", "Colanta", KILOGRAMO, "9000", "2000", "14", true));
        insumos.put("Huevo", insumo("Huevo",
                "Proteinas", "Granja El Roble", UNIDAD, "180", "60", "550", true));
        insumos.put("Mantequilla", insumo("Mantequilla",
                "Grasas y aceites", "Alpina", KILOGRAMO, "2500", "500", "12", true));
        insumos.put("Aceite de girasol", insumo("Aceite de girasol",
                "Grasas y aceites", "Familia", LITRO, "5000", "1000", "9", true));
        insumos.put("Sal refinada", insumo("Sal refinada",
                "Abarrotes", "Refisal", KILOGRAMO, "4000", "1000", "2.5", true));
        insumos.put("Azucar", insumo("Azucar",
                "Abarrotes", "Incauca", KILOGRAMO, "3000", "1000", "3.2", true));
        insumos.put("Chocolate en polvo", insumo("Chocolate en polvo",
                "Abarrotes", "Lukas", KILOGRAMO, "0", "500", "16", true));
        insumos.put("Bolsa de papel kraft", insumo("Bolsa de papel kraft",
                "Empaques", "Envicol", UNIDAD, "300", "100", "150", true));
        insumos.put("Curcuma en polvo", insumo("Curcuma en polvo",
                "Abarrotes", "La Perla", KILOGRAMO, "300", "500", "8", false));
        insumoRepository.saveAll(insumos.values());
    }

    private Insumo insumo(String nombre, String categoria, String marca,
                          UnidadMedida presentacion, String stockBase,
                          String minimoBase, String precioPorBase, boolean activo) {
        return Insumo.builder()
                .nombre(nombre)
                .categoria(categoria)
                .marca(marca)
                .stockActual(new BigDecimal(stockBase))
                .stockMinimo(new BigDecimal(minimoBase))
                .precioPorGramo(new BigDecimal(precioPorBase))
                .unidadMedida(presentacion)
                .activo(activo)
                .build();
    }

    /**
     * Recetas con cantidades por unidad de producto, ya convertidas a la
     * unidad base (g / ml / u), tal como las guarda RecetaForm.filasValidas().
     */
    private void crearRecetas() {
        recetas = new LinkedHashMap<>();
        recetas.put("Arepa de queso asada", receta("Arepa de queso asada",
                "Arepa gruesa rellena de queso, asada en plancha.",
                detalle("Harina de maiz precocida", "120", GRAMO),
                detalle("Queso campesino", "40", GRAMO),
                detalle("Mantequilla", "8", GRAMO),
                detalle("Sal refinada", "2", GRAMO)));

        recetas.put("Arepa plana", receta("Arepa plana",
                "Arepa delgada tradicional, asada y sin relleno.",
                detalle("Harina de maiz precocida", "100", GRAMO),
                detalle("Aceite de girasol", "3", MILILITRO),
                detalle("Sal refinada", "2", GRAMO)));

        recetas.put("Arepa de huevo", receta("Arepa de huevo",
                "Arepa abierta con huevo frito adentro.",
                detalle("Harina de maiz precocida", "110", GRAMO),
                detalle("Huevo", "1", UNIDAD),
                detalle("Queso campesino", "20", GRAMO),
                detalle("Sal refinada", "2", GRAMO)));

        recetas.put("Chocolate caliente", receta("Chocolate caliente",
                "Bebida caliente con chocolate Lukas.",
                detalle("Chocolate en polvo", "25", GRAMO),
                detalle("Azucar", "10", GRAMO)));
    }

    private Receta receta(String nombre, String descripcion, DetalleReceta... detalles) {
        Receta receta = Receta.builder().nombre(nombre).descripcion(descripcion).build();
        for (DetalleReceta d : detalles) {
            // Igual que RecetaService: el detalle debe apuntar a su receta
            // para que el cascade persista la FK.
            d.setReceta(receta);
            receta.getDetalles().add(d);
        }
        // cascade = ALL en Receta.detalles: se guardan junto con la receta.
        return recetaRepository.save(receta);
    }

    private DetalleReceta detalle(String insumoNombre, String cantidad, UnidadMedida unidadBase) {
        return DetalleReceta.builder()
                .insumo(insumos.get(insumoNombre))
                .cantidad(new BigDecimal(cantidad))
                .unidadMedida(unidadBase)
                .build();
    }

    private void crearProductos() {
        productos = new LinkedHashMap<>();
        productos.put("Arepa de queso asada", producto("Arepa de queso asada", "Arepa de queso asada", 3500, 10, true));
        productos.put("Arepa plana", producto("Arepa plana", "Arepa plana", 2500, 10, true));
        productos.put("Arepa de huevo", producto("Arepa de huevo", "Arepa de huevo", 4000, 8, true));
        productos.put("Chocolate caliente", producto("Chocolate caliente", "Chocolate caliente", 3000, 5, true));
        // Comprada lista para vender: no tiene receta, el stock se ingresa directo.
        productos.put("Gaseosa 400 ml", producto("Gaseosa 400 ml", null, 2000, null, true, 24));
        // Producto antiguo desactivado: sirve para ver el estado "Inactivo" en el catalogo.
        productos.put("Combo familiar", producto("Combo familiar", null, 18000, null, false, 0));
        productoRepository.saveAll(productos.values());
    }

    private Producto producto(String nombre, String recetaNombre, int precioVenta,
                              Integer stockMinimo, boolean activo) {
        return producto(nombre, recetaNombre, precioVenta, stockMinimo, activo, 0);
    }

    private Producto producto(String nombre, String recetaNombre, int precioVenta,
                              Integer stockMinimo, boolean activo, int stockInicial) {
        Producto.ProductoBuilder builder = Producto.builder()
                .nombre(nombre)
                .stockActual(stockInicial)
                .precioVenta(BigDecimal.valueOf(precioVenta))
                .activo(activo);
        if (recetaNombre != null) {
            builder.receta(recetas.get(recetaNombre));
        }
        if (stockMinimo != null) {
            builder.stockMinimo(stockMinimo);
        }
        return builder.build();
    }

    /* ====================== Historial (últimos 9 días) ====================== */

    /**
     * Producciones, ventas y gastos operativos de los últimos 9 días. Las
     * fechas retroactivas se construyen a mano porque los servicios sellan
     * LocalDateTime.now(); aquí se replican exactamente los mismos efectos.
     */
    private void sembrarHistorial() {
        LocalDate hoy = LocalDate.now();

        // ---- Día -9 ----
        LocalDate d = hoy.minusDays(9);
        Jornada j = jornadaPasada(d);
        produccionCompletada("Arepa de queso asada", 40, d.atTime(6, 30));
        produccionCompletada("Arepa de huevo", 30, d.atTime(6, 45));
        gastoOperativo(CategoriaGasto.TRANSPORTE, "Gasolina del motocarro", 8000, d.atTime(8, 0));
        ventaPasada(d.atTime(10, 15), j, null, linea("Arepa de queso asada", 5));
        ventaPasada(d.atTime(12, 30), j, null, linea("Gaseosa 400 ml", 2));

        // ---- Día -8 ----
        d = hoy.minusDays(8);
        j = jornadaPasada(d);
        produccionCompletada("Arepa plana", 50, d.atTime(6, 30));
        gastoOperativo(CategoriaGasto.SERVICIOS, "Recarga de gas propano", 28000, d.atTime(8, 30));
        ventaPasada(d.atTime(11, 0), j, "Laura Gómez", linea("Arepa plana", 8));
        ventaPasada(d.atTime(15, 45), j, null, linea("Arepa de queso asada", 4));

        // ---- Día -7 ----
        d = hoy.minusDays(7);
        j = jornadaPasada(d);
        ventaPasada(d.atTime(10, 0), j, null, linea("Arepa de queso asada", 6));
        ventaPasada(d.atTime(13, 20), j, null, linea("Arepa plana", 5));
        ventaPasada(d.atTime(17, 0), j, "Andrés Ruiz", linea("Arepa de huevo", 4));

        // ---- Día -6 ----
        d = hoy.minusDays(6);
        j = jornadaPasada(d);
        gastoOperativo(CategoriaGasto.TRANSPORTE, "Pasajes para recoger insumos", 6500, d.atTime(9, 0));
        ventaPasada(d.atTime(9, 45), j, "Pedro Ruiz", linea("Arepa de huevo", 6));
        ventaPasada(d.atTime(16, 30), j, null, linea("Arepa plana", 10));

        // ---- Día -5 ----
        d = hoy.minusDays(5);
        j = jornadaPasada(d);
        produccionCompletada("Arepa de queso asada", 40, d.atTime(6, 30));
        ventaPasada(d.atTime(11, 30), j, null, linea("Arepa de queso asada", 10));
        ventaPasada(d.atTime(14, 0), j, "Marta Salas", linea("Arepa de huevo", 5));

        // ---- Día -4 ----
        d = hoy.minusDays(4);
        j = jornadaPasada(d);
        gastoOperativo(CategoriaGasto.MANTENIMIENTO, "Reparación de la plancha de asar", 15000, d.atTime(8, 0));
        ventaPasada(d.atTime(10, 30), j, null, linea("Arepa plana", 12));
        ventaPasada(d.atTime(18, 15), j, "Camilo Duarte", linea("Gaseosa 400 ml", 6));

        // ---- Día -3 ----
        d = hoy.minusDays(3);
        j = jornadaPasada(d);
        produccionCompletada("Arepa de huevo", 30, d.atTime(6, 30));
        ventaPasada(d.atTime(12, 0), j, null, linea("Arepa de huevo", 7));
        ventaPasada(d.atTime(16, 40), j, null, linea("Arepa de queso asada", 6));

        // ---- Día -2 ----
        d = hoy.minusDays(2);
        j = jornadaPasada(d);
        gastoOperativo(CategoriaGasto.OTROS, "Bolsas de papel y servilletas", 10000, d.atTime(9, 0));
        ventaPasada(d.atTime(10, 10), j, null, linea("Arepa de queso asada", 12));
        ventaPasada(d.atTime(13, 0), j, null, linea("Arepa plana", 8));
        ventaPasada(d.atTime(17, 30), j, "Andrés Bedoya",
                linea("Arepa de huevo", 6), linea("Gaseosa 400 ml", 4));

        // ---- Día -1 ----
        d = hoy.minusDays(1);
        j = jornadaPasada(d);
        produccionCompletada("Arepa plana", 50, d.atTime(6, 30));
        produccionCompletada("Arepa de queso asada", 30, d.atTime(6, 40));
        gastoOperativo(CategoriaGasto.SERVICIOS, "Agua y energía del local", 45000, d.atTime(8, 0));
        ventaPasada(d.atTime(11, 0), j, null, linea("Arepa de queso asada", 9));
        ventaPasada(d.atTime(15, 0), j, null, linea("Arepa plana", 7));
        ventaPasada(d.atTime(18, 30), j, "Diana Pérez", linea("Arepa de huevo", 8));
    }

    private Jornada jornadaPasada(LocalDate dia) {
        return jornadaRepository.save(Jornada.builder()
                .fechaApertura(dia.atTime(7, 0))
                .fechaCierre(dia.atTime(19, 30))
                .abiertaPor("admin")
                .activa(Boolean.FALSE)
                .build());
    }

    /**
     * Producción COMPLETADA retroactiva: replica ProduccionService.registrar +
     * aplicarProduccion (descuento de insumos, aumento de producto y gasto de
     * materia prima con snapshot de precio y produccionId de referencia).
     */
    private void produccionCompletada(String productoNombre, int cantidad, LocalDateTime fecha) {
        Producto producto = productos.get(productoNombre);
        Receta receta = producto.getReceta();
        if (receta == null) {
            throw new IllegalStateException("El producto de prueba '" + productoNombre + "' no tiene receta.");
        }

        Produccion produccion = Produccion.builder()
                .producto(producto)
                .cantidad(cantidad)
                .fecha(fecha)
                .costoTotal(BigDecimal.ZERO)
                .estado(EstadoProduccion.PENDIENTE)
                .build();

        List<DetalleProduccion> detalles = new ArrayList<>();
        List<Insumo> consumidos = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;
        for (DetalleReceta detalleReceta : receta.getDetalles()) {
            Insumo insumo = detalleReceta.getInsumo();
            BigDecimal requerida = detalleReceta.getCantidad().multiply(BigDecimal.valueOf(cantidad));
            if (insumo.getStockActual().compareTo(requerida) < 0) {
                throw new IllegalStateException("Datos de prueba inconsistentes: stock insuficiente de '"
                        + insumo.getNombre() + "' para " + productoNombre + " x" + cantidad);
            }
            BigDecimal snapshot = insumo.getPrecioPorGramo();
            BigDecimal costoLinea = requerida.multiply(snapshot);
            detalles.add(DetalleProduccion.builder()
                    .produccion(produccion)
                    .insumo(insumo)
                    .cantidadRequerida(requerida)
                    .cantidadUsada(requerida)
                    .precioPorGramoSnapshot(snapshot)
                    .costoLinea(costoLinea)
                    .suficiente(Boolean.TRUE)
                    .build());
            insumo.setStockActual(insumo.getStockActual().subtract(requerida));
            consumidos.add(insumo);
            costoTotal = costoTotal.add(costoLinea);
        }

        produccion.setDetalles(detalles);
        produccion.setCostoTotal(costoTotal);
        produccion.setEstado(EstadoProduccion.COMPLETADA);
        Produccion guardada = produccionRepository.save(produccion);

        insumoRepository.saveAll(consumidos);
        producto.setStockActual(producto.getStockActual() + cantidad);
        productoRepository.save(producto);

        gastoRepository.save(Gasto.builder()
                .descripcion("Materia prima — " + producto.getNombre() + " x" + cantidad
                        + " (producción #" + guardada.getId() + ")")
                .monto(costoTotal.setScale(2, RoundingMode.HALF_UP))
                .categoria(CategoriaGasto.MATERIA_PRIMA)
                .fecha(fecha)
                .produccionId(guardada.getId())
                .build());
    }

    private void gastoOperativo(CategoriaGasto categoria, String descripcion, long monto, LocalDateTime fecha) {
        gastoRepository.save(Gasto.builder()
                .descripcion(descripcion)
                .monto(BigDecimal.valueOf(monto))
                .categoria(categoria)
                .fecha(fecha)
                .build());
    }

    /** Venta retroactiva: replica VentaService.registrar con fecha fija. */
    private void ventaPasada(LocalDateTime fecha, Jornada jornada, String cliente,
                             DetalleVentaRequest... lineas) {
        Venta venta = Venta.builder()
                .fecha(fecha)
                .total(BigDecimal.ZERO)
                .nombreCliente(cliente)
                .jornadaId(jornada.getId())
                .build();

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVentaRequest linea : lineas) {
            Producto producto = productos.get(productoNombre(linea.productoId()));
            if (producto.getStockActual() < linea.cantidad()) {
                throw new IllegalStateException("Datos de prueba inconsistentes: stock insuficiente de '"
                        + producto.getNombre() + "' para la venta simulada.");
            }
            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(linea.cantidad()));
            producto.setStockActual(producto.getStockActual() - linea.cantidad());
            productoRepository.save(producto);
            detalles.add(DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(linea.cantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        venta.setTotal(total);
        venta.setDetalles(detalles);
        ventaRepository.save(venta);
    }

    private DetalleVentaRequest linea(String productoNombre, int cantidad) {
        return new DetalleVentaRequest(productos.get(productoNombre).getId(), cantidad);
    }

    private String productoNombre(Long productoId) {
        return productos.entrySet().stream()
                .filter(e -> e.getValue().getId().equals(productoId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Producto no encontrado en el sembrado: id " + productoId));
    }

    /* ============================ Día de hoy ============================ */

    /**
     * Operación del día de hoy usando los servicios reales: abre jornada,
     * repone producto, registra ventas y un gasto, y deja una producción
     * PENDIENTE (chocolate en cero) para probar el flujo de verificación.
     */
    private void sembrarHoy() {
        jornadaService.abrir("admin");

        produccionService.registrar(new ProduccionRequest(
                productos.get("Arepa plana").getId(), 30));

        ventaService.registrar(new VentaRequest("Cliente presencial", List.of(
                new DetalleVentaRequest(productos.get("Arepa de queso asada").getId(), 3),
                new DetalleVentaRequest(productos.get("Gaseosa 400 ml").getId(), 1))));
        ventaService.registrar(new VentaRequest(null, List.of(
                new DetalleVentaRequest(productos.get("Arepa plana").getId(), 2))));
        ventaService.registrar(new VentaRequest("Camila Torres", List.of(
                new DetalleVentaRequest(productos.get("Arepa de huevo").getId(), 1),
                new DetalleVentaRequest(productos.get("Arepa de queso asada").getId(), 2))));

        gastoService.registrar(new GastoRequest(
                "Combustible para domicilios", BigDecimal.valueOf(7000), CategoriaGasto.TRANSPORTE));

        // Queda PENDIENTE porque el chocolate está en cero: permite probar el
        // flujo "Verificar stock" desde el detalle de la producción.
        produccionService.registrar(new ProduccionRequest(
                productos.get("Chocolate caliente").getId(), 20));
    }
}