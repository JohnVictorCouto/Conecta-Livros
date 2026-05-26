package util;
import  java.sql.Connection;
import  java.sql.DriverManager;
import  java.sql.PreparedStatement;
import  java.sql.ResultSet;
import  java.sql.SQLException;

public  class ConnectionFactory
{   private static final String URL  =   "jdbc:mysql://localhost:3306/conecta_livros";
    private static final String USER =   "root";
    private static final String PASS =   "02092005";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String ERROBD = "Erro na conexão com o Banco de Dados: "; 

    public static Connection getConnection()
    {   try
        {   Class.forName(DRIVER);
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false);
            return conn;
        }
        catch(ClassNotFoundException | SQLException e) 
        {   throw new RuntimeException(ERROBD + e);
        }
    }

    public static void closeConnection(Connection conn)
    {   try
        {   if(conn != null) 
            {   conn.close();
            }
        } 
        catch(SQLException e) 
        {   throw new RuntimeException(ERROBD + e);
        }
    }
    public static void closeConnection(Connection conn, PreparedStatement stmt) 
    {   closeConnection(conn);
        try
        {   if(stmt != null) 
            {   stmt.close();
            }
        } 
        catch(SQLException e) 
        {   throw new RuntimeException(ERROBD + e);
        }
    }
    public static void closeConnection(Connection conn, PreparedStatement stmt, ResultSet rs) 
    {   closeConnection(conn, stmt);
        try
        {   if(rs != null) 
            {   rs.close();
            }
        } 
        catch(SQLException e) 
        {   throw new RuntimeException(ERROBD + e);
        }
    }
}