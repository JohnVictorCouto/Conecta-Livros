package util;

import  java.sql.Connection;
import  java.sql.DriverManager;
import  java.sql.PreparedStatement;
import  java.sql.ResultSet;
import  java.sql.SQLException;

/**
 * Centraliza a abertura de conexões JDBC e o fechamento dos recursos
 * associados, mantendo os detalhes de acesso ao banco fora das telas.
 */
public class ConnectionFactory {

    private static final String DEFAULT_URL =
        "jdbc:mysql://localhost:3306/conecta_livros?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * Abre uma conexão JDBC usando configuração de sistema, ambiente ou padrão.
     * A conexão é devolvida com transações manuais para os DAOs confirmarem
     * operações compostas de forma explícita.
     * @return conexão pronta para uso
     * @throws SQLException se o driver, a conexão ou a configuração falhar
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver do MySQL não encontrado no classpath.", e);
        }

        String url = configuracao("DB_URL", "db.url", DEFAULT_URL);
        String user = configuracao("DB_USER", "db.user", "root");
        String password = configuracao("DB_PASSWORD", "db.password", "02092005");

        Connection conn = DriverManager.getConnection(url, user, password);
        try {
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            fecharSilenciosamente(conn);
            throw e;
        }
    }

    private static String configuracao(String variavel, String propriedade, String padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    /** Fecha uma conexão, ignorando a referência nula.
     * @param conn conexão a encerrar
     */
    public static void closeConnection(Connection conn) {
        fecharSilenciosamente(conn);
    }

    /** Fecha primeiro a instrução e depois a conexão.
     * @param conn conexão associada
     * @param stmt instrução preparada a encerrar
     */
    public static void closeConnection(Connection conn, PreparedStatement stmt) {
        fecharSilenciosamente(stmt);
        fecharSilenciosamente(conn);
    }

    /** Fecha o conjunto de resultados, a instrução e a conexão nessa ordem.
     * @param conn conexão associada
     * @param stmt instrução preparada associada
     * @param rs conjunto de resultados associado
     */
    public static void closeConnection(Connection conn, PreparedStatement stmt, ResultSet rs) {
        fecharSilenciosamente(rs);
        fecharSilenciosamente(stmt);
        fecharSilenciosamente(conn);
    }

    private static void fecharSilenciosamente(AutoCloseable recurso) {
        if (recurso == null) return;
        try {
            recurso.close();
        } catch (Exception e) {
            System.err.println("Erro ao fechar recurso do banco de dados: " + e.getMessage());
        }
    }
}