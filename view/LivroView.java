package view;

import dao.LivroDAO;
import dao.GeneroDAO;
import model.Livro;
import model.Genero;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Visualização de Livros com galeria visual de capas
 */
public class LivroView extends JPanel {
    
    private JPanel painelGaleria;
    private JComboBox<Genero> comboGenero;
    private JCheckBox checkDisponiveis;
    private JLabel lblTotalLivros;
    
    // Constantes de tamanho padronizadas
    private static final int CAPA_LARGURA = 140;
    private static final int CAPA_ALTURA = 200;
    
    public LivroView() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);
        
        // Header com filtros
        add(criarHeader(), BorderLayout.NORTH);
        
        // Galeria de livros
        JScrollPane scroll = ComponenteUI.scrollLimpo(criarGaleria());
        scroll.setBackground(UITheme.BG_APP);
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
        
        JLabel titulo = new JLabel("Galeria de Livros");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);
        
        lblTotalLivros = new JLabel("Carregando...");
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
        
        checkDisponiveis = new JCheckBox("Apenas disponíveis");
        checkDisponiveis.setFont(UITheme.fontSmall());
        checkDisponiveis.setForeground(UITheme.TEXT_PRIMARY);
        checkDisponiveis.setBackground(UITheme.BG_APP);
        checkDisponiveis.addActionListener(e -> recarregar());
        
        ComponenteUI.BotaoContorno btnRecarregar = new ComponenteUI.BotaoContorno("Recarregar");
        btnRecarregar.addActionListener(e -> recarregar());
        
        filtros.add(comboGenero);
        filtros.add(checkDisponiveis);
        filtros.add(btnRecarregar);
        
        header.add(filtros, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel criarGaleria() {
        painelGaleria = new JPanel();
        painelGaleria.setLayout(new GridLayout(0, 3, 16, 16));
        painelGaleria.setBackground(UITheme.BG_APP);
        painelGaleria.setBorder(BorderFactory.createEmptyBorder(16, 32, 32, 32));
        painelGaleria.setAlignmentX(LEFT_ALIGNMENT);
        
        return painelGaleria;
    }
    
    private void carregarGeneros() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                List<Genero> generos = GeneroDAO.obterTodos();
                SwingUtilities.invokeLater(() -> {
                    comboGenero.removeAllItems();
                    comboGenero.addItem(null);
                    for (Genero g : generos) {
                        comboGenero.addItem(g);
                    }
                });
                return null;
            }
        }.execute();
    }
    
    public void recarregar() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                try {
                    List<Livro> livros = LivroDAO.obterTodos();
                    
                    // Filtrar por gênero se selecionado
                    Genero generoSel = (Genero) comboGenero.getSelectedItem();
                    if (generoSel != null) {
                        livros.removeIf(l -> l.getGenreId() != generoSel.getId());
                    }
                    
                    // Filtrar por disponibilidade
                    if (checkDisponiveis.isSelected()) {
                        livros.removeIf(l -> !l.isDisponibilidade());
                    }
                    
                    final List<Livro> livrosFinal = livros;
                    SwingUtilities.invokeLater(() -> {
                        painelGaleria.removeAll();
                        for (Livro livro : livrosFinal) {
                            painelGaleria.add(criarCardLivro(livro));
                        }
                        lblTotalLivros.setText(livrosFinal.size() + " livro" + (livrosFinal.size() != 1 ? "s" : ""));
                        painelGaleria.revalidate();
                        painelGaleria.repaint();
                    });
                    
                    return null;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
    
    private JPanel criarCardLivro(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(220, 420)); // Tamanho fixo para o card
        
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
        capa.setRaioCantos(6);
        capaContainer.add(capa);
        
        // Informações
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        JLabel lblTitulo = new JLabel(livro.getTitle() != null ? livro.getTitle() : "Sem título");
        lblTitulo.setFont(UITheme.fontSmallBold());
        lblTitulo.setForeground(UITheme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);
        lblTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel lblAutor = new JLabel(livro.getAuthors() != null ? livro.getAuthors() : "Desconhecido");
        lblAutor.setFont(UITheme.fontSmall());
        lblAutor.setForeground(UITheme.TEXT_SECONDARY);
        lblAutor.setAlignmentX(LEFT_ALIGNMENT);
        
        String generoNome = DadosCache.getNomeGenero(livro.getGenreId());
        ComponenteUI.BadgeGenero badge = new ComponenteUI.BadgeGenero(generoNome);
        badge.setAlignmentX(LEFT_ALIGNMENT);
        
        ComponenteUI.BadgeStatus status = new ComponenteUI.BadgeStatus(livro.isDisponibilidade());
        status.setAlignmentX(LEFT_ALIGNMENT);
        
        info.add(lblTitulo);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(lblAutor);
        info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(badge);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(status);
        info.add(Box.createVerticalGlue());
        
        // Botões
        JPanel botaoPanel = new JPanel(new BorderLayout(8, 0));
        botaoPanel.setOpaque(false);

        JButton btnEdit = new JButton("✏️");
        btnEdit.setToolTipText("Editar livro");
        btnEdit.setFocusPainted(false);
        btnEdit.setBorderPainted(false);
        btnEdit.setContentAreaFilled(false);
        btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEdit.setPreferredSize(new Dimension(36, 30));
        btnEdit.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            LivroFormDialog dialog = new LivroFormDialog(topFrame, livro);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                DadosCache.limpar();
                recarregar();
            }
        });
        botaoPanel.add(btnEdit, BorderLayout.WEST);
        
        if (livro.isDisponibilidade()) {
            ComponenteUI.BotaoPrimarioSm btn = new ComponenteUI.BotaoPrimarioSm("Emprestar");
            btn.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar empréstimo de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), false);
                    recarregar();
                }
            });
            botaoPanel.add(btn, BorderLayout.CENTER);
        } else {
            ComponenteUI.BotaoContornoSm btn = new ComponenteUI.BotaoContornoSm("Devolver");
            btn.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this,
                    "Registrar devolução de \"" + livro.getTitle() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    LivroDAO.atualizarDisponibilidade(livro.getId(), true);
                    recarregar();
                }
            });
            botaoPanel.add(btn, BorderLayout.CENTER);
        }
        
        card.add(capaContainer, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.add(botaoPanel, BorderLayout.SOUTH);
        
        return card;
    }
}
