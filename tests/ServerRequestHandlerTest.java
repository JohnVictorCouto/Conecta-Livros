package server;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRequestHandlerTest {

    @Test
    void healthCheckIsAvailableWithoutAnAuthenticatedSession() {
        ServerRequestHandler handler = new ServerRequestHandler(null, new AtomicBoolean());

        Map<String, Object> response = handler.handle(Map.of("operation", "server.health"));

        assertEquals(Boolean.TRUE, response.get("ok"));
        assertEquals("online", response.get("data"));
    }

    @Test
    void serverPortMatchesRemoteClientDefault() {
        assertEquals(3305, Server.PORT);
    }

    @Test
    void catalogOperationsRequireTheAdminSession() {
        ServerRequestHandler handler = new ServerRequestHandler(null, new AtomicBoolean());

        Map<String, Object> response = handler.handle(Map.of(
            "operation", "livro.obterTodos", "arguments", java.util.List.of()));

        assertEquals(Boolean.FALSE, response.get("ok"));
        assertTrue(String.valueOf(response.get("error")).contains("ADMIN"));
        assertFalse(Boolean.TRUE.equals(response.get("data")));
    }

    @Test
    void registrationRequestRequiresUsernameEmailAndPassword() {
        ServerRequestHandler handler = new ServerRequestHandler(null, new AtomicBoolean());

        Map<String, Object> response = handler.handle(Map.of(
            "operation", "auth.registration.begin",
            "arguments", java.util.List.of("reader", "reader@example.com")));

        assertEquals(Boolean.FALSE, response.get("ok"));
        assertTrue(String.valueOf(response.get("error")).contains("usuário"));
    }

    @Test
    void verificationCodeAlwaysContainsEightDigits() {
        for (int attempt = 0; attempt < 100; attempt++) {
            assertTrue(ServerRequestHandler.generateVerificationCode().matches("\\d{8}"));
        }
    }

    @Test
    void registrationChallengeRemainsActiveUntilItsTenMinuteExpiration() {
        assertTrue(ServerRequestHandler.isRegistrationActive(1_000, 999));
        assertTrue(ServerRequestHandler.isRegistrationActive(1_000, 1_000));
        assertFalse(ServerRequestHandler.isRegistrationActive(1_000, 1_001));
    }

    @Test
    void registrationCannotBeVerifiedWithoutAnActiveChallenge() {
        ServerRequestHandler handler = new ServerRequestHandler(null, new AtomicBoolean());

        Map<String, Object> response = handler.handle(Map.of(
            "operation", "auth.registration.verify",
            "arguments", java.util.List.of("unknown-challenge", "12345678")));

        assertEquals(Boolean.FALSE, response.get("ok"));
        assertTrue(String.valueOf(response.get("error")).contains("ativa"));
    }
}
