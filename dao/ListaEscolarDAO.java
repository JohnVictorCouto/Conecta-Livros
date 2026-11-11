package dao;

import model.ListaEscolar;
import model.Livro;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para listas escolares e seus vínculos com livros.
 * Encaminha as chamadas ao Servidor para centralizar as operações no banco.
 */
public final class ListaEscolarDAO {
    private ListaEscolarDAO() {}

    public static boolean inserir(ListaEscolar lista) {
        return RemoteClient.call("listaEscolar.inserir", Boolean.class, null, lista);
    }

    public static boolean atualizar(ListaEscolar lista) {
        return RemoteClient.call("listaEscolar.atualizar", Boolean.class, null, lista);
    }

    public static boolean deletar(int id) {
        return RemoteClient.call("listaEscolar.deletar", Boolean.class, null, id);
    }

    public static List<ListaEscolar> obterTodos() {
        return RemoteClient.callList("listaEscolar.obterTodos", ListaEscolar.class);
    }

    public static ListaEscolar obterPorId(int id) {
        return RemoteClient.call("listaEscolar.obterPorId", ListaEscolar.class, null, id);
    }

    public static List<Livro> obterLivrosDaLista(int listaId) {
        return RemoteClient.callList("listaEscolar.obterLivrosDaLista", Livro.class, listaId);
    }

    public static boolean adicionarLivro(int listaId, int livroId) {
        return RemoteClient.call("listaEscolar.adicionarLivro", Boolean.class, null, listaId, livroId);
    }

    public static boolean removerLivro(int listaId, int livroId) {
        return RemoteClient.call("listaEscolar.removerLivro", Boolean.class, null, listaId, livroId);
    }
}
