package dao;

import model.Livro;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste e consulta livros, reunindo também os totais de exemplares
 * disponíveis e emprestados usados pelas telas do catálogo.
 */
public class LivroDAO {

    private static final String SELECT_LIVRO_COM_SITUACOES =
        "SELECT l.*, " +
        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id " +
        "AND ex.situacao = 'DISPONIVEL') AS exemplares_disponiveis, " +
        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id " +
        "AND ex.situacao = 'EMPRESTADO') AS exemplares_emprestados " +
        "FROM livro l ";
    
    /**
     * Insere um novo livro no banco de dados.
     */
    public static boolean inserir(Livro livro) {
        Connection conn = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            
            String sql = "INSERT INTO livro (title, authors, cover, " +
                        "publish_year, genre_id, " +
                        "publisher_id, isbn, idioma) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, livro.getTitle());
                stmt.setString(2, livro.getAuthors());
                stmt.setString(3, livro.getCover());
                stmt.setInt(4, livro.getPublishYear());
                stmt.setInt(5, livro.getGenreId());
                stmt.setInt(6, livro.getPublisherId());
                stmt.setString(7, livro.getIsbn());
                stmt.setString(8, livro.getIdioma());

                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    livro.setId(keys.getInt(1));
                }
            }

            try (PreparedStatement exemplarStmt = conn.prepareStatement(
                    "INSERT INTO exemplar (livro_id, codigo, localizacao, conservacao, situacao) " +
                    "VALUES (?, ?, ?, ?, 'DISPONIVEL')")) {
                exemplarStmt.setInt(1, livro.getId());
                exemplarStmt.setString(2, "EX-" + livro.getId() + "-001");
                exemplarStmt.setString(3, "");
                exemplarStmt.setString(4, "Bom");
                exemplarStmt.executeUpdate();
            }
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
            ConnectionFactory.closeConnection(conn);
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
                        "cover = ?, publish_year = ?, genre_id = ?, " +
                        "publisher_id = ?, isbn = ?, idioma = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, livro.getTitle());
            stmt.setString(2, livro.getAuthors());
            stmt.setString(3, livro.getCover());
            stmt.setInt(4, livro.getPublishYear());
            stmt.setInt(5, livro.getGenreId());
            stmt.setInt(6, livro.getPublisherId());
            stmt.setString(7, livro.getIsbn());
            stmt.setString(8, livro.getIdioma());
            stmt.setInt(9, livro.getId());
            
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
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
            
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
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
            
            String sql = SELECT_LIVRO_COM_SITUACOES + "WHERE l.id = ?";
            
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
            
            String sql = SELECT_LIVRO_COM_SITUACOES + "ORDER BY l.title ASC";
            
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
            
            String sql = SELECT_LIVRO_COM_SITUACOES +
                        "WHERE l.title LIKE ? OR l.authors LIKE ? OR l.isbn LIKE ? OR l.idioma LIKE ? " +
                        "ORDER BY l.title ASC";

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
            
            String sql = SELECT_LIVRO_COM_SITUACOES +
                         "WHERE l.genre_id = ? ORDER BY l.title ASC";
            
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
            
            String sql = SELECT_LIVRO_COM_SITUACOES +
                         "WHERE EXISTS (SELECT 1 FROM exemplar ex " +
                         "WHERE ex.livro_id = l.id AND ex.estante_id = ?) " +
                         "ORDER BY l.title ASC";
            
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
     * Busca livros associados a exemplares cuja localização contém o texto informado.
     */
    public static List<Livro> obterPorLocalizacao(String localizacao) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();

        try {
            conn = ConnectionFactory.getConnection();
            String sql = SELECT_LIVRO_COM_SITUACOES +
                "WHERE EXISTS (SELECT 1 FROM exemplar ex " +
                "WHERE ex.livro_id = l.id AND ex.localizacao LIKE ?) " +
                "ORDER BY l.title ASC";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + localizacao + "%");
            rs = stmt.executeQuery();
            while (rs.next()) {
                livros.add(mapearResultSet(rs));
            }
            return livros;
        } catch (SQLException e) {
            System.err.println("Erro ao buscar livros por localização: " + e.getMessage());
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
            
            String sql = SELECT_LIVRO_COM_SITUACOES +
                         "WHERE EXISTS (SELECT 1 FROM exemplar ex " +
                         "WHERE ex.livro_id = l.id AND ex.situacao = 'DISPONIVEL') " +
                         "ORDER BY l.title ASC";
            
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
     * Mapeia um ResultSet para um objeto Livro.
     */
    private static Livro mapearResultSet(ResultSet rs) throws SQLException {
        Livro livro = new Livro(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("authors"),
            rs.getString("cover"),
            rs.getInt("publish_year"),
            rs.getInt("genre_id"),
            rs.getInt("publisher_id"),
            rs.getString("isbn"),
            rs.getString("idioma")
        );
        livro.setExemplaresDisponiveis(rs.getInt("exemplares_disponiveis"));
        livro.setExemplaresEmprestados(rs.getInt("exemplares_emprestados"));
        return livro;
    }
}
