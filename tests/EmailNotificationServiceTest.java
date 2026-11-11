package server;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailNotificationServiceTest {

    @Test
    void reminderMessageTargetsTheBorrowerAndPreservesUtf8Content() throws Exception {
        Session session = Session.getInstance(new Properties());

        MimeMessage message = EmailNotificationService.createMessage(
            session, "biblioteca@example.com", "leitor+teste@example.com",
            "Lembrete: devolução", "Olá, leitor! O livro está atrasado."
        );

        assertEquals("leitor+teste@example.com",
            ((jakarta.mail.internet.InternetAddress) message.getRecipients(Message.RecipientType.TO)[0])
                .getAddress());
        assertEquals("Lembrete: devolução", message.getSubject());
        assertEquals("Olá, leitor! O livro está atrasado.", message.getContent());
    }

    @Test
    void reminderRequiresExactlyOneValidRecipient() {
        Session session = Session.getInstance(new Properties());

        assertThrows(Exception.class, () -> EmailNotificationService.createMessage(
            session, "biblioteca@example.com", "leitor@example.com, outro@example.com",
            "Lembrete", "Mensagem"
        ));
        assertThrows(Exception.class, () -> EmailNotificationService.createMessage(
            session, "biblioteca@example.com", "não é e-mail", "Lembrete", "Mensagem"
        ));
    }
}