package dao;

import model.Editora;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Data Access Object para Editoras
public class EditoraDAO {
    
    // Insere uma nova editora no banco de dados
    public static boolean inserir(Editora editora) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO editora (name) VALUES (?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, editora.getName());
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir editora: " + e.getMessage());
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
    
    // Atualiza os dados de uma editora existente
    public static boolean atualizar(Editora editora) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("UPDATE editora SET name = ? WHERE id = ?");
            stmt.setString(1, editora.getName());
            stmt.setInt(2, editora.getId());
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar editora: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    // Exclui uma editora pelo ID
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("DELETE FROM editora WHERE id = ?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar editora: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    // Lista todas as editoras em ordem alfabetica
    public static List<Editora> obterTodos() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Editora> editoras = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM editora ORDER BY name ASC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                editoras.add(new Editora(rs.getInt("id"), rs.getString("name")));
            }
            
            return editoras;
        } catch (SQLException e) {
            System.err.println("Erro ao obter editoras: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    // Busca uma editora pelo ID
    public static Editora obterPorId(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM editora WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Editora(rs.getInt("id"), rs.getString("name"));
            }
            
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao obter editora: " + e.getMessage());
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
}




