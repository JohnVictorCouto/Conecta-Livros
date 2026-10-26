package view;

import dao.LivroDAO;
import dao.GeneroDAO;
import dao.EditoraDAO;
import model.Livro;
import model.Genero;
import model.Editora;

import javax.swing.*;
import java.awt.*;
import util.DadosCache;
import util.IsbnUtils;
import util.GoogleBooksClient;
import util.Tradutor;

/**
 * Formulário de inclusão ou edição de um livro, com carregamento de catálogos
 * e gravação fora da thread de eventos da interface.
 */
public class LivroFormDialog extends JDialog {

    private static class DadosFormulario {
        private final Genero genero;
        private final Editora editora;

        DadosFormulario(Genero genero, Editora editora) {
            this.genero = genero;
            this.editora = editora;
        }
    }

    private final Livro livro;
    private boolean salvo = false;

    private ComponenteUI.CampoTexto campoIsbn, campoTitulo, campoAutor, campoIdioma, campoCapa, campoAno,
                                    campoGenero, campoEditora;

    /**
     * Abre o formulário para incluir um livro ou editar o livro recebido.
     * @param owner janela proprietária do formulário
     * @param livro obra a editar, ou {@code null} para iniciar um novo cadastro
     */
    public LivroFormDialog(Frame owner, Livro livro) {
        super(owner, livro != null ? Tradutor.t("books_form.edit_title") : Tradutor.t("books_form.new_title"), true);
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
                buscarDadosGoogleBooks(isbn.trim());
            }
        }
    }

    private JPanel criarHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));

        JLabel titulo = new JLabel(livro != null
            ? Tradutor.t("books_form.edit_title") : Tradutor.t("books_form.new_title"));
        titulo.setFont(UITheme.fontPageTitle().deriveFont(20f));
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel(livro != null
            ? Tradutor.t("books_form.edit_subtitle")
            : Tradutor.t("books_form.new_subtitle"));
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

        campoIsbn    = new ComponenteUI.CampoTexto(Tradutor.t("books_form.isbn_placeholder"));
        campoTitulo  = new ComponenteUI.CampoTexto(Tradutor.t("books_form.title_placeholder"));
        campoAutor   = new ComponenteUI.CampoTexto(Tradutor.t("books_form.author_placeholder"));
        campoIdioma  = new ComponenteUI.CampoTexto(Tradutor.t("books_form.language_placeholder"));
        campoCapa    = new ComponenteUI.CampoTexto(Tradutor.t("books_form.cover_placeholder"));
        campoAno     = new ComponenteUI.CampoTexto(Tradutor.t("books_form.year_placeholder"));
        campoGenero  = new ComponenteUI.CampoTexto(Tradutor.t("books_form.genre_placeholder"));
        campoEditora = new ComponenteUI.CampoTexto(Tradutor.t("books_form.publisher_placeholder"));

        int row = 0;

        addField(form, gbc, Tradutor.t("books_form.isbn_label"),    campoIsbn,   row++);
        addField(form, gbc, Tradutor.t("books_form.title_label"),   campoTitulo, row++);
        addField(form, gbc, Tradutor.t("books_form.author_label"),  campoAutor,  row++);
        addField(form, gbc, Tradutor.t("books_form.language_label"),campoIdioma, row++);
        addField(form, gbc, Tradutor.t("books_form.cover_label"),   campoCapa,   row++);

        addField(form, gbc, Tradutor.t("books_form.publication_year"), campoAno, row++);

        addField(form, gbc, Tradutor.t("books_form.genre_label"),    campoGenero,  row++);
        addField(form, gbc, Tradutor.t("books_form.publisher_label"),campoEditora, row++);

        // Empurra para cima
        gbc.gridy   = row * 2;
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

    private JPanel criarRodape() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoContorno btnCancelar = new ComponenteUI.BotaoContorno(Tradutor.t("action.cancel"));
        ComponenteUI.BotaoPrimario btnSalvar   = new ComponenteUI.BotaoPrimario(
            livro != null ? Tradutor.t("action.save_changes") : Tradutor.t("action.register_book"));

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        p.add(btnCancelar);
        p.add(btnSalvar);
        return p;
    }

    private void carregarCombos() {
        new SwingWorker<DadosFormulario, Void>() {
            protected DadosFormulario doInBackground() {
                return new DadosFormulario(
                    livro != null ? GeneroDAO.obterPorId(livro.getGenreId()) : null,
                    livro != null ? EditoraDAO.obterPorId(livro.getPublisherId()) : null
                );
            }

            protected void done() {
                try {
                    DadosFormulario dados = get();
                    if (livro != null) {
                        if (dados.genero != null) campoGenero.setText(dados.genero.getName());
                        if (dados.editora != null) campoEditora.setText(dados.editora.getName());
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LivroFormDialog.this,
                        Tradutor.t("books_form.loading_error"),
                        Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void preencherCampos() {
        campoIsbn.setText(livro.getIsbn() != null ? livro.getIsbn() : "");
        campoTitulo.setText(livro.getTitle()   != null ? livro.getTitle()   : "");
        campoAutor.setText(livro.getAuthors()  != null ? livro.getAuthors() : "");
        campoIdioma.setText(livro.getIdioma()  != null ? livro.getIdioma()  : "");
        campoCapa.setText(livro.getCover()     != null ? livro.getCover()   : "");
        campoAno.setText(livro.getPublishYear()  > 0 ? String.valueOf(livro.getPublishYear())  : "");
    }

    private String perguntarIsbn() {
        return JOptionPane.showInputDialog(this,
            Tradutor.t("books_form.ask_isbn"), Tradutor.t("books_form.search_isbn"),
            JOptionPane.QUESTION_MESSAGE);
    }

    private void buscarDadosGoogleBooks(String isbn) {
        new SwingWorker<Livro, Void>() {
            @Override
            protected Livro doInBackground() throws Exception {
                return GoogleBooksClient.buscarPorISBN(isbn);
            }

            @Override
            protected void done() {
                try {
                    Livro dados = get();
                    if (dados != null) {
                        preencherCamposGoogleBooks(dados);
                    } else {
                        JOptionPane.showMessageDialog(LivroFormDialog.this,
                            Tradutor.t("books_form.not_found"),
                            Tradutor.t("message.info"), JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(LivroFormDialog.this,
                        Tradutor.t("books_form.search_error"),
                        Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("Erro ao buscar livro na Google Books API: " + cause.getMessage());
                    JOptionPane.showMessageDialog(LivroFormDialog.this,
                        Tradutor.t("books_form.search_error"),
                        Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void preencherCamposGoogleBooks(Livro dados) {
        preencherCampoSeVazio(campoIsbn, dados.getIsbn());
        preencherCampoSeVazio(campoTitulo, dados.getTitle());
        preencherCampoSeVazio(campoAutor, dados.getAuthors());
        preencherCampoSeVazio(campoIdioma, dados.getIdioma());
        preencherCampoSeVazio(campoCapa, dados.getCover());
        preencherCampoSeVazio(campoGenero, dados.getGeneroCatalogo());
        preencherCampoSeVazio(campoEditora, dados.getEditoraCatalogo());
        if (campoAno.getText().isBlank() && dados.getPublishYear() > 0) {
            campoAno.setText(String.valueOf(dados.getPublishYear()));
        }
    }

    private void preencherCampoSeVazio(JTextField campo, String valor) {
        if (valor != null && !valor.isBlank() && campo.getText().isBlank()) {
            campo.setText(valor.trim());
        }
    }

    private void salvar() {
        String titulo = campoTitulo.getText().trim();
        String nomeGenero = campoGenero.getText().trim();
        String nomeEditora = campoEditora.getText().trim();
        if (titulo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.title_required"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nomeGenero.isEmpty() || nomeEditora.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.genre_publisher_required"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String isbnDigitado = campoIsbn.getText().trim();
        if (!isbnDigitado.isEmpty() && !IsbnUtils.isValid(isbnDigitado)) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.isbn_invalid"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ano;
        try {
            ano = campoAno.getText().isBlank() ? 0 : Integer.parseInt(campoAno.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.invalid_year"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!campoAno.getText().isBlank() && (ano < 1901 || ano > 2155)) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.year_range"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        Genero genero = obterOuCriarGenero(nomeGenero);
        Editora editora = obterOuCriarEditora(nomeEditora);
        if (genero == null || editora == null) {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.genre_publisher_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Livro l = livro != null ? livro : new Livro();
        l.setTitle(titulo);
        l.setAuthors(campoAutor.getText().trim());
        l.setCover(campoCapa.getText().trim());
        l.setPublishYear(ano);
        l.setGenreId(genero.getId());
        l.setPublisherId(editora.getId());

        l.setIsbn(IsbnUtils.clean(isbnDigitado));
        l.setIdioma(campoIdioma.getText().trim());

        boolean ok = livro != null ? LivroDAO.atualizar(l) : LivroDAO.inserir(l);
        if (ok) {
            DadosCache.limpar();
            salvo = true;
            dispose();
            JOptionPane.showMessageDialog(getOwner(),
                Tradutor.t(livro != null ? "books_form.updated" : "books_form.created"),
                Tradutor.t("message.success"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                Tradutor.t("books_form.save_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
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

    /** @return {@code true} se o formulário concluiu a gravação com sucesso */
    public boolean isSalvo() { return salvo; }
}
