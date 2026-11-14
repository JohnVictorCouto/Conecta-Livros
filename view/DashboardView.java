package view;

import dao.LivroDAO;
import dao.EstanteDAO;
import dao.EmprestimoDAO;
import model.Emprestimo;
import model.Livro;
import util.DadosCache;
import util.Tradutor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Painel inicial com indicadores resumidos do acervo e livros em destaque;
 * a atualização dos dados ocorre sem bloquear a interface.
 */
public class DashboardView extends JPanel {

    private final MainFrame frame;
    private JPanel painelStats;
    private JPanel painelCards;
    private JLabel lblTotalLivros, lblDisponiveis, lblEmprestados, lblEstantes;
    private JLabel lblEmprestimosAtivos, lblAtrasados;
    
    // Dimensões compartilhadas mantêm alinhados os cartões e as capas do painel.
    private static final int CAPA_LARGURA = 180;
    private static final int CAPA_ALTURA = 200;

    /**
     * Cria o painel inicial e guarda a janela para acionar a navegação contextual.
     * @param frame janela principal da aplicação
     */
    public DashboardView(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);

        JScrollPane scroll = ComponenteUI.scrollLimpo(criarConteudo());
        scroll.setBackground(UITheme.BG_APP);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(UITheme.BG_APP);
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

        JLabel titulo = new JLabel(Tradutor.t("dashboard.title"));
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel(Tradutor.t("dashboard.subtitle"));
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

        // Indicadores resumidos do acervo para consulta rápida no painel inicial.
        painelStats = new JPanel(new GridLayout(0, 3, 16, 16));
        painelStats.setOpaque(false);
        painelStats.setAlignmentX(LEFT_ALIGNMENT);
        painelStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 236));

        lblTotalLivros  = new JLabel("...");
        lblDisponiveis  = new JLabel("...");
        lblEmprestados  = new JLabel("...");
        lblEstantes     = new JLabel("...");
        lblEmprestimosAtivos = new JLabel("...");
        lblAtrasados = new JLabel("...");

        painelStats.add(criarStatCard(Tradutor.t("dashboard.books"),     lblTotalLivros, "📚", UITheme.STAT_BLUE_BG, UITheme.STAT_BLUE_FG));
        painelStats.add(criarStatCard(Tradutor.t("dashboard.available"), lblDisponiveis, "✅", UITheme.STAT_GREEN_BG, UITheme.STAT_GREEN_FG));
        painelStats.add(criarStatCard(Tradutor.t("dashboard.borrowed"),  lblEmprestados, "📤", UITheme.STAT_RED_BG, UITheme.STAT_RED_FG));
        painelStats.add(criarStatCard(Tradutor.t("dashboard.shelves"),   lblEstantes,    "📦", UITheme.STAT_PURPLE_BG, UITheme.STAT_PURPLE_FG));
        painelStats.add(criarStatCard(Tradutor.t("dashboard.active_loans"), lblEmprestimosAtivos, "📖", UITheme.STAT_BLUE_BG, UITheme.STAT_BLUE_FG));
        painelStats.add(criarStatCard(Tradutor.t("dashboard.overdue_loans"), lblAtrasados, "⏰", UITheme.STAT_RED_BG, UITheme.STAT_RED_FG));

        conteudo.add(painelStats);
        conteudo.add(Box.createRigidArea(new Dimension(0, 28)));

        // Cabeçalho que identifica a seção de recomendações do acervo.
        JPanel recHeader = new JPanel(new BorderLayout());
        recHeader.setOpaque(false);
        recHeader.setAlignmentX(LEFT_ALIGNMENT);
        recHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblRec = new JLabel(Tradutor.t("dashboard.recommendations"));
        lblRec.setFont(UITheme.fontBodyBold().deriveFont(15f));
        lblRec.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(Tradutor.t("dashboard.recommendations_subtitle"));
        lblSub.setFont(UITheme.fontSmall());
        lblSub.setForeground(UITheme.TEXT_MUTED);

        ComponenteUI.BotaoContorno btnSortear = new ComponenteUI.BotaoContorno(Tradutor.t("dashboard.shuffle"));
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

        // Grade que distribui os indicadores sem alterar a ordem em que são apresentados.
        painelCards = new JPanel(new GridLayout(0, 2, 16, 16));
        painelCards.setOpaque(false);
        painelCards.setAlignmentX(LEFT_ALIGNMENT);
        painelCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        conteudo.add(painelCards);

        return conteudo;
    }

    // Cartão que resume uma métrica do acervo, como quantidade de livros ou estantes.
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

    // Cartão de livro com capa, dados principais e ação para abrir o cadastro.
    private JPanel criarLivroCard(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // A capa pode vir de uma URL; quando ausente, o componente exibe uma alternativa colorida.
        ImagemCapa capa = new ImagemCapa(
            livro.getCover(), 
            livro.getId(), 
            livro.getTitle()
        );
        capa.setPreferredSize(new Dimension(CAPA_LARGURA, CAPA_ALTURA));
        capa.setRaioCantos(8);

        // Dados textuais do livro exibidos ao lado da capa dentro do cartão.
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JTextArea lblTitulo = ComponenteUI.labelMultilinha(
            livro.getTitle() != null ? livro.getTitle() : Tradutor.t("value.no_title"),
            UITheme.fontBookTitle().deriveFont(16f),
            UITheme.TEXT_PRIMARY,
            58
        );

        String autorAno = (livro.getAuthors() != null ? livro.getAuthors() : "")
            + (livro.getPublishYear() > 0 ? " · " + livro.getPublishYear() : "");
        JLabel lblAutor = new JLabel(autorAno.isEmpty() ? Tradutor.t("value.unknown_author") : autorAno);
        lblAutor.setFont(UITheme.fontBookAuthor().deriveFont(13f));
        lblAutor.setForeground(UITheme.TEXT_SECONDARY);
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);

        ComponenteUI.BadgeGenero badge = new ComponenteUI.BadgeGenero(DadosCache.getNomeGenero(livro.getGenreId()));
        badge.setAlignmentX(LEFT_ALIGNMENT);

        ComponenteUI.BadgeStatus status = new ComponenteUI.BadgeStatus(
            livro.getExemplaresDisponiveis(), livro.getExemplaresEmprestados());
        status.setAlignmentX(LEFT_ALIGNMENT);

        info.add(lblTitulo);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(lblAutor);
        info.add(Box.createRigidArea(new Dimension(0, 8)));
        info.add(badge);
        info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(status);
        info.add(Box.createVerticalGlue());

        // Ação principal disponível para o livro destacado no painel.
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel acoesEmprestimo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        acoesEmprestimo.setOpaque(false);
        if (livro.getExemplaresDisponiveis() > 0) {
            ComponenteUI.BotaoPrimarioSm btn = new ComponenteUI.BotaoPrimarioSm(Tradutor.t("action.register_loan"));
            btn.setPreferredSize(new Dimension(180, 36)); // Mantém a ação legível e alinhada aos cartões do painel.
            btn.addActionListener(e ->
                LivroAcoes.registrarEmprestimo(this, livro.getId(), this::recarregar));
            acoesEmprestimo.add(btn);
        }
        if (livro.getExemplaresEmprestados() > 0) {
            ComponenteUI.BotaoContornoSm btn = new ComponenteUI.BotaoContornoSm(Tradutor.t("action.return_book"));
            btn.setPreferredSize(new Dimension(180, 36));
            btn.addActionListener(e ->
                LivroAcoes.registrarDevolucao(this, livro.getId(), this::recarregar));
            acoesEmprestimo.add(btn);
        }
        bottom.add(acoesEmprestimo, BorderLayout.WEST);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(info, BorderLayout.CENTER);
        centro.add(bottom, BorderLayout.SOUTH);

        card.add(capa, BorderLayout.WEST);
        card.add(centro, BorderLayout.CENTER);
        return card;
    }

    // Carrega os dados iniciais e permite atualizar os cartões após mudanças no acervo.
    /** Atualiza os indicadores e os livros exibidos sem bloquear o Swing. */
    public void recarregar() {
        new SwingWorker<int[], Void>() {
            protected int[] doInBackground() {
                List<Livro> todos = LivroDAO.obterTodos();
                int estantes = EstanteDAO.obterTodos().size();
                List<Emprestimo> emprestimos = EmprestimoDAO.obterTodos();
                return calcularEstatisticas(todos, emprestimos, estantes);
            }
            protected void done() {
                try {
                    int[] nums = get();
                    lblTotalLivros.setText(String.valueOf(nums[0]));
                    lblDisponiveis.setText(String.valueOf(nums[1]));
                    lblEmprestados.setText(String.valueOf(nums[2]));
                    lblEstantes.setText(String.valueOf(nums[3]));
                    lblEmprestimosAtivos.setText(String.valueOf(nums[4]));
                    lblAtrasados.setText(String.valueOf(nums[5]));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErroCarregamento(e);
                } catch (java.util.concurrent.ExecutionException e) {
                    mostrarErroCarregamento(e.getCause() == null ? e : e.getCause());
                }
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
                        painelCards.add(criarLivroCard(l));
                    }
                    painelCards.revalidate();
                    painelCards.repaint();
                    DashboardView.this.revalidate();
                    DashboardView.this.repaint();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErroCarregamento(e);
                } catch (java.util.concurrent.ExecutionException e) {
                    mostrarErroCarregamento(e.getCause() == null ? e : e.getCause());
                }
            }
        }.execute();
    }

    static int[] calcularEstatisticas(List<Livro> livros, List<Emprestimo> emprestimos,
                                      int quantidadeEstantes) {
        int disponiveis = (int) livros.stream().filter(Livro::isDisponibilidade).count();
        int emprestados = (int) livros.stream()
            .filter(livro -> livro.getExemplaresEmprestados() > 0).count();
        int emprestimosAtivos = (int) emprestimos.stream().filter(Emprestimo::isAtivo).count();
        int atrasados = (int) emprestimos.stream().filter(Emprestimo::isAtrasado).count();
        return new int[] {
            livros.size(), disponiveis, emprestados, quantidadeEstantes, emprestimosAtivos, atrasados
        };
    }

    private void mostrarErroCarregamento(Throwable erro) {
        String detalhe = erro.getMessage();
        if (detalhe == null || detalhe.isBlank()) detalhe = erro.getClass().getSimpleName();
        JOptionPane.showMessageDialog(this,
            Tradutor.formatar("message.error_detail", detalhe),
            Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
    }
}
