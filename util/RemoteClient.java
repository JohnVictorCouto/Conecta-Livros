package util;

import javax.swing.SwingUtilities;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Fachada síncrona de comunicação com o Servidor.
 * Executa a entrada e saída do socket em uma thread de trabalho para não bloquear
 * a thread de despacho de eventos (EDT) exigida pela interface Swing.
 */
public final class RemoteClient {
    private static final ScheduledExecutorService HEARTBEAT = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "conecta-livros-client-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    // O executor serializa as operações porque todas compartilham o mesmo socket e os mesmos fluxos.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "conecta-livros-client-io");
        thread.setDaemon(true);
        return thread;
    });

    private static final RemoteClient INSTANCE = new RemoteClient();
    private static final long HEARTBEAT_INTERVAL_SECONDS = 10;
    private Socket socket;
    private BufferedReader input;
    private BufferedWriter output;
    private volatile boolean authenticated;
    private ScheduledFuture<?> heartbeatTask;

    private RemoteClient() {}

    public static boolean authenticate(String username, char[] password) {
        String secret = new String(password);
        try {
            Map<?, ?> response = INSTANCE.execute("auth.login", username, secret);
            boolean authenticated = Boolean.TRUE.equals(response.get("data"));
            INSTANCE.authenticated = authenticated;
            if (authenticated) INSTANCE.startHeartbeat();
            return authenticated;
        } catch (RuntimeException ex) {
            INSTANCE.disconnect();
            throw ex;
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public static String beginAccountRegistration(String username, String email, char[] password) {
        String secret = new String(password);
        try {
            Map<?, ?> response = INSTANCE.execute("auth.registration.begin", username, email, secret);
            Object challengeId = response.get("data");
            if (challengeId instanceof String) return (String) challengeId;
            throw new IllegalStateException("Resposta inválida ao iniciar a verificação de e-mail.");
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public static void verifyAccountRegistration(String challengeId, String code) {
        INSTANCE.execute("auth.registration.verify", challengeId, code);
    }

    public static void cancelAccountRegistration(String challengeId) {
        INSTANCE.execute("auth.registration.cancel", challengeId);
    }

    public static <T> T call(String operation, Class<T> resultType, Class<?> itemType,
                             Object... arguments) {
        if (!INSTANCE.authenticated) {
            throw new IllegalStateException("A sessão ADMIN não está autenticada.");
        }
        Map<?, ?> response = INSTANCE.execute(operation, arguments);
        Object data = response.get("data");
        if (data == null) return null;
        if (List.class.isAssignableFrom(resultType)) {
            if (!(data instanceof List<?>)) throw new IllegalStateException("Resposta inválida do servidor.");
            List<Object> converted = new ArrayList<>();
            for (Object item : (List<?>) data) converted.add(WireJson.convert(item, itemType));
            @SuppressWarnings("unchecked")
            T list = (T) converted;
            return list;
        }
        return WireJson.convert(data, resultType);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> callList(String operation, Class<T> itemType, Object... arguments) {
        return (List<T>) call(operation, List.class, itemType, arguments);
    }

    public static void logout() {
        INSTANCE.stopHeartbeat();
        if (INSTANCE.authenticated) {
            try {
                INSTANCE.execute("auth.logout");
            } catch (RuntimeException ex) {
                System.err.println("Falha ao encerrar sessão no servidor: " + ex.getMessage());
            }
        }
        INSTANCE.disconnect();
    }

    private Map<?, ?> execute(String operation, Object... arguments) {
        CompletableFuture<Map<?, ?>> future =
            CompletableFuture.supplyAsync(() -> executeOnWorker(operation, arguments), IO);
        try {
            Map<?, ?> response;
            if (SwingUtilities.isEventDispatchThread()) {
                EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                SecondaryLoop loop = queue.createSecondaryLoop();
                // Mantém a EDT processando eventos enquanto a chamada aguarda a resposta de rede.
                future.whenComplete((result, error) -> EventQueue.invokeLater(loop::exit));
                if (!loop.enter()) throw new IllegalStateException("Não foi possível iniciar a espera de rede.");
                response = future.get();
            } else {
                response = future.get();
            }
            if (!Boolean.TRUE.equals(response.get("ok"))) {
                throw new IllegalStateException(String.valueOf(response.get("error")));
            }
            return response;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A comunicação com o servidor foi interrompida.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new IllegalStateException("Falha na comunicação com o servidor.", cause);
        }
    }

    private synchronized Map<?, ?> executeOnWorker(String operation, Object... arguments) {
        try {
            ensureConnected();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("operation", operation);
            request.put("arguments", java.util.Arrays.asList(arguments));
            output.write(WireJson.stringify(request));
            output.newLine();
            output.flush();
            String line = input.readLine();
            if (line == null) throw new IOException("O servidor encerrou a conexão.");
            Object parsed = WireJson.parse(line);
            if (!(parsed instanceof Map<?, ?>)) throw new IOException("Resposta do servidor inválida.");
            return (Map<?, ?>) parsed;
        } catch (IOException | IllegalArgumentException ex) {
            disconnect();
            throw new IllegalStateException("Não foi possível comunicar com o servidor: " + ex.getMessage(), ex);
        }
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        ServerAddress address = ServerAddress.load();
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(address.host, address.port), 5000);
        newSocket.setSoTimeout(30000);
        socket = newSocket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private synchronized void disconnect() {
        stopHeartbeat();
        authenticated = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ex) {
            System.err.println("Falha ao fechar conexão com o servidor: " + ex.getMessage());
        } finally {
            socket = null;
            input = null;
            output = null;
        }
    }

    private synchronized void startHeartbeat() {
        stopHeartbeat();
        // O servidor fecha conexões ociosas em 30 s; este intervalo mantém a sessão viva.
        heartbeatTask = HEARTBEAT.scheduleWithFixedDelay(() -> {
            try {
                Map<?, ?> response = execute("server.health");
                if (!"online".equals(response.get("data"))) {
                    throw new IllegalStateException("Resposta inválida ao verificar a conexão com o servidor.");
                }
            } catch (RuntimeException ex) {
                System.err.println("A sessão foi desconectada do servidor: " + ex.getMessage());
                disconnect();
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    private static final class ServerAddress {
        private final String host;
        private final int port;

        private ServerAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private static ServerAddress load() {
            String host = setting("SERVER_HOST", "127.0.0.1");
            String portValue = setting("SERVER_PORT", "3305");
            try {
                int port = Integer.parseInt(portValue);
                if (port < 1 || port > 65535) throw new NumberFormatException();
                return new ServerAddress(host, port);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("SERVER_PORT deve estar entre 1 e 65535.");
            }
        }

        private static String setting(String name, String fallback) {
            String value = System.getProperty(name.toLowerCase().replace('_', '.'));
            if (value == null || value.isBlank()) value = System.getenv(name);
            if (value == null || value.isBlank()) value = fileSetting(name);
            return value == null || value.isBlank() ? fallback : value.trim();
        }

        private static String fileSetting(String key) {
            java.nio.file.Path path = java.nio.file.Paths.get("config.properties");
            if (!java.nio.file.Files.isRegularFile(path)) return null;
            try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(key + "=") || trimmed.startsWith(key + " =")) {
                        return trimmed.substring(trimmed.indexOf('=') + 1).trim();
                    }
                }
                return null;
            } catch (IOException ex) {
                throw new IllegalStateException("Não foi possível ler host/porta de config.properties.", ex);
            }
        }
    }
}
