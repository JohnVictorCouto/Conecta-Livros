package server;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Envia códigos de verificação de cadastro usando a conta de e-mail configurada
 * no Servidor, sem expor as credenciais SMTP ao Cliente.
 */
final class EmailVerificationService {
    private EmailVerificationService() {}

    static void sendCode(String recipient, String code) throws Exception {
        String sender = setting("EMAIL_HOST", "email.host", null);
        String password = setting("EMAIL_PASSWORD", "email.password", null);
        if (sender == null || sender.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Configure EMAIL_HOST e EMAIL_PASSWORD no servidor.");
        }

        String smtpHost = setting("EMAIL_SMTP_HOST", "email.smtp.host", "smtp.gmail.com");
        String smtpPort = setting("EMAIL_SMTP_PORT", "email.smtp.port", "587");
        Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.starttls.required", "true");
        mailProperties.put("mail.smtp.host", smtpHost);
        mailProperties.put("mail.smtp.port", smtpPort);
        mailProperties.put("mail.smtp.connectiontimeout", "10000");
        mailProperties.put("mail.smtp.timeout", "10000");
        mailProperties.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(mailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, true));
        message.setSubject("Código de verificação - Conecta Livros", "UTF-8");
        message.setText("Seu código de verificação para criar uma conta no Conecta Livros é: "
            + code + "\n\nO código expira em 10 minutos.", "UTF-8");
        Transport.send(message);
    }

    private static String setting(String environmentName, String propertyName, String fallback) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) value = System.getenv(environmentName);
        if (value == null || value.isBlank()) value = readConfig(environmentName);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String readConfig(String key) {
        Path configFile = Paths.get("config.properties");
        if (!Files.isRegularFile(configFile)) return null;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configFile)) {
            properties.load(input);
            return properties.getProperty(key);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler config.properties do servidor.", ex);
        }
    }
}
