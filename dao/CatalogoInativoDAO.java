package dao;

import model.RegistroInativo;
import util.RemoteClient;

import java.util.List;

/**
 * Fachada do Cliente para consultar registros inativados do catálogo.
 * A consulta é somente de leitura: a inativação preserva o histórico e
 * as alterações no banco são executadas pelo Servidor.
 */
public final class CatalogoInativoDAO {
    private CatalogoInativoDAO() {}

    public static List<RegistroInativo> obterTodos() {
        return RemoteClient.callList("catalogoInativo.obterTodos", RegistroInativo.class);
    }
}
