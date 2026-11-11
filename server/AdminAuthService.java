package server;

import util.ConnectionFactory;
import util.CryptoAES;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Autentica contas ADMIN e de usuários e coordena as regras de cadastro.
 * A validação e a persistência das credenciais são executadas exclusivamente no Servidor.
 */
final class AdminAuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final File keyFile;

    AdminAuthService() throws Exception {
        keyFile = keyFilePath().toFile();
        ensureEncryptionKey();
    }

    boolean authenticate(String username, String password) throws Exception {
        if (username == null || password == null) return false;
        byte[] encrypted = null;
        boolean active = false;
        boolean adminAccountFound = false;
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT password_ciphertext, active FROM admin_account WHERE id = 1 AND username = ?")) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    encrypted = result.getBytes("password_ciphertext");
                    active = result.getBoolean("active");
                    adminAccountFound = true;
                }
            }
            connection.commit();
        }
        if (adminAccountFound && active && encrypted != null && senhaIgual(encrypted, password)) return true;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT password_ciphertext, active FROM app_account WHERE username = ?")) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    connection.commit();
                    return false;
                }
                encrypted = result.getBytes("password_ciphertext");
                active = result.getBoolean("active");
            }
            connection.commit();
        }
        return active && encrypted != null && senhaIgual(encrypted, password);
    }

    private boolean senhaIgual(byte[] encrypted, String password) throws Exception {
        CryptoAES crypto = new CryptoAES();
        crypto.geraDecifra(encrypted, keyFile);
        byte[] actual = crypto.getTextoDecifrado();
        byte[] supplied = password.getBytes(StandardCharsets.UTF_8);
        try {
            // A comparação não revela o prefixo coincidente; os buffers são apagados logo depois.
            return MessageDigest.isEqual(actual, supplied);
        } finally {
            java.util.Arrays.fill(actual, (byte) 0);
            java.util.Arrays.fill(supplied, (byte) 0);
        }
    }

    void registerAccount(String username, String email, String password) throws Exception {
        validateRegistration(username, email, password);
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        byte[] plainPassword = password.getBytes(StandardCharsets.UTF_8);
        if (plainPassword.length > 479) {
            java.util.Arrays.fill(plainPassword, (byte) 0);
            throw new IllegalArgumentException("A senha excede o tamanho máximo permitido.");
        }
        CryptoAES crypto = new CryptoAES();
        try {
            crypto.geraCifra(plainPassword, keyFile);
        } finally {
            java.util.Arrays.fill(plainPassword, (byte) 0);
        }
        byte[] encrypted = crypto.getTextoCifrado();
        try (Connection connection = ConnectionFactory.getConnection()) {
            // Bloqueia a linha única da conta ADMIN durante a checagem para evitar conflito de identidade.
            try (PreparedStatement adminCheck = connection.prepareStatement(
                    "SELECT username FROM admin_account WHERE id = 1 FOR UPDATE");
                 ResultSet admin = adminCheck.executeQuery()) {
                if (admin.next() && normalizedUsername.equalsIgnoreCase(admin.getString("username"))) {
                    throw new IllegalArgumentException("Esse usuário já está cadastrado.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app_account (username, email, password_ciphertext, active) VALUES (?, ?, ?, TRUE)")) {
                statement.setString(1, normalizedUsername);
                statement.setString(2, normalizedEmail);
                statement.setBytes(3, encrypted);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Usuário ou e-mail já cadastrado.");
            }
            throw ex;
        } finally {
            java.util.Arrays.fill(encrypted, (byte) 0);
        }
    }

    void validateRegistration(String username, String email, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedUsername.isEmpty() || normalizedUsername.length() > 80) {
            throw new IllegalArgumentException("O usuário deve ter entre 1 e 80 caracteres.");
        }
        if (normalizedEmail.length() > 254 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido com até 254 caracteres.");
        }
        if (password == null || password.length() < 8 || password.length() > 256) {
            throw new IllegalArgumentException("A senha deve ter entre 8 e 256 caracteres.");
        }
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            if (passwordBytes.length > 479) {
                throw new IllegalArgumentException("A senha excede o tamanho máximo permitido.");
            }
        } finally {
            java.util.Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    void ensureAccountAvailable(String username, String email) throws SQLException {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();
        try (Connection connection = ConnectionFactory.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT username FROM admin_account WHERE id = 1");
                 ResultSet result = statement.executeQuery()) {
                if (result.next() && normalizedUsername.equalsIgnoreCase(result.getString("username"))) {
                    throw new IllegalArgumentException("Esse usuário já está cadastrado.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM app_account WHERE username = ? OR email = ? LIMIT 1")) {
                statement.setString(1, normalizedUsername);
                statement.setString(2, normalizedEmail);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        throw new IllegalArgumentException("Usuário ou e-mail já cadastrado.");
                    }
                }
            }
            connection.commit();
        }
    }

    boolean accountActive(String username) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT active FROM admin_account WHERE id = 1 AND username = ?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    boolean active = result.getBoolean("active");
                    connection.commit();
                    return active;
                }
            }
        }
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT active FROM app_account WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                boolean active = result.next() && result.getBoolean("active");
                connection.commit();
                return active;
            }
        }
    }

    private void ensureEncryptionKey() throws Exception {
        if (keyFile.isFile()) return;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT (SELECT COUNT(*) FROM admin_account) + (SELECT COUNT(*) FROM app_account)");
             ResultSet result = statement.executeQuery()) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(
                    "A chave AES não foi encontrada em "
                        + keyFile.getAbsolutePath()
                        + ". Restaure o arquivo da chave para manter o acesso às contas existentes.");
            }
            connection.commit();
        }

        Path parent = keyFile.toPath().getParent();
        if (parent != null) Files.createDirectories(parent);
        new CryptoAES().geraChave(keyFile);
        System.out.println("Chave AES criada. Cadastre a primeira conta pelo Cliente.");
    }

    private static Path keyFilePath() {
        String configured = System.getenv("ADMIN_AES_KEY_FILE");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("admin.aes.keyFile");
        }
        if (configured == null || configured.isBlank()) {
            configured = Paths.get(System.getProperty("user.home"), ".conecta-livros", "admin.key").toString();
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }
}
