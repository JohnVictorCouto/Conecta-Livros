package server;

import util.WireJson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Valida mensagens do protocolo, controla a sessão ADMIN e encaminha operações
 * permitidas aos DAOs do Servidor, evitando expor chamadas arbitrárias ao Cliente.
 */
final class ServerRequestHandler {
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long REGISTRATION_CODE_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_REGISTRATION_CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    // O bloqueio é compartilhado entre conexões para que novas conexões não reiniciem as tentativas.
    private static int failedLoginAttempts;
    private static long loginBlockedUntil;

    private final AdminAuthService authService;
    private final AtomicBoolean adminSessionActive;
    private final Map<String, Method> operations = new LinkedHashMap<>();
    private boolean authenticated;
    private String authenticatedUsername;
    private PendingRegistration pendingRegistration;

    ServerRequestHandler(AdminAuthService authService, AtomicBoolean adminSessionActive) {
        this.authService = authService;
        this.adminSessionActive = adminSessionActive;
        registrar("livro", ServerDAO.LivroDAO.class);
        registrar("genero", ServerDAO.GeneroDAO.class);
        registrar("editora", ServerDAO.EditoraDAO.class);
        registrar("estante", ServerDAO.EstanteDAO.class);
        registrar("exemplar", ServerDAO.ExemplarDAO.class);
        registrar("emprestimo", ServerDAO.EmprestimoDAO.class);
        registrar("listaEscolar", ServerDAO.ListaEscolarDAO.class);
        registrar("catalogoInativo", ServerDAO.CatalogoInativoDAO.class);
    }

    Map<String, Object> handle(Object message) {
        if (!(message instanceof Map<?, ?>)) return resposta(false, null, "Requisição inválida.");
        Map<?, ?> request = (Map<?, ?>) message;
        Object operationValue = request.get("operation");
        if (!(operationValue instanceof String)) return resposta(false, null, "Operação ausente.");
        String operation = (String) operationValue;

        if ("server.health".equals(operation)) return resposta(true, "online", null);
        if ("auth.login".equals(operation)) return login(request.get("arguments"));
        if ("auth.registration.begin".equals(operation)) return beginRegistration(request.get("arguments"));
        if ("auth.registration.verify".equals(operation)) return verifyRegistration(request.get("arguments"));
        if ("auth.registration.cancel".equals(operation)) return cancelRegistration(request.get("arguments"));
        if ("auth.logout".equals(operation)) {
            endSession();
            return resposta(true, Boolean.TRUE, null);
        }
        if (!authenticated) return resposta(false, null, "Autenticação ADMIN necessária.");
        try {
            if (!authService.accountActive(authenticatedUsername)) {
                endSession();
                return resposta(false, null, "A conta ADMIN está inativa.");
            }
        } catch (Exception ex) {
            endSession();
            System.err.println("Falha ao validar sessão ADMIN: " + safeMessage(ex));
            return resposta(false, null, "Não foi possível validar a sessão ADMIN.");
        }

        Method method = operations.get(operation);
        if (method == null) return resposta(false, null, "Operação não permitida.");
        List<?> supplied = request.get("arguments") instanceof List<?>
            ? (List<?>) request.get("arguments") : java.util.Collections.emptyList();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (supplied.size() != parameterTypes.length) {
            return resposta(false, null, "Quantidade de parâmetros inválida.");
        }

        try {
            Object[] arguments = new Object[parameterTypes.length];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = WireJson.convert(supplied.get(i), parameterTypes[i]);
            }
            Object result;
            try {
                result = method.invoke(null, arguments);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                System.err.println("Falha na operação " + operation + ": " + safeMessage(cause));
                if ("emprestimo.enviarLembrete".equals(operation)) {
                    return resposta(false, null, safeMessage(cause));
                }
                return resposta(false, null, "A operação não pôde ser concluída.");
            }
            if (isMutation(method.getName())) {
                try {
                    ServerDAO.AuditoriaDAO.registrar(
                        authenticatedUsername, operation, identificarRecurso(arguments), Boolean.TRUE.equals(result));
                } catch (RuntimeException ex) {
                    System.err.println("Falha ao auditar operação " + operation + ": " + safeMessage(ex));
                    return resposta(false, null,
                        "A operação foi processada, mas a auditoria falhou. Consulte os dados antes de repetir.");
                }
            }
            return resposta(true, result, null);
        } catch (IllegalArgumentException ex) {
            return resposta(false, null, "Parâmetros inválidos para a operação.");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            System.err.println("Falha ao processar operação " + operation + ": " + safeMessage(ex));
            return resposta(false, null, "A operação não pôde ser concluída.");
        }
    }

    void endSession() {
        if (authenticated) adminSessionActive.set(false);
        authenticated = false;
        authenticatedUsername = null;
    }

    boolean hasPendingRegistration() {
        if (pendingRegistration == null) return false;
        if (!isRegistrationActive(pendingRegistration.expiresAt, System.currentTimeMillis())) {
            pendingRegistration = null;
            return false;
        }
        return true;
    }

    static boolean isRegistrationActive(long expiresAt, long now) {
        return now <= expiresAt;
    }

    private Map<String, Object> login(Object rawArguments) {
        if (authenticated) return resposta(false, null, "Esta conexão já possui uma sessão ativa.");
        if (!(rawArguments instanceof List<?>) || ((List<?>) rawArguments).size() != 2) {
            registrarFalhaLogin();
            return resposta(false, null, "Credenciais inválidas.");
        }
        List<?> arguments = (List<?>) rawArguments;
        if (!(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String)) {
            registrarFalhaLogin();
            return resposta(false, null, "Credenciais inválidas.");
        }
        String username = (String) arguments.get(0);
        if (username.length() > 80 || bloqueado()) {
            registrarFalhaLogin();
            return resposta(false, null, "Credenciais inválidas ou limite de tentativas excedido.");
        }
        try {
            if (!authService.authenticate(username, (String) arguments.get(1))) {
                registrarFalhaLogin();
                return resposta(false, null, "Credenciais inválidas.");
            }
            if (!adminSessionActive.compareAndSet(false, true)) {
                return resposta(false, null, "Já existe uma sessão ADMIN ativa.");
            }
            authenticated = true;
            authenticatedUsername = username.trim();
            limparFalhasLogin();
            return resposta(true, Boolean.TRUE, null);
        } catch (Exception ex) {
            System.err.println("Falha de autenticação no servidor: " + safeMessage(ex));
            return resposta(false, null, "Não foi possível autenticar. Verifique a configuração do servidor.");
        }
    }

    private Map<String, Object> beginRegistration(Object rawArguments) {
        if (!(rawArguments instanceof List<?>) || ((List<?>) rawArguments).size() != 3) {
            return resposta(false, null, "Informe usuário, e-mail e senha.");
        }
        List<?> arguments = (List<?>) rawArguments;
        if (!(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String)
                || !(arguments.get(2) instanceof String)) {
            return resposta(false, null, "Informe usuário, e-mail e senha válidos.");
        }
        pendingRegistration = null;
        String username = (String) arguments.get(0);
        String email = (String) arguments.get(1);
        String password = (String) arguments.get(2);
        try {
            authService.validateRegistration(username, email, password);
            authService.ensureAccountAvailable(username.trim(), email.trim());
        } catch (IllegalArgumentException ex) {
            return resposta(false, null, ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Falha ao validar dados do cadastro (" + ex.getClass().getSimpleName() + ").");
            return resposta(false, null, "Não foi possível validar o cadastro. Verifique o servidor.");
        }

        try {
            String code = generateVerificationCode();
            String challengeId = UUID.randomUUID().toString();
            EmailVerificationService.sendCode(email.trim(), code);
            pendingRegistration = new PendingRegistration(
                challengeId, code, username.trim(), email.trim(), password,
                System.currentTimeMillis() + REGISTRATION_CODE_MILLIS);
            return resposta(true, challengeId, null);
        } catch (IllegalArgumentException ex) {
            return resposta(false, null, ex.getMessage());
        } catch (Exception ex) {
            if (causedByAuthenticationFailure(ex)) {
                System.err.println("Falha SMTP: o provedor recusou a autenticação.");
                return resposta(false, null,
                    "O Gmail recusou a autenticação SMTP. Configure uma senha de app válida em EMAIL_PASSWORD "
                        + "no servidor; não use a senha normal da conta.");
            }
            System.err.println("Falha ao enviar o código de verificação: " + rootCauseSummary(ex));
            return resposta(false, null,
                "Não foi possível enviar o código de verificação. Verifique a conexão SMTP e a configuração de e-mail "
                    + "do servidor. Detalhe: " + ex.getClass().getSimpleName() + ".");
        }
    }

    private static boolean causedByAuthenticationFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof jakarta.mail.AuthenticationFailedException) return true;
            if (cause instanceof jakarta.mail.MessagingException) {
                Exception next = ((jakarta.mail.MessagingException) cause).getNextException();
                if (next instanceof jakarta.mail.AuthenticationFailedException) return true;
            }
        }
        return false;
    }

    private static String rootCauseSummary(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + safeMessage(cause);
    }

    static String generateVerificationCode() {
        return String.format(java.util.Locale.ROOT, "%08d", RANDOM.nextInt(100_000_000));
    }

    private Map<String, Object> verifyRegistration(Object rawArguments) {
        if (!(rawArguments instanceof List<?>) || ((List<?>) rawArguments).size() != 2
                || !(((List<?>) rawArguments).get(0) instanceof String)
                || !(((List<?>) rawArguments).get(1) instanceof String)) {
            return resposta(false, null, "Informe o desafio e o código de verificação.");
        }
        List<?> arguments = (List<?>) rawArguments;
        PendingRegistration pending = pendingRegistration;
        if (pending == null || !pending.challengeId.equals(arguments.get(0))) {
            return resposta(false, null, "Não há uma verificação de cadastro ativa.");
        }
        if (!isRegistrationActive(pending.expiresAt, System.currentTimeMillis())) {
            pendingRegistration = null;
            return resposta(false, null, "O código expirou. Inicie o cadastro novamente.");
        }
        String suppliedCode = (String) arguments.get(1);
        if (!suppliedCode.matches("\\d{8}") || !MessageDigest.isEqual(
                pending.code.getBytes(StandardCharsets.US_ASCII),
                suppliedCode.getBytes(StandardCharsets.US_ASCII))) {
            pending.attempts++;
            if (pending.attempts >= MAX_REGISTRATION_CODE_ATTEMPTS) {
                pendingRegistration = null;
                return resposta(false, null, "Limite de tentativas excedido. Inicie o cadastro novamente.");
            }
            return resposta(false, null, "Código incorreto. Tente novamente.");
        }

        // Consome o desafio antes de gravar para impedir reutilização mesmo se o cadastro falhar.
        pendingRegistration = null;
        try {
            authService.registerAccount(pending.username, pending.email, pending.password);
            return resposta(true, Boolean.TRUE, null);
        } catch (IllegalArgumentException ex) {
            return resposta(false, null, ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Falha ao concluir cadastro após verificação ("
                + ex.getClass().getSimpleName() + ").");
            return resposta(false, null, "Não foi possível concluir o cadastro. Verifique o servidor.");
        }
    }

    private Map<String, Object> cancelRegistration(Object rawArguments) {
        if (!(rawArguments instanceof List<?>) || ((List<?>) rawArguments).size() != 1
                || !(((List<?>) rawArguments).get(0) instanceof String)) {
            return resposta(false, null, "Desafio de verificação inválido.");
        }
        PendingRegistration pending = pendingRegistration;
        if (pending != null && pending.challengeId.equals(((List<?>) rawArguments).get(0))) {
            pendingRegistration = null;
        }
        return resposta(true, Boolean.TRUE, null);
    }

    private static final class PendingRegistration {
        private final String challengeId;
        private final String code;
        private final String username;
        private final String email;
        private final String password;
        private final long expiresAt;
        private int attempts;

        private PendingRegistration(String challengeId, String code, String username, String email,
                                    String password, long expiresAt) {
            this.challengeId = challengeId;
            this.code = code;
            this.username = username;
            this.email = email;
            this.password = password;
            this.expiresAt = expiresAt;
        }
    }

    private static synchronized boolean bloqueado() {
        return System.currentTimeMillis() < loginBlockedUntil;
    }

    private static synchronized void registrarFalhaLogin() {
        long now = System.currentTimeMillis();
        if (now < loginBlockedUntil) return;
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_LOGIN_ATTEMPTS) {
            failedLoginAttempts = 0;
            loginBlockedUntil = now + LOGIN_LOCK_MILLIS;
        }
    }

    private static synchronized void limparFalhasLogin() {
        failedLoginAttempts = 0;
        loginBlockedUntil = 0;
    }

    private void registrar(String prefixo, Class<?> dao) {
        for (Method method : dao.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !method.isSynthetic()) {
                operations.put(prefixo + "." + method.getName(), method);
            }
        }
    }

    private static boolean isMutation(String method) {
        return "inserir".equals(method) || "atualizar".equals(method) || "deletar".equals(method)
            || "registrar".equals(method) || "registrarDevolucao".equals(method)
            || "enviarLembrete".equals(method)
            || "adicionarLivro".equals(method) || "removerLivro".equals(method);
    }

    private static String identificarRecurso(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof Number) return String.valueOf(argument);
            if (argument != null && argument.getClass().getName().startsWith("model.")) {
                try {
                    Method getter = argument.getClass().getMethod("getId");
                    return String.valueOf(getter.invoke(argument));
                } catch (ReflectiveOperationException ignored) {
                    return "n/a";
                }
            }
        }
        return "n/a";
    }

    private static String safeMessage(Throwable error) {
        if (error == null || error.getMessage() == null) return "erro sem mensagem";
        return error.getMessage().replaceAll("[\\r\\n\\t]", " ");
    }

    private static Map<String, Object> resposta(boolean ok, Object data, String error) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", ok);
        response.put("data", data);
        if (error != null) response.put("error", error);
        return response;
    }
}
