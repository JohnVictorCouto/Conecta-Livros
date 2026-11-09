package server;

import model.Editora;
import model.Emprestimo;
import model.Estante;
import model.Exemplar;
import model.Genero;
import model.ListaEscolar;
import model.Livro;
import model.RegistroInativo;
import util.ConnectionFactory;
import util.IsbnUtils;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada JDBC exclusiva do processo Servidor.
 * Executa consultas e alterações em transações para manter o acesso ao banco
 * fora do Cliente e permitir rollback quando uma operação falha.
 */
final class ServerDAO {
    private static final String SELECT_BOOKS =
        "SELECT l.id, l.title, l.authors, l.cover, l.publish_year, l.genre_id, l.publisher_id, " +
        "l.isbn, l.idioma, g.name AS genero_catalogo, ed.name AS editora_catalogo, " +
        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id AND ex.ativo = TRUE " +
        "AND ex.situacao = 'DISPONIVEL') AS exemplares_disponiveis, " +
        "(SELECT COUNT(*) FROM exemplar ex WHERE ex.livro_id = l.id AND ex.ativo = TRUE " +
        "AND ex.situacao = 'EMPRESTADO') AS exemplares_emprestados " +
        "FROM livro l JOIN genero g ON g.id = l.genre_id " +
        "JOIN editora ed ON ed.id = l.publisher_id " +
        "WHERE l.ativo = TRUE AND g.ativo = TRUE AND ed.ativo = TRUE";

    private ServerDAO() {}

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private static <T> T transacao(String contexto, SqlWork<T> trabalho) {
        Connection connection = null;
        try {
            connection = ConnectionFactory.getConnection();
            T resultado = trabalho.execute(connection);
            connection.commit();
            return resultado;
        } catch (SQLException ex) {
            // Preserva o erro original e anexa eventual falha do rollback como exceção suprimida.
            rollback(connection, ex);
            throw new IllegalStateException(contexto, ex);
        } catch (RuntimeException ex) {
            rollback(connection, ex);
            throw ex;
        } finally {
            ConnectionFactory.closeConnection(connection);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        if (connection == null) return;
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private static List<Livro> consultarLivros(String sql, Binder binder) {
        return transacao("Não foi possível consultar os livros.", connection -> {
            List<Livro> livros = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) livros.add(mapearLivro(result));
                }
            }
            return livros;
        });
    }

    private static Livro mapearLivro(ResultSet result) throws SQLException {
        Livro livro = new Livro(
            result.getInt("id"),
            result.getString("title"),
            result.getString("authors"),
            result.getString("cover"),
            result.getInt("publish_year"),
            result.getInt("genre_id"),
            result.getInt("publisher_id"),
            result.getString("isbn"),
            result.getString("idioma"));
        livro.setGeneroCatalogo(result.getString("genero_catalogo"));
        livro.setEditoraCatalogo(result.getString("editora_catalogo"));
        livro.setExemplaresDisponiveis(result.getInt("exemplares_disponiveis"));
        livro.setExemplaresEmprestados(result.getInt("exemplares_emprestados"));
        return livro;
    }

    private static String isbnNormalizado(String isbn) {
        if (isbn == null || isbn.isBlank()) return null;
        String normalized = IsbnUtils.clean(isbn);
        if (!IsbnUtils.isValid(normalized)) return null;
        return normalized;
    }

    private static boolean referenciasAtivas(Connection connection, int generoId, int editoraId)
        throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM genero WHERE id = ? AND ativo = TRUE) " +
            "AND EXISTS(SELECT 1 FROM editora WHERE id = ? AND ativo = TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, generoId);
            statement.setInt(2, editoraId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static boolean isbnDuplicado(Connection connection, String isbn, int livroId)
        throws SQLException {
        if (isbn == null) return false;
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, isbn FROM livro WHERE ativo = TRUE AND id <> ? AND isbn IS NOT NULL")) {
            statement.setInt(1, livroId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (isbn.equals(IsbnUtils.clean(result.getString("isbn")))) return true;
                }
            }
        }
        return false;
    }

    private static void preencherLivro(PreparedStatement statement, Livro livro, String isbn,
                                       int primeiroParametro) throws SQLException {
        statement.setString(primeiroParametro, livro.getTitle().trim());
        statement.setString(primeiroParametro + 1, livro.getAuthors());
        statement.setString(primeiroParametro + 2, livro.getCover());
        statement.setString(primeiroParametro + 3, isbn);
        statement.setString(primeiroParametro + 4, livro.getIdioma());
        if (livro.getPublishYear() > 0) {
            statement.setInt(primeiroParametro + 5, livro.getPublishYear());
        } else {
            statement.setNull(primeiroParametro + 5, Types.INTEGER);
        }
        statement.setInt(primeiroParametro + 6, livro.getGenreId());
        statement.setInt(primeiroParametro + 7, livro.getPublisherId());
    }

    public static final class LivroDAO {
        private LivroDAO() {}

        public static boolean inserir(Livro livro) {
            if (livro == null || livro.getTitle() == null || livro.getTitle().isBlank()) return false;
            String isbn = isbnNormalizado(livro.getIsbn());
            if (livro.getIsbn() != null && !livro.getIsbn().isBlank() && isbn == null) return false;
            return transacao("Não foi possível inserir o livro.", connection -> {
                if (!referenciasAtivas(connection, livro.getGenreId(), livro.getPublisherId())
                    || isbnDuplicado(connection, isbn, 0)) return false;
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO livro (title, authors, cover, isbn, idioma, publish_year, genre_id, publisher_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    preencherLivro(statement, livro, isbn, 1);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(Livro livro) {
            if (livro == null || livro.getId() <= 0 || livro.getTitle() == null
                || livro.getTitle().isBlank()) return false;
            String isbn = isbnNormalizado(livro.getIsbn());
            if (livro.getIsbn() != null && !livro.getIsbn().isBlank() && isbn == null) return false;
            return transacao("Não foi possível atualizar o livro.", connection -> {
                if (!referenciasAtivas(connection, livro.getGenreId(), livro.getPublisherId())
                    || isbnDuplicado(connection, isbn, livro.getId())) return false;
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE livro SET title = ?, authors = ?, cover = ?, isbn = ?, idioma = ?, " +
                    "publish_year = ?, genre_id = ?, publisher_id = ? WHERE id = ? AND ativo = TRUE")) {
                    preencherLivro(statement, livro, isbn, 1);
                    statement.setInt(9, livro.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int id) {
            if (id <= 0) return false;
            return transacao("Não foi possível inativar o livro.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM emprestimo em JOIN exemplar ex ON ex.id = em.exemplar_id " +
                    "WHERE ex.livro_id = ? AND em.data_devolucao IS NULL)")) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || result.getBoolean(1)) return false;
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE livro SET ativo = FALSE WHERE id = ? AND ativo = TRUE")) {
                    statement.setInt(1, id);
                    if (statement.executeUpdate() != 1) return false;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE exemplar SET ativo = FALSE, situacao = 'INDISPONIVEL' " +
                    "WHERE livro_id = ? AND ativo = TRUE")) {
                    statement.setInt(1, id);
                    statement.executeUpdate();
                }
                return true;
            });
        }

        public static Livro obterPorId(int id) {
            List<Livro> livros = consultarLivros(SELECT_BOOKS + " AND l.id = ?", s -> s.setInt(1, id));
            return livros.isEmpty() ? null : livros.get(0);
        }

        public static List<Livro> obterTodos() {
            return consultarLivros(SELECT_BOOKS + " ORDER BY l.title", s -> {});
        }

        public static List<Livro> buscar(String termo) {
            if (termo == null || termo.isBlank()) return obterTodos();
            String filtro = "%" + termo.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            String sql = SELECT_BOOKS + " AND (LOWER(l.title) LIKE ? OR LOWER(COALESCE(l.authors, '')) LIKE ? " +
                "OR LOWER(COALESCE(l.isbn, '')) LIKE ? OR LOWER(g.name) LIKE ? OR LOWER(ed.name) LIKE ?) " +
                "ORDER BY l.title";
            return consultarLivros(sql, statement -> {
                for (int i = 1; i <= 5; i++) statement.setString(i, filtro);
            });
        }

        public static List<Livro> obterPorGenero(int generoId) {
            return consultarLivros(SELECT_BOOKS + " AND l.genre_id = ? ORDER BY l.title",
                s -> s.setInt(1, generoId));
        }

        public static List<Livro> obterPorEstante(int estanteId) {
            String sql = SELECT_BOOKS + " AND EXISTS (SELECT 1 FROM exemplar ex WHERE ex.livro_id = l.id " +
                "AND ex.estante_id = ? AND ex.ativo = TRUE) ORDER BY l.title";
            return consultarLivros(sql, s -> s.setInt(1, estanteId));
        }

        public static List<Livro> obterDisponiveis() {
            String sql = SELECT_BOOKS + " AND EXISTS (SELECT 1 FROM exemplar ex WHERE ex.livro_id = l.id " +
                "AND ex.ativo = TRUE AND ex.situacao = 'DISPONIVEL') ORDER BY l.title";
            return consultarLivros(sql, s -> {});
        }
    }

    public static final class GeneroDAO {
        private GeneroDAO() {}

        public static boolean inserir(Genero genero) {
            if (genero == null || genero.getName() == null || genero.getName().isBlank()) return false;
            return transacao("Não foi possível inserir o gênero.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO genero (name) VALUES (?)")) {
                    statement.setString(1, genero.getName().trim());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(Genero genero) {
            if (genero == null || genero.getId() <= 0 || genero.getName() == null
                || genero.getName().isBlank()) return false;
            return transacao("Não foi possível atualizar o gênero.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE genero SET name = ? WHERE id = ? AND ativo = TRUE")) {
                    statement.setString(1, genero.getName().trim());
                    statement.setInt(2, genero.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int id) {
            if (id <= 0) return false;
            return transacao("Não foi possível inativar o gênero.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE genero SET ativo = FALSE WHERE id = ? AND ativo = TRUE " +
                    "AND NOT EXISTS (SELECT 1 FROM livro WHERE genre_id = ? AND ativo = TRUE) " +
                    "AND NOT EXISTS (SELECT 1 FROM estante WHERE genre_id = ? AND ativo = TRUE)")) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    statement.setInt(3, id);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static List<Genero> obterTodos() {
            return transacao("Não foi possível consultar os gêneros.", connection -> {
                List<Genero> generos = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM genero WHERE ativo = TRUE ORDER BY name");
                     ResultSet result = statement.executeQuery()) {
                    while (result.next()) generos.add(new Genero(result.getInt("id"), result.getString("name")));
                }
                return generos;
            });
        }

        public static Genero obterPorId(int id) {
            return transacao("Não foi possível consultar o gênero.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM genero WHERE id = ? AND ativo = TRUE")) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? new Genero(result.getInt("id"), result.getString("name")) : null;
                    }
                }
            });
        }
    }

    public static final class EditoraDAO {
        private EditoraDAO() {}

        public static boolean inserir(Editora editora) {
            if (editora == null || editora.getName() == null || editora.getName().isBlank()) return false;
            return transacao("Não foi possível inserir a editora.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO editora (name) VALUES (?)")) {
                    statement.setString(1, editora.getName().trim());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(Editora editora) {
            if (editora == null || editora.getId() <= 0 || editora.getName() == null
                || editora.getName().isBlank()) return false;
            return transacao("Não foi possível atualizar a editora.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE editora SET name = ? WHERE id = ? AND ativo = TRUE")) {
                    statement.setString(1, editora.getName().trim());
                    statement.setInt(2, editora.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int id) {
            if (id <= 0) return false;
            return transacao("Não foi possível inativar a editora.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE editora SET ativo = FALSE WHERE id = ? AND ativo = TRUE " +
                    "AND NOT EXISTS (SELECT 1 FROM livro WHERE publisher_id = ? AND ativo = TRUE)")) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static List<Editora> obterTodos() {
            return transacao("Não foi possível consultar as editoras.", connection -> {
                List<Editora> editoras = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM editora WHERE ativo = TRUE ORDER BY name");
                     ResultSet result = statement.executeQuery()) {
                    while (result.next()) editoras.add(new Editora(result.getInt("id"), result.getString("name")));
                }
                return editoras;
            });
        }

        public static Editora obterPorId(int id) {
            return transacao("Não foi possível consultar a editora.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM editora WHERE id = ? AND ativo = TRUE")) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? new Editora(result.getInt("id"), result.getString("name")) : null;
                    }
                }
            });
        }
    }

    public static final class EstanteDAO {
        private EstanteDAO() {}

        private static boolean generoAtivo(Connection connection, int generoId) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM genero WHERE id = ? AND ativo = TRUE)")) {
                statement.setInt(1, generoId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getBoolean(1);
                }
            }
        }

        public static boolean inserir(Estante estante) {
            if (estante == null || estante.getName() == null || estante.getName().isBlank()) return false;
            return transacao("Não foi possível inserir a estante.", connection -> {
                if (!generoAtivo(connection, estante.getGenreId())) return false;
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO estante (name, genre_id) VALUES (?, ?)")) {
                    statement.setString(1, estante.getName().trim());
                    statement.setInt(2, estante.getGenreId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(Estante estante) {
            if (estante == null || estante.getId() <= 0 || estante.getName() == null
                || estante.getName().isBlank()) return false;
            return transacao("Não foi possível atualizar a estante.", connection -> {
                if (!generoAtivo(connection, estante.getGenreId())) return false;
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE estante SET name = ?, genre_id = ? WHERE id = ? AND ativo = TRUE")) {
                    statement.setString(1, estante.getName().trim());
                    statement.setInt(2, estante.getGenreId());
                    statement.setInt(3, estante.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int id) {
            if (id <= 0) return false;
            return transacao("Não foi possível inativar a estante.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE estante SET ativo = FALSE WHERE id = ? AND ativo = TRUE " +
                    "AND NOT EXISTS (SELECT 1 FROM exemplar WHERE estante_id = ? AND ativo = TRUE)")) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static List<Estante> obterTodos() {
            return transacao("Não foi possível consultar as estantes.", connection -> {
                List<Estante> estantes = new ArrayList<>();
                String sql = "SELECT e.id, e.name, e.genre_id FROM estante e JOIN genero g " +
                    "ON g.id = e.genre_id WHERE e.ativo = TRUE AND g.ativo = TRUE ORDER BY e.name";
                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        estantes.add(new Estante(result.getInt("id"), result.getString("name"),
                            result.getInt("genre_id")));
                    }
                }
                return estantes;
            });
        }

        public static Estante obterPorId(int id) {
            return transacao("Não foi possível consultar a estante.", connection -> {
                String sql = "SELECT e.id, e.name, e.genre_id FROM estante e JOIN genero g " +
                    "ON g.id = e.genre_id WHERE e.id = ? AND e.ativo = TRUE AND g.ativo = TRUE";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? new Estante(result.getInt("id"), result.getString("name"),
                            result.getInt("genre_id")) : null;
                    }
                }
            });
        }
    }

    public static final class ExemplarDAO {
        private ExemplarDAO() {}

        private static boolean valido(Exemplar exemplar) {
            return exemplar != null && exemplar.getLivroId() > 0 && exemplar.getCodigo() != null
                && !exemplar.getCodigo().isBlank() && exemplar.getSituacao() != null;
        }

        private static void setEstante(PreparedStatement statement, int parameter, int estanteId)
            throws SQLException {
            if (estanteId > 0) statement.setInt(parameter, estanteId);
            else statement.setNull(parameter, Types.INTEGER);
        }

        private static boolean referenciasAtivas(Connection connection, int livroId, int estanteId)
            throws SQLException {
            String sql = "SELECT EXISTS(SELECT 1 FROM livro WHERE id = ? AND ativo = TRUE)";
            if (estanteId > 0) {
                sql = "SELECT EXISTS(SELECT 1 FROM livro WHERE id = ? AND ativo = TRUE) " +
                    "AND EXISTS(SELECT 1 FROM estante WHERE id = ? AND ativo = TRUE)";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, livroId);
                if (estanteId > 0) statement.setInt(2, estanteId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getBoolean(1);
                }
            }
        }

        public static List<Exemplar> obterPorLivro(int livroId) {
            return transacao("Não foi possível consultar os exemplares.", connection -> {
                List<Exemplar> exemplares = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, livro_id, estante_id, codigo, conservacao, situacao " +
                    "FROM exemplar WHERE livro_id = ? AND ativo = TRUE ORDER BY codigo")) {
                    statement.setInt(1, livroId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) exemplares.add(mapear(result));
                    }
                }
                return exemplares;
            });
        }

        public static boolean inserir(Exemplar exemplar) {
            if (!valido(exemplar)) return false;
            return transacao("Não foi possível inserir o exemplar.", connection -> {
                if (!referenciasAtivas(connection, exemplar.getLivroId(), exemplar.getEstanteId())) return false;
                String sql = "INSERT INTO exemplar (livro_id, estante_id, codigo, conservacao, situacao) " +
                    "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, exemplar.getLivroId());
                    setEstante(statement, 2, exemplar.getEstanteId());
                    statement.setString(3, exemplar.getCodigo().trim());
                    statement.setString(4, exemplar.getConservacao());
                    statement.setString(5, exemplar.getSituacao().name());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(Exemplar exemplar) {
            if (!valido(exemplar) || exemplar.getId() <= 0) return false;
            return transacao("Não foi possível atualizar o exemplar.", connection -> {
                if (!referenciasAtivas(connection, exemplar.getLivroId(), exemplar.getEstanteId())) return false;
                String sql = "UPDATE exemplar SET estante_id = ?, codigo = ?, conservacao = ?, " +
                    "situacao = ? WHERE id = ? AND livro_id = ? AND ativo = TRUE " +
                    "AND NOT EXISTS (SELECT 1 FROM emprestimo WHERE exemplar_id = ? AND data_devolucao IS NULL)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    setEstante(statement, 1, exemplar.getEstanteId());
                    statement.setString(2, exemplar.getCodigo().trim());
                    statement.setString(3, exemplar.getConservacao());
                    statement.setString(4, exemplar.getSituacao().name());
                    statement.setInt(5, exemplar.getId());
                    statement.setInt(6, exemplar.getLivroId());
                    statement.setInt(7, exemplar.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int exemplarId, int livroId) {
            if (exemplarId <= 0 || livroId <= 0) return false;
            return transacao("Não foi possível inativar o exemplar.", connection -> {
                String sql = "UPDATE exemplar SET ativo = FALSE, situacao = 'INDISPONIVEL' " +
                    "WHERE id = ? AND livro_id = ? AND ativo = TRUE " +
                    "AND NOT EXISTS (SELECT 1 FROM emprestimo WHERE exemplar_id = ? AND data_devolucao IS NULL)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, exemplarId);
                    statement.setInt(2, livroId);
                    statement.setInt(3, exemplarId);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        private static Exemplar mapear(ResultSet result) throws SQLException {
            Exemplar exemplar = new Exemplar();
            exemplar.setId(result.getInt("id"));
            exemplar.setLivroId(result.getInt("livro_id"));
            exemplar.setEstanteId(result.getInt("estante_id"));
            exemplar.setCodigo(result.getString("codigo"));
            exemplar.setConservacao(result.getString("conservacao"));
            exemplar.setSituacao(Exemplar.Situacao.valueOf(result.getString("situacao")));
            return exemplar;
        }
    }

    public static final class EmprestimoDAO {
        private static final String SELECT_LOANS =
            "SELECT em.id, em.exemplar_id, l.title AS titulo_livro, ex.codigo AS codigo_exemplar, " +
            "em.tomador_nome, em.tomador_contato, em.data_emprestimo, em.data_vencimento, em.data_devolucao " +
            "FROM emprestimo em JOIN exemplar ex ON ex.id = em.exemplar_id " +
            "JOIN livro l ON l.id = ex.livro_id";

        private EmprestimoDAO() {}

        public static List<Emprestimo> obterTodos() {
            return consultar(null);
        }

        public static List<Emprestimo> obterAtivosPorLivro(int livroId) {
            return consultar(livroId);
        }

        private static List<Emprestimo> consultar(Integer livroId) {
            return transacao("Não foi possível consultar os empréstimos.", connection -> {
                String sql = SELECT_LOANS + (livroId == null ? "" :
                    " WHERE ex.livro_id = ? AND em.data_devolucao IS NULL") +
                    " ORDER BY em.data_emprestimo DESC, em.id DESC";
                List<Emprestimo> emprestimos = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    if (livroId != null) statement.setInt(1, livroId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            Date devolucao = result.getDate("data_devolucao");
                            emprestimos.add(new Emprestimo(
                                result.getInt("id"),
                                result.getInt("exemplar_id"),
                                result.getString("titulo_livro"),
                                result.getString("codigo_exemplar"),
                                result.getString("tomador_nome"),
                                result.getString("tomador_contato"),
                                result.getDate("data_emprestimo").toLocalDate(),
                                result.getDate("data_vencimento").toLocalDate(),
                                devolucao == null ? null : devolucao.toLocalDate()));
                        }
                    }
                }
                return emprestimos;
            });
        }

        public static boolean registrar(int exemplarId, String nomeTomador, String contatoTomador,
                                        LocalDate dataVencimento) {
            if (exemplarId <= 0 || nomeTomador == null || nomeTomador.isBlank()
                || contatoTomador == null || contatoTomador.isBlank() || dataVencimento == null
                || dataVencimento.isBefore(LocalDate.now())) return false;
            return transacao("Não foi possível registrar o empréstimo.", connection -> {
                try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT ex.situacao FROM exemplar ex JOIN livro l ON l.id = ex.livro_id " +
                    "WHERE ex.id = ? AND ex.ativo = TRUE AND l.ativo = TRUE FOR UPDATE")) {
                    lock.setInt(1, exemplarId);
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next() || !"DISPONIVEL".equals(result.getString("situacao"))) return false;
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE exemplar SET situacao = 'EMPRESTADO' WHERE id = ? AND ativo = TRUE " +
                    "AND situacao = 'DISPONIVEL'")) {
                    update.setInt(1, exemplarId);
                    if (update.executeUpdate() != 1) return false;
                }
                try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO emprestimo (exemplar_id, tomador_nome, tomador_contato, data_emprestimo, " +
                    "data_vencimento) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setInt(1, exemplarId);
                    insert.setString(2, nomeTomador.trim());
                    insert.setString(3, contatoTomador.trim());
                    insert.setDate(4, Date.valueOf(LocalDate.now()));
                    insert.setDate(5, Date.valueOf(dataVencimento));
                    return insert.executeUpdate() == 1;
                }
            });
        }

        public static boolean registrarDevolucao(int emprestimoId) {
            if (emprestimoId <= 0) return false;
            return transacao("Não foi possível registrar a devolução.", connection -> {
                int exemplarId;
                try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT exemplar_id FROM emprestimo WHERE id = ? AND data_devolucao IS NULL FOR UPDATE")) {
                    lock.setInt(1, emprestimoId);
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next()) return false;
                        exemplarId = result.getInt("exemplar_id");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE emprestimo SET data_devolucao = ? WHERE id = ? AND data_devolucao IS NULL")) {
                    update.setDate(1, Date.valueOf(LocalDate.now()));
                    update.setInt(2, emprestimoId);
                    if (update.executeUpdate() != 1) return false;
                }
                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE exemplar SET situacao = CASE WHEN ativo = TRUE AND EXISTS " +
                    "(SELECT 1 FROM livro WHERE id = exemplar.livro_id AND ativo = TRUE) " +
                    "THEN 'DISPONIVEL' ELSE 'INDISPONIVEL' END WHERE id = ?")) {
                    update.setInt(1, exemplarId);
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("O exemplar do empréstimo não foi encontrado.");
                    }
                }
                return true;
            });
        }
    }

    public static final class ListaEscolarDAO {
        private ListaEscolarDAO() {}

        public static boolean inserir(ListaEscolar lista) {
            if (lista == null || lista.getName() == null || lista.getName().isBlank()) return false;
            return transacao("Não foi possível inserir a lista escolar.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO lista_escolar (name, description) VALUES (?, ?)")) {
                    statement.setString(1, lista.getName().trim());
                    statement.setString(2, lista.getDescription());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean atualizar(ListaEscolar lista) {
            if (lista == null || lista.getId() <= 0 || lista.getName() == null
                || lista.getName().isBlank()) return false;
            return transacao("Não foi possível atualizar a lista escolar.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE lista_escolar SET name = ?, description = ? WHERE id = ?")) {
                    statement.setString(1, lista.getName().trim());
                    statement.setString(2, lista.getDescription());
                    statement.setInt(3, lista.getId());
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean deletar(int id) {
            if (id <= 0) return false;
            return transacao("Não foi possível excluir a lista escolar.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lista_escolar WHERE id = ?")) {
                    statement.setInt(1, id);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static List<ListaEscolar> obterTodos() {
            return transacao("Não foi possível consultar as listas escolares.", connection -> {
                List<ListaEscolar> listas = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, description FROM lista_escolar ORDER BY name");
                     ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        listas.add(new ListaEscolar(result.getInt("id"), result.getString("name"),
                            result.getString("description")));
                    }
                }
                return listas;
            });
        }

        public static ListaEscolar obterPorId(int id) {
            return transacao("Não foi possível consultar a lista escolar.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, description FROM lista_escolar WHERE id = ?")) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? new ListaEscolar(result.getInt("id"), result.getString("name"),
                            result.getString("description")) : null;
                    }
                }
            });
        }

        public static List<Livro> obterLivrosDaLista(int listaId) {
            String sql = SELECT_BOOKS + " AND EXISTS (SELECT 1 FROM lista_livro ll " +
                "WHERE ll.livro_id = l.id AND ll.lista_id = ?) ORDER BY l.title";
            return consultarLivros(sql, statement -> statement.setInt(1, listaId));
        }

        public static boolean adicionarLivro(int listaId, int livroId) {
            if (listaId <= 0 || livroId <= 0) return false;
            return transacao("Não foi possível adicionar o livro à lista.", connection -> {
                String sql = "INSERT INTO lista_livro (lista_id, livro_id) " +
                    "SELECT ?, ? WHERE EXISTS (SELECT 1 FROM lista_escolar WHERE id = ?) " +
                    "AND EXISTS (SELECT 1 FROM livro WHERE id = ? AND ativo = TRUE) " +
                    "AND NOT EXISTS (SELECT 1 FROM lista_livro WHERE lista_id = ? AND livro_id = ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, listaId);
                    statement.setInt(2, livroId);
                    statement.setInt(3, listaId);
                    statement.setInt(4, livroId);
                    statement.setInt(5, listaId);
                    statement.setInt(6, livroId);
                    return statement.executeUpdate() == 1;
                }
            });
        }

        public static boolean removerLivro(int listaId, int livroId) {
            if (listaId <= 0 || livroId <= 0) return false;
            return transacao("Não foi possível remover o livro da lista.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lista_livro WHERE lista_id = ? AND livro_id = ?")) {
                    statement.setInt(1, listaId);
                    statement.setInt(2, livroId);
                    return statement.executeUpdate() == 1;
                }
            });
        }
    }

    public static final class CatalogoInativoDAO {
        private CatalogoInativoDAO() {}

        public static List<RegistroInativo> obterTodos() {
            return transacao("Não foi possível consultar os registros inativos.", connection -> {
                String sql = "SELECT tipo, id, descricao FROM (" +
                    "SELECT 'Livro' AS tipo, id, title AS descricao FROM livro WHERE ativo = FALSE " +
                    "UNION ALL SELECT 'Gênero', id, name FROM genero WHERE ativo = FALSE " +
                    "UNION ALL SELECT 'Editora', id, name FROM editora WHERE ativo = FALSE " +
                    "UNION ALL SELECT 'Estante', id, name FROM estante WHERE ativo = FALSE " +
                    "UNION ALL SELECT 'Exemplar', ex.id, CONCAT(ex.codigo, ' - ', COALESCE(l.title, '')) " +
                    "FROM exemplar ex LEFT JOIN livro l ON l.id = ex.livro_id WHERE ex.ativo = FALSE" +
                    ") inativos ORDER BY tipo, id";
                List<RegistroInativo> registros = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        registros.add(new RegistroInativo(result.getString("tipo"), result.getInt("id"),
                            result.getString("descricao")));
                    }
                }
                return registros;
            });
        }
    }

    public static final class AuditoriaDAO {
        private AuditoriaDAO() {}

        public static void registrar(String ator, String acao, String recurso, boolean sucesso) {
            transacao("A auditoria não pôde ser registrada.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO audit_log (actor, action, resource_id, success) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, ator);
                    statement.setString(2, acao);
                    statement.setString(3, recurso);
                    statement.setBoolean(4, sucesso);
                    statement.executeUpdate();
                    return null;
                }
            });
        }
    }
}
