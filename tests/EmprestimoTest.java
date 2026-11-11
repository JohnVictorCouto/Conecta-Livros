package model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmprestimoTest {

    @Test
    void loanWithoutReturnDateIsActive() {
        Emprestimo emprestimo = emprestimo(LocalDateTime.now().plusDays(7), null);

        assertTrue(emprestimo.isAtivo());
    }

    @Test
    void activeLoanPastDueDateIsOverdue() {
        Emprestimo emprestimo = emprestimo(LocalDateTime.now().minusDays(1), null);

        assertTrue(emprestimo.isAtivo());
        assertTrue(emprestimo.isAtrasado());
    }

    @Test
    void futureDueDateIsNotOverdue() {
        Emprestimo emprestimo = emprestimo(LocalDateTime.now().plusDays(1), null);

        assertFalse(emprestimo.isAtrasado());
    }

    @Test
    void overdueStatusUsesTimeEvenWhenDueDateIsToday() {
        Emprestimo atrasadoHoje = emprestimo(LocalDateTime.now().minusMinutes(1), null);
        Emprestimo aindaNoPrazoHoje = emprestimo(LocalDateTime.now().plusMinutes(1), null);

        assertTrue(atrasadoHoje.isAtrasado());
        assertFalse(aindaNoPrazoHoje.isAtrasado());
    }

    @Test
    void returnedLoanIsNeitherActiveNorOverdue() {
        Emprestimo emprestimo = emprestimo(
            LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(2)
        );

        assertFalse(emprestimo.isAtivo());
        assertFalse(emprestimo.isAtrasado());
    }

    @Test
    void constructorPreservesLoanDetails() {
        LocalDateTime emprestadoEm = LocalDateTime.of(2026, 1, 10, 9, 15);
        LocalDateTime vencimento = LocalDateTime.of(2026, 1, 24, 17, 30);
        Emprestimo emprestimo = new Emprestimo(
            12, 4, "Livro", "EX-004", "Pessoa", "email@example.com",
            emprestadoEm, vencimento, null
        );

        assertAll(
            () -> assertEquals(12, emprestimo.getId()),
            () -> assertEquals(4, emprestimo.getExemplarId()),
            () -> assertEquals("Livro", emprestimo.getTituloLivro()),
            () -> assertEquals("EX-004", emprestimo.getCodigoExemplar()),
            () -> assertEquals("Pessoa", emprestimo.getNomeTomador()),
            () -> assertEquals("email@example.com", emprestimo.getContatoTomador()),
            () -> assertEquals(emprestadoEm, emprestimo.getDataEmprestimo()),
            () -> assertEquals(vencimento, emprestimo.getDataVencimento()),
            () -> assertNull(emprestimo.getDataDevolucao())
        );
    }

    private Emprestimo emprestimo(LocalDateTime vencimento, LocalDateTime devolucao) {
        return new Emprestimo(
            1, 2, "Livro", "EX-002", "Pessoa", "contato",
            LocalDateTime.now().minusDays(3), vencimento, devolucao
        );
    }
}
