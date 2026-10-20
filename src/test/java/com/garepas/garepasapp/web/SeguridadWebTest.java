package com.garepas.garepasapp.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad web (auditoria basica, hallazgo A-09):
 * login, CSRF, autorizacion por rol, API publica y healthcheck.
 *
 * Usa el perfil `dev` (H2 en memoria + datos de prueba) con contrasenas
 * fijas para que el resultado no dependa del entorno.
 */
@SpringBootTest(properties = {
        "app.seed.demo-data=true",
        "app.seed.admin-password=admin123",
        "app.seed.demo-cajero-password=cajero123"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SeguridadWebTest {

    @Autowired
    private MockMvc mockMvc;

    /* ---------- Login con formulario y CSRF ---------- */

    @Test
    void loginConCredencialesValidasAutentica() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(redirectedUrl("/reportes"))
                .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    void loginConCredencialesInvalidasFalla() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "incorrecta"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void loginSinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void postDeNegocioSinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/ventas").param("productoId", "1").param("cantidad", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gastos").param("descripcion", "x").param("monto", "1"))
                .andExpect(status().isForbidden());
    }

    /* ---------- Autorizacion por rol ---------- */

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/reportes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "cajero1", roles = "VENTAS")
    void cajeroNoPuedeVerZonasDeAdministrador() throws Exception {
        mockMvc.perform(get("/reportes")).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(get("/insumos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/productos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/recetas")).andExpect(status().isForbidden());
        mockMvc.perform(get("/producciones")).andExpect(status().isForbidden());
        mockMvc.perform(get("/gastos")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cajero1", roles = "VENTAS")
    void cajeroPuedeVerPuntoDeVenta() throws Exception {
        mockMvc.perform(get("/ventas")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cajero1", roles = "VENTAS")
    void cajeroNoPuedeVerHistorialDeVentas() throws Exception {
        mockMvc.perform(get("/ventas/historial")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPuedeVerZonasDeAdministrador() throws Exception {
        mockMvc.perform(get("/reportes")).andExpect(status().isOk());
        mockMvc.perform(get("/insumos")).andExpect(status().isOk());
        mockMvc.perform(get("/usuarios")).andExpect(status().isOk());
        mockMvc.perform(get("/ventas/historial")).andExpect(status().isOk());
    }

    /* ---------- Buscador de insumos ---------- */

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void busquedaDeInsumosFiltraPorNombreCategoriaOMarca() throws Exception {
        mockMvc.perform(get("/insumos").param("q", "Mantequilla"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mantequilla")))
                .andExpect(content().string(not(containsString("Chocolate en polvo"))));

        mockMvc.perform(get("/insumos").param("q", "Colanta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Queso campesino")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void busquedaSinResultadosMuestraEstadoVacio() throws Exception {
        mockMvc.perform(get("/insumos").param("q", "zzz-no-existe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin resultados")));
    }

    /* ---------- Healthcheck ---------- */

    @Test
    void healthcheckRespondeSinAutenticacionYSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}