package view;

import dao.LivroDAO;
import model.Livro;
import util.DadosCache;
import util.Tradutor;

import javax.swing.*;
import java.awt.*;

/**
 * Centraliza ações comuns sobre livros e os callbacks de atualização
 * disparados após empréstimos e devoluções.
 */
final class LivroAcoes {
    private LivroAcoes() {}

    static void registrarEmprestimo(Component pai, int livroId, Runnable aoAtualizar) {
        EmprestimoDialog.abrir(SwingUtilities.getWindowAncestor(pai), livroId,
            () -> aposAlteracaoEmprestimo(pai, aoAtualizar));
    }

    static void registrarDevolucao(Component pai, int livroId, Runnable aoAtualizar) {
        EmprestimoDialog.devolver(SwingUtilities.getWindowAncestor(pai), livroId,
            () -> aposAlteracaoEmprestimo(pai, aoAtualizar));
    }

    private static void aposAlteracaoEmprestimo(Component pai, Runnable aoAtualizar) {
        aoAtualizar.run();
        Window janela = SwingUtilities.getWindowAncestor(pai);
        if (janela instanceof MainFrame) {
            ((MainFrame) janela).recarregarEmprestimos();
        }
    }

    static void excluir(Component pai, Livro livro, Runnable aoExcluir) {
        int resposta = JOptionPane.showConfirmDialog(
            pai,
            Tradutor.formatar("books.delete_prompt", livro.getTitle()),
            Tradutor.t("books.confirm_delete"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) return;

        if (!LivroDAO.deletar(livro.getId())) {
            JOptionPane.showMessageDialog(pai,
                Tradutor.t("books.delete_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        DadosCache.limpar();
        JOptionPane.showMessageDialog(pai,
            Tradutor.t("books.deleted"),
            Tradutor.t("message.success"), JOptionPane.INFORMATION_MESSAGE);
        aoExcluir.run();
    }
}
