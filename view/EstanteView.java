package view;

import dao.EstanteDAO;
import dao.GeneroDAO;
import model.Estante;
import model.Genero;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

// Tela de gerenciamento de estantes
public class EstanteView extends JPanel {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private ComponenteUI.CampoBusca campoBusca;
    private JLabel labelContagem;
    private List<Estante> cache;
    private List<Genero>  cacheGeneros;

    private static final String[] COLUNAS = {"ID", "Nome da Estante", "GÃªnero"};

    public EstanteView() {
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

        JLabel titulo = new JLabel("Estantes");
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);

        labelContagem = new JLabel("Carregando...");
        labelContagem.setFont(UITheme.fontSubtitle());
        labelContagem.setForeground(UITheme.TEXT_SECONDARY);

        esq.add(titulo); esq.add(Box.createRigidArea(new Dimension(0,3))); esq.add(labelContagem);
        h.add(esq, BorderLayout.WEST);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dir.setOpaque(false);

        campoBusca = new ComponenteUI.CampoBusca("Buscar estante...", 220);
        campoBusca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrar(campoBusca.getText()); }
        });

        ComponenteUI.BotaoPrimario btnNovo = new ComponenteUI.BotaoPrimario("+ Nova Estante");
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
        tabela.getColumnModel().getColumn(1).setPreferredWidth(300);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(200);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        acoes.setBackground(UITheme.BG_CARD);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        ComponenteUI.BotaoContorno btnEditar    = new ComponenteUI.BotaoContorno("Editar");
        ComponenteUI.BotaoPerigo   btnDeletar   = new ComponenteUI.BotaoPerigo("Excluir");
        ComponenteUI.BotaoContorno btnAtualizar = new ComponenteUI.BotaoContorno("Atualizar");

        btnEditar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione uma estante para editar."); return; }
            abrirFormulario(cache.get(r));
        });
        btnDeletar.addActionListener(e -> {
            int r = tabela.getSelectedRow();
            if (r < 0) { aviso("Selecione uma estante para excluir."); return; }
            confirmarDelecao(cache.get(r));
        });
        btnAtualizar.addActionListener(e -> carregarDados());

        acoes.add(btnAtualizar); acoes.add(btnEditar); acoes.add(btnDeletar);
        corpo.add(ComponenteUI.scrollLimpo(tabela), BorderLayout.CENTER);
        corpo.add(acoes, BorderLayout.SOUTH);
        return corpo;
    }

    private void carregarDados() {
        new SwingWorker<Object[], Void>() {
            protected Object[] doInBackground() {
                return new Object[]{EstanteDAO.obterTodos(), GeneroDAO.obterTodos()};
            }
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] res = get();
                    cache        = (List<Estante>) res[0];
                    cacheGeneros = (List<Genero>)  res[1];
                    preencher(cache);
                    labelContagem.setText(cache.size() + " estante(s) cadastrada(s)");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EstanteView.this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String nomeGenero(int id) {
        if (cacheGeneros == null) return String.valueOf(id);
        return cacheGeneros.stream().filter(g -> g.getId() == id).map(Genero::getName).findFirst().orElse("ID " + id);
    }

    private void preencher(List<Estante> lista) {
        modeloTabela.setRowCount(0);
        for (Estante e : lista)
            modeloTabela.addRow(new Object[]{e.getId(), e.getName(), nomeGenero(e.getGenreId())});
    }

    private void filtrar(String t) {
        if (cache == null) return;
        if (t == null || t.isBlank()) { preencher(cache); return; }
        String lc = t.toLowerCase();
        preencher(cache.stream().filter(e -> e.getName() != null && e.getName().toLowerCase().contains(lc)).collect(Collectors.toList()));
    }

    private void abrirFormulario(Estante estante) {
        boolean edicao = estante != null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            edicao ? "Editar Estante" : "Nova Estante", true);
        dlg.setSize(440, 300);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(UITheme.BG_WHITE);

        dlg.add(headerDlg(edicao ? "Editar Estante" : "Cadastrar Nova Estante"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;

        ComponenteUI.CampoTexto campoNome = new ComponenteUI.CampoTexto("Nome da estante");
        JComboBox<Genero> comboGen = new JComboBox<>();
        comboGen.setFont(UITheme.fontBody());
        comboGen.setBackground(UITheme.BG_WHITE);
        comboGen.setBorder(new ComponenteUI.RoundedBorder(UITheme.BORDER, 8));
        comboGen.setPreferredSize(new Dimension(200, 40));
        if (cacheGeneros != null) for (Genero g : cacheGeneros) comboGen.addItem(g);

        if (edicao) {
            campoNome.setText(estante.getName());
            for (int i = 0; i < comboGen.getItemCount(); i++)
                if (comboGen.getItemAt(i).getId() == estante.getGenreId()) { comboGen.setSelectedIndex(i); break; }
        }

        gbc.gridx=0; gbc.gridy=0; gbc.insets=new Insets(0,0,4,0);
        form.add(ComponenteUI.labelForm("Nome da Estante *"), gbc);
        gbc.gridy=1; gbc.insets=new Insets(0,0,14,0);
        form.add(campoNome, gbc);
        gbc.gridy=2; gbc.insets=new Insets(0,0,4,0);
        form.add(ComponenteUI.labelForm("GÃªnero *"), gbc);
        gbc.gridy=3; gbc.insets=new Insets(0,0,0,0);
        form.add(comboGen, gbc);
        dlg.add(form, BorderLayout.CENTER);

        dlg.add(rodapeDlg(dlg, () -> {
            String nome = campoNome.getText().trim();
            if (nome.isEmpty()) { aviso("O nome Ã© obrigatÃ³rio."); return; }
            if (comboGen.getSelectedItem() == null) { aviso("Selecione um gÃªnero."); return; }
            Estante est = edicao ? estante : new Estante();
            est.setName(nome);
            est.setGenreId(((Genero) comboGen.getSelectedItem()).getId());
            boolean ok = edicao ? EstanteDAO.atualizar(est) : EstanteDAO.inserir(est);
            if (ok) { dlg.dispose(); carregarDados(); sucesso(edicao ? "Estante atualizada!" : "Estante cadastrada!"); }
            else aviso("NÃ£o foi possÃ­vel salvar.");
        }), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void confirmarDelecao(Estante e) {
        int r = JOptionPane.showConfirmDialog(this,
            "Excluir a estante \"" + e.getName() + "\"?\nLivros vinculados serÃ£o afetados.",
            "Confirmar exclusÃ£o", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION && EstanteDAO.deletar(e.getId())) { carregarDados(); sucesso("Estante excluÃ­da."); }
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

    private void aviso(String m)   { JOptionPane.showMessageDialog(this, m, "AtenÃ§Ã£o", JOptionPane.WARNING_MESSAGE); }
    private void sucesso(String m) { JOptionPane.showMessageDialog(this, m, "Sucesso",  JOptionPane.INFORMATION_MESSAGE); }
}



