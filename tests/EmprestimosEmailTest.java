package view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmprestimosEmailTest {

    @Test
    void reminderTextIncludesBorrowerBookAndDueDate() {
        assertEquals("Lembrete de devolução: Dom Casmurro",
            util.Tradutor.formatar("loans.email_subject", "Dom Casmurro"));
        assertEquals("Olá, Ana!\n\nO prazo de devolução do livro “Dom Casmurro” venceu em 11/11/2026 10:30. "
                + "Por favor, devolva o exemplar ou entre em contato para combinarmos.\n\nAtenciosamente,",
            util.Tradutor.formatar("loans.email_body", "Ana", "Dom Casmurro", "11/11/2026 10:30"));
    }
}
