package dao;

import model.Exemplar;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para as operações de exemplares físicos.
 * Encaminha as chamadas ao Servidor para manter toda a persistência centralizada.
 */
public final class ExemplarDAO {
    private ExemplarDAO() {}

    public static List<Exemplar> obterPorLivro(int livroId) {
        return RemoteClient.callList("exemplar.obterPorLivro", Exemplar.class, livroId);
    }

    public static boolean inserir(Exemplar exemplar) {
        return RemoteClient.call("exemplar.inserir", Boolean.class, null, exemplar);
    }

    public static boolean atualizar(Exemplar exemplar) {
        return RemoteClient.call("exemplar.atualizar", Boolean.class, null, exemplar);
    }

    public static boolean deletar(int exemplarId, int livroId) {
        return RemoteClient.call("exemplar.deletar", Boolean.class, null, exemplarId, livroId);
    }
}
