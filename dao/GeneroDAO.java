package dao;

import model.Genero;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para as operações de gêneros.
 * Encaminha as chamadas ao Servidor, que é o único processo que acessa o banco.
 */
public final class GeneroDAO {
    private GeneroDAO() {}

    public static boolean inserir(Genero genero) {
        return RemoteClient.call("genero.inserir", Boolean.class, null, genero);
    }

    public static boolean atualizar(Genero genero) {
        return RemoteClient.call("genero.atualizar", Boolean.class, null, genero);
    }

    public static boolean deletar(int id) {
        return RemoteClient.call("genero.deletar", Boolean.class, null, id);
    }

    public static List<Genero> obterTodos() {
        return RemoteClient.callList("genero.obterTodos", Genero.class);
    }

    public static Genero obterPorId(int id) {
        return RemoteClient.call("genero.obterPorId", Genero.class, null, id);
    }
}
