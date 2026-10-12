package dao;

import model.ListaEscolar;
import model.Livro;
import util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste listas escolares e os vínculos entre cada lista e seus livros.
 */
public class ListaEscolarDAO {
    
    /** Insere uma lista escolar sem modificar os vínculos de livros.
     * @param lista dados da lista a persistir
     * @return {@code true} quando uma linha é gravada
     */
    public static boolean inserir(ListaEscolar lista) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO lista_escolar (name, description) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, lista.getName());
            stmt.setString(2, lista.getDescription());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir lista escolar: " + e.getMessage());
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
    
    /** Atualiza nome e descrição da lista informada.
     * @param lista identificador e dados atualizados
     * @return {@code true} quando uma linha é atualizada
     */
    public static boolean atualizar(ListaEscolar lista) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "UPDATE lista_escolar SET name = ?, description = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, lista.getName());
            stmt.setString(2, lista.getDescription());
            stmt.setInt(3, lista.getId());
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar lista escolar: " + e.getMessage());
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
    
    /** Remove uma lista pelo identificador.
     * @param id identificador da lista
     * @return {@code true} quando uma linha é removida
     */
    public static boolean deletar(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "DELETE FROM lista_escolar WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar lista escolar: " + e.getMessage());
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
    
    /** Recupera as listas escolares cadastradas.
     * @return listas ordenadas pelo nome
     */
    public static List<ListaEscolar> obterTodos() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<ListaEscolar> listas = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM lista_escolar ORDER BY name ASC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                listas.add(new ListaEscolar(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
            }
            
            return listas;
        } catch (SQLException e) {
            System.err.println("Erro ao obter listas escolares: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /** Localiza uma lista escolar pelo identificador.
     * @param id identificador procurado
     * @return lista correspondente, ou {@code null} se não existir
     */
    public static ListaEscolar obterPorId(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM lista_escolar WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new ListaEscolar(rs.getInt("id"), rs.getString("name"), rs.getString("description"));
            }
            
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao obter lista escolar: " + e.getMessage());
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Obtém todos os livros de uma lista escolar.
     */
    public static List<Livro> obterLivrosDaLista(int listaId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Livro> livros = new ArrayList<>();
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT l.*, " +
                        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id " +
                        "AND ex.situacao = 'DISPONIVEL') AS exemplares_disponiveis, " +
                        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id " +
                        "AND ex.situacao = 'EMPRESTADO') AS exemplares_emprestados " +
                        "FROM livro l " +
                        "INNER JOIN lista_livro ll ON l.id = ll.livro_id " +
                        "WHERE ll.lista_id = ? ORDER BY l.title ASC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, listaId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                livros.add(mapearLivro(rs));
            }
            
            return livros;
        } catch (SQLException e) {
            System.err.println("Erro ao obter livros da lista: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }
    
    /**
     * Adiciona um livro a uma lista escolar.
     */
    public static boolean adicionarLivro(int listaId, int livroId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO lista_livro (lista_id, livro_id) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, listaId);
            stmt.setInt(2, livroId);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar livro à lista: " + e.getMessage());
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
     * Remove um livro de uma lista escolar.
     */
    public static boolean removerLivro(int listaId, int livroId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "DELETE FROM lista_livro WHERE lista_id = ? AND livro_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, listaId);
            stmt.setInt(2, livroId);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao remover livro da lista: " + e.getMessage());
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
    
    private static Livro mapearLivro(ResultSet rs) throws SQLException {
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
