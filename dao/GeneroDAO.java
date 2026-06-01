package dao;

import model.Genero;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Data Access Object para Generos
public class GeneroDAO {
    
    // Insere um novo genero no banco de dados
    public static boolean inserir(Genero genero) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO genero (name) VALUES (?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, genero.getName());
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir gÃªnero: " + e.getMessage());
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
    
    // Atualiza os dados de um genero existente
    public static boolean atualizar(Genero genero) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("UPDATE genero SET name = ? WHERE id = ?");
            stmt.setString(1, genero.getName());
            stmt.setInt(2, genero.getId());
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar gÃªnero: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    // Exclui um genero pelo ID
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("DELETE FROM genero WHERE id = ?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar gÃªnero: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    // Lista todos os generos em ordem alfabetica
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
            System.err.println("Erro ao obter gÃªneros: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    // Busca um genero pelo ID
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
            System.err.println("Erro ao obter gÃªnero: " + e.getMessage());
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
}




