package com.garepas.garepasapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que el esquema de migraciones (db/migration/V1__esquema_inicial.sql)
 * crea una base valida para las entidades JPA.
 *
 * Flyway ejecuta V1 sobre una H2 vacia (modo PostgreSQL) y Hibernate la valida
 * con ddl-auto=validate: si una entidad no coincide con el script, el contexto
 * no arranca y la prueba falla. La base usa un nombre propio para no mezclarse
 * con la de otros contextos de prueba.
 */
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
    void migracionV1CreaEsquemaValidoYRegistraLaVersion() {
        // Solo migraciones SQL: Flyway tambien registra una fila de marcador (tipo SCHEMA).
        Integer migracionesAplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where type = 'SQL' and version = '1' and success = true",
                Integer.class);
        assertEquals(1, migracionesAplicadas);

        Integer tablasDelNegocio = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name in "
                        + "('productos','producciones','ventas','insumos','jornadas')",
                Integer.class);
        assertEquals(5, tablasDelNegocio);
    }
}
