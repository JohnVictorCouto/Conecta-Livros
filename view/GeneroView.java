package view;

import dao.GeneroDAO;
import model.Genero;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class GeneroView extends JPanel {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private ComponenteUI.CampoBusca campoBusca;
    private JLabel labelContagem;
    private List<Genero> cache;

    private static final String[] COLUNAS = {"ID", "Nome do Gênero"};

    public GeneroView() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        add(criarHeader(), BorderLayout.NORTH);
        add(criarCorpo(),  BorderLayout.CENTER);
        carregarDados();
    }

    private JPanel criarHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel esq = new JPanel();
        esq.setOpaque(false);
        esq.setLayout(new BoxLayout(esq, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Gêneros Literários");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        labelContagem = new JLabel("Carregando...");
        labelContagem.setFont(UITheme.fontSubtitle());
        labelContagem.setForeground(UITheme.TEXT_SECONDARY);

        esq.add(titulo);
        esq.add(Box.createRigidArea(new Dimension(0, 3)));
        esq.add(labelContagem);
        h.add(esq, BorderLayout.WEST);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dir.setOpaque(false);

        campoBusca = new ComponenteUI.CampoBusca("Buscar gênero...", 200);
        campoBusca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrar(campoBusca.getText()); }
        });

        ComponenteUI.BotaoPrimario btnNovo = new ComponenteUI.BotaoPrimario("+ Novo Gênero");
        btnNovo.addActionListener(e -> abrirFormulario(null));

        dir.add(campoBusca);
        dir.add(btnNovo);
        h.add(dir, BorderLayout.EAST);
        return h;
    }

    private JPanel criarCorpo() {
        ComponenteUI.CardPanel corpo = new ComponenteUI.CardPanel();
        corpo.setLayout(new BorderLayout());

        modeloTabela = new DefaultTableModel(COLUNAS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tabela = new JTable(modeloTabela);
        ComponenteUI.estilizarTabela(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(500);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        acoes.setBackground(UITheme.BG_CARD);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoContorno btnEditar    = new ComponenteUI.BotaoContorno("Editar");
        ComponenteUI.BotaoPerigo   btnDeletar   = new ComponenteUI.BotaoPerigo("Excluir");
        ComponenteUI.BotaoContorno btnAtualizar = new ComponenteUI.BotaoContorno("Atualizar");

        btnEditar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione um gênero para editar."); return; }
            abrirFormulario(cache.get(r));
        });
        btnDeletar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione um gênero para excluir."); return; }
            confirmarDelecao(cache.get(r));
        });
        btnAtualizar.addActionListener(e -> carregarDados());

        acoes.add(btnAtualizar);
        acoes.add(btnEditar);
        acoes.add(btnDeletar);

        corpo.add(ComponenteUI.scrollLimpo(tabela), BorderLayout.CENTER);
        corpo.add(acoes, BorderLayout.SOUTH);
        return corpo;
    }

    private void carregarDados() {
        new SwingWorker<List<Genero>, Void>() {
            protected List<Genero> doInBackground() { return GeneroDAO.obterTodos(); }
            protected void done() {
                try {
                    cache = get();
                    preencher(cache);
                    labelContagem.setText(cache.size() + " gênero(s) cadastrado(s)");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GeneroView.this,
                        "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void preencher(List<Genero> lista) {
        modeloTabela.setRowCount(0);
        for (Genero g : lista)
            modeloTabela.addRow(new Object[]{g.getId(), g.getName()});
    }

    private void filtrar(String t) {
        if (cache == null) return;
        if (t == null || t.isBlank()) { preencher(cache); return; }
        String lc = t.toLowerCase();
        preencher(cache.stream()
            .filter(g -> g.getName() != null && g.getName().toLowerCase().contains(lc))
            .toList());
    }

    private void abrirFormulario(Genero genero) {
        boolean edicao = genero != null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            edicao ? "Editar Gênero" : "Novo Gênero", true);
        dlg.setSize(420, 240);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(UITheme.BG_WHITE);

        dlg.add(headerDlg(edicao ? "Editar Gênero" : "Cadastrar Novo Gênero"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        ComponenteUI.CampoTexto campoNome = new ComponenteUI.CampoTexto("Ex: Ficção Científica, Romance...");
        if (edicao) campoNome.setText(genero.getName());

        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(ComponenteUI.labelForm("Nome do Gênero *"), gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(campoNome, gbc);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(rodapeDlg(dlg, () -> {
            String nome = campoNome.getText().trim();
            if (nome.isEmpty()) { aviso("O nome é obrigatório."); return; }
            Genero g2 = edicao ? genero : new Genero();
            g2.setName(nome);
            boolean ok = edicao ? GeneroDAO.atualizar(g2) : GeneroDAO.inserir(g2);
            if (ok) { dlg.dispose(); carregarDados(); sucesso(edicao ? "Gênero atualizado!" : "Gênero cadastrado!"); }
            else aviso("Não foi possível salvar.");
        }), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void confirmarDelecao(Genero g) {
        int r = JOptionPane.showConfirmDialog(this,
            "Excluir o gênero \"" + g.getName() + "\"?\nIsso pode afetar livros e estantes vinculados.",
            "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION && GeneroDAO.deletar(g.getId())) {
            carregarDados();
            sucesso("Gênero excluído.");
        }
    }

    private JPanel headerDlg(String txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        JLabel l = new JLabel(txt);
        l.setFont(UITheme.fontPageTitle().deriveFont(18f));
        l.setForeground(UITheme.TEXT_PRIMARY);
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel rodapeDlg(JDialog dlg, Runnable acao) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        ComponenteUI.BotaoContorno bc = new ComponenteUI.BotaoContorno("Cancelar");
        ComponenteUI.BotaoPrimario bp = new ComponenteUI.BotaoPrimario("Salvar");
        bc.addActionListener(e -> dlg.dispose());
        bp.addActionListener(e -> acao.run());
        p.add(bc); p.add(bp);
        return p;
    }

    private void aviso(String m)   { JOptionPane.showMessageDialog(this, m, "Atenção", JOptionPane.WARNING_MESSAGE); }
    private void sucesso(String m) { JOptionPane.showMessageDialog(this, m, "Sucesso",  JOptionPane.INFORMATION_MESSAGE); }
}
