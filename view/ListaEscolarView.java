package view;

import dao.ListaEscolarDAO;
import dao.LivroDAO;
import model.ListaEscolar;
import model.Livro;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ListaEscolarView extends JPanel {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private ComponenteUI.CampoBusca campoBusca;
    private JLabel labelContagem;
    private List<ListaEscolar> cache;

    private static final String[] COLUNAS = {"ID", "Nome da Lista", "Descrição"};

    public ListaEscolarView() {
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

        JLabel titulo = new JLabel("Listas Escolares");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        labelContagem = new JLabel("Carregando...");
        labelContagem.setFont(UITheme.fontSubtitle());
        labelContagem.setForeground(UITheme.TEXT_SECONDARY);

        esq.add(titulo); esq.add(Box.createRigidArea(new Dimension(0,3))); esq.add(labelContagem);
        h.add(esq, BorderLayout.WEST);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dir.setOpaque(false);

        campoBusca = new ComponenteUI.CampoBusca("Buscar lista...", 220);
        campoBusca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrar(campoBusca.getText()); }
        });

        ComponenteUI.BotaoPrimario btnNovo = new ComponenteUI.BotaoPrimario("+ Nova Lista");
        btnNovo.addActionListener(e -> abrirFormulario(null));

        dir.add(campoBusca); dir.add(btnNovo);
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
        tabela.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(460);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        acoes.setBackground(UITheme.BG_CARD);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoContorno btnEditar    = new ComponenteUI.BotaoContorno("Editar");
        ComponenteUI.BotaoPerigo   btnDeletar   = new ComponenteUI.BotaoPerigo("Excluir");
        ComponenteUI.BotaoContorno btnLivros    = new ComponenteUI.BotaoContorno("Livros");
        ComponenteUI.BotaoContorno btnAtualizar = new ComponenteUI.BotaoContorno("Atualizar");

        btnEditar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione uma lista para editar."); return; }
            abrirFormulario(cache.get(r));
        });
        btnDeletar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione uma lista para excluir."); return; }
            confirmarDelecao(cache.get(r));
        });
        btnLivros.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione uma lista para ver os livros."); return; }
            abrirGerenciadorLivros(cache.get(r));
        });
        btnAtualizar.addActionListener(e -> carregarDados());

        acoes.add(btnAtualizar); acoes.add(btnLivros); acoes.add(btnEditar); acoes.add(btnDeletar);
        corpo.add(ComponenteUI.scrollLimpo(tabela), BorderLayout.CENTER);
        corpo.add(acoes, BorderLayout.SOUTH);
        return corpo;
    }

    private void carregarDados() {
        new SwingWorker<List<ListaEscolar>, Void>() {
            protected List<ListaEscolar> doInBackground() { return ListaEscolarDAO.obterTodos(); }
            protected void done() {
                try {
                    cache = get();
                    preencher(cache);
                    labelContagem.setText(cache.size() + " lista(s) cadastrada(s)");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ListaEscolarView.this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void preencher(List<ListaEscolar> lista) {
        modeloTabela.setRowCount(0);
        for (ListaEscolar l : lista)
            modeloTabela.addRow(new Object[]{l.getId(), l.getName(), l.getDescription()});
    }

    private void filtrar(String t) {
        if (cache == null) return;
        if (t == null || t.isBlank()) { preencher(cache); return; }
        String lc = t.toLowerCase();
        preencher(cache.stream().filter(l ->
            (l.getName() != null && l.getName().toLowerCase().contains(lc)) ||
            (l.getDescription() != null && l.getDescription().toLowerCase().contains(lc))
        ).toList());
    }

    private void abrirFormulario(ListaEscolar lista) {
        boolean edicao = lista != null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            edicao ? "Editar Lista" : "Nova Lista Escolar", true);
        dlg.setSize(480, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(UITheme.BG_WHITE);

        dlg.add(headerDlg(edicao ? "Editar Lista Escolar" : "Cadastrar Nova Lista"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;

        ComponenteUI.CampoTexto campoNome = new ComponenteUI.CampoTexto("Ex: Lista do 9º Ano A");
        ComponenteUI.AreaTexto  campoDesc = new ComponenteUI.AreaTexto();
        JScrollPane sdesc = new JScrollPane(campoDesc);
        sdesc.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 8));
        sdesc.setPreferredSize(new Dimension(0, 100));

        if (edicao) {
            campoNome.setText(lista.getName());
            if (lista.getDescription() != null) campoDesc.setText(lista.getDescription());
        }

        gbc.gridx=0; gbc.gridy=0; gbc.insets=new Insets(0,0,4,0);
        form.add(ComponenteUI.labelForm("Nome da Lista *"), gbc);
        gbc.gridy=1; gbc.insets=new Insets(0,0,14,0);
        form.add(campoNome, gbc);
        gbc.gridy=2; gbc.insets=new Insets(0,0,4,0);
        form.add(ComponenteUI.labelForm("Descrição"), gbc);
        gbc.gridy=3; gbc.insets=new Insets(0,0,0,0);
        form.add(sdesc, gbc);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(rodapeDlg(dlg, () -> {
            String nome = campoNome.getText().trim();
            if (nome.isEmpty()) { aviso("O nome é obrigatório."); return; }
            ListaEscolar l2 = edicao ? lista : new ListaEscolar();
            l2.setName(nome); l2.setDescription(campoDesc.getText().trim());
            boolean ok = edicao ? ListaEscolarDAO.atualizar(l2) : ListaEscolarDAO.inserir(l2);
            if (ok) { dlg.dispose(); carregarDados(); sucesso(edicao ? "Lista atualizada!" : "Lista cadastrada!"); }
            else aviso("Não foi possível salvar.");
        }), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void abrirGerenciadorLivros(ListaEscolar lista) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Livros da Lista: " + lista.getName(), true);
        dlg.setSize(720, 580);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(UITheme.BG_APP);

        dlg.add(headerDlg("Gerenciar Livros — " + lista.getName()), BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 14, 0));
        split.setBackground(UITheme.BG_APP);
        split.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));

        String[] cols = {"ID", "Título"};
        DefaultTableModel mNaLista = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        DefaultTableModel mTodos   = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };

        JTable tNaLista = new JTable(mNaLista); ComponenteUI.estilizarTabela(tNaLista);
        JTable tTodos   = new JTable(mTodos);   ComponenteUI.estilizarTabela(tTodos);

        Runnable recarregarNaLista = () -> {
            mNaLista.setRowCount(0);
            for (Livro l : ListaEscolarDAO.obterLivrosDaLista(lista.getId()))
                mNaLista.addRow(new Object[]{l.getId(), l.getTitle()});
        };

        for (Livro l : ListaEscolarDAO.obterLivrosDaLista(lista.getId()))
            mNaLista.addRow(new Object[]{l.getId(), l.getTitle()});
        for (Livro l : LivroDAO.obterTodos())
            mTodos.addRow(new Object[]{l.getId(), l.getTitle()});

        split.add(painelLista("Livros na Lista", ComponenteUI.scrollLimpo(tNaLista)));
        split.add(painelLista("Todos os Livros", ComponenteUI.scrollLimpo(tTodos)));
        dlg.add(split, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        acoes.setBackground(UITheme.BG_WHITE);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoPrimario btnAdd = new ComponenteUI.BotaoPrimario("← Adicionar à Lista");
        ComponenteUI.BotaoPerigo btnRem   = new ComponenteUI.BotaoPerigo("Remover →");
        ComponenteUI.BotaoContorno btnFec = new ComponenteUI.BotaoContorno("Fechar");

        btnAdd.addActionListener(e -> {
            int r = tTodos.getSelectedRow();
            if (r < 0) { aviso("Selecione um livro da lista da direita."); return; }
            int id = (int) mTodos.getValueAt(r, 0);
            if (ListaEscolarDAO.adicionarLivro(lista.getId(), id)) recarregarNaLista.run();
            else aviso("Livro já está na lista ou ocorreu um erro.");
        });

        btnRem.addActionListener(e -> {
            int r = tNaLista.getSelectedRow();
            if (r < 0) { aviso("Selecione um livro da lista da esquerda."); return; }
            int id = (int) mNaLista.getValueAt(r, 0);
            if (ListaEscolarDAO.removerLivro(lista.getId(), id)) recarregarNaLista.run();
        });

        btnFec.addActionListener(e -> dlg.dispose());
        acoes.add(btnRem); acoes.add(btnAdd); acoes.add(btnFec);
        dlg.add(acoes, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel painelLista(String titulo, JScrollPane scroll) {
        ComponenteUI.CardPanel p = new ComponenteUI.CardPanel(10);
        p.setLayout(new BorderLayout());
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(UITheme.fontBodyBold());
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        lbl.setOpaque(true);
        lbl.setBackground(UITheme.BG_APP);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        p.add(lbl, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void confirmarDelecao(ListaEscolar l) {
        int r = JOptionPane.showConfirmDialog(this,
            "Excluir a lista \"" + l.getName() + "\"?\nTodos os vínculos com livros serão removidos.",
            "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION && ListaEscolarDAO.deletar(l.getId())) { carregarDados(); sucesso("Lista excluída."); }
    }

    private JPanel headerDlg(String txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,UITheme.BORDER),
            BorderFactory.createEmptyBorder(16,24,16,24)));
        JLabel l = new JLabel(txt);
        l.setFont(UITheme.fontPageTitle().deriveFont(18f));
        l.setForeground(UITheme.TEXT_PRIMARY);
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel rodapeDlg(JDialog dlg, Runnable acao) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        p.setBackground(UITheme.BG_WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1,0,0,0,UITheme.BORDER));
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
