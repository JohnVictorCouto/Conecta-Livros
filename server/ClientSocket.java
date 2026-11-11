package server;

import util.WireJson;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class ClientSocket implements AutoCloseable {
    private static final int MAX_MESSAGE_CHARS = 1_048_576;
    private static final int IDLE_TIMEOUT_MILLIS = 30_000;
    private final Socket socket;
    private final BufferedReader entrada;
    private final PrintWriter saida;

    public ClientSocket(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(IDLE_TIMEOUT_MILLIS);
        System.out.println("Conexão recebida de " + socket.getRemoteSocketAddress());
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }
    public void waitForRegistrationTimeout() throws IOException {
        socket.setSoTimeout(10_000);
    }
    public void restoreIdleTimeout() throws IOException {
        socket.setSoTimeout(IDLE_TIMEOUT_MILLIS);
    }
    public void close() throws IOException {
        entrada.close();
        saida.close();
        socket.close();
    }
    public Object readMessage() throws IOException {
        // O limite evita que uma mensagem sem quebra de linha consuma memória sem controle.
        StringBuilder message = new StringBuilder();
        int character;
        while ((character = entrada.read()) != -1) {
            if (character == '\n') break;
            if (character != '\r') message.append((char) character);
            if (message.length() > MAX_MESSAGE_CHARS) {
                throw new IOException("A mensagem ultrapassou o limite permitido.");
            }
        }
        if (character == -1 && message.length() == 0) return null;
        return WireJson.parse(message.toString());
    }

    public void sendMessage(Object message) throws IOException {
        saida.println(WireJson.stringify(message));
        if (saida.checkError()) throw new IOException("Falha ao enviar resposta ao cliente.");
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException ex) {
            System.err.println("Erro ao fechar conexão: " + ex.getMessage());
        }
    }
}
