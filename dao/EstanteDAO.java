package dao;

import model.Estante;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso ao cadastro de estantes e à sua associação com gêneros.
 */
public class EstanteDAO {
    
    /** Cadastra uma estante com seu nome e gênero associado.
     * @param estante dados da estante a inserir
     * @return {@code true} quando o cadastro é gravado
     */
    public static boolean inserir(Estante estante) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO estante (name, genre_id) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, estante.getName());
            stmt.setInt(2, estante.getGenreId());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir estante: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }
    
    /** Remove uma estante pelo identificador.
     * @param id identificador da estante
     * @return {@code true} quando uma linha é removida
     */
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "DELETE FROM estante WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar estante: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }
    
    /** Atualiza os dados da estante existente.
     * @param estante identificador e novos dados da estante
     * @return {@code true} quando uma linha é atualizada
     */
    public static boolean atualizar(Estante estante) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("UPDATE estante SET name = ?, genre_id = ? WHERE id = ?");
            stmt.setString(1, estante.getName());
            stmt.setInt(2, estante.getGenreId());
            stmt.setInt(3, estante.getId());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estante: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    /** Recupera todas as estantes cadastradas.
     * @return lista de estantes, vazia quando não há registros
     */
    public static List<Estante> obterTodos() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Estante> estantes = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM estante ORDER BY name ASC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                estantes.add(new Estante(rs.getInt("id"), rs.getString("name"), rs.getInt("genre_id")));
            }
            
            return estantes;
        } catch (SQLException e) {
            System.err.println("Erro ao obter estantes: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /** Localiza uma estante pelo identificador.
     * @param id identificador procurado
     * @return estante encontrada, ou {@code null} se não existir
     */
    public static Estante obterPorId(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM estante WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Estante(rs.getInt("id"), rs.getString("name"), rs.getInt("genre_id"));
            }
            
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao obter estante: " + e.getMessage());
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
}
