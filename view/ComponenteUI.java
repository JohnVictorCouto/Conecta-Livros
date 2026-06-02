package view;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class ComponenteUI {

    // Botão Primário (azul cheio)
    public static class BotaoPrimario extends JButton {
        private boolean hovered = false;
        public BotaoPrimario(String texto) {
            super(texto);
            setFont(UITheme.fontButton());
            setForeground(UITheme.TEXT_WHITE);
            setFocusPainted(false); setBorderPainted(false);
            setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension ps = getPreferredSize();
            setPreferredSize(new Dimension(ps.width + 24, 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, hovered ? UITheme.ACCENT_HOVER : UITheme.ACCENT, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Botão pequeno
    public static class BotaoPrimarioSm extends JButton {
        private boolean hovered = false;
        public BotaoPrimarioSm(String texto) {
            super(texto);
            setFont(UITheme.fontSmallBold());
            setForeground(UITheme.TEXT_WHITE);
            setFocusPainted(false); setBorderPainted(false);
            setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 30));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, hovered ? UITheme.ACCENT_HOVER : UITheme.ACCENT, 0, 0, getWidth(), getHeight(), 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Botão Perigo
    public static class BotaoPerigo extends JButton {
        private boolean hovered = false;
        public BotaoPerigo(String texto) {
            super(texto);
            setFont(UITheme.fontButton());
            setForeground(UITheme.TEXT_WHITE);
            setFocusPainted(false); setBorderPainted(false);
            setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension ps = getPreferredSize();
            setPreferredSize(new Dimension(ps.width + 24, 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, hovered ? new Color(185, 28, 28) : UITheme.DANGER, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Botão Contorno
    public static class BotaoContorno extends JButton {
        protected boolean hovered = false;
        public BotaoContorno(String texto) {
            super(texto);
            setFont(UITheme.fontButton());
            setForeground(UITheme.ACCENT_TEXT);
            setFocusPainted(false); setBorderPainted(false);
            setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension ps = getPreferredSize();
            setPreferredSize(new Dimension(ps.width + 24, 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, hovered ? UITheme.ACCENT_LIGHT : UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 8);
            UITheme.drawRounded(g2, UITheme.ACCENT, 1.5f, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class BotaoContornoSm extends BotaoContorno {
        public BotaoContornoSm(String texto) {
            super(texto);
            setFont(UITheme.fontSmallBold());
            setPreferredSize(new Dimension(140, 30));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, hovered ? UITheme.ACCENT_LIGHT : UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 6);
            UITheme.drawRounded(g2, UITheme.ACCENT, 1.2f, 0, 0, getWidth(), getHeight(), 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    //Botão de Filtro
    public static class BotaoFiltro extends JButton {
        private boolean ativo = false;
        private boolean hovered = false;
        public BotaoFiltro(String texto) {
            super(texto);
            setFont(UITheme.fontBodyBold());
            setFocusPainted(false); setBorderPainted(false);
            setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension ps = getPreferredSize();
            setPreferredSize(new Dimension(ps.width + 20, 32));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
            atualizarCores();
        }
        public void setAtivo(boolean ativo) {
            this.ativo = ativo;
            atualizarCores();
            repaint();
        }
        public boolean isAtivo() { return ativo; }
        private void atualizarCores() {
            setForeground(ativo ? UITheme.TEXT_WHITE : UITheme.TEXT_SECONDARY);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            if (ativo) {
                UITheme.fillRounded(g2, UITheme.ACCENT, 0, 0, getWidth(), getHeight(), 20);
            } else {
                UITheme.fillRounded(g2, hovered ? UITheme.DIVIDER : UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 20);
                UITheme.drawRounded(g2, UITheme.BORDER, 1f, 0, 0, getWidth(), getHeight(), 20);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Campo de Busca com ícone
    public static class CampoBusca extends JPanel {
        private final JTextField campo;
        public CampoBusca(String placeholder, int width) {
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(width, 42));

            JLabel ico = new JLabel("🔍");
            ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            ico.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

            campo = new JTextField() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAA(g2);
                    g2.setColor(UITheme.BG_WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                    if (getText().isEmpty() && !isFocusOwner()) {
                        Graphics2D g3 = (Graphics2D) g.create();
                        g3.setColor(UITheme.TEXT_MUTED);
                        g3.setFont(UITheme.fontBody());
                        FontMetrics fm = g3.getFontMetrics();
                        g3.drawString(placeholder, 4, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                        g3.dispose();
                    }
                }
            };
            campo.setFont(UITheme.fontBody());
            campo.setForeground(UITheme.TEXT_PRIMARY);
            campo.setOpaque(false);
            campo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
            campo.setBackground(UITheme.BG_WHITE);
            add(ico, BorderLayout.WEST);
            add(campo, BorderLayout.CENTER);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 8);
            UITheme.drawRounded(g2, UITheme.BORDER, 1.5f, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
        }
        public JTextField getCampo() { return campo; }
        public String getText() { return campo.getText(); }
        public void addKeyListener(KeyListener kl) { campo.addKeyListener(kl); }
    }

    // Campo de texto simples
    public static class CampoTexto extends JTextField {
        private final String placeholder;
        private boolean focused = false;
        public CampoTexto(String placeholder) {
            this.placeholder = placeholder;
            setFont(UITheme.fontBody());
            setForeground(UITheme.TEXT_PRIMARY);
            setBackground(UITheme.BG_WHITE);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setPreferredSize(new Dimension(200, 40));
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { focused = true;  repaint(); }
                public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 8);
            UITheme.drawRounded(g2, focused ? UITheme.BORDER_FOCUS : UITheme.BORDER, focused ? 2f : 1.5f, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setColor(UITheme.TEXT_MUTED);
                g3.setFont(UITheme.fontBody());
                FontMetrics fm = g3.getFontMetrics();
                g3.drawString(placeholder, 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g3.dispose();
            }
        }
    }

    // Area de texto
    public static class AreaTexto extends JTextArea {
        public AreaTexto() {
            setFont(UITheme.fontBody());
            setForeground(UITheme.TEXT_PRIMARY);
            setBackground(UITheme.BG_WHITE);
            setLineWrap(true); setWrapStyleWord(true);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, UITheme.BG_WHITE, 0, 0, getWidth(), getHeight(), 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Badge de Gênero
    public static class BadgeGenero extends JLabel {
        private final Color bg, fg;
        public BadgeGenero(String genero) {
            super(genero);
            this.bg = UITheme.badgeBg(genero);
            this.fg = UITheme.badgeFg(genero);
            setFont(UITheme.fontBadge());
            setForeground(fg);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            UITheme.fillRounded(g2, bg, 0, 0, getWidth(), getHeight(), 20);
            g2.dispose();
            super.paintComponent(g);
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 4, d.height + 4);
        }
    }

    // Badge de Status (Disponível / Emprestado)
    public static class BadgeStatus extends JLabel {
        public BadgeStatus(boolean disponivel) {
            super(disponivel ? "● Disponível" : "● Emprestado");
            setFont(UITheme.fontSmall());
            setForeground(disponivel ? UITheme.SUCCESS : UITheme.DANGER);
            setOpaque(false);
        }
    }

    // Card branco com sombra
    public static class CardPanel extends JPanel {
        private final int radius;
        public CardPanel() { this(12); }
        public CardPanel(int radius) {
            this.radius = radius;
            setBackground(UITheme.BG_CARD);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            // sombra
            for (int i = 3; i >= 0; i--) {
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(i, i + 1, getWidth() - i * 2, getHeight() - i * 2, radius + 2, radius + 2);
            }
            UITheme.fillRounded(g2, UITheme.BG_CARD, 0, 0, getWidth(), getHeight(), radius);
            g2.dispose();
        }
    }

    // Borda arredondada 
    public static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int radius;
        public RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAA(g2);
            g2.setColor(color); g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
    }

    // Tabela moderna 
    public static void estilizarTabela(JTable t) {
        t.setRowHeight(48);
        t.setFont(UITheme.fontTableCell());
        t.setBackground(UITheme.BG_WHITE);
        t.setSelectionBackground(UITheme.ACCENT_LIGHT);
        t.setSelectionForeground(UITheme.ACCENT_TEXT);
        t.setGridColor(UITheme.DIVIDER);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFocusable(false);
        t.getTableHeader().setFont(UITheme.fontTableHeader());
        t.getTableHeader().setBackground(UITheme.BG_APP);
        t.getTableHeader().setForeground(UITheme.TEXT_SECONDARY);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        t.setDefaultRenderer(Object.class, new RenderTabela());
    }

    static class RenderTabela extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            c.setFont(UITheme.fontTableCell());
            c.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            c.setOpaque(true);
            c.setBackground(sel ? UITheme.ACCENT_LIGHT : (row % 2 == 0 ? UITheme.BG_WHITE : UITheme.BG_APP));
            c.setForeground(sel ? UITheme.ACCENT_TEXT : UITheme.TEXT_PRIMARY);
            return c;
        }
    }

    // ScrollPane limpo
    public static JScrollPane scrollLimpo(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getVerticalScrollBar().setUnitIncrement(16);
        s.setBackground(UITheme.BG_APP);
        return s;
    }

    // Label de formulário
    public static JLabel labelForm(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(UITheme.fontBodyBold());
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    public static JTextArea labelMultilinha(String texto, Font fonte, Color cor, int altura) {
        JTextArea area = new JTextArea(texto);
        area.setFont(fonte);
        area.setForeground(cor);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
        return area;
    }
}
