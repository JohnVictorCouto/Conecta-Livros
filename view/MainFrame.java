package view;

import javax.swing.*;

import util.DadosCache;
import util.Tradutor;

import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

/**
 * Janela principal da aplicação: coordena a navegação entre telas e
 * encaminha as notificações de atualização compartilhadas entre elas.
 */
public class MainFrame extends JFrame {

    private CardLayout   cardLayout;
    private JPanel       painelConteudo;

    private DashboardView    dashboardView;
    private LivroView        livroView;
    private EstanteView      estanteView;
    private ListaEscolarView listaView;
    private BuscaView        buscaView;
    private EmprestimosView  emprestimosView;

    private NavLink[] links;
    private NavLink   linkAtivo;
    private String telaAtual = "dashboard";

    /** Inicializa a janela e apresenta a primeira tela da aplicação. */
    public MainFrame() {
        setTitle(Tradutor.t("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 740);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        setBackground(UITheme.BG_APP);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        setLayout(new BorderLayout(0, 0));
        construirInterface(telaAtual, "");
    }

    private void construirInterface(String tela, String termoBusca) {
        getContentPane().removeAll();
        setTitle(Tradutor.t("app.title"));
        add(criarNavbar(), BorderLayout.NORTH);

        cardLayout     = new CardLayout();
        painelConteudo = new JPanel(cardLayout);
        painelConteudo.setBackground(UITheme.BG_APP);

        dashboardView = new DashboardView(this);
        livroView     = new LivroView(this::recarregarCadastros, this::recarregarLivros);
        estanteView   = new EstanteView();
        listaView     = new ListaEscolarView();
        buscaView     = new BuscaView(this::recarregarLivros);
        emprestimosView = new EmprestimosView(this::recarregarLivros);

        painelConteudo.add(dashboardView, "dashboard");
        painelConteudo.add(livroView,     "livros");
        painelConteudo.add(estanteView,   "estantes");
        painelConteudo.add(listaView,     "listas");
        painelConteudo.add(buscaView,     "busca");
        painelConteudo.add(emprestimosView, "emprestimos");

        add(painelConteudo, BorderLayout.CENTER);
        telaAtual = tela;
        cardLayout.show(painelConteudo, telaAtual);
        linkAtivo = null;
        for (NavLink link : links) {
            if (link.getChave().equals(telaAtual)) {
                ativarLink(link);
                break;
            }
        }
        if ("busca".equals(telaAtual) && termoBusca != null && !termoBusca.isBlank()) {
            buscaView.buscar(termoBusca);
        }
        revalidate();
        repaint();
    }

    // ─── Navbar ───────────────────────────────────────────────────────────────
    private JPanel criarNavbar() {
        JPanel nav = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(UITheme.NAV_BORDER);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        nav.setBackground(UITheme.NAV_BG);
        nav.setPreferredSize(new Dimension(0, 56));
        nav.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        // ── Logo ──────────────────────────────────────────────────────────────
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logo.setOpaque(false);
        logo.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel icLogo = new JLabel();

        java.net.URL logoUrl = getClass().getResource("/images/Icon.png");
        if (logoUrl != null) {
            ImageIcon logoIcon = new ImageIcon(logoUrl);
            Image imagem = logoIcon.getImage()
                    .getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            icLogo.setIcon(new ImageIcon(imagem));
        } else {
            icLogo.setText("📚");
            icLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        }
        icLogo.setPreferredSize(new Dimension(30,30));
        icLogo.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        icLogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                irParaDashboard();
            }
        });

        JLabel txtLogo = new JLabel(Tradutor.t("app.title"));
        txtLogo.setFont(UITheme.fontAppTitle());
        txtLogo.setForeground(UITheme.TEXT_PRIMARY);
        txtLogo.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        txtLogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                irParaDashboard();
            }
        });

        logo.add(icLogo);
        logo.add(txtLogo);
        nav.add(logo, BorderLayout.WEST);

        // ── Links centrais ────────────────────────────────────────────────────
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 10));
        linksPanel.setOpaque(false);

        NavLink lLivros   = new NavLink(Tradutor.t("nav.books"),   "livros");
        NavLink lEstantes = new NavLink(Tradutor.t("nav.shelves"), "estantes");
        NavLink lListas   = new NavLink(Tradutor.t("nav.lists"),   "listas");
        NavLink lBusca    = new NavLink(Tradutor.t("nav.search"),  "busca");
        NavLink lEmprestimos = new NavLink(Tradutor.t("nav.loans"), "emprestimos");

        links = new NavLink[]{lLivros, lEstantes, lListas, lBusca, lEmprestimos};
        linkAtivo = null;

        for (NavLink nl : links) {
            nl.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    telaAtual = nl.getChave();
                    ativarLink(nl);
                    cardLayout.show(painelConteudo, nl.getChave());
                }
            });
            linksPanel.add(nl);
        }

        nav.add(linksPanel, BorderLayout.CENTER);

        // ── Lado direito: idioma e botão de cadastro ─────────────────────────
        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        direita.setOpaque(false);

        JLabel lblIdioma = new JLabel(Tradutor.t("language.label") + ":");
        lblIdioma.setFont(UITheme.fontSmall());
        lblIdioma.setForeground(UITheme.TEXT_SECONDARY);

        JComboBox<IdiomaOpcao> comboIdioma = new JComboBox<>();
        for (Locale locale : Tradutor.getIdiomasDisponiveis()) {
            comboIdioma.addItem(new IdiomaOpcao(locale));
        }
        comboIdioma.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component componente = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                Locale locale = value instanceof IdiomaOpcao
                    ? ((IdiomaOpcao) value).locale : Tradutor.getIdioma();
                componente.setFont(UITheme.fontForLocale(
                    locale, UITheme.fontSmall().getStyle(), UITheme.fontSmall().getSize()));
                return componente;
            }
        });
        for (int i = 0; i < comboIdioma.getItemCount(); i++) {
            if (comboIdioma.getItemAt(i).locale.equals(Tradutor.getIdioma())) {
                comboIdioma.setSelectedIndex(i);
                break;
            }
        }
        comboIdioma.setFont(UITheme.fontSmall());
        comboIdioma.setToolTipText(Tradutor.t("language.label"));
        comboIdioma.setPreferredSize(new Dimension(112, 32));
        comboIdioma.addActionListener(e -> {
            IdiomaOpcao opcao = (IdiomaOpcao) comboIdioma.getSelectedItem();
            if (opcao != null && !opcao.locale.equals(Tradutor.getIdioma())) {
                String termo = "busca".equals(telaAtual) && buscaView != null
                    ? buscaView.getTermoBusca() : "";
                Tradutor.definirIdioma(opcao.locale);
                construirInterface(telaAtual, termo);
            }
        });

        ComponenteUI.BotaoPrimario btnCadastrar = new ComponenteUI.BotaoPrimario(Tradutor.t("action.new_book"));
        btnCadastrar.addActionListener(e -> abrirDialogLivro());
        direita.add(lblIdioma);
        direita.add(comboIdioma);
        direita.add(btnCadastrar);
        nav.add(direita, BorderLayout.EAST);

        return nav;
    }

    // ─── Navegação pública ────────────────────────────────────────────────────
    /** Marca o destino escolhido e sincroniza o estado visual da navegação.
     * @param link item de navegação selecionado
     */
    public void ativarLink(NavLink link) {
        if (linkAtivo != null) linkAtivo.setAtivo(false);
        if (link != null) link.setAtivo(true);
        linkAtivo = link;
    }

    /** Navega para o painel inicial. */
    public void irParaDashboard() {
        telaAtual = "dashboard";
        ativarLink(null);
        cardLayout.show(painelConteudo, "dashboard");
    }

    /** Abre a busca e encaminha o termo inicial para seus filtros.
     * @param termo expressão de pesquisa inicial
     */
    public void irParaBusca(String termo) {
        telaAtual = "busca";
        for (NavLink nl : links)
            if ("busca".equals(nl.getChave())) { ativarLink(nl); break; }
        buscaView.buscar(termo);
        cardLayout.show(painelConteudo, "busca");
    }

    private static class IdiomaOpcao {
        private final Locale locale;

        IdiomaOpcao(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String toString() {
            return Tradutor.getNomeIdioma(locale);
        }
    }

    /** Abre o formulário de inclusão a partir da janela principal. */
    public void abrirDialogLivro() {
        LivroFormDialog d = new LivroFormDialog(this, null);
        d.setVisible(true);
        if (d.isSalvo()) {
            if (dashboardView != null) dashboardView.recarregar();
            if (livroView != null) livroView.recarregar();
            if (buscaView != null) buscaView.buscar("");
            DadosCache.limpar();
        }
    }

    /** Atualiza os dados do painel inicial, quando já foi construído. */
    public void recarregarDashboard() {
        if (dashboardView != null) dashboardView.recarregar();
    }

    private void recarregarCadastros() {
        DadosCache.limpar();
        if (dashboardView != null) dashboardView.recarregar();
        if (livroView != null) livroView.recarregarCatalogos();
        if (estanteView != null) estanteView.recarregar();
        if (buscaView != null) buscaView.recarregarCatalogos();
    }

    private void recarregarLivros() {
        DadosCache.limpar();
        if (dashboardView != null) dashboardView.recarregar();
        if (livroView != null) livroView.recarregar();
        if (buscaView != null) buscaView.buscar(buscaView.getTermoBusca());
        recarregarEmprestimos();
    }

    /** Atualiza a listagem de empréstimos, quando a tela está disponível. */
    public void recarregarEmprestimos() {
        if (emprestimosView != null) emprestimosView.recarregar();
    }

    // ─── NavLink ──────────────────────────────────────────────────────────────
    public static class NavLink extends JLabel {
        protected boolean ativo   = false;
        protected boolean hovered = false;
        private final String chave;

        public NavLink(String texto, String chave) {
            super(texto);
            this.chave = chave;
            setFont(UITheme.fontNavItem());
            setForeground(UITheme.TEXT_SECONDARY);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        public String getChave() { return chave; }

        public void setAtivo(boolean ativo) {
            this.ativo = ativo;
            setFont(ativo ? UITheme.fontBodyBold() : UITheme.fontNavItem());
            setForeground(ativo ? UITheme.ACCENT_TEXT : UITheme.TEXT_SECONDARY);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            if (hovered && !ativo)
                UITheme.fillRounded(g2, UITheme.DIVIDER, 0, 0, getWidth(), getHeight(), 6);
            if (ativo)
                UITheme.fillRounded(g2, UITheme.ACCENT_LIGHT, 0, 0, getWidth(), getHeight(), 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ─── Entry point ──────────────────────────────────────────────────────────
    /** Inicia a interface no Event Dispatch Thread do Swing.
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
