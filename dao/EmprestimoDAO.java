package dao;

import model.Emprestimo;
import util.RemoteClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fachada do Cliente para consultar e registrar empréstimos e devoluções.
 * Encaminha as chamadas ao Servidor, responsável pela persistência e pelas regras.
 */
public final class EmprestimoDAO {
    private EmprestimoDAO() {}

    public static List<Emprestimo> obterTodos() {
        return RemoteClient.callList("emprestimo.obterTodos", Emprestimo.class);
    }

    public static List<Emprestimo> obterAtivosPorLivro(int livroId) {
        return RemoteClient.callList("emprestimo.obterAtivosPorLivro", Emprestimo.class, livroId);
    }

    public static boolean registrar(int exemplarId, String nomeTomador, String contatoTomador,
                                    LocalDateTime dataEmprestimo, LocalDateTime dataVencimento) {
        return RemoteClient.call("emprestimo.registrar", Boolean.class, null,
            exemplarId, nomeTomador, contatoTomador, dataEmprestimo, dataVencimento);
    }

    public static boolean registrarDevolucao(int emprestimoId) {
        return RemoteClient.call("emprestimo.registrarDevolucao", Boolean.class, null, emprestimoId);
    }

    public static boolean enviarLembrete(int emprestimoId, String assunto, String corpo) {
        return RemoteClient.call("emprestimo.enviarLembrete", Boolean.class, null,
            emprestimoId, assunto, corpo);
    }
}
