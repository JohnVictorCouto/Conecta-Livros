package server;

import util.WireJson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ponto de entrada do Servidor, responsável por escutar conexões TCP
 * e executar as operações que acessam o banco MySQL.
 */
public final class Server implements AutoCloseable {
    public static final String ADDRESS = "127.0.0.1";
    public static final int PORT = 3305;

    private final String bindAddress;
    private final int port;
    private final AdminAuthService authService;
    private final AtomicBoolean adminSessionActive = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    // Limita conexões simultâneas e pendentes para evitar crescimento ilimitado de threads e memória.
    private final ThreadPoolExecutor clients = new ThreadPoolExecutor(
        2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8), task -> {
            Thread thread = new Thread(task, "conecta-livros-client-handler");
            thread.setDaemon(false);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    private ServerSocket serverSocket;

    public Server() throws Exception {
        bindAddress = setting("SERVER_BIND_ADDRESS", ADDRESS);
        port = parsePort(setting("SERVER_PORT", String.valueOf(PORT)));
        authService = new AdminAuthService();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindAddress, port), 16);
        running.set(true);
        System.out.println("Servidor Conecta Livros escutando em " + bindAddress + ":" + port);
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                try {
                    clients.execute(() -> processarCliente(socket));
                } catch (java.util.concurrent.RejectedExecutionException ex) {
                    socket.close();
                    System.err.println("Conexão recusada: limite de conexões não autenticadas atingido.");
                }
            } catch (IOException ex) {
                if (running.get()) throw ex;
            }
        }
    }

    private void processarCliente(Socket socket) {
        ServerRequestHandler handler = new ServerRequestHandler(authService, adminSessionActive);
        try (ClientSocket client = new ClientSocket(socket)) {
            while (running.get()) {
                Object request;
                try {
                    request = client.readMessage();
                } catch (SocketTimeoutException ex) {
                    if (handler.hasPendingRegistration()) {
                        // Enquanto o código de verificação estiver ativo, aguarda uma tentativa adicional.
                        client.waitForRegistrationTimeout();
                        continue;
                    }
                    throw ex;
                }
                if (request == null) break;
                Map<String, Object> response = handler.handle(request);
                client.sendMessage(response);
                if (!handler.hasPendingRegistration()) client.restoreIdleTimeout();
                if (isLogout(request)) break;
            }
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Conexão encerrada após erro de protocolo: " + ex.getMessage());
        } finally {
            handler.endSession();
        }
    }

    private static boolean isLogout(Object request) {
        if (!(request instanceof Map<?, ?>)) return false;
        return "auth.logout".equals(((Map<?, ?>) request).get("operation"));
    }

    @Override
    public void close() {
        running.set(false);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ex) {
            System.err.println("Erro ao encerrar o socket do servidor: " + ex.getMessage());
        }
        clients.shutdown();
        try {
            if (!clients.awaitTermination(5, TimeUnit.SECONDS)) clients.shutdownNow();
        } catch (InterruptedException ex) {
            clients.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "conecta-livros-shutdown"));
            server.start();
        } catch (Exception ex) {
            System.err.println("Servidor não iniciado: " + ex.getMessage());
        }
    }

    private static int parsePort(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 65535) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("SERVER_PORT deve estar entre 1 e 65535.");
        }
    }

    static String setting(String name, String fallback) {
        String value = System.getProperty(name.toLowerCase().replace('_', '.'));
        if (value == null || value.isBlank()) value = System.getenv(name);
        if (value == null || value.isBlank()) value = fileSetting(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String fileSetting(String name) {
        Path path = Paths.get("config.properties");
        if (!Files.isRegularFile(path)) return null;
        Properties properties = new Properties();
        try (java.io.InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties.getProperty(name);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler config.properties do servidor.", ex);
        }
    }
}
