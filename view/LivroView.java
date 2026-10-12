package view;

import dao.LivroDAO;
import dao.GeneroDAO;
import model.Livro;
import util.DadosCache;
import util.Tradutor;
import model.Genero;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Tela de gerenciamento do catálogo de livros, com navegação por abas,
 * filtros e operações de cadastro sobre as obras.
 */
public class LivroView extends JPanel {
    
    private final Runnable aoAlterarCatalogos;
    private final Runnable aoAlterarLivros;
    private JPanel painelGaleria;
    private JComboBox<Genero> comboGenero;
    private JCheckBox checkDisponiveis;
    private JLabel lblTotalLivros;
    private long cargaGeneration = 0;
    
    // Constantes de tamanho padronizadas
    private static final int CAPA_LARGURA = 118;
    private static final int CAPA_ALTURA = 168;
    private static final int LARGURA_CARD_MINIMA = 238;
    private static final int ESPACAMENTO_CARD = 18;
    
    /**
     * Cria a galeria e recebe callbacks para sincronizar catálogos e livros.
     * @param aoAlterarCatalogos ação chamada quando gêneros ou editoras mudam
     * @param aoAlterarLivros ação chamada quando livros são alterados
     */
    public LivroView(Runnable aoAlterarCatalogos, Runnable aoAlterarLivros) {
        this.aoAlterarCatalogos = aoAlterarCatalogos;
        this.aoAlterarLivros = aoAlterarLivros;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);
        
        // Header com filtros
        add(criarHeader(), BorderLayout.NORTH);
        
        // Galeria de livros
        JScrollPane scroll = ComponenteUI.scrollLimpo(criarGaleria());
        scroll.setBackground(UITheme.BG_APP);
        scroll.getViewport().setBackground(UITheme.BG_APP);
        add(scroll, BorderLayout.CENTER);
        
        carregarGeneros();
        recarregar();
    }
    
    private JPanel criarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_APP);
        header.setBorder(BorderFactory.createEmptyBorder(20, 32, 16, 32));
        
        // Título à esquerda
        JPanel tituloBox = new JPanel();
        tituloBox.setOpaque(false);
        tituloBox.setLayout(new BoxLayout(tituloBox, BoxLayout.Y_AXIS));
        
        JLabel titulo = new JLabel(Tradutor.t("books.title"));
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);
        
        lblTotalLivros = new JLabel(Tradutor.t("state.loading"));
        lblTotalLivros.setFont(UITheme.fontSmall());
        lblTotalLivros.setForeground(UITheme.TEXT_SECONDARY);
        
        tituloBox.add(titulo);
        tituloBox.add(Box.createRigidArea(new Dimension(0, 4)));
        tituloBox.add(lblTotalLivros);
        
        header.add(tituloBox, BorderLayout.WEST);
        
        // Filtros à direita
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filtros.setOpaque(false);
        
        comboGenero = new JComboBox<>();
        comboGenero.setFont(UITheme.fontBody());
        comboGenero.setBackground(UITheme.BG_WHITE);
        comboGenero.setForeground(UITheme.TEXT_PRIMARY);
        comboGenero.setPreferredSize(new Dimension(150, 32));
        comboGenero.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 6));
        comboGenero.addActionListener(e -> recarregar());
        
        checkDisponiveis = new JCheckBox(Tradutor.t("books.only_available"));
        checkDisponiveis.setFont(UITheme.fontSmall());
        checkDisponiveis.setForeground(UITheme.TEXT_PRIMARY);
        checkDisponiveis.setBackground(UITheme.BG_APP);
        checkDisponiveis.addActionListener(e -> recarregar());
        
        ComponenteUI.BotaoContorno btnRecarregar = new ComponenteUI.BotaoContorno(Tradutor.t("action.reload"));
        btnRecarregar.addActionListener(e -> recarregar());
        ComponenteUI.BotaoContorno btnCadastros =
            new ComponenteUI.BotaoContorno(Tradutor.t("books.manage_catalogs"));
        btnCadastros.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new CatalogoDialog(owner, aoAlterarCatalogos).setVisible(true);
        });
        
        filtros.add(comboGenero);
        filtros.add(checkDisponiveis);
        filtros.add(btnRecarregar);
        filtros.add(btnCadastros);
        
        header.add(filtros, BorderLayout.EAST);
        
        return header;
    }

    /** Atualiza as opções de gênero usadas pelos filtros do catálogo. */
    public void recarregarCatalogos() {
        carregarGeneros();
        recarregar();
    }
    
    private JPanel criarGaleria() {
        painelGaleria = new PainelGaleria();
        painelGaleria.setLayout(new GridLayout(0, 3, ESPACAMENTO_CARD, ESPACAMENTO_CARD));
        painelGaleria.setBackground(UITheme.BG_APP);
        painelGaleria.setBorder(BorderFactory.createEmptyBorder(12, 28, 28, 28));
        painelGaleria.setAlignmentX(LEFT_ALIGNMENT);
        painelGaleria.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int larguraUtil = painelGaleria.getWidth() - 56;
                int colunas = Math.max(1, (larguraUtil + ESPACAMENTO_CARD)
                    / (LARGURA_CARD_MINIMA + ESPACAMENTO_CARD));
                GridLayout layout = (GridLayout) painelGaleria.getLayout();
                if (layout.getColumns() != colunas) {
                    painelGaleria.setLayout(new GridLayout(0, colunas,
                        ESPACAMENTO_CARD, ESPACAMENTO_CARD));
                    painelGaleria.revalidate();
                }
            }
        });
        
        return painelGaleria;
    }

    private static class PainelGaleria extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(760, 520);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 24, 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
    
    private void carregarGeneros() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                List<Genero> generos = GeneroDAO.obterTodos();
                SwingUtilities.invokeLater(() -> {
                    comboGenero.removeAllItems();
                    comboGenero.addItem(new Genero(0, Tradutor.t("search.genre_placeholder")));
                    for (Genero g : generos) {
                        comboGenero.addItem(g);
                    }
                });
                return null;
            }
        }.execute();
    }
    
    /** Atualiza as obras apresentadas na galeria. */
    public void recarregar() {
        Genero generoSelecionado = (Genero) comboGenero.getSelectedItem();
        final int generoId = generoSelecionado != null ? generoSelecionado.getId() : 0;
        final boolean somenteDisponiveis = checkDisponiveis.isSelected();
        final long generation = ++cargaGeneration;

        new SwingWorker<List<Livro>, Void>() {
            protected List<Livro> doInBackground() {
                List<Livro> livros = LivroDAO.obterTodos();
                if (generoId > 0) {
                    livros.removeIf(l -> l.getGenreId() != generoId);
                }
                if (somenteDisponiveis) {
                    livros.removeIf(l -> !l.isDisponibilidade());
                }
                return livros;
            }

            protected void done() {
                if (generation != cargaGeneration) return;
                try {
                    List<Livro> livros = get();
                    painelGaleria.removeAll();
                    for (Livro livro : livros) painelGaleria.add(criarCardLivro(livro));
                    lblTotalLivros.setText(Tradutor.formatar("books.count", livros.size()));
                    painelGaleria.revalidate();
                    painelGaleria.repaint();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LivroView.this,
                        Tradutor.t("books.load_error"), Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private JPanel criarCardLivro(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        card.setPreferredSize(new Dimension(LARGURA_CARD_MINIMA, 430));
        
        // Container para a capa para evitar que ela estique horizontalmente
        JPanel capaContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        capaContainer.setOpaque(false);
        
        // Capa - tamanho padronizado
        ImagemCapa capa = new ImagemCapa(
            livro.getCover(),
            livro.getId(),
            livro.getTitle()
        );
        capa.setPreferredSize(new Dimension(CAPA_LARGURA, CAPA_ALTURA));
        capa.setRaioCantos(8);
        capaContainer.add(capa);
        
        // Informações
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
        
        JTextArea lblTitulo = ComponenteUI.labelMultilinha(
            livro.getTitle() != null ? livro.getTitle() : Tradutor.t("value.no_title"),
            UITheme.fontBookTitle(),
            UITheme.TEXT_PRIMARY,
            54
        );
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);
        
        JTextArea lblAutor = ComponenteUI.labelMultilinha(
            livro.getAuthors() != null && !livro.getAuthors().isBlank()
                ? livro.getAuthors() : Tradutor.t("value.unknown"),
            UITheme.fontBookAuthor(),
            UITheme.TEXT_SECONDARY,
            54
        );
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);
        
        String generoNome = DadosCache.getNomeGenero(livro.getGenreId());
        ComponenteUI.BadgeGenero badge = new ComponenteUI.BadgeGenero(generoNome);
        badge.setAlignmentX(LEFT_ALIGNMENT);
        
        ComponenteUI.BadgeStatus status = new ComponenteUI.BadgeStatus(livro.getExemplaresDisponiveis() > 0);
        status.setAlignmentX(LEFT_ALIGNMENT);

        JPanel metadados = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        metadados.setOpaque(false);
        metadados.setAlignmentX(LEFT_ALIGNMENT);
        metadados.add(badge);
        if (livro.getPublishYear() > 0) {
            JLabel ano = new JLabel("  ·  " + livro.getPublishYear());
            ano.setFont(UITheme.fontSmall());
            ano.setForeground(UITheme.TEXT_MUTED);
            metadados.add(ano);
        }

        JLabel resumoExemplares = new JLabel(Tradutor.formatar(
            "books.copies_summary",
            livro.getExemplaresDisponiveis(),
            livro.getExemplaresEmprestados()
        ));
        resumoExemplares.setFont(UITheme.fontSmall());
        resumoExemplares.setForeground(UITheme.TEXT_SECONDARY);
        resumoExemplares.setAlignmentX(LEFT_ALIGNMENT);
        resumoExemplares.setToolTipText(Tradutor.t("copies.manage"));
        
        info.add(lblTitulo);
        info.add(Box.createRigidArea(new Dimension(0, 3)));
        info.add(lblAutor);
        info.add(Box.createRigidArea(new Dimension(0, 7)));
        info.add(metadados);
        info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(status);
        info.add(Box.createRigidArea(new Dimension(0, 3)));
        info.add(resumoExemplares);
        info.add(Box.createRigidArea(new Dimension(0, 2)));
        if (livro.getIdioma() != null && !livro.getIdioma().isBlank()) {
            JLabel lblIdioma = new JLabel(livro.getIdioma());
            lblIdioma.setFont(UITheme.fontSmall());
            lblIdioma.setForeground(UITheme.TEXT_MUTED);
            lblIdioma.setAlignmentX(LEFT_ALIGNMENT);
            info.add(Box.createRigidArea(new Dimension(0, 4)));
            info.add(lblIdioma);
        }
        info.add(Box.createVerticalGlue());
        
        // Botões
        JPanel botaoPanel = new JPanel();
        botaoPanel.setOpaque(false);
        botaoPanel.setLayout(new BoxLayout(botaoPanel, BoxLayout.Y_AXIS));

        JButton btnAcoes = new JButton(Tradutor.t("books.card_actions"));
        btnAcoes.setFont(UITheme.fontSmallBold());
        btnAcoes.setForeground(UITheme.ACCENT_TEXT);
        btnAcoes.setBackground(UITheme.BG_WHITE);
        btnAcoes.setFocusPainted(false);
        btnAcoes.setBorder(new ComponenteUI.RoundedBorder(UITheme.ACCENT, 6));
        btnAcoes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAcoes.setPreferredSize(new Dimension(132, 30));

        JMenuItem itemEditar = new JMenuItem(Tradutor.t("search.edit_book"));
        itemEditar.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            LivroFormDialog dialog = new LivroFormDialog(topFrame, livro);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                aoAlterarLivros.run();
            }
        });
        JMenuItem itemExemplares = new JMenuItem(Tradutor.t("copies.manage"));
        itemExemplares.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            new ExemplarDialog(topFrame, livro, aoAlterarLivros).setVisible(true);
        });

        JMenuItem itemEmprestar = null;
        if (livro.getExemplaresDisponiveis() > 0) {
            itemEmprestar = new JMenuItem(Tradutor.t("action.loan"));
            itemEmprestar.addActionListener(e ->
                LivroAcoes.registrarEmprestimo(this, livro.getId(), this::recarregar));
        }

        JMenuItem itemExcluir = new JMenuItem(Tradutor.t("action.delete"));
        itemExcluir.setForeground(UITheme.DANGER);
        itemExcluir.addActionListener(e ->
            LivroAcoes.excluir(this, livro, aoAlterarLivros));

        JPopupMenu popupAcoes = new JPopupMenu();
        popupAcoes.setBackground(UITheme.BG_WHITE);
        popupAcoes.setBorder(BorderFactory.createCompoundBorder(
            new ComponenteUI.RoundedBorder(UITheme.BORDER, 8),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        popupAcoes.add(itemEditar);
        popupAcoes.add(itemExemplares);
        if (itemEmprestar != null) {
            popupAcoes.add(itemEmprestar);
        }
        popupAcoes.addSeparator();
        popupAcoes.add(itemExcluir);
        btnAcoes.addActionListener(e -> popupAcoes.show(btnAcoes, 0, btnAcoes.getHeight()));

        JPanel acoesLivro = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        acoesLivro.setOpaque(false);
        acoesLivro.add(btnAcoes);
        acoesLivro.setAlignmentX(LEFT_ALIGNMENT);
        botaoPanel.add(acoesLivro);
        
        JPanel acoesEmprestimo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acoesEmprestimo.setOpaque(false);
        if (livro.getExemplaresEmprestados() > 0) {
            ComponenteUI.BotaoContornoSm btn = new ComponenteUI.BotaoContornoSm(Tradutor.t("action.return"));
            btn.addActionListener(e ->
                LivroAcoes.registrarDevolucao(this, livro.getId(), this::recarregar));
            acoesEmprestimo.add(btn);
        }
        acoesEmprestimo.setAlignmentX(RIGHT_ALIGNMENT);
        botaoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        botaoPanel.add(acoesEmprestimo);
        
        card.add(capaContainer, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.add(botaoPanel, BorderLayout.SOUTH);
        
        return card;
    }
}
