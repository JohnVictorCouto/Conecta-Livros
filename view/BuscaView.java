package view;

import dao.LivroDAO;
import dao.GeneroDAO;
import dao.EditoraDAO;
import model.Livro;
import model.Genero;
import model.Editora;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BuscaView extends JPanel {

    private ComponenteUI.CampoBusca campoBusca;
    private JPanel painelFiltros;
    private JPanel painelResultados;
    private JLabel lblContagem;

    private List<Genero> generos = new ArrayList<>();
    private List<Editora> editoras = new ArrayList<>();
    private JComboBox<Genero> comboGenero;
    private JComboBox<Editora> comboEditora;
    private JComboBox<String> comboOrdenacao;
    private boolean apenasDisponiveis = false;
    private JCheckBox checkDisp;
    
    // Constantes de tamanho padronizadas
    private static final int CAPA_LARGURA = 80;
    private static final int CAPA_ALTURA = 110;

    private static class DadosFiltros {
        private final List<Genero> generos;
        private final List<Editora> editoras;

        DadosFiltros(List<Genero> generos, List<Editora> editoras) {
            this.generos = generos;
            this.editoras = editoras;
        }
    }

    public BuscaView() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);

        JScrollPane scroll = ComponenteUI.scrollLimpo(criarConteudo());
        scroll.setBackground(UITheme.BG_APP);
        add(scroll, BorderLayout.CENTER);
        carregarGeneros();
    }

    private JPanel criarConteudo() {
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBackground(UITheme.BG_APP);
        conteudo.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        // Título
        JLabel titulo = new JLabel("Busca de Livros");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);
        titulo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Pesquise por título, autor, ISBN, idioma, editora ou gênero");
        sub.setFont(UITheme.fontSubtitle());
        sub.setForeground(UITheme.TEXT_SECONDARY);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        conteudo.add(titulo);
        conteudo.add(Box.createRigidArea(new Dimension(0, 4)));
        conteudo.add(sub);
        conteudo.add(Box.createRigidArea(new Dimension(0, 20)));

        // Caixa de busca
        JPanel buscaRow = new JPanel(new BorderLayout(12, 0));
        buscaRow.setOpaque(false);
        buscaRow.setAlignmentX(LEFT_ALIGNMENT);
        buscaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        campoBusca = new ComponenteUI.CampoBusca("Buscar por título, autor, ISBN, idioma ou editora...", 400);
        campoBusca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) buscar(campoBusca.getText());
            }
        });

        ComponenteUI.BotaoPrimario btnBuscar = new ComponenteUI.BotaoPrimario("Buscar");
        btnBuscar.setPreferredSize(new Dimension(100, 42));
        btnBuscar.addActionListener(e -> buscar(campoBusca.getText()));

        buscaRow.add(campoBusca, BorderLayout.CENTER);
        buscaRow.add(btnBuscar, BorderLayout.EAST);
        conteudo.add(buscaRow);
        conteudo.add(Box.createRigidArea(new Dimension(0, 12)));

        // Filtros de gênero
        painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelFiltros.setOpaque(false);
        painelFiltros.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblFiltros = new JLabel("Filtros:");
        lblFiltros.setFont(UITheme.fontBodyBold());
        lblFiltros.setForeground(UITheme.TEXT_SECONDARY);
        painelFiltros.add(lblFiltros);

        comboGenero = criarComboFiltro(160);
        comboGenero.addActionListener(e -> buscar(campoBusca.getText()));
        painelFiltros.add(comboGenero);

        comboEditora = criarComboFiltro(180);
        comboEditora.addActionListener(e -> buscar(campoBusca.getText()));
        painelFiltros.add(comboEditora);

        comboOrdenacao = new JComboBox<>(new String[]{"Ordenar: A-Z", "Ordenar: Z-A"});
        comboOrdenacao.setFont(UITheme.fontBody());
        comboOrdenacao.setBackground(UITheme.BG_WHITE);
        comboOrdenacao.setForeground(UITheme.TEXT_PRIMARY);
        comboOrdenacao.setPreferredSize(new Dimension(140, 32));
        comboOrdenacao.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 6));
        comboOrdenacao.addActionListener(e -> buscar(campoBusca.getText()));
        painelFiltros.add(comboOrdenacao);

        ComponenteUI.BotaoContorno btnLimpar = new ComponenteUI.BotaoContorno("Limpar filtros");
        btnLimpar.setPreferredSize(new Dimension(130, 32));
        btnLimpar.addActionListener(e -> limparFiltros());
        painelFiltros.add(btnLimpar);

        checkDisp = new JCheckBox("Apenas disponíveis");
        checkDisp.setFont(UITheme.fontBody());
        checkDisp.setForeground(UITheme.TEXT_SECONDARY);
        checkDisp.setOpaque(false);
        checkDisp.addActionListener(e -> {
            apenasDisponiveis = checkDisp.isSelected();
            buscar(campoBusca.getText());
        });

        JPanel filtrosWrap = new JPanel(new BorderLayout());
        filtrosWrap.setOpaque(false);
        filtrosWrap.setAlignmentX(LEFT_ALIGNMENT);
        filtrosWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        filtrosWrap.add(painelFiltros, BorderLayout.WEST);
        filtrosWrap.add(checkDisp, BorderLayout.EAST);
        conteudo.add(filtrosWrap);
        conteudo.add(Box.createRigidArea(new Dimension(0, 16)));

        // Contagem
        lblContagem = new JLabel("");
        lblContagem.setFont(UITheme.fontSmall());
        lblContagem.setForeground(UITheme.TEXT_MUTED);
        lblContagem.setAlignmentX(LEFT_ALIGNMENT);
        conteudo.add(lblContagem);
        conteudo.add(Box.createRigidArea(new Dimension(0, 10)));

        // Resultados
        painelResultados = new JPanel();
        painelResultados.setLayout(new BoxLayout(painelResultados, BoxLayout.Y_AXIS));
        painelResultados.setOpaque(false);
        painelResultados.setAlignmentX(LEFT_ALIGNMENT);
        conteudo.add(painelResultados);

        return conteudo;
    }

    private <T> JComboBox<T> criarComboFiltro(int largura) {
        JComboBox<T> combo = new JComboBox<>();
        combo.setFont(UITheme.fontBody());
        combo.setBackground(UITheme.BG_WHITE);
        combo.setForeground(UITheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(largura, 32));
        combo.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 6));
        return combo;
    }

    private void carregarGeneros() {
        new SwingWorker<DadosFiltros, Void>() {
            protected DadosFiltros doInBackground() {
                return new DadosFiltros(GeneroDAO.obterTodos(), EditoraDAO.obterTodos());
            }
            protected void done() {
                try {
                    DadosFiltros dados = get();
                    generos = dados.generos;
                    editoras = dados.editoras;

                    comboGenero.removeAllItems();
                    comboGenero.addItem(new Genero(0, "Gêneros"));
                    for (Genero g : generos) comboGenero.addItem(g);

                    comboEditora.removeAllItems();
                    comboEditora.addItem(new Editora(0, "Editoras"));
                    for (Editora e : editoras) comboEditora.addItem(e);

                    buscar("");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void limparFiltros() {
        if (comboGenero.getItemCount() > 0) comboGenero.setSelectedIndex(0);
        if (comboEditora.getItemCount() > 0) comboEditora.setSelectedIndex(0);
        comboOrdenacao.setSelectedIndex(0);
        checkDisp.setSelected(false);
        apenasDisponiveis = false;
        buscar(campoBusca.getText());
    }

    public void buscar(String termo) {
        if (campoBusca != null && termo != null && !campoBusca.getText().equals(termo)) {
            campoBusca.getCampo().setText(termo);
        }

        new SwingWorker<List<Livro>, Void>() {
            protected List<Livro> doInBackground() {
                List<Livro> livros;
                if (termo == null || termo.isBlank()) {
                    livros = LivroDAO.obterTodos();
                } else {
                    livros = LivroDAO.buscar(termo.trim());
                }
                // Filtro gênero
                Genero generoSel = (Genero) comboGenero.getSelectedItem();
                if (generoSel != null && generoSel.getId() > 0) {
                    int gId = generoSel.getId();
                    livros = livros.stream().filter(l -> l.getGenreId() == gId).collect(Collectors.toList());
                }
                Editora editoraSel = (Editora) comboEditora.getSelectedItem();
                if (editoraSel != null && editoraSel.getId() > 0) {
                    int eId = editoraSel.getId();
                    livros = livros.stream().filter(l -> l.getPublisherId() == eId).collect(Collectors.toList());
                }
                // Filtro disponibilidade
                if (apenasDisponiveis) {
                    livros = livros.stream().filter(Livro::isDisponibilidade).collect(Collectors.toList());
                }
                Comparator<Livro> porTitulo = Comparator.comparing(
                    l -> l.getTitle() != null ? l.getTitle() : "",
                    String.CASE_INSENSITIVE_ORDER
                );
                if (comboOrdenacao.getSelectedIndex() == 1) {
                    porTitulo = porTitulo.reversed();
                }
                livros = livros.stream().sorted(porTitulo).collect(Collectors.toList());
                return livros;
            }
            protected void done() {
                try {
                    List<Livro> livros = get();
                    exibirResultados(livros);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void exibirResultados(List<Livro> livros) {
        painelResultados.removeAll();
        lblContagem.setText(livros.size() + " resultado(s) encontrado(s)");

        if (livros.isEmpty()) {
            JLabel vazio = new JLabel("Nenhum livro encontrado para esta busca.");
            vazio.setFont(UITheme.fontBody());
            vazio.setForeground(UITheme.TEXT_MUTED);
            vazio.setAlignmentX(LEFT_ALIGNMENT);
            painelResultados.add(Box.createRigidArea(new Dimension(0, 20)));
            painelResultados.add(vazio);
        } else {
            for (Livro l : livros) {
                painelResultados.add(criarLinhaLivro(l));
                painelResultados.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        painelResultados.revalidate();
        painelResultados.repaint();
    }

    private JPanel criarLinhaLivro(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel(10);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Capa - tamanho padronizado
        ImagemCapa capa = new ImagemCapa(livro.getCover(), livro.getId(), livro.getTitle());
        capa.setPreferredSize(new Dimension(CAPA_LARGURA, CAPA_ALTURA));
        capa.setRaioCantos(6);

        // Info do livro
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel(livro.getTitle() != null ? livro.getTitle() : "Sem título");
        lblTitulo.setFont(UITheme.fontBookTitle());
        lblTitulo.setForeground(UITheme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);

        String autorAno = (livro.getAuthors() != null ? livro.getAuthors() : "Desconhecido")
            + (livro.getPublishYear() > 0 ? " · " + livro.getPublishYear() : "");
        JLabel lblAutor = new JLabel(autorAno);
        lblAutor.setFont(UITheme.fontBookAuthor());
        lblAutor.setForeground(UITheme.TEXT_SECONDARY);
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(LEFT_ALIGNMENT);

        String nomeGenero = generos.stream()
            .filter(g -> g.getId() == livro.getGenreId())
            .map(Genero::getName)
            .findFirst().orElse("Geral");

        badgeRow.add(new ComponenteUI.BadgeGenero(nomeGenero));

        String nomeEstante = livro.getEstanteId() > 0 ? DadosCache.getNomeEstante(livro.getEstanteId()) : "Sem estante";
        JLabel lblEstante = new JLabel("📍 " + nomeEstante);
        lblEstante.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lblEstante.setForeground(UITheme.TEXT_MUTED);
        badgeRow.add(lblEstante);
        if (livro.getIdioma() != null && !livro.getIdioma().isBlank()) {
            JLabel lblIdioma = new JLabel("🌐 " + livro.getIdioma());
            lblIdioma.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            lblIdioma.setForeground(UITheme.TEXT_MUTED);
            badgeRow.add(lblIdioma);
        }
        if (livro.getIsbn() != null && !livro.getIsbn().isBlank()) {
            JLabel lblIsbn = new JLabel("ISBN: " + livro.getIsbn());
            lblIsbn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            lblIsbn.setForeground(UITheme.TEXT_MUTED);
            badgeRow.add(lblIsbn);
        }

        info.add(lblTitulo);
        info.add(Box.createRigidArea(new Dimension(0, 2)));
        info.add(lblAutor);
        info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(badgeRow);

        // Direita: status + botão
        JPanel direita = new JPanel();
        direita.setOpaque(false);
        direita.setLayout(new BoxLayout(direita, BoxLayout.Y_AXIS));
        direita.setAlignmentX(RIGHT_ALIGNMENT);

        ComponenteUI.BadgeStatus badgeStatus = new ComponenteUI.BadgeStatus(livro.isDisponibilidade());
        badgeStatus.setAlignmentX(RIGHT_ALIGNMENT);

        JPanel botoesAcao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botoesAcao.setOpaque(false);

        JButton btnEdit = new JButton("✏️");
        btnEdit.setToolTipText("Editar livro");
        btnEdit.setPreferredSize(new Dimension(32, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            LivroFormDialog dialog = new LivroFormDialog(topFrame, livro);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                DadosCache.limpar();
                buscar(campoBusca.getText());
            }
        });
        botoesAcao.add(btnEdit);

        direita.add(badgeStatus);
        direita.add(Box.createRigidArea(new Dimension(0, 8)));
        direita.add(botoesAcao);
        direita.add(Box.createRigidArea(new Dimension(0, 4)));

        if (livro.isDisponibilidade()) {
            ComponenteUI.BotaoPrimarioSm btnEmp = new ComponenteUI.BotaoPrimarioSm("Emprestar");
            btnEmp.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar empréstimo de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), false);
                    buscar(campoBusca.getText());
                }
            });
            direita.add(btnEmp);
        } else {
            ComponenteUI.BotaoContornoSm btnDev = new ComponenteUI.BotaoContornoSm("Devolver");
            btnDev.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar devolução de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), true);
                    buscar(campoBusca.getText());
                }
            });
            direita.add(btnDev);
        }

        card.add(capa, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(direita, BorderLayout.EAST);
        return card;
    }
}
