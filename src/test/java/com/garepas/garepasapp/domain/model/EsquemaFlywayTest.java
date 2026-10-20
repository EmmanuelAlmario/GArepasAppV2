package com.garepas.garepasapp.domain.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

  








@SpringBootTest(properties = {
        "app.seed.demo-data=false",
        "app.seed.admin-password=admin123",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:esquema_flyway_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("dev")
class EsquemaFlywayTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void lasMigracionesSeAplicanYRegistran() {
          
        Integer migracionesAplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where type = 'SQL' and success = true",
                Integer.class);
        assertEquals(3, migracionesAplicadas, "Deben aplicarse V1 (esquema), V2 (redisenio) y V3 (cartera)");
    }

    @Test
    void elEsquemaTieneLasTablasDelNegocio() {
        Integer tablasDelNegocio = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name in "
                        + "('productos','producciones','ventas','insumos','jornadas','ajustes_inventario',"
                        + "'clientes','abonos','configuracion_negocio')",
                Integer.class);
        assertEquals(9, tablasDelNegocio);
    }

    @Test
    void laVentaTieneFormaDePagoSaldoYCliente() {
        for (String columna : new String[]{"forma_pago", "saldo", "subtotal", "impuestos",
                "fecha_vencimiento", "cliente_id"}) {
            Boolean existe = jdbcTemplate.queryForObject(
                    "select count(*) > 0 from information_schema.columns "
                            + "where table_name = 'ventas' and column_name = ?",
                    Boolean.class, columna);
            assertTrue(existe, "Falta la columna ventas." + columna);
        }
    }

    @Test
    void laConfiguracionDelNegocioVieneConValoresPorDefecto() {
        String nombre = jdbcTemplate.queryForObject(
                "select nombre_comercial from configuracion_negocio where id = 1", String.class);
        assertEquals("Gordo Arepas", nombre);
    }

    @Test
    void losInsumosGuardanLaUnidadDePresentacion() {
        Boolean existePresentacion = jdbcTemplate.queryForObject(
                "select count(*) > 0 from information_schema.columns "
                        + "where table_name = 'insumos' and column_name = 'unidad_presentacion'",
                Boolean.class);
        assertTrue(existePresentacion, "La unidad de presentacion reemplaza a la ambigua unidad_medida");

        Boolean existeBase = jdbcTemplate.queryForObject(
                "select count(*) > 0 from information_schema.columns "
                        + "where table_name = 'insumos' and column_name = 'precio_unidad_base'",
                Boolean.class);
        assertTrue(existeBase);
    }

    @Test
    void elDetalleDeRecetaYaNoGuardaLaUnidad() {
        Boolean existe = jdbcTemplate.queryForObject(
                "select count(*) > 0 from information_schema.columns "
                        + "where table_name = 'detalle_recetas' and column_name = 'unidad_medida'",
                Boolean.class);
        assertTrue(!existe, "La unidad del detalle se deriva del insumo");
    }

    @Test
    void lasVentasTienenEstado() {
        Boolean existe = jdbcTemplate.queryForObject(
                "select count(*) > 0 from information_schema.columns "
                        + "where table_name = 'ventas' and column_name = 'estado'",
                Boolean.class);
        assertTrue(existe);
    }

    @Test
    void lasReferenciasSueltasAhoraSonClavesForaneas() {
        Integer claves = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints "
                        + "where constraint_type = 'FOREIGN KEY' "
                        + "and (constraint_name = 'fk_gastos_produccion' "
                        + "or constraint_name = 'fk_producciones_venta' "
                        + "or constraint_name = 'fk_ventas_jornada')",
                Integer.class);
        assertEquals(3, claves);
    }
}
