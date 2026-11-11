package dao;

import model.Estante;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para as operações de estantes.
 * Encaminha as chamadas ao Servidor, que é o único processo que acessa o banco.
 */
public final class EstanteDAO {
    private EstanteDAO() {}

    public static boolean inserir(Estante estante) {
        return RemoteClient.call("estante.inserir", Boolean.class, null, estante);
    }

    public static boolean deletar(int id) {
        return RemoteClient.call("estante.deletar", Boolean.class, null, id);
    }

    public static boolean atualizar(Estante estante) {
        return RemoteClient.call("estante.atualizar", Boolean.class, null, estante);
    }

    public static List<Estante> obterTodos() {
        return RemoteClient.callList("estante.obterTodos", Estante.class);
    }

    public static Estante obterPorId(int id) {
        return RemoteClient.call("estante.obterPorId", Estante.class, null, id);
    }
}
