package view;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmprestimosEmailTest {

    @Test
    void mailtoUriEncodesSubjectAndBodyWithoutLosingFormatting() {
        URI uri = EmprestimosView.criarUriEmail(
            "leitor+teste@example.com", "Devolução: livro", "Olá!\n\nAté logo.");

        assertEquals("mailto:leitor%2Bteste@example.com"
            + "?subject=Devolu%C3%A7%C3%A3o%3A%20livro"
            + "&body=Ol%C3%A1%21%0A%0AAt%C3%A9%20logo.", uri.toString());
    }

    @Test
    void mailtoUriRejectsContactsThatAreNotEmailAddresses() {
        assertThrows(IllegalArgumentException.class,
            () -> EmprestimosView.criarUriEmail("5511999999999", "Assunto", "Mensagem"));
    }
}
