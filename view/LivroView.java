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
        painelGaleria.setLayout(new GridLayout(0, 4, 16, 16));
        painelGaleria.setBackground(UITheme.BG_APP);
        painelGaleria.setBorder(BorderFactory.createEmptyBorder(16, 32, 32, 32));
        painelGaleria.setAlignmentX(LEFT_ALIGNMENT);
        
        return painelGaleria;
    }
    
    private void carregarGeneros() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<Genero> generos = GeneroDAO.obterTodos();
                SwingUtilities.invokeLater(() -> {
                    comboGenero.addItem(new Genero(0, "Todos os Gêneros"));
                    for (Genero g : generos) {
                        comboGenero.addItem(g);
                    }
                });
                return null;
            }
        }.execute();
    }
    
    public void recarregar() {
        new SwingWorker<List<Livro>, Void>() {
            @Override
            protected List<Livro> doInBackground() {
                List<Livro> livros;
                
                Genero generoSelecionado = (Genero) comboGenero.getSelectedItem();
                
                if (generoSelecionado != null && generoSelecionado.getId() > 0) {
                    livros = LivroDAO.obterPorGenero(generoSelecionado.getId());
                } else {
                    livros = LivroDAO.obterTodos();
                }
                
                if (checkDisponiveis.isSelected()) {
                    livros.removeIf(l -> !l.isDisponibilidade());
                }
                
                return livros;
            }
            
            @Override
            protected void done() {
                try {
                    List<Livro> livros = get();
                    painelGaleria.removeAll();
                    
                    for (Livro livro : livros) {
                        painelGaleria.add(criarCardLivro(livro));
                    }
                    
                    lblTotalLivros.setText(livros.size() + " livro(s) encontrado(s)");
                    
                    painelGaleria.revalidate();
                    painelGaleria.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
    
    private JPanel criarCardLivro(Livro livro) {
        ComponenteUI.CardPanel card = new ComponenteUI.CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Capa
        ImagemCapa capa = new ImagemCapa(
            livro.getCover(),
            livro.getId(),
            livro.getTitle()
        );
        capa.setPreferredSize(new Dimension(100, 140));
        capa.setRaioCantos(6);
        
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
        JPanel botaoPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        botaoPanel.setOpaque(false);

        JButton btnEdit = new JButton("✏️");
        btnEdit.setToolTipText("Editar livro");
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            LivroFormDialog dialog = new LivroFormDialog(topFrame, livro);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                DadosCache.limpar();
                recarregar();
            }
        });
        botaoPanel.add(btnEdit);
        
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
        
        card.add(capa, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.add(botaoPanel, BorderLayout.SOUTH);
        
        return card;
    }
}
