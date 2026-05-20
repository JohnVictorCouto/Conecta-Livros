package src;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class Busca extends JFrame {

    private JTextField txtBusca;

    private JPanel resultados;

    public Busca() {

        setTitle("Conecta Livros");
        setSize(1000,700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel principal = new JPanel();
        principal.setLayout(new BorderLayout());
        principal.setBackground(new Color(245,245,245));

        // ===== MENU =====

        JPanel menu = new JPanel(new BorderLayout());

        menu.setBackground(Color.WHITE);

        menu.setBorder(
                new EmptyBorder(
                        10,20,10,20
                )
        );

        JLabel logo =
                new JLabel(
                        "Conecta Livros"
                );

        JPanel nav =
                new JPanel();

        nav.setBackground(Color.WHITE);

        nav.add(new JButton("Estantes"));
        nav.add(new JButton("Listas"));
        nav.add(new JButton("Busca"));

        JButton cadastrar =
                new JButton(
                        "+ Cadastrar Livro"
                );

        cadastrar.setBackground(
                new Color(
                        40,
                        90,
                        255
                )
        );

        cadastrar.setForeground(
                Color.WHITE
        );

        nav.add(cadastrar);

        menu.add(
                logo,
                BorderLayout.WEST
        );

        menu.add(
                nav,
                BorderLayout.EAST
        );


        // ===== CENTRO =====

        JPanel centro =
                new JPanel();

        centro.setLayout(
                new BoxLayout(
                        centro,
                        BoxLayout.Y_AXIS
                )
        );

        centro.setBackground(
                new Color(
                        245,
                        245,
                        245
                )
        );

        JLabel titulo =
                new JLabel(
                        "Busca de Livros"
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );

        titulo.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        10,
                        20
                )
        );

        centro.add(titulo);


        // ===== PESQUISA =====

        JPanel pesquisa =
                new JPanel();

        pesquisa.setBackground(
                Color.WHITE
        );

        pesquisa.setBorder(

                new CompoundBorder(

                        new LineBorder(
                                Color.LIGHT_GRAY
                        ),

                        new EmptyBorder(
                                20,
                                20,
                                20,
                                20
                        )
                )
        );

        txtBusca =
                new JTextField(
                        35
                );

        JButton btn =
                new JButton(
                        "Buscar"
                );

        btn.setBackground(
                new Color(
                        40,
                        90,
                        255
                )
        );

        btn.setForeground(
                Color.WHITE
        );

        // Evento botão buscar
        btn.addActionListener(
                e -> buscarLivro()
        );

        pesquisa.add(txtBusca);
        pesquisa.add(btn);

        centro.add(pesquisa);


        // ===== RESULTADOS =====

        resultados =
                new JPanel();

        resultados.setLayout(
                new GridLayout(
                        0,
                        1,
                        15,
                        15
                )
        );

        resultados.setBackground(
                new Color(
                        245,
                        245,
                        245
                )
        );

        resultados.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        JScrollPane scroll =
                new JScrollPane(
                        resultados
                );

        centro.add(scroll);

        principal.add(
                menu,
                BorderLayout.NORTH
        );

        principal.add(
                centro,
                BorderLayout.CENTER
        );

        add(principal);

        setVisible(true);

    }


    // Busca no banco
    private void buscarLivro(){

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        resultados.removeAll();

        String pesquisa =
                txtBusca.getText();

        try{

            conn =
                    ConnFactory.getConn();

            String sql=

            "SELECT title,authors " +
            "FROM livro " +
            "WHERE title LIKE ? " +
            "OR authors LIKE ?";

            stmt=
                    conn.prepareStatement(
                            sql
                    );

            stmt.setString(
                    1,
                    "%" + pesquisa + "%"
            );

            stmt.setString(
                    2,
                    "%" + pesquisa + "%"
            );

            rs=
                    stmt.executeQuery();

            while(rs.next()){

                resultados.add(

                        criarLivro(

                                rs.getString(
                                        "title"
                                ),

                                rs.getString(
                                        "authors"
                                )
                        )
                );

            }

            resultados.revalidate();
            resultados.repaint();

        }

        catch(Exception e){

            JOptionPane.showMessageDialog(

                    null,

                    "Erro: "
                    + e.getMessage()

            );

        }

        finally{

            ConnFactory.closeConn(

                    conn,
                    stmt,
                    rs

            );

        }

    }


    private JPanel criarLivro(

            String nome,
            String autor

    ){

        JPanel livro=
                new JPanel(
                        new BorderLayout()
                );

        livro.setBackground(
                Color.WHITE
        );

        livro.setBorder(

                new CompoundBorder(

                        new LineBorder(
                                Color.LIGHT_GRAY
                        ),

                        new EmptyBorder(
                                15,
                                15,
                                15,
                                15
                        )
                )
        );

        JLabel titulo=
                new JLabel(
                        nome
                );

        titulo.setFont(

                new Font(
                        "Arial",
                        Font.BOLD,
                        18
                )
        );

        JLabel escritor=
                new JLabel(
                        autor
                );

        JButton emprestar=
                new JButton(
                        "Emprestar"
                );

        emprestar.setBackground(
                new Color(
                        40,
                        90,
                        255
                )
        );

        emprestar.setForeground(
                Color.WHITE
        );

        livro.add(
                titulo,
                BorderLayout.NORTH
        );

        livro.add(
                escritor,
                BorderLayout.CENTER
        );

        livro.add(
                emprestar,
                BorderLayout.EAST
        );

        return livro;

    }


    public static void main(String[] args) {

        new Busca();

    }

}