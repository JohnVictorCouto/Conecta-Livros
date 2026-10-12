package dao;

import model.Exemplar;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso aos exemplares físicos de uma obra, incluindo sua localização
 * e situação de circulação.
 */
public class ExemplarDAO {

    /** Lista as cópias físicas associadas ao livro informado.
     * @param livroId identificador da obra
     * @return exemplares ordenados pelo código
     */
    public static List<Exemplar> obterPorLivro(int livroId) {
        List<Exemplar> exemplares = new ArrayList<>();
        String sql = "SELECT id, livro_id, estante_id, codigo, localizacao, conservacao, situacao " +
                     "FROM exemplar WHERE livro_id = ? ORDER BY codigo";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, livroId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    exemplares.add(mapear(rs));
                }
            }
            conn.commit();
            return exemplares;
        } catch (SQLException e) {
            System.err.println("Erro ao obter exemplares do livro " + livroId + ": " + e.getMessage());
            return exemplares;
        }
    }

    /** Cadastra um exemplar físico no acervo.
     * @param exemplar dados do exemplar a inserir
     * @return {@code true} quando o cadastro é gravado
     */
    public static boolean inserir(Exemplar exemplar) {
        if (!valido(exemplar) || exemplar.getSituacao() == Exemplar.Situacao.EMPRESTADO) {
            System.err.println("Não foi possível inserir exemplar: dados obrigatórios ausentes.");
            return false;
        }
        String sql = "INSERT INTO exemplar (livro_id, estante_id, codigo, localizacao, conservacao, situacao) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, exemplar.getLivroId());
            setEstante(stmt, 2, exemplar.getEstanteId());
            stmt.setString(3, exemplar.getCodigo().trim());
            stmt.setString(4, exemplar.getLocalizacao());
            stmt.setString(5, exemplar.getConservacao());
            stmt.setString(6, exemplar.getSituacao().name());

            if (stmt.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    exemplar.setId(keys.getInt(1));
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir exemplar: " + e.getMessage());
            return false;
        }
    }

    /** Atualiza os dados de identificação e situação de um exemplar.
     * @param exemplar identificador e dados atualizados
     * @return {@code true} quando uma linha é atualizada
     */
    public static boolean atualizar(Exemplar exemplar) {
        if (!valido(exemplar) || exemplar.getId() <= 0) {
            System.err.println("Não foi possível atualizar exemplar: dados obrigatórios ausentes.");
            return false;
        }
        String sql = "UPDATE exemplar SET estante_id = ?, codigo = ?, localizacao = ?, conservacao = ?, situacao = ? " +
                     "WHERE id = ? AND livro_id = ? " +
                     "AND ((situacao = 'EMPRESTADO' AND ? = 'EMPRESTADO') " +
                     "OR (situacao <> 'EMPRESTADO' AND ? <> 'EMPRESTADO'))";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setEstante(stmt, 1, exemplar.getEstanteId());
            stmt.setString(2, exemplar.getCodigo().trim());
            stmt.setString(3, exemplar.getLocalizacao());
            stmt.setString(4, exemplar.getConservacao());
            stmt.setString(5, exemplar.getSituacao().name());
            stmt.setInt(6, exemplar.getId());
            stmt.setInt(7, exemplar.getLivroId());
            stmt.setString(8, exemplar.getSituacao().name());
            stmt.setString(9, exemplar.getSituacao().name());
            if (stmt.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar exemplar " + exemplar.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /** Remove o exemplar identificado, respeitando também o livro informado.
     * @param exemplarId identificador do exemplar
     * @param livroId identificador do livro ao qual pertence
     * @return {@code true} quando uma linha é removida
     */
    public static boolean deletar(int exemplarId, int livroId) {
        String sql = "DELETE FROM exemplar WHERE id = ? AND livro_id = ? AND situacao <> 'EMPRESTADO'";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, exemplarId);
            stmt.setInt(2, livroId);
            if (stmt.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir exemplar " + exemplarId + ": " + e.getMessage());
            return false;
        }
    }

    private static Exemplar mapear(ResultSet rs) throws SQLException {
        return new Exemplar(
            rs.getInt("id"),
            rs.getInt("livro_id"),
            rs.getInt("estante_id"),
            rs.getString("codigo"),
            rs.getString("localizacao"),
            rs.getString("conservacao"),
            Exemplar.Situacao.valueOf(rs.getString("situacao"))
        );
    }

    private static boolean valido(Exemplar exemplar) {
        return exemplar != null && exemplar.getLivroId() > 0
            && exemplar.getCodigo() != null && !exemplar.getCodigo().isBlank()
            && exemplar.getSituacao() != null;
    }

    private static void setEstante(PreparedStatement stmt, int index, int estanteId)
            throws SQLException {
        if (estanteId > 0) {
            stmt.setInt(index, estanteId);
        } else {
            stmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
}
