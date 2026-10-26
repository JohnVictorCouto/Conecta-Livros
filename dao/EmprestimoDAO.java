package dao;

import model.Emprestimo;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste empréstimos e devoluções e monta os dados de circulação
 * combinando empréstimos, exemplares e livros.
 */
public class EmprestimoDAO {

    private static final String SELECT_EMPRESTIMOS =
        "SELECT ep.id, ep.exemplar_id, l.title, ex.codigo, ep.tomador_nome, " +
        "ep.tomador_contato, ep.data_emprestimo, ep.data_vencimento, ep.data_devolucao " +
        "FROM emprestimo ep " +
        "JOIN exemplar ex ON ex.id = ep.exemplar_id " +
        "JOIN livro l ON l.id = ex.livro_id ";

    private EmprestimoDAO() {}

    /** Recupera os empréstimos com os dados do livro e do exemplar associado.
     * @return empréstimos cadastrados, com devoluções incluídas
     */
    public static List<Emprestimo> obterTodos() {
        String sql = SELECT_EMPRESTIMOS +
            "ORDER BY ep.data_devolucao IS NULL DESC, ep.data_vencimento ASC, ep.id DESC";
        return consultar(sql, null);
    }

    /** Obtém os empréstimos ainda ativos para uma obra.
     * @param livroId identificador do livro
     * @return empréstimos não devolvidos vinculados à obra
     */
    public static List<Emprestimo> obterAtivosPorLivro(int livroId) {
        String sql = SELECT_EMPRESTIMOS +
            "WHERE ex.livro_id = ? AND ep.data_devolucao IS NULL " +
            "ORDER BY ep.data_vencimento ASC, ep.id ASC";
        return consultar(sql, livroId);
    }

    /** Registra o empréstimo do exemplar e atualiza sua situação na mesma transação.
     * @param exemplarId exemplar emprestado
     * @param nomeTomador nome de quem recebe o exemplar
     * @param contatoTomador contato informado para o tomador
     * @param dataVencimento data prevista para devolução
     * @return {@code true} quando o empréstimo e a atualização do exemplar são confirmados
     */
    public static boolean registrar(int exemplarId, String nomeTomador, String contatoTomador,
                                    LocalDate dataVencimento) {
        if (exemplarId <= 0 || nomeTomador == null || nomeTomador.isBlank()
                || nomeTomador.trim().length() > 150 || contatoTomador == null
                || contatoTomador.isBlank() || contatoTomador.trim().length() > 255
                || dataVencimento == null || dataVencimento.isBefore(LocalDate.now())) {
            return false;
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            try (PreparedStatement lock = conn.prepareStatement(
                    "SELECT situacao FROM exemplar WHERE id = ? FOR UPDATE")) {
                lock.setInt(1, exemplarId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next() || !"DISPONIVEL".equals(rs.getString("situacao"))) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO emprestimo " +
                    "(exemplar_id, tomador_nome, tomador_contato, data_emprestimo, data_vencimento) " +
                    "VALUES (?, ?, ?, CURDATE(), ?)")) {
                insert.setInt(1, exemplarId);
                insert.setString(2, nomeTomador.trim());
                insert.setString(3, contatoTomador.trim());
                insert.setDate(4, Date.valueOf(dataVencimento));
                if (insert.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE exemplar SET situacao = 'EMPRESTADO' " +
                    "WHERE id = ? AND situacao = 'DISPONIVEL'")) {
                update.setInt(1, exemplarId);
                if (update.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar empréstimo: " + e.getMessage());
            return false;
        }
    }

    /** Registra a devolução e torna o exemplar disponível novamente.
     * @param emprestimoId empréstimo que está sendo encerrado
     * @return {@code true} quando a devolução é persistida
     */
    public static boolean registrarDevolucao(int emprestimoId) {
        if (emprestimoId <= 0) return false;

        try (Connection conn = ConnectionFactory.getConnection()) {
            int exemplarId;
            try (PreparedStatement lock = conn.prepareStatement(
                    "SELECT exemplar_id FROM emprestimo " +
                    "WHERE id = ? AND data_devolucao IS NULL FOR UPDATE")) {
                lock.setInt(1, emprestimoId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    exemplarId = rs.getInt("exemplar_id");
                }
            }

            try (PreparedStatement updateEmprestimo = conn.prepareStatement(
                    "UPDATE emprestimo SET data_devolucao = CURDATE() " +
                    "WHERE id = ? AND data_devolucao IS NULL");
                 PreparedStatement updateExemplar = conn.prepareStatement(
                    "UPDATE exemplar SET situacao = 'DISPONIVEL' " +
                    "WHERE id = ? AND situacao = 'EMPRESTADO'")) {
                updateEmprestimo.setInt(1, emprestimoId);
                updateExemplar.setInt(1, exemplarId);
                if (updateEmprestimo.executeUpdate() != 1 || updateExemplar.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar devolução: " + e.getMessage());
            return false;
        }
    }

    private static List<Emprestimo> consultar(String sql, Integer livroId) {
        List<Emprestimo> emprestimos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (livroId != null) stmt.setInt(1, livroId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date devolucao = rs.getDate("data_devolucao");
                    emprestimos.add(new Emprestimo(
                        rs.getInt("id"),
                        rs.getInt("exemplar_id"),
                        rs.getString("title"),
                        rs.getString("codigo"),
                        rs.getString("tomador_nome"),
                        rs.getString("tomador_contato"),
                        rs.getDate("data_emprestimo").toLocalDate(),
                        rs.getDate("data_vencimento").toLocalDate(),
                        devolucao == null ? null : devolucao.toLocalDate()
                    ));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao consultar empréstimos.", e);
        }
        return emprestimos;
    }
}
