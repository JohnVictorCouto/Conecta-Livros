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
import util.OpenLibraryClient;

public class LivroFormDialog extends JDialog {

    private final Livro livro;
    private boolean salvo = false;

    private ComponenteUI.CampoTexto campoIsbn, campoTitulo, campoAutor, campoIdioma, campoCapa, campoMes, campoAno,
                                    campoGenero, campoEditora;
    private JComboBox<Estante>      comboEstante;
    private JCheckBox               checkDisp;

    public LivroFormDialog(Frame owner, Livro livro) {
        super(owner, livro != null ? "Editar Livro" : "Cadastrar Novo Livro", true);
        this.livro = livro;

        setSize(560, 580);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_WHITE);
        setResizable(false);

        add(criarHeader(), BorderLayout.NORTH);
        add(criarForm(),   BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        carregarCombos();

        if (livro != null) {
            preencherCampos();
        } else {
            String isbn = perguntarIsbn();
            if (isbn != null && !isbn.isBlank()) {
                campoIsbn.setText(isbn.trim());
                buscarDadosOpenLibrary(isbn.trim());
            }
        }
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

        campoIsbn    = new ComponenteUI.CampoTexto("ISBN do livro");
        campoTitulo  = new ComponenteUI.CampoTexto("Título do livro");
        campoAutor   = new ComponenteUI.CampoTexto("Nome(s) do(s) autor(es)");
        campoIdioma  = new ComponenteUI.CampoTexto("Idioma");
        campoCapa    = new ComponenteUI.CampoTexto("URL da imagem de capa (opcional)");
        campoMes     = new ComponenteUI.CampoTexto("Mês (1-12)");
        campoAno     = new ComponenteUI.CampoTexto("Ano ex: 2001");
        campoGenero  = new ComponenteUI.CampoTexto("Nome do gênero");
        campoEditora = new ComponenteUI.CampoTexto("Nome da editora");

        comboEstante = estilizarCombo(new JComboBox<>());

        checkDisp = new JCheckBox("Disponível para empréstimo");
        checkDisp.setFont(UITheme.fontBody());
        checkDisp.setForeground(UITheme.TEXT_PRIMARY);
        checkDisp.setBackground(UITheme.BG_WHITE);
        checkDisp.setSelected(true);

        int row = 0;

        addField(form, gbc, "ISBN *",      campoIsbn,   row++);
        addField(form, gbc, "Título *",    campoTitulo, row++);
        addField(form, gbc, "Autor(es) *", campoAutor,  row++);
        addField(form, gbc, "Idioma",      campoIdioma, row++);
        addField(form, gbc, "URL da Capa", campoCapa,   row++);

        // Mês/Ano lado a lado
        JPanel pubPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        pubPanel.setOpaque(false);
        pubPanel.add(campoMes);
        pubPanel.add(campoAno);
        addField(form, gbc, "Mês / Ano de Publicação", pubPanel, row++);

        addField(form, gbc, "Gênero *",  campoGenero,  row++);
        addField(form, gbc, "Editora *", campoEditora, row++);

        addField(form, gbc, "Estante (opcional)", comboEstante, row++);

        gbc.gridy  = row * 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        form.add(checkDisp, gbc);

        // Empurra para cima
        gbc.gridy   = row * 2 + 1;
        gbc.weighty = 1.0;
        gbc.fill    = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private void addField(JPanel form, GridBagConstraints gbc, String label,
                          java.awt.Component comp, int row) {
        gbc.gridy   = row * 2;
        gbc.insets  = new Insets(row == 0 ? 0 : 8, 0, 3, 0);
        gbc.weighty = 0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        form.add(ComponenteUI.labelForm(label), gbc);
        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(comp, gbc);
    }

    private <T> JComboBox<T> estilizarCombo(JComboBox<T> combo) {
        combo.setFont(UITheme.fontBody());
        combo.setBackground(UITheme.BG_WHITE);
        combo.setForeground(UITheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(0, 40));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
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
                List<Estante> st = EstanteDAO.obterTodos();
                SwingUtilities.invokeLater(() -> {
                    comboEstante.removeAllItems();
                    comboEstante.addItem(null); // Opção para livro sem estante
                    for (Estante s : st) comboEstante.addItem(s);
                    if (livro != null) preencherCombos();
                });
                return null;
            }
        }.execute();
    }

    private void preencherCampos() {
        campoIsbn.setText(livro.getIsbn() != null ? livro.getIsbn() : "");
        campoTitulo.setText(livro.getTitle()   != null ? livro.getTitle()   : "");
        campoAutor.setText(livro.getAuthors()  != null ? livro.getAuthors() : "");
        campoIdioma.setText(livro.getIdioma()  != null ? livro.getIdioma()  : "");
        campoCapa.setText(livro.getCover()     != null ? livro.getCover()   : "");
        campoMes.setText(livro.getPublishMonth() > 0 ? String.valueOf(livro.getPublishMonth()) : "");
        campoAno.setText(livro.getPublishYear()  > 0 ? String.valueOf(livro.getPublishYear())  : "");
        Genero genero = GeneroDAO.obterPorId(livro.getGenreId());
        Editora editora = EditoraDAO.obterPorId(livro.getPublisherId());
        campoGenero.setText(genero != null && genero.getName() != null ? genero.getName() : "");
        campoEditora.setText(editora != null && editora.getName() != null ? editora.getName() : "");
        checkDisp.setSelected(livro.isDisponibilidade());
    }

    private void preencherCombos() {
        if (livro.getEstanteId() > 0) {
            for (int i = 0; i < comboEstante.getItemCount(); i++) {
                Estante s = comboEstante.getItemAt(i);
                if (s != null && s.getId() == livro.getEstanteId()) {
                    comboEstante.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            comboEstante.setSelectedIndex(0); // item null = sem estante
        }
    }

    private String perguntarIsbn() {
        return JOptionPane.showInputDialog(this,
            "Informe o ISBN do livro:", "Buscar livro pelo ISBN", JOptionPane.QUESTION_MESSAGE);
    }

    private void buscarDadosOpenLibrary(String isbn) {
        new SwingWorker<Livro, Void>() {
            @Override
            protected Livro doInBackground() {
                try {
                    return OpenLibraryClient.buscarPorISBN(isbn);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Livro dados = get();
                    if (dados != null) {
                        preencherCamposOpenLibrary(dados);
                    } else {
                        JOptionPane.showMessageDialog(LivroFormDialog.this,
                            "Livro não encontrado pelo ISBN informado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LivroFormDialog.this,
                        "Erro ao buscar livro pelo ISBN.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void preencherCamposOpenLibrary(Livro dados) {
        if (dados.getIsbn() != null) campoIsbn.setText(dados.getIsbn());
        if (dados.getTitle() != null) campoTitulo.setText(dados.getTitle());
        if (dados.getAuthors() != null) campoAutor.setText(dados.getAuthors());
        if (dados.getIdioma() != null) campoIdioma.setText(dados.getIdioma());
        if (dados.getCover() != null) campoCapa.setText(dados.getCover());
        if (dados.getPublishYear() > 0) campoAno.setText(String.valueOf(dados.getPublishYear()));
        if (dados.getPublishMonth() > 0) campoMes.setText(String.valueOf(dados.getPublishMonth()));
    }

    private void salvar() {
        String titulo = campoTitulo.getText().trim();
        String nomeGenero = campoGenero.getText().trim();
        String nomeEditora = campoEditora.getText().trim();
        if (titulo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "O título é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nomeGenero.isEmpty() || nomeEditora.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Informe gênero e editora.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Genero genero = obterOuCriarGenero(nomeGenero);
            Editora editora = obterOuCriarEditora(nomeEditora);
            if (genero == null || editora == null) {
                JOptionPane.showMessageDialog(this,
                    "Não foi possível localizar ou cadastrar gênero/editora.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Livro l = livro != null ? livro : new Livro();
            l.setTitle(titulo);
            l.setAuthors(campoAutor.getText().trim());
            l.setCover(campoCapa.getText().trim());
            l.setPublishMonth(campoMes.getText().isBlank() ? 0
                : Integer.parseInt(campoMes.getText().trim()));
            l.setPublishYear(campoAno.getText().isBlank() ? 0
                : Integer.parseInt(campoAno.getText().trim()));
            l.setGenreId(genero.getId());
            l.setPublisherId(editora.getId());

            Estante estanteSel = (Estante) comboEstante.getSelectedItem();
            l.setEstanteId(estanteSel != null ? estanteSel.getId() : 0);

            l.setDisponibilidade(checkDisp.isSelected());
            l.setIsbn(campoIsbn.getText().trim());
            l.setIdioma(campoIdioma.getText().trim());

            boolean ok = livro != null ? LivroDAO.atualizar(l) : LivroDAO.inserir(l);
            if (ok) {
                DadosCache.limpar();
                salvo = true;
                dispose();
                JOptionPane.showMessageDialog(getOwner(),
                    livro != null ? "Livro atualizado com sucesso!" : "Livro cadastrado com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Não foi possível salvar. Verifique os dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Mês e ano devem ser números válidos.", "Atenção", JOptionPane.WARNING_MESSAGE);
        }
    }

    private Genero obterOuCriarGenero(String nome) {
        for (Genero g : GeneroDAO.obterTodos()) {
            if (g.getName() != null && g.getName().equalsIgnoreCase(nome)) {
                return g;
            }
        }

        Genero novo = new Genero();
        novo.setName(nome);
        if (!GeneroDAO.inserir(novo)) {
            return null;
        }

        for (Genero g : GeneroDAO.obterTodos()) {
            if (g.getName() != null && g.getName().equalsIgnoreCase(nome)) {
                return g;
            }
        }
        return null;
    }

    private Editora obterOuCriarEditora(String nome) {
        for (Editora e : EditoraDAO.obterTodos()) {
            if (e.getName() != null && e.getName().equalsIgnoreCase(nome)) {
                return e;
            }
        }

        Editora nova = new Editora();
        nova.setName(nome);
        if (!EditoraDAO.inserir(nova)) {
            return null;
        }

        for (Editora e : EditoraDAO.obterTodos()) {
            if (e.getName() != null && e.getName().equalsIgnoreCase(nome)) {
                return e;
            }
        }
        return null;
    }

    public boolean isSalvo() { return salvo; }
}
