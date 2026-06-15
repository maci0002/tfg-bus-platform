package com.tfg.busplatform.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.busplatform.dto.LoginRequest;
import com.tfg.busplatform.dto.RegisterRequest;
import com.tfg.busplatform.notification.dto.ContactRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web (HTTP) con el contexto completo de Spring y MockMvc.
 *
 * <p>Verifican el cableado real de seguridad (rutas públicas frente a protegidas),
 * la validación de los cuerpos de petición y el flujo de extremo a extremo de
 * autenticación: registro → inicio de sesión → acceso autorizado con el token JWT.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebLayerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── Endpoints públicos de transporte ─────────────────────────────────────

    @Test
    @DisplayName("GET /transport/lines es público y devuelve la lista de líneas")
    void transportLinesIsPublic() throws Exception {
        mockMvc.perform(get("/transport/lines"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    @DisplayName("GET /transport/stops es público y devuelve las paradas")
    void transportStopsIsPublic() throws Exception {
        mockMvc.perform(get("/transport/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    // ── Verificación pública del ticket ──────────────────────────────────────

    @Test
    @DisplayName("GET /bookings/verify es público; un código inexistente devuelve no válido")
    void verifyIsPublic() throws Exception {
        mockMvc.perform(get("/bookings/verify")
                        .param("code", "NO-EXISTE")
                        .param("token", "x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ── Rutas protegidas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /bookings/my sin token devuelve 401 No autorizado")
    void myBookingsRequiresAuth() throws Exception {
        mockMvc.perform(get("/bookings/my"))
                .andExpect(status().isUnauthorized());
    }

    // ── Formulario de contacto ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /contact con datos válidos devuelve 200 y un mensaje de confirmación")
    void contactValid() throws Exception {
        ContactRequest req = ContactRequest.builder()
                .name("Ana López")
                .email("ana@example.com")
                .subject("Consulta de horarios")
                .message("Quería saber los horarios de la línea de Martos, gracias.")
                .build();

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /contact con un mensaje demasiado corto devuelve 400")
    void contactInvalid() throws Exception {
        ContactRequest req = ContactRequest.builder()
                .name("Ana")
                .email("no-es-un-email")
                .subject("Hola")
                .message("corto")
                .build();

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Autenticación ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register con un email inválido devuelve 400")
    void registerInvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Juan");
        req.setLastName("García");
        req.setEmail("esto-no-es-email");
        req.setPassword("12345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Flujo completo: registro → login → acceso autorizado con el token JWT")
    void registerLoginAndAuthorizedAccess() throws Exception {
        String email = "viajero-" + System.nanoTime() + "@example.com";

        // 1) Registro: devuelve un token de sesión.
        RegisterRequest register = new RegisterRequest();
        register.setFirstName("Lucía");
        register.setLastName("Moya");
        register.setEmail(email);
        register.setPassword("claveSegura1");

        String registerBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn().getResponse().getContentAsString();

        String registerToken = objectMapper.readTree(registerBody).get("token").asText();
        assertThat(registerToken).isNotBlank();

        // 2) Login con las mismas credenciales: devuelve otro token válido.
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword("claveSegura1");

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginBody);
        String token = loginJson.get("token").asText();
        assertThat(token).isNotBlank();

        // 3) Acceso a una ruta protegida con el token: 200 y lista vacía de reservas.
        mockMvc.perform(get("/bookings/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /auth/login con credenciales incorrectas devuelve 401")
    void loginWithWrongCredentials() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("desconocido@example.com");
        login.setPassword("contraseñaIncorrecta");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
