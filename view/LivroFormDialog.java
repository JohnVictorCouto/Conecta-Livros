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
import java.util.List;

public class LivroFormDialog extends JDialog {

    private final Livro livro;
    private boolean salvo = false;

    private ComponenteUI.CampoTexto campoTitulo, campoAutor, campoCapa, campoMes, campoAno;
    private ComponenteUI.AreaTexto  campoDesc;
    private JComboBox<Genero>       comboGenero;
    private JComboBox<Editora>      comboEditora;
    private JComboBox<Estante>      comboEstante;
    private JCheckBox               checkDisp;

    public LivroFormDialog(Frame owner, Livro livro) {
        super(owner, livro != null ? "Editar Livro" : "Cadastrar Novo Livro", true);
        this.livro = livro;

        setSize(600, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_WHITE);
        setResizable(false);

        add(criarHeader(), BorderLayout.NORTH);
        add(criarForm(),   BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        carregarCombos();

        if (livro != null) preencherCampos();
    }

    private JPanel criarHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));

        JLabel titulo = new JLabel(livro != null ? "Editar Livro" : "Cadastrar Novo Livro");
        titulo.setFont(UITheme.fontPageTitle().deriveFont(20f));
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel(livro != null
            ? "Atualize as informações do livro"
            : "Preencha os dados para cadastrar um novo livro");
        sub.setFont(UITheme.fontSubtitle());
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(titulo);
        box.add(Box.createRigidArea(new Dimension(0, 2)));
        box.add(sub);
        p.add(box, BorderLayout.WEST);
        return p;
    }

    private JScrollPane criarForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx   = 0;

        campoTitulo = new ComponenteUI.CampoTexto("Título do livro");
        campoAutor  = new ComponenteUI.CampoTexto("Nome(s) do(s) autor(es)");
        campoCapa   = new ComponenteUI.CampoTexto("URL da imagem de capa (opcional)");
        campoMes    = new ComponenteUI.CampoTexto("Mês (1-12)");
        campoAno    = new ComponenteUI.CampoTexto("Ano ex: 2001");

        campoDesc = new ComponenteUI.AreaTexto();
        JScrollPane scrollDesc = new JScrollPane(campoDesc);
        scrollDesc.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 8));
        scrollDesc.setPreferredSize(new Dimension(0, 80));

        comboGenero  = estilizarCombo(new JComboBox<>());
        comboEditora = estilizarCombo(new JComboBox<>());
        comboEstante = estilizarCombo(new JComboBox<>());

        // Botões de cadastro rápido
        JButton btnNovoGenero = criarBotaoAdd();
        btnNovoGenero.addActionListener(e -> abrirNovoGenero());
        
        JButton btnNovaEditora = criarBotaoAdd();
        btnNovaEditora.addActionListener(e -> abrirNovaEditora());

        checkDisp = new JCheckBox("Disponível para empréstimo");
        checkDisp.setFont(UITheme.fontBody());
        checkDisp.setForeground(UITheme.TEXT_PRIMARY);
        checkDisp.setBackground(UITheme.BG_WHITE);
        checkDisp.setSelected(true);

        int row = 0;

        addField(form, gbc, "Título *",     campoTitulo,  row++);
        addField(form, gbc, "Autor(es) *",  campoAutor,   row++);
        addField(form, gbc, "Descrição",    scrollDesc,   row++);
        addField(form, gbc, "URL da Capa",  campoCapa,    row++);

        // Mês/Ano lado a lado
        JPanel pubPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        pubPanel.setOpaque(false);
        pubPanel.add(campoMes);
        pubPanel.add(campoAno);
        addField(form, gbc, "Mês / Ano de Publicação", pubPanel, row++);

        // Gênero com botão add
        JPanel genPanel = new JPanel(new BorderLayout(8, 0));
        genPanel.setOpaque(false);
        genPanel.add(comboGenero, BorderLayout.CENTER);
        genPanel.add(btnNovoGenero, BorderLayout.EAST);
        addField(form, gbc, "Gênero *",  genPanel,  row++);

        // Editora com botão add
        JPanel edtPanel = new JPanel(new BorderLayout(8, 0));
        edtPanel.setOpaque(false);
        edtPanel.add(comboEditora, BorderLayout.CENTER);
        edtPanel.add(btnNovaEditora, BorderLayout.EAST);
        addField(form, gbc, "Editora *", edtPanel, row++);

        addField(form, gbc, "Estante (opcional)", comboEstante, row++);

        gbc.gridy  = row * 2;
        gbc.insets = new Insets(6, 0, 0, 0);
        form.add(checkDisp, gbc);

        // Empurra para cima
        gbc.gridy  = row * 2 + 1;
        gbc.weighty = 1.0;
        gbc.fill   = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private JButton criarBotaoAdd() {
        JButton btn = new JButton("+") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAA(g2);
                g2.setColor(getModel().isPressed() ? UITheme.ACCENT_HOVER : (getModel().isRollover() ? UITheme.ACCENT_HOVER : UITheme.ACCENT));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("+")) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent() - 1;
                g2.drawString("+", x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Cadastrar novo");
        return btn;
    }

    private void addField(JPanel form, GridBagConstraints gbc, String label, java.awt.Component comp, int row) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 6, 0, 3, 0);
        gbc.weighty = 0;
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        form.add(ComponenteUI.labelForm(label), gbc);
        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(comp, gbc);
    }

    private <T> JComboBox<T> estilizarCombo(JComboBox<T> combo) {
        combo.setFont(UITheme.fontBody());
        combo.setBackground(UITheme.BG_WHITE);
        combo.setForeground(UITheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(200, 40));
        combo.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 8));
        return combo;
    }

    private JPanel criarRodape() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoContorno btnCancelar = new ComponenteUI.BotaoContorno("Cancelar");
        ComponenteUI.BotaoPrimario btnSalvar   = new ComponenteUI.BotaoPrimario(
            livro != null ? "Salvar Alterações" : "Cadastrar Livro");

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        p.add(btnCancelar);
        p.add(btnSalvar);
        return p;
    }

    private void carregarCombos() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                List<Genero>  gs = GeneroDAO.obterTodos();
                List<Editora> es = EditoraDAO.obterTodos();
                List<Estante> st = EstanteDAO.obterTodos();
                SwingUtilities.invokeLater(() -> {
                    comboGenero.removeAllItems();
                    comboEditora.removeAllItems();
                    comboEstante.removeAllItems();
                    comboEstante.addItem(null); // Opção para livro sem estante
                    for (Genero  g : gs) comboGenero.addItem(g);
                    for (Editora e : es) comboEditora.addItem(e);
                    for (Estante s : st) comboEstante.addItem(s);
                    if (livro != null) preencherCombos();
                });
                return null;
            }
        }.execute();
    }

    private void preencherCampos() {
        campoTitulo.setText(livro.getTitle() != null ? livro.getTitle() : "");
        campoAutor.setText(livro.getAuthors() != null ? livro.getAuthors() : "");
        campoDesc.setText(livro.getDescription() != null ? livro.getDescription() : "");
        campoCapa.setText(livro.getCover() != null ? livro.getCover() : "");
        campoMes.setText(livro.getPublishMonth() > 0 ? String.valueOf(livro.getPublishMonth()) : "");
        campoAno.setText(livro.getPublishYear() > 0 ? String.valueOf(livro.getPublishYear()) : "");
        checkDisp.setSelected(livro.isDisponibilidade());
    }

    private void preencherCombos() {
        for (int i = 0; i < comboGenero.getItemCount(); i++)
            if (comboGenero.getItemAt(i).getId() == livro.getGenreId()) { comboGenero.setSelectedIndex(i); break; }
        for (int i = 0; i < comboEditora.getItemCount(); i++)
            if (comboEditora.getItemAt(i).getId() == livro.getPublisherId()) { comboEditora.setSelectedIndex(i); break; }
        for (int i = 0; i < comboEstante.getItemCount(); i++)
            if (comboEstante.getItemAt(i).getId() == livro.getEstanteId()) { comboEstante.setSelectedIndex(i); break; }
    }

    private void abrirNovoGenero() {
        String nome = JOptionPane.showInputDialog(this, "Nome do novo Gênero:", "Cadastrar Gênero", JOptionPane.PLAIN_MESSAGE);
        if (nome != null && !nome.trim().isEmpty()) {
            Genero g = new Genero();
            g.setName(nome.trim());
            if (GeneroDAO.inserir(g)) {
                DadosCache.limpar();
                carregarCombos();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao cadastrar gênero.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirNovaEditora() {
        String nome = JOptionPane.showInputDialog(this, "Nome da nova Editora:", "Cadastrar Editora", JOptionPane.PLAIN_MESSAGE);
        if (nome != null && !nome.trim().isEmpty()) {
            Editora ed = new Editora();
            ed.setName(nome.trim());
            if (EditoraDAO.inserir(ed)) {
                DadosCache.limpar();
                carregarCombos();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao cadastrar editora.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void salvar() {
        String titulo = campoTitulo.getText().trim();
        if (titulo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O título é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (comboGenero.getSelectedItem() == null || comboEditora.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selecione gênero e editora.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Livro l = livro != null ? livro : new Livro();
            l.setTitle(titulo);
            l.setAuthors(campoAutor.getText().trim());
            l.setDescription(campoDesc.getText().trim());
            l.setCover(campoCapa.getText().trim());
            l.setPublishMonth(campoMes.getText().isBlank() ? 0 : Integer.parseInt(campoMes.getText().trim()));
            l.setPublishYear(campoAno.getText().isBlank() ? 0 : Integer.parseInt(campoAno.getText().trim()));
            l.setGenreId(((Genero)  comboGenero.getSelectedItem()).getId());
            l.setPublisherId(((Editora) comboEditora.getSelectedItem()).getId());
            
            Estante estanteSel = (Estante) comboEstante.getSelectedItem();
            l.setEstanteId(estanteSel != null ? estanteSel.getId() : 0);
            
            l.setDisponibilidade(checkDisp.isSelected());

            boolean ok = livro != null ? LivroDAO.atualizar(l) : LivroDAO.inserir(l);
            if (ok) {
                salvo = true;
                dispose();
                JOptionPane.showMessageDialog(getOwner(),
                    livro != null ? "Livro atualizado com sucesso!" : "Livro cadastrado com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível salvar. Verifique os dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Mês e ano devem ser números válidos.", "Atenção", JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean isSalvo() { return salvo; }
}
