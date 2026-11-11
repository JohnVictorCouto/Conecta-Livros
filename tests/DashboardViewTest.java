package view;

import model.Emprestimo;
import model.Livro;
import org.junit.jupiter.api.Test;
import util.Tradutor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardViewTest {

    @Test
    void countsOnlyBorrowedBooksAsBorrowedAndSeparatesActiveAndOverdueLoans() {
        Livro semExemplares = new Livro();
        Livro disponivel = new Livro();
        disponivel.setExemplaresDisponiveis(1);
        Livro emprestado = new Livro();
        emprestado.setExemplaresEmprestados(2);

        Emprestimo ativo = emprestimo(LocalDateTime.now().plusDays(2), null);
        Emprestimo atrasado = emprestimo(LocalDateTime.now().minusDays(1), null);
        Emprestimo devolvido = emprestimo(
            LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2));

        assertArrayEquals(
            new int[] {3, 1, 1, 4, 2, 1},
            DashboardView.calcularEstatisticas(
                List.of(semExemplares, disponivel, emprestado),
                List.of(ativo, atrasado, devolvido),
                4
            )
        );
    }

    @Test
    void bookWithoutCopiesIsNotLabeledAsBorrowed() {
        assertEquals(
            Tradutor.t("state.unavailable"),
            new ComponenteUI.BadgeStatus(0, 0).getText()
        );
        assertEquals(
            Tradutor.t("state.borrowed"),
            new ComponenteUI.BadgeStatus(0, 1).getText()
        );
    }

    private Emprestimo emprestimo(LocalDateTime vencimento, LocalDateTime devolucao) {
        return new Emprestimo(
            1, 2, "Livro", "EX-002", "Pessoa", "contato",
            LocalDateTime.now().minusDays(3), vencimento, devolucao
        );
    }
}
