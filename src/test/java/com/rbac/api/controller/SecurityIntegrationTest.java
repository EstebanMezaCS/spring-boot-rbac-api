package com.rbac.api.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security tests: the full Spring context runs against an in-memory H2 database,
 * and requests go through the real filter chain (JWT filter + @PreAuthorize).
 * The demo users (admin, manager, clerk, auditor, viewer) are created by DataInitializer at startup.
 * @Transactional rolls back changes after each test so tests don't affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    private static final String USERS = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    // ---------- helpers ----------

    /** Logs in through the real endpoint and returns the "Authorization" header value. */
    private String bearer(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}""".formatted(username, password);

        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + JsonPath.read(json, "$.token");
    }

    /** Creates a throwaway user as admin and returns its id. */
    private String createTempUser(String adminToken) throws Exception {
        String json = mockMvc.perform(post(USERS)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"temp","password":"temp123","name":"Temp User","email":"temp@company.com"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(json, "$.data.id");
    }

    // ---------- authentication ----------

    @Test
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(USERS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void requestWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistrationAlwaysAssignsViewerRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newbie","password":"newbie123","name":"New User","email":"newbie@company.com"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("VIEWER"));
    }

    // ---------- authorization (role permissions) ----------

    @Test
    void viewerCannotListUsers() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", bearer("viewer", "viewer123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void auditorCanListUsers() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", bearer("auditor", "auditor123")))
                .andExpect(status().isOk());
    }

    @Test
    void auditorCannotDeleteUsers() throws Exception {
        String id = createTempUser(bearer("admin", "admin123"));

        mockMvc.perform(delete(USERS + "/" + id).header("Authorization", bearer("auditor", "auditor123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteUsers() throws Exception {
        String admin = bearer("admin", "admin123");
        String id = createTempUser(admin);

        mockMvc.perform(delete(USERS + "/" + id).header("Authorization", admin))
                .andExpect(status().isOk());
    }

    @Test
    void anyAuthenticatedUserCanListRoles() throws Exception {
        mockMvc.perform(get(USERS + "/roles").header("Authorization", bearer("viewer", "viewer123")))
                .andExpect(status().isOk());
    }
}
