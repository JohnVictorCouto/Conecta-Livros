package dao;

import model.Editora;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para as operações de editoras.
 * Encaminha as chamadas ao Servidor, que é o único processo que acessa o banco.
 */
public final class EditoraDAO {
    private EditoraDAO() {}

    public static boolean inserir(Editora editora) {
        return RemoteClient.call("editora.inserir", Boolean.class, null, editora);
    }

    public static boolean atualizar(Editora editora) {
        return RemoteClient.call("editora.atualizar", Boolean.class, null, editora);
    }

    public static boolean deletar(int id) {
        return RemoteClient.call("editora.deletar", Boolean.class, null, id);
    }

    public static List<Editora> obterTodos() {
        return RemoteClient.callList("editora.obterTodos", Editora.class);
    }

    public static Editora obterPorId(int id) {
        return RemoteClient.call("editora.obterPorId", Editora.class, null, id);
    }
}
