package dao;

import model.Livro;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe LivroDAO - Data Access Object para Livros
 */
public class LivroDAO {
    
    /**
     * Insere um novo livro no banco de dados.
     */
    public static boolean inserir(Livro livro) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "INSERT INTO livro (title, authors, cover, " +
                        "publish_month, publish_year, disponibilidade, genre_id, " +
                        "publisher_id, estante_id, isbn, idioma) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, livro.getTitle());
            stmt.setString(2, livro.getAuthors());
            stmt.setString(3, livro.getCover());
            stmt.setInt(4, livro.getPublishMonth());
            stmt.setInt(5, livro.getPublishYear());
            stmt.setBoolean(6, livro.isDisponibilidade());
            stmt.setInt(7, livro.getGenreId());
            stmt.setInt(8, livro.getPublisherId());
            if (livro.getEstanteId() > 0) {
                stmt.setInt(9, livro.getEstanteId());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.setString(10, livro.getIsbn());
            stmt.setString(11, livro.getIdioma());
            
            stmt.executeUpdate();
            conn.commit();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erro ao inserir livro: " + e.getMessage());
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
    
    /**
     * Atualiza um livro existente no banco de dados.
     */
    public static boolean atualizar(Livro livro) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "UPDATE livro SET title = ?, authors = ?, " +
                        "cover = ?, publish_month = ?, publish_year = ?, " +
                        "disponibilidade = ?, genre_id = ?, publisher_id = ?, " +
                        "estante_id = ?, isbn = ?, idioma = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, livro.getTitle());
            stmt.setString(2, livro.getAuthors());
            stmt.setString(3, livro.getCover());
            stmt.setInt(4, livro.getPublishMonth());
            stmt.setInt(5, livro.getPublishYear());
            stmt.setBoolean(6, livro.isDisponibilidade());
            stmt.setInt(7, livro.getGenreId());
            stmt.setInt(8, livro.getPublisherId());
            if (livro.getEstanteId() > 0) {
                stmt.setInt(9, livro.getEstanteId());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.setString(10, livro.getIsbn());
            stmt.setString(11, livro.getIdioma());
            stmt.setInt(12, livro.getId());
            
            stmt.executeUpdate();
            conn.commit();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar livro: " + e.getMessage());
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
    
    /**
     * Deleta um livro do banco de dados.
     */
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "DELETE FROM livro WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            
            stmt.executeUpdate();
            conn.commit();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erro ao deletar livro: " + e.getMessage());
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
    
    /**
     * Recupera um livro pelo ID.
     */
    public static Livro obterPorId(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapearResultSet(rs);
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("Erro ao obter livro: " + e.getMessage());
            return null;
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Recupera todos os livros do banco de dados.
     */
    public static List<Livro> obterTodos() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro ORDER BY title ASC";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            
            return livros;
            
        } catch (SQLException e) {
            System.err.println("Erro ao obter todos os livros: " + e.getMessage());
            return new ArrayList<>();
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Busca livros por título ou autor.
     */
    public static List<Livro> buscar(String termo) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro WHERE title LIKE ? OR authors LIKE ? OR isbn LIKE ? OR idioma LIKE ? " +
                        "ORDER BY title ASC";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + termo + "%");
            stmt.setString(2, "%" + termo + "%");
            stmt.setString(3, "%" + termo + "%");
            stmt.setString(4, "%" + termo + "%");
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            
            return livros;
            
        } catch (SQLException e) {
            System.err.println("Erro ao buscar livros: " + e.getMessage());
            return new ArrayList<>();
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Busca livros por gênero.
     */
    public static List<Livro> obterPorGenero(int genreId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro WHERE genre_id = ? ORDER BY title ASC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, genreId);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            
            return livros;
            
        } catch (SQLException e) {
            System.err.println("Erro ao obter livros por gênero: " + e.getMessage());
            return new ArrayList<>();
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Busca livros por estante.
     */
    public static List<Livro> obterPorEstante(int estanteId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro WHERE estante_id = ? ORDER BY title ASC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, estanteId);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            
            return livros;
            
        } catch (SQLException e) {
            System.err.println("Erro ao obter livros por estante: " + e.getMessage());
            return new ArrayList<>();
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Obtém apenas livros disponíveis.
     */
    public static List<Livro> obterDisponiveis() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "SELECT * FROM livro WHERE disponibilidade = TRUE ORDER BY title ASC";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            
            return livros;
            
        } catch (SQLException e) {
            System.err.println("Erro ao obter livros disponíveis: " + e.getMessage());
            return new ArrayList<>();
            
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Atualiza o status de disponibilidade de um livro.
     */
    public static boolean atualizarDisponibilidade(int livroId, boolean disponibilidade) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "UPDATE livro SET disponibilidade = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, disponibilidade);
            stmt.setInt(2, livroId);
            
            stmt.executeUpdate();
            conn.commit();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar disponibilidade: " + e.getMessage());
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
    
    /**
     * Mapeia um ResultSet para um objeto Livro.
     */
    private static Livro mapearResultSet(ResultSet rs) throws SQLException {
        return new Livro(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("authors"),
            rs.getString("cover"),
            rs.getInt("publish_month"),
            rs.getInt("publish_year"),
            rs.getBoolean("disponibilidade"),
            rs.getInt("genre_id"),
            rs.getInt("publisher_id"),
            rs.getInt("estante_id"),
            rs.getString("isbn"),
            rs.getString("idioma")
        );
    }
}
