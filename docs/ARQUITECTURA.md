# Arquitectura de GArepasAppV2

> Documento de referencia del rediseño (2026-10-19). Describe cómo está
> organizada la aplicación, qué reglas garantiza el dominio y por qué se tomó
> cada decisión.

---

## 1. El problema que se quería resolver

La aplicación gestionaba la materia prima, la producción, las ventas y los
gastos de un negocio de arepas. Funcionaba, pero tenía cuatro problemas de
fondo que erosionaban la confianza en los datos:

| Problema | Efecto en el negocio |
|---|---|
| El stock se mutaba desde cuatro servicios distintos, cada uno con su propia forma de bloquear y validar | Ajustes sin rastro, descuentos que se pisaban entre sí, inventories descuadrados |
| Las unidades de medida se convertían en varios sitios (formulario, API, servicio) | La misma receta Depending de por dónde entrara costaba 1000 veces más o menos |
| Las referencias entre entidades eran "ids sueltos" sin clave foránea | Ventas apuntando a jornadas inexistentes, gastos apuntando a producciones borradas |
| Los reportes se calculaban en memoria cargando tablas completas | El punto de venta arrastraba todas las ventas en cada render y el reporte tenía un día fantasma |

El rediseño no es una reescritura: es una **replanificación** que hace
explícitas las reglas del negocio y las concentrates en pocos lugares.

---

## 2. Capas y paquetes

```
com.garepas.garepasapp
├── domain/                  El negocio. No depende de Spring Web ni de la vista.
│   ├── model/               Entidades JPA (12)
│   ├── repository/          Spring Data (12)
│   ├── service/             Servicios de aplicación (12)
│   ├── command/             Intenciones de entrada (commands)
│   ├── enums/               UnidadMedida, Rol, EstadoVenta, EstadoProduccion, CategoriaGasto
│   └── exception/           Excepciones de dominio
├── web/                     La capa HTTP con vistas Thymeleaf.
│   ├── controller/          10 controladores MVC
│   ├── form/                Formularios (objetos de binding)
│   ├── view/                DTOs de vista (17)
│   └── advice/              ModeloAdvice + GlobalExceptionHandler
├── api/                     API pública (menú para el cliente React)
├── config/                  Seguridad, CORS, validación de producción
│   └── seed/                Semilla de datos (solo dev) y usuario de respaldo
├── security/                Adaptador de autenticación
└── shared/                  Utilidades transversales (Formato)
```

**Regla de dependencia:** `web` y `api` dependen de `domain`; `domain` no
conoce a `web`. Por eso los comandos (`domain.command`) son objetos propios y
no DTOs de HTTP: la capa web los arma y el dominio los ejecuta.

---

## 3. Las tres reglas que sostienen todo

### 3.1 Una sola conversión de unidades

`UnidadMedida` es el único lugar donde se sabe cuánto es un kilo en gramos o
un litro en mililitros. El sistema guarda **siempre** en unidad base
(gramo, mililitro o unidad) y la **unidad de presentación** es solo lo que el
usuario elige para ver y capturar.

```
Usuario escribe            Aplicacion                    Base de datos
"5" + "kg"          ──►    UnidadMedida.aBase()  ──►    stock_actual = 5000
"$4500" + "kg"      ──►    precioABase()         ──►    precio_unidad_base = 4.5
```

La conversión ocurre **en el servicio**, nunca en el formulario ni en el
controlador. Por eso el formulario y la API producen exactamente el mismo
dato.

Se eliminaron `CUCHARADA` y `TAZA`: convertir "taza" a gramos exige la
densidad del material (1 taza de harina son ~120 g, no 240 g) y el sistema no
modela densidades. Con factores fijos de agua los costos salian mal.

### 3.2 Un solo lugar que muta el inventario

`InventarioService` es la única clase que modifica stock:

- **Bloquea siempre ordenando por id.** Dos transacciones que compartan
  insumos enganchen los bloqueos en el mismo orden y no se produce un
  deadlock. Antes cada servicio bloqueaba como podía.
- **Distingue movimientos de negocio y ajustes manuales.** Una venta o una
  producción no dejan asiento de auditoría (ya quedan trazados en la venta o
  la producción); un ajuste manual sí, en `ajustes_inventario` (quién, por
  qué, cuánto valía y cómo quedó el stock).
- **Un ajuste de compra no genera gasto.** El gasto de materia prima se
  reconoce cuando el insumo se consume en una producción. Generarlo también en
  la compra contaría el mismo dinero dos veces.

### 3.3 Las entidades se relacionan de verdad

| Antes (id suelto, sin FK) | Ahora (relación con clave foránea) |
|---|---|
| `ventas.jornada_id` → `Long` | `Venta.jornada` → `@ManyToOne Jornada` |
| `producciones.venta_id` → `Long` | `Produccion.venta` → `@ManyToOne Venta` |
| `gastos.produccion_id` → `Long` | `Gasto.produccion` → `@ManyToOne Produccion` |

La migración `V2__rediseño_dominio.sql` limpia las referencias huérfanas y
crea las claves foráneas.

---

## 4. Cambios de comportamiento del dominio

### 4.1 Las ventas se anulan, no se borran

`EstadoVenta.ACTIVA | ANULADA`. Anular devuelve el stock, revierte las
producciones automáticas y conserva el registro. Los reportes, el arqueo de
jornada y el top de productos solo cuentan las activas.

Antes, anular una venta la borraba: los reportes ya emitidos cambiaban y no
quedaba rastro de lo que pasó.

### 4.2 El costo por unidad tiene en cuenta el rendimiento

`receta.rendimiento` significa "cuántas unidades rinde un lote". El costo por
unidad es `costo de la receta / rendimiento`. Antes el margen se calculaba
como si toda receta rindiera una unidad.

El precio de un insumo fabricado es **siempre** `costo de su receta /
rendimiento`: una sola fuente de verdad. Antes cada edición de un insumo
reescribía los precios de todos los insumos fabricados y pisaba el ajuste
manual del usuario.

### 4.3 Las líneas repetidas se consolidan

El mismo producto puede aparecer en varias líneas de una venta o de una
receta. Antes se validaba línea por línea contra el stock original: dos
líneas de 5 con 8 disponibles pasaban las dos y el inventario quedaba en −2.
Ahora se consolidan antes de validar.

### 4.4 El rango de los reportes es semiabierto

`[desde 00:00, hasta+1 00:00)`. Antes se usaba un `BETWEEN` inclusivo con el
límite en la medianoche del día siguiente: ese día fantasma entraba en los
totales pero no aparecía en la serie diaria.

---

## 5. Datos

### 5.1 Migraciones

- `V1__esquema_inicial.sql` — esquema base (no se edita nunca).
- `V2__rediseño_dominio.sql` — unidades, estado de venta, claves foráneas,
  índices faltantes y bitácora de ajustes.
- `V3__clientes_credito_configuracion.sql` — clientes, abonos, configuración
  del negocio y el desglose de importes de la venta.

En producción la base ya existía, así que Flyway la marca en la versión 1
(`baseline-version=1`) y ejecuta V2 y V3. En una base nueva ejecuta las tres.

### 5.2 Índices agregados en V2

`detalle_recetas(receta_id)`, `detalle_recetas(insumo_id)`,
`gastos(produccion_id)`, `insumos(categoria)`, `insumos(activo)`,
`ventas(jornada_id)`, `ventas(estado)`, `producciones(producto_id)`.

---

## 6. Capa web

- `GlobalExceptionHandler` traduce las excepciones de dominio a mensajes
  útiles. Antes, un id inexistente terminaba en la página 500.
- Las búsquedas del historial (ventas, gastos) se resuelven **en base de
  datos** con paginación. Antes se cargaba la tabla completa y se filtraba en
  memoria.
- `InsumoForm`, `RecetaForm` y `AjusteStockForm` ya no convierten unidades:
  arman el comando y el servicio lo hace.

---

## 7. Cartera, crédito y configuración del negocio

Módulo agregado siguiendo el modelo de producto de **TREINTA** (sistema de
gestión para pequeños negocios latinoamericanos). Se implementó la
*funcionalidad*, no su código ni su marca.

### 7.1 Clientes

`Cliente` sustituye al nombre suelto que antes se escribía en cada venta:

- Nombre, teléfono, dirección, notas y estado.
- Un cliente con ventas registradas **no se borra**: se desactiva, para no
  romper el histórico.
- La lista muestra el saldo pendiente de cada uno y el total por cobrar.

### 7.2 Ventas a crédito y abonos

`Venta` ahora trae forma de pago (`EFECTIVO`, `TARJETA`, `TRANSFERENCIA`,
`CREDITO`), desglose de importes y saldo:

```
subtotal  = Σ (precio del servidor × cantidad)
impuestos = subtotal × impuesto del negocio
total     = subtotal + impuestos
saldo     = total        si la venta es a crédito
            0            si se paga al contado
```

- Una venta a crédito **exige cliente** y admite fecha de vencimiento.
- Cada abono se registra en la tabla `abonos` y baja el saldo de la venta.
- No se puede abonar más que el saldo pendiente, ni a una venta anulada.
- **No se puede anular una venta con saldo pendiente**: primero hay que
  devolver el dinero (si no, el cliente aparecería debiendo una venta que ya
  no existe).
- El saldo pendiente vive en la venta (consultable y sumable en SQL) y los
  abonos quedan como bitácora de cobros.

### 7.3 Configuración del negocio

`ConfiguracionNegocio` es una fila única con:

- Nombre comercial, identificación fiscal, teléfono y dirección.
- Impuesto por defecto (se aplica a cada venta nueva).
- Mensaje del recibo y ruta del logo.

Si la fila no existe (base nueva sin la V3), se crea con los valores por
defecto la primera vez que se consulta: la aplicación nunca se queda sin
nombre comercial ni impuesto.

### 7.4 Recibo

`/ventas/{id}/recibo` genera el comprobante con los datos del negocio, el
desglose, la forma de pago, los abonos y el mensaje de pie. Tiene hoja de
estilos de impresión: al imprimir sale solo el recibo, sin barra lateral ni
botones.

---

## 8. Frontend

### 8.1 Sistema visual

La paleta **se mantiene** (crema, burdeos, ladrillo, naranja y oro del arepero);
lo que se rediseñó es el sistema que la usa. Cuatro capas de CSS, en orden:

| Capa | Archivo | Qué resuelve |
|---|---|---|
| Tokens | `css/tokens.css` | Color, tipografía, forma, espacio, sombra y movimiento. Única fuente de verdad. |
| Base | `css/base.css` | Lienzo, escala tipográfica, foco visible, scrollbars, `prefers-reduced-motion`. |
| Componentes | `css/components.css` | Bootstrap reestilizado (botones, tarjetas, tablas, formularios, insignias, alertas, modales) + piezas propias (KPIs, barras, chips, estados vacíos). |
| Layout | `css/layout.css` | El armazón: barra lateral, barra superior e inferior en móvil, encabezado de página, punto de venta, login y errores. |

Decisiones de diseño:

- **Tipografía con dos voces.** Fraunces para lo editorial (marca, títulos de
  página, cifras grandes de los indicadores no) e Inter para la interfaz. Las
  cifras usan `tabular-nums` para que las columnas de dinero cuadren.
- **Numeración en sans, no en serif.** Antes los precios se veían en Fraunces
  y se leían pesados; ahora van en Inter con cifras tabulares.
- **Barra lateral como ancla.** Fondo en degradado burdeos, secciones
  rotuladas en oro con una regla, iconos en cuadro y el enlace activo como
  pastilla de degradado ladrillo-naranja con barra dorada.
- **Indicadores (KPI) unificados.** Antes eran cuatro tarjetas de colores
  distintos (dos macizas, una blanca, una amarilla). Ahora todas comparten la
  misma estructura: barra de color superior, icono en chip, valor grande y
  nota; el color solo cambia el acento.
- **Tablas sin ruido.** Sin zebra, sin bordes verticales: cabeceras en
  microversalita, separadores de un píxel y realce arena al pasar el ratón.
- **Botones con un solo lenguaje.** Primario (degradado ladrillo-naranja),
  contorno, fantasma y peligro tiñe. El botón deprimido se eleva al pasar el
  ratón.
- **En móvil, cada fila de tabla se convierte en tarjeta** con la etiqueta de
  la columna a la izquierda (`tablas-moviles.js`).
- **Estados vacíos ilustrados**: círculo con icono, título y explicación.
- **Rangos de reporte segmentados** en lugar de un grupo de botones.

### 8.2 Sin CDN

Bootstrap 5.3.8, Bootstrap Icons 1.11.3 y Chart.js 4.5.1 se sirven desde
`static/vendor`. Antes, si el CDN caía, el modal de confirmación no existía y
**todas las acciones destructivas se enviaban sin preguntar**. Ahora, si
Bootstrap no está, `ui-feedback.js` cae a `window.confirm`.

> Las tipografías (Fraunces e Inter) sí siguen viniendo de Google Fonts: si no
> cargan, el navegador usa las del sistema sin romper nada.

### 8.3 El service worker no cachea páginas

Las páginas muestran datos de la sesión y del negocio: cachearlas hacia que,
en un dispositivo compartido, la persona siguiente viera el HTML de la
anterior. El SW cachea solo estáticos y responde `/offline.html` sin red.

### 8.4 Otros ajustes

- `pos-cart.js` valida los datos que llegan del servidor antes de usarlos.
- La ruta de recursos estática **`/vendor/**` está en el `permitAll`** de
  Spring Security: sin esa línea el navegador recibía el HTML del login en vez
  del CSS y la aplicación se veía sin estilos.

---

## 9. Pruebas

95 pruebas (antes 49). Cada corrección de fondo tiene su prueba de regresión:

| Prueba | Qué fija |
|---|---|
| `VentaServiceTest.rechazaVentaConElMismoProductoRepetidoQueExcedeElStock` | No hay overselling |
| `InsumoServiceTest.convierteElAjusteDeKilogramosAGramos` | Conversión de unidades |
| `InsumoServiceTest.alCrearConvierteStockYPrecioDePresentacionABase` | Persistencia en unidad base |
| `RecetaServiceTest.convierteLaCantidadDeKilogramosAGramos` | Conversión en recetas |
| `RecetaServiceTest.consolidaElMismoInsumoRepetidoEnUnaSolaLinea` | Consolidación de ingredientes |
| `ProduccionServiceTest.consolidaLosIngredientesRepetidosAlCalcularLaSuficiencia` | Suficiencia agregada |
| `ReporteServiceTest.elRangoEsSemiabiertoYNoIncluyeLaMedianocheDelDiaSiguiente` | Rango sin día fantasma |
| `CostoServiceTest.elCostoUnitarioDividePorElRendimientoDeLaReceta` | Costo por unidad real |
| `CostoServiceTest.elMargenDeSugerenciaSeInterpretaComoPorcentaje` | Margen porcentual |
| `UsuarioServiceTest.noSePuedeEliminarElUltimoAdministradorActivo` | Sistema con administrador |
| `EsquemaFlywayTest` | El esquema de las migraciones (V1, V2 y V3) valida contra las entidades |
| `CarteraTest` | Crédito, abonos, saldo pendiente, impuestos y baja de clientes |

---

## 10. Pendientes conocidos

- **Una sola jornada activa** se garantiza en el servicio, no con un índice
  único parcial de PostgreSQL (que no es portable a H2). Con concurrencia
  real conviven dos jornadas activas.
- **La unicidad de nombres ignora mayúsculas** solo en el servicio
  (`existsByNombreIgnoreCase`); la base sigue con `unique` sensible a
  mayúsculas. Un índice único sobre `lower(nombre)` requiere una migración
  específica de PostgreSQL.
- **Los ajustes de stock de productos terminados no se auditan** (la bitácora
  es de insumos). El stock de producto se ajusta con bloqueo y control de
  concurrencia, pero sin asiento.
- **Spring Boot 3.4** quedó fuera de soporte en diciembre de 2025. El plan de
  salto a 4.x sigue pendiente.
