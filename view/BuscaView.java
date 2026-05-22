package view;

import dao.LivroDAO;
import dao.GeneroDAO;
import dao.EditoraDAO;
import dao.EstanteDAO;
import model.Livro;
import model.Genero;
import model.Editora;
import model.Estante;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BuscaView extends JPanel {

    private ComponenteUI.CampoBusca campoBusca;
    private JPanel painelFiltros;
    private JPanel painelResultados;
    private JLabel lblContagem;

    private List<Genero> generos = new ArrayList<>();
    private String filtroGeneroAtual = null;
    private boolean apenasDisponiveis = false;
    private List<ComponenteUI.BotaoFiltro> botoesFiltro = new ArrayList<>();
    private JCheckBox checkDisp;

    public BuscaView() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);

        JScrollPane scroll = ComponenteUI.scrollLimpo(criarConteudo());
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

        JLabel sub = new JLabel("Pesquise por título, autor, editora ou gênero");
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

        campoBusca = new ComponenteUI.CampoBusca("Buscar por título, autor ou editora...", 400);
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

        ComponenteUI.BotaoFiltro btnTodos = new ComponenteUI.BotaoFiltro("Todos");
        btnTodos.setAtivo(true);
        btnTodos.addActionListener(e -> {
            filtroGeneroAtual = null;
            desativarFiltros();
            btnTodos.setAtivo(true);
            buscar(campoBusca.getText());
        });
        botoesFiltro.add(btnTodos);
        painelFiltros.add(btnTodos);

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

    private void carregarGeneros() {
        new SwingWorker<List<Genero>, Void>() {
            protected List<Genero> doInBackground() { return GeneroDAO.obterTodos(); }
            protected void done() {
                try {
                    generos = get();
                    for (Genero g : generos) {
                        ComponenteUI.BotaoFiltro btn = new ComponenteUI.BotaoFiltro(g.getName());
                        btn.addActionListener(e -> {
                            filtroGeneroAtual = g.getName();
                            desativarFiltros();
                            btn.setAtivo(true);
                            buscar(campoBusca.getText());
                        });
                        botoesFiltro.add(btn);
                        painelFiltros.add(btn);
                    }
                    painelFiltros.revalidate();
                    buscar("");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void desativarFiltros() {
        for (ComponenteUI.BotaoFiltro b : botoesFiltro) b.setAtivo(false);
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
                if (filtroGeneroAtual != null) {
                    Genero g = generos.stream()
                        .filter(x -> x.getName().equals(filtroGeneroAtual))
                        .findFirst().orElse(null);
                    if (g != null) {
                        int gId = g.getId();
                        livros = livros.stream().filter(l -> l.getGenreId() == gId).toList();
                    }
                }
                // Filtro disponibilidade
                if (apenasDisponiveis) {
                    livros = livros.stream().filter(Livro::isDisponibilidade).toList();
                }
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

    // ─── Linha de resultado (estilo screenshot) ───────────────────────────────
    private JPanel criarLinhaLivro(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel(10);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Capa placeholder
        JPanel capa = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAA(g2);
                Color[] cores = {
                    new Color(219, 234, 254), new Color(220, 252, 231),
                    new Color(254, 226, 226), new Color(237, 233, 254),
                    new Color(254, 243, 199)
                };
                Color[] fgCores = {
                    UITheme.STAT_BLUE_FG, UITheme.STAT_GREEN_FG,
                    UITheme.STAT_RED_FG, UITheme.STAT_PURPLE_FG, UITheme.WARNING
                };
                int idx = (livro.getId() % cores.length + cores.length) % cores.length;
                UITheme.fillRounded(g2, cores[idx], 0, 0, getWidth(), getHeight(), 6);
                g2.setColor(fgCores[idx]);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                String ini = livro.getTitle() != null && !livro.getTitle().isEmpty()
                    ? String.valueOf(livro.getTitle().charAt(0)) : "?";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, (getWidth() - fm.stringWidth(ini)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        capa.setPreferredSize(new Dimension(52, 68));
        capa.setOpaque(false);

        // Info do livro
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel(livro.getTitle() != null ? livro.getTitle() : "Sem título");
        lblTitulo.setFont(UITheme.fontBookTitle());
        lblTitulo.setForeground(UITheme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);

        String autorStr = (livro.getAuthors() != null ? livro.getAuthors() : "Desconhecido")
            + (livro.getPublishYear() > 0 ? " · " + livro.getPublishYear() : "");
        JLabel lblAutor = new JLabel(autorStr);
        lblAutor.setFont(UITheme.fontBookAuthor());
        lblAutor.setForeground(UITheme.TEXT_SECONDARY);
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);

        // Badges em linha
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(LEFT_ALIGNMENT);

        // Busca nome do gênero
        String nomeGenero = generos.stream()
            .filter(g -> g.getId() == livro.getGenreId())
            .map(Genero::getName)
            .findFirst().orElse("Geral");

        badgeRow.add(new ComponenteUI.BadgeGenero(nomeGenero));

        // Estante
        JLabel lblEstante = new JLabel("📍 Estante " + livro.getEstanteId());
        lblEstante.setFont(UITheme.fontSmall());
        lblEstante.setForeground(UITheme.TEXT_MUTED);
        badgeRow.add(lblEstante);

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

        direita.add(badgeStatus);
        direita.add(Box.createRigidArea(new Dimension(0, 8)));

        if (livro.isDisponibilidade()) {
            ComponenteUI.BotaoPrimario btnEmp = new ComponenteUI.BotaoPrimario("Emprestar");
            btnEmp.setPreferredSize(new Dimension(110, 34));
            btnEmp.setMaximumSize(new Dimension(110, 34));
            btnEmp.setAlignmentX(RIGHT_ALIGNMENT);
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
            JLabel ind = new JLabel("Indisponível");
            ind.setFont(UITheme.fontSmall());
            ind.setForeground(UITheme.TEXT_MUTED);
            ind.setBorder(BorderFactory.createCompoundBorder(
                new ComponenteUI.RoundedBorder(UITheme.BORDER, 6),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
            ind.setAlignmentX(RIGHT_ALIGNMENT);
            direita.add(ind);
        }

        card.add(capa, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(direita, BorderLayout.EAST);
        return card;
    }
}
