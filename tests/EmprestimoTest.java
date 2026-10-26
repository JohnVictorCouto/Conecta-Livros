package model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmprestimoTest {

    @Test
    void loanWithoutReturnDateIsActive() {
        Emprestimo emprestimo = emprestimo(LocalDate.now().plusDays(7), null);

        assertTrue(emprestimo.isAtivo());
    }

    @Test
    void activeLoanPastDueDateIsOverdue() {
        Emprestimo emprestimo = emprestimo(LocalDate.now().minusDays(1), null);

        assertTrue(emprestimo.isAtivo());
        assertTrue(emprestimo.isAtrasado());
    }

    @Test
    void futureDueDateIsNotOverdue() {
        Emprestimo emprestimo = emprestimo(LocalDate.now().plusDays(1), null);

        assertFalse(emprestimo.isAtrasado());
    }

    @Test
    void returnedLoanIsNeitherActiveNorOverdue() {
        Emprestimo emprestimo = emprestimo(
            LocalDate.now().minusDays(5), LocalDate.now().minusDays(2)
        );

        assertFalse(emprestimo.isAtivo());
        assertFalse(emprestimo.isAtrasado());
    }

    @Test
    void constructorPreservesLoanDetails() {
        LocalDate emprestadoEm = LocalDate.of(2026, 1, 10);
        LocalDate vencimento = LocalDate.of(2026, 1, 24);
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

    private Emprestimo emprestimo(LocalDate vencimento, LocalDate devolucao) {
        return new Emprestimo(
            1, 2, "Livro", "EX-002", "Pessoa", "contato",
            LocalDate.now().minusDays(3), vencimento, devolucao
        );
    }
}
