package scr;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Classe Busca herda JFrame para criar uma janela
public class Busca extends JFrame {

    // Campo para digitar o texto da busca
    private JTextField txtBusca;

    // Caixa de seleção do tipo de busca
    private JComboBox<String> cbTipoBusca;

    // Área onde os resultados serão exibidos
    private JTextArea areaResultado;

    // Botão de busca
    private JButton btnBuscar;

    // Construtor da classe
    public Busca() {

        // Define o título da janela
        setTitle("Busca de Livros");

        // Define largura e altura da janela
        setSize(700,500);

        // Centraliza a janela na tela
        setLocationRelativeTo(null);

        // Fecha somente esta janela
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Define o layout principal
        setLayout(new BorderLayout());

        // Cria um painel superior
        JPanel painel = new JPanel();

        // Cria caixa de texto com tamanho 20
        txtBusca = new JTextField(20);

        // Vetor contendo os tipos de busca
        String tipos[] = {

                "Título",
                "Autor",
                "Gênero",
                "Editora",
                "Disponibilidade"
        };

        // Cria ComboBox usando o vetor
        cbTipoBusca = new JComboBox<>(tipos);

        // Cria botão Buscar
        btnBuscar = new JButton("Buscar");

        // Adiciona componentes ao painel
        painel.add(new JLabel("Pesquisar: "));
        painel.add(txtBusca);
        painel.add(cbTipoBusca);
        painel.add(btnBuscar);

        // Cria área de texto
        areaResultado = new JTextArea();

        // Impede edição pelo usuário
        areaResultado.setEditable(false);

        // Adiciona painel ao topo
        add(painel, BorderLayout.NORTH);

        // Adiciona área com barra de rolagem
        add(
            new JScrollPane(areaResultado),
            BorderLayout.CENTER
        );

        // Evento do botão
        btnBuscar.addActionListener(
            e -> buscarLivro()
        );

        // Torna a janela visível
        setVisible(true);
    }


    // Método responsável pela busca
    private void buscarLivro() {

        // Objetos para conexão com banco
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        // Limpa resultados anteriores
        areaResultado.setText("");

        // Captura texto digitado
        String texto = txtBusca.getText();

        // Variável SQL
        String sql = "";

        try {

            // Verifica tipo escolhido
            switch(
                cbTipoBusca
                .getSelectedItem()
                .toString()
            ) {

                // Busca por título
                case "Título":

                    sql =
                    "SELECT title, authors " +
                    "FROM livro " +
                    "WHERE title LIKE ?";

                    break;

                // Busca por autor
                case "Autor":

                    sql =
                    "SELECT title, authors " +
                    "FROM livro " +
                    "WHERE authors LIKE ?";

                    break;

                // Busca por gênero
                case "Gênero":

                    sql =
                    "SELECT l.title,l.authors " +
                    "FROM livro l " +
                    "INNER JOIN genero g " +
                    "ON l.genre_id=g.id " +
                    "WHERE g.name LIKE ?";

                    break;

                // Busca por editora
                case "Editora":

                    sql =
                    "SELECT l.title,l.authors " +
                    "FROM livro l " +
                    "INNER JOIN editora e " +
                    "ON l.publisher_id=e.id " +
                    "WHERE e.name LIKE ?";

                    break;

                // Busca por disponibilidade
                case "Disponibilidade":

                    sql =
                    "SELECT title,authors " +
                    "FROM livro " +
                    "WHERE disponibilidade=?";

                    break;
            }

            // Abre conexão usando ConnFactory
            conn = ConnFactory.getConn();

            // Prepara SQL
            stmt = conn.prepareStatement(sql);

            // Verifica se busca é disponibilidade
            if(
                cbTipoBusca
                .getSelectedItem()
                .equals("Disponibilidade")
            ) {

                // Converte texto em boolean
                boolean valor =
                texto.equalsIgnoreCase("sim");

                // Define valor
                stmt.setBoolean(1, valor);

            }
            else {

                // Adiciona texto na consulta
                stmt.setString(
                    1,
                    "%" + texto + "%"
                );
            }

            // Executa consulta
            rs = stmt.executeQuery();

            // Controle para verificar resultado
            boolean encontrou = false;

            // Percorre resultados encontrados
            while(rs.next()) {

                encontrou = true;

                // Mostra resultados
                areaResultado.append(

                    "Livro: "
                    + rs.getString("title")

                    + "\nAutor: "
                    + rs.getString("authors")

                    + "\n-----------------\n"
                );
            }

            // Caso não encontre registros
            if(!encontrou) {

                areaResultado.setText(
                    "Nenhum livro encontrado."
                );
            }

        }
        catch(Exception e) {

            // Exibe mensagem de erro
            JOptionPane.showMessageDialog(
                null,
                "Erro: "
                + e.getMessage()
            );
        }

        finally {

            // Fecha conexão e recursos
            ConnFactory.closeConn(
                conn,
                stmt,
                rs
            );
        }
    }

    public static void main(String[] args) {

        // Cria objeto da tela
        new Busca();

    }
}
