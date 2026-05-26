package view;

import dao.LivroDAO;
import dao.EstanteDAO;
import model.Livro;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class DashboardView extends JPanel {

    private final MainFrame frame;
    private JPanel painelStats;
    private JPanel painelCards;
    private JLabel lblTotalLivros, lblDisponiveis, lblEmprestados, lblEstantes;
    
    // Constantes de tamanho padronizadas
    private static final int CAPA_LARGURA = 180;
    private static final int CAPA_ALTURA = 200;

    public DashboardView(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);

        JScrollPane scroll = ComponenteUI.scrollLimpo(criarConteudo());
        scroll.setBackground(UITheme.BG_APP);
        add(scroll, BorderLayout.CENTER);

        recarregar();
    }

    private JPanel criarConteudo() {
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBackground(UITheme.BG_APP);
        conteudo.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        // Título
        JPanel secTitulo = new JPanel(new BorderLayout());
        secTitulo.setOpaque(false);
        secTitulo.setAlignmentX(LEFT_ALIGNMENT);
        secTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel titulo = new JLabel("Dashboard");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Visão geral do acervo e recomendações do dia");
        subtitulo.setFont(UITheme.fontSubtitle());
        subtitulo.setForeground(UITheme.TEXT_SECONDARY);

        JPanel tituloBox = new JPanel();
        tituloBox.setOpaque(false);
        tituloBox.setLayout(new BoxLayout(tituloBox, BoxLayout.Y_AXIS));
        tituloBox.add(titulo);
        tituloBox.add(Box.createRigidArea(new Dimension(0, 2)));
        tituloBox.add(subtitulo);
        secTitulo.add(tituloBox, BorderLayout.WEST);
        conteudo.add(secTitulo);
        conteudo.add(Box.createRigidArea(new Dimension(0, 20)));

        // Stats
        painelStats = new JPanel(new GridLayout(1, 4, 16, 0));
        painelStats.setOpaque(false);
        painelStats.setAlignmentX(LEFT_ALIGNMENT);
        painelStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        lblTotalLivros  = new JLabel("...");
        lblDisponiveis  = new JLabel("...");
        lblEmprestados  = new JLabel("...");
        lblEstantes     = new JLabel("...");

        painelStats.add(criarStatCard("Livros no Acervo", lblTotalLivros,  "📚", UITheme.STAT_BLUE_BG,   UITheme.STAT_BLUE_FG));
        painelStats.add(criarStatCard("Disponíveis",      lblDisponiveis,  "✅", UITheme.STAT_GREEN_BG,  UITheme.STAT_GREEN_FG));
        painelStats.add(criarStatCard("Emprestados",      lblEmprestados,  "📤", UITheme.STAT_RED_BG,    UITheme.STAT_RED_FG));
        painelStats.add(criarStatCard("Estantes",         lblEstantes,     "📦", UITheme.STAT_PURPLE_BG, UITheme.STAT_PURPLE_FG));

        conteudo.add(painelStats);
        conteudo.add(Box.createRigidArea(new Dimension(0, 28)));

        // Recomendações header
        JPanel recHeader = new JPanel(new BorderLayout());
        recHeader.setOpaque(false);
        recHeader.setAlignmentX(LEFT_ALIGNMENT);
        recHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblRec = new JLabel("Recomendações do Dia");
        lblRec.setFont(UITheme.fontBodyBold().deriveFont(15f));
        lblRec.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Selecionados aleatoriamente do acervo");
        lblSub.setFont(UITheme.fontSmall());
        lblSub.setForeground(UITheme.TEXT_MUTED);

        ComponenteUI.BotaoContorno btnSortear = new ComponenteUI.BotaoContorno("Sortear novos");
        btnSortear.setPreferredSize(new Dimension(140, 30));
        btnSortear.addActionListener(e -> recarregarCards());

        JPanel recLeft = new JPanel();
        recLeft.setOpaque(false);
        recLeft.setLayout(new BoxLayout(recLeft, BoxLayout.Y_AXIS));
        recLeft.add(lblRec);
        recLeft.add(lblSub);

        recHeader.add(recLeft, BorderLayout.WEST);
        recHeader.add(btnSortear, BorderLayout.EAST);
        conteudo.add(recHeader);
        conteudo.add(Box.createRigidArea(new Dimension(0, 14)));

        // Grid de cards
        painelCards = new JPanel(new GridLayout(0, 3, 16, 16));
        painelCards.setOpaque(false);
        painelCards.setAlignmentX(LEFT_ALIGNMENT);
        conteudo.add(painelCards);

        return conteudo;
    }

    // Stat Card
    private JPanel criarStatCard(String label, JLabel valor, String emoji, Color bg, Color fg) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setOpaque(false);

        JLabel icone = new JLabel(emoji) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAA(g2);
                UITheme.fillRounded(g2, bg, 0, 0, getWidth(), getHeight(), 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icone.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        icone.setHorizontalAlignment(JLabel.CENTER);
        icone.setPreferredSize(new Dimension(44, 44));
        icone.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel textoPanel = new JPanel();
        textoPanel.setOpaque(false);
        textoPanel.setLayout(new BoxLayout(textoPanel, BoxLayout.Y_AXIS));
        textoPanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));

        valor.setFont(UITheme.fontStat());
        valor.setForeground(fg);
        valor.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.fontStatLabel());
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);

        textoPanel.add(valor);
        textoPanel.add(lbl);

        conteudo.add(icone, BorderLayout.WEST);
        conteudo.add(textoPanel, BorderLayout.CENTER);
        card.add(conteudo, BorderLayout.CENTER);
        return card;
    }

    // Card de Livro 
    private JPanel criarLivroCard(Livro livro, String generoNome) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Capa com suporte a URL ou fallback colorido - tamanho padronizado
        ImagemCapa capa = new ImagemCapa(
            livro.getCover(), 
            livro.getId(), 
            livro.getTitle()
        );
        capa.setPreferredSize(new Dimension(CAPA_LARGURA, CAPA_ALTURA));
        capa.setRaioCantos(8);

        // Informações
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel(livro.getTitle() != null ? livro.getTitle() : "Sem título");
        lblTitulo.setFont(UITheme.fontBookTitle().deriveFont(16f)); // Título maior
        lblTitulo.setForeground(UITheme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);

        String autorAno = (livro.getAuthors() != null ? livro.getAuthors() : "")
            + (livro.getPublishYear() > 0 ? " · " + livro.getPublishYear() : "");
        JLabel lblAutor = new JLabel(autorAno.isEmpty() ? "Autor desconhecido" : autorAno);
        lblAutor.setFont(UITheme.fontBookAuthor().deriveFont(13f));
        lblAutor.setForeground(UITheme.TEXT_SECONDARY);
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);

        ComponenteUI.BadgeGenero badge = new ComponenteUI.BadgeGenero(DadosCache.getNomeGenero(livro.getGenreId()));
        badge.setAlignmentX(LEFT_ALIGNMENT);

        ComponenteUI.BadgeStatus status = new ComponenteUI.BadgeStatus(livro.isDisponibilidade());
        status.setAlignmentX(LEFT_ALIGNMENT);

        info.add(lblTitulo);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(lblAutor);
        info.add(Box.createRigidArea(new Dimension(0, 8)));
        info.add(badge);
        info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(status);
        info.add(Box.createVerticalGlue());

        // Botão
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        if (livro.isDisponibilidade()) {
            ComponenteUI.BotaoPrimarioSm btn = new ComponenteUI.BotaoPrimarioSm("Registrar Empréstimo");
            btn.setPreferredSize(new Dimension(180, 36)); // Botão maior e mais largo
            btn.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar empréstimo de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), false);
                    recarregar();
                }
            });
            bottom.add(btn, BorderLayout.WEST);
        } else {
            ComponenteUI.BotaoContornoSm btn = new ComponenteUI.BotaoContornoSm("Devolver Livro");
            btn.setPreferredSize(new Dimension(180, 36));
            btn.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar devolução de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), true);
                    recarregar();
                }
            });
            bottom.add(btn, BorderLayout.WEST);
        }

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(info, BorderLayout.CENTER);
        centro.add(bottom, BorderLayout.SOUTH);

        card.add(capa, BorderLayout.WEST);
        card.add(centro, BorderLayout.CENTER);
        return card;
    }

    // Carregar / Recarregar
    public void recarregar() {
        new SwingWorker<int[], Void>() {
            protected int[] doInBackground() {
                List<Livro> todos = LivroDAO.obterTodos();
                int total    = todos.size();
                int disp     = (int) todos.stream().filter(Livro::isDisponibilidade).count();
                int empr     = total - disp;
                int estantes = EstanteDAO.obterTodos().size();
                return new int[]{total, disp, empr, estantes};
            }
            protected void done() {
                try {
                    int[] nums = get();
                    lblTotalLivros.setText(String.valueOf(nums[0]));
                    lblDisponiveis.setText(String.valueOf(nums[1]));
                    lblEmprestados.setText(String.valueOf(nums[2]));
                    lblEstantes.setText(String.valueOf(nums[3]));
                } catch (Exception ignored) {}
            }
        }.execute();

        recarregarCards();
    }

    private void recarregarCards() {
        new SwingWorker<List<Livro>, Void>() {
            protected List<Livro> doInBackground() {
                List<Livro> todos = LivroDAO.obterTodos();
                Collections.shuffle(todos);
                return todos.subList(0, Math.min(6, todos.size()));
            }
            protected void done() {
                try {
                    List<Livro> selecionados = get();
                    painelCards.removeAll();
                    for (Livro l : selecionados) {
                        painelCards.add(criarLivroCard(l, "Geral"));
                    }
                    painelCards.revalidate();
                    painelCards.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}
