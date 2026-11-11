package dao;

import model.Livro;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para consultar e alterar livros do catálogo.
 * Encaminha as chamadas ao Servidor, que executa as consultas e gravações.
 */
public final class LivroDAO {
    private LivroDAO() {}

    public static boolean inserir(Livro livro) {
        return RemoteClient.call("livro.inserir", Boolean.class, null, livro);
    }

    public static boolean atualizar(Livro livro) {
        return RemoteClient.call("livro.atualizar", Boolean.class, null, livro);
    }

    public static boolean deletar(int id) {
        return RemoteClient.call("livro.deletar", Boolean.class, null, id);
    }

    public static Livro obterPorId(int id) {
        return RemoteClient.call("livro.obterPorId", Livro.class, null, id);
    }

    public static List<Livro> obterTodos() {
        return RemoteClient.callList("livro.obterTodos", Livro.class);
    }

    public static List<Livro> buscar(String termo) {
        return RemoteClient.callList("livro.buscar", Livro.class, termo);
    }

    public static List<Livro> obterPorGenero(int genreId) {
        return RemoteClient.callList("livro.obterPorGenero", Livro.class, genreId);
    }

    public static List<Livro> obterPorEstante(int estanteId) {
        return RemoteClient.callList("livro.obterPorEstante", Livro.class, estanteId);
    }

    public static List<Livro> obterDisponiveis() {
        return RemoteClient.callList("livro.obterDisponiveis", Livro.class);
    }
}
