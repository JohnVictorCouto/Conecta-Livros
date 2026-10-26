package dao;

import model.Genero;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso ao cadastro de gêneros usado para classificar livros e estantes.
 */
public class GeneroDAO {
    
    /** Insere um gênero no catálogo.
     * @param genero dados do gênero a persistir
     * @return {@code true} quando uma linha é gravada
     */
    public static boolean inserir(Genero genero) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO genero (name) VALUES (?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, genero.getName());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir gênero: " + e.getMessage());
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
    
    /** Atualiza o gênero já cadastrado.
     * @param genero identificador e novo nome do gênero
     * @return {@code true} quando uma linha é atualizada
     */
    public static boolean atualizar(Genero genero) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("UPDATE genero SET name = ? WHERE id = ?");
            stmt.setString(1, genero.getName());
            stmt.setInt(2, genero.getId());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar gênero: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    /** Remove um gênero pelo identificador.
     * @param id identificador do gênero
     * @return {@code true} quando uma linha é removida
     */
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("DELETE FROM genero WHERE id = ?");
            stmt.setInt(1, id);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar gênero: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    /** Recupera os gêneros cadastrados.
     * @return lista ordenada de gêneros, vazia quando não há resultados
     */
    public static List<Genero> obterTodos() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Genero> generos = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM genero ORDER BY name ASC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                generos.add(new Genero(rs.getInt("id"), rs.getString("name")));
            }
            
            return generos;
        } catch (SQLException e) {
            System.err.println("Erro ao obter gêneros: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /** Localiza um gênero pelo identificador.
     * @param id identificador procurado
     * @return gênero correspondente, ou {@code null} se não existir
     */
    public static Genero obterPorId(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM genero WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Genero(rs.getInt("id"), rs.getString("name"));
            }
            
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao obter gênero: " + e.getMessage());
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
}
