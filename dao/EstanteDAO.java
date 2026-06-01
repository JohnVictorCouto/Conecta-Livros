package dao;

import model.Estante;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Data Access Object para Estantes
public class EstanteDAO {
    
    // Insere uma nova estante no banco de dados
    public static boolean inserir(Estante estante) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO estante (name, genre_id) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, estante.getName());
            stmt.setInt(2, estante.getGenreId());
            stmt.executeUpdate();
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
    
    // Exclui uma estante pelo ID
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "DELETE FROM estante WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
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
    
    // Atualiza os dados de uma estante existente
    public static boolean atualizar(Estante estante) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            stmt = conn.prepareStatement("UPDATE estante SET name = ?, genre_id = ? WHERE id = ?");
            stmt.setString(1, estante.getName());
            stmt.setInt(2, estante.getGenreId());
            stmt.setInt(3, estante.getId());
            stmt.executeUpdate();
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

    // Lista todas as estantes em ordem alfabetica
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
    
    // Busca uma estante pelo ID
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




