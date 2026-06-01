package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Janela principal da aplicacao e controle da navegacao entre telas
public class MainFrame extends JFrame {

    private CardLayout   cardLayout;
    private JPanel       painelConteudo;

    private DashboardView    dashboardView;
    private LivroView        livroView;
    private EstanteView      estanteView;
    private ListaEscolarView listaView;
    private BuscaView        buscaView;

    private NavLink[] links;
    private NavLink   linkAtivo;

    public MainFrame() {
        setTitle("Conecta Livros");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 740);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        setBackground(UITheme.BG_APP);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        setLayout(new BorderLayout(0, 0));
        add(criarNavbar(), BorderLayout.NORTH);

        cardLayout     = new CardLayout();
        painelConteudo = new JPanel(cardLayout);
        painelConteudo.setBackground(UITheme.BG_APP);

        dashboardView = new DashboardView(this);
        livroView     = new LivroView();
        estanteView   = new EstanteView();
        listaView     = new ListaEscolarView();
        buscaView     = new BuscaView();

        painelConteudo.add(dashboardView, "dashboard");
        painelConteudo.add(livroView,     "livros");
        painelConteudo.add(estanteView,   "estantes");
        painelConteudo.add(listaView,     "listas");
        painelConteudo.add(buscaView,     "busca");

        add(painelConteudo, BorderLayout.CENTER);
        cardLayout.show(painelConteudo, "dashboard");
    }

    // Barra de navegacao superior
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

        // Logo e nome do sistema
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logo.setOpaque(false);
        logo.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel icLogo = new JLabel();

        ImageIcon logoIcon = new ImageIcon(
                getClass().getResource("/images/Icon.png")
        );

        Image imagem = logoIcon.getImage()
                .getScaledInstance(30, 30, Image.SCALE_SMOOTH);

        icLogo.setIcon(new ImageIcon(imagem));
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

        JLabel txtLogo = new JLabel("Conecta Livros");
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

        // Links centrais de navegacao
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 10));
        linksPanel.setOpaque(false);

        NavLink lLivros   = new NavLink("Livros",    "livros");
        NavLink lEstantes = new NavLink("Estantes",  "estantes");
        NavLink lListas   = new NavLink("Listas",    "listas");
        NavLink lBusca    = new NavLink("Busca",     "busca");

        links = new NavLink[]{lLivros, lEstantes, lListas, lBusca};
        linkAtivo = null;

        for (NavLink nl : links) {
            nl.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    ativarLink(nl);
                    cardLayout.show(painelConteudo, nl.getChave());
                }
            });
            linksPanel.add(nl);
        }

        nav.add(linksPanel, BorderLayout.CENTER);

        // Botao de cadastro no lado direito
        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        direita.setOpaque(false);

        ComponenteUI.BotaoPrimario btnCadastrar = new ComponenteUI.BotaoPrimario("+ Cadastrar Livro");
        btnCadastrar.addActionListener(e -> abrirDialogLivro());
        direita.add(btnCadastrar);
        nav.add(direita, BorderLayout.EAST);

        return nav;
    }

    // Metodos publicos de navegacao
    public void ativarLink(NavLink link) {
        if (linkAtivo != null) linkAtivo.setAtivo(false);
        if (link != null) link.setAtivo(true);
        linkAtivo = link;
    }

    public void irParaDashboard() {
        ativarLink(null);
        cardLayout.show(painelConteudo, "dashboard");
    }

    public void irParaBusca(String termo) {
        for (NavLink nl : links)
            if ("busca".equals(nl.getChave())) { ativarLink(nl); break; }
        buscaView.buscar(termo);
        cardLayout.show(painelConteudo, "busca");
    }

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

    public void recarregarDashboard() {
        if (dashboardView != null) dashboardView.recarregar();
    }

    // Link customizado da barra de navegacao
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

    // Ponto de entrada da aplicacao
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}



