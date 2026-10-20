package com.garepas.garepasapp.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API publica del menu (/api/public/menu): acceso sin sesion,
 * lista blanca de campos, CORS restringido y solo lectura.
 *
 * Perfil dev (H2 + datos de prueba). El origen permitido por defecto en dev
 * es http://localhost:5173.
 */
@SpringBootTest(properties = {
        "app.seed.demo-data=true",
        "app.seed.admin-password=admin123",
        "app.seed.demo-cajero-password=cajero123"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PublicMenuWebTest {

    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";
    private static final String ORIGEN_AJENO = "https://sitio-ajeno.example";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void menuEsPublicoSinSesion() throws Exception {
        mockMvc.perform(get("/api/public/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].nombre").exists())
                .andExpect(jsonPath("$[0].precio").exists())
                .andExpect(jsonPath("$[0].disponible").exists());
    }

    @Test
    void menuNoExponeDatosInternosDelNegocio() throws Exception {
        mockMvc.perform(get("/api/public/menu"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("costoUnitario"))))
                .andExpect(content().string(not(containsString("margen"))))
                .andExpect(content().string(not(containsString("stockActual"))))
                .andExpect(content().string(not(containsString("stockMinimo"))))
                .andExpect(content().string(not(containsString("receta"))))
                .andExpect(content().string(not(containsString("insumo"))));
    }

    @Test
    void menuNoIncluyeProductosInactivos() throws Exception {
        // "Combo familiar" es inactivo en los datos demo (DataSeeder).
        mockMvc.perform(get("/api/public/menu"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Combo familiar"))));
    }

    @Test
    void menuTieneCacheCortaDelLadoDelNavegador() throws Exception {
        mockMvc.perform(get("/api/public/menu"))
                .andExpect(header().string("Cache-Control", containsString("max-age=60")));
    }

    @Test
    void menuPermiteLecturaDesdeOrigenConfigurado() throws Exception {
        mockMvc.perform(get("/api/public/menu").header("Origin", ORIGEN_PERMITIDO))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO));
    }

    @Test
    void menuNoPermiteLecturaDesdeOrigenAjeno() throws Exception {
        mockMvc.perform(get("/api/public/menu").header("Origin", ORIGEN_AJENO))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void menuEsSoloLectura() throws Exception {
        // Sin token CSRF, cualquier escritura es rechazada: no hay forma de
        // modificar el catalogo desde la API publica.
        mockMvc.perform(post("/api/public/menu"))
                .andExpect(status().is4xxClientError());
    }
}
