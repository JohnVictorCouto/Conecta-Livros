package view;

import dao.EditoraDAO;
import dao.GeneroDAO;
import model.Editora;
import model.Genero;
import util.DadosCache;
import util.Tradutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Diálogo compartilhado para administrar gêneros e editoras, delegando
 * a persistência ao adaptador específico de cada tipo de cadastro.
 */
public class CatalogoDialog extends JDialog {

    private interface Cadastro<T> {
        String titulo();
        int tamanhoMaximo();
        List<T> obterTodos();
        int id(T item);
        String nome(T item);
        T criar(String nome);
        void renomear(T item, String nome);
        boolean inserir(T item);
        boolean atualizar(T item);
        boolean deletar(int id);
    }

    private static final Cadastro<Genero> GENEROS = new Cadastro<Genero>() {
        public String titulo() { return Tradutor.t("catalog.genres"); }
        public int tamanhoMaximo() { return 100; }
        public List<Genero> obterTodos() { return GeneroDAO.obterTodos(); }
        public int id(Genero item) { return item.getId(); }
        public String nome(Genero item) { return item.getName(); }
        public Genero criar(String nome) { return new Genero(0, nome); }
        public void renomear(Genero item, String nome) { item.setName(nome); }
        public boolean inserir(Genero item) { return GeneroDAO.inserir(item); }
        public boolean atualizar(Genero item) { return GeneroDAO.atualizar(item); }
        public boolean deletar(int id) { return GeneroDAO.deletar(id); }
    };

    private static final Cadastro<Editora> EDITORAS = new Cadastro<Editora>() {
        public String titulo() { return Tradutor.t("catalog.publishers"); }
        public int tamanhoMaximo() { return 150; }
        public List<Editora> obterTodos() { return EditoraDAO.obterTodos(); }
        public int id(Editora item) { return item.getId(); }
        public String nome(Editora item) { return item.getName(); }
        public Editora criar(String nome) { return new Editora(0, nome); }
        public void renomear(Editora item, String nome) { item.setName(nome); }
        public boolean inserir(Editora item) { return EditoraDAO.inserir(item); }
        public boolean atualizar(Editora item) { return EditoraDAO.atualizar(item); }
        public boolean deletar(int id) { return EditoraDAO.deletar(id); }
    };

    private final Runnable aoAlterar;

    /**
     * Abre a administração de catálogos no contexto da janela proprietária.
     * @param owner janela que contém o diálogo
     * @param aoAlterar callback para atualizar as telas após uma alteração
     */
    public CatalogoDialog(Frame owner, Runnable aoAlterar) {
        super(owner, Tradutor.t("catalog.title"), true);
        this.aoAlterar = aoAlterar;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 480);
        setMinimumSize(new Dimension(580, 380));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UITheme.BG_APP);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab(Tradutor.t("catalog.genres"), new PainelCadastro<>(GENEROS));
        abas.addTab(Tradutor.t("catalog.publishers"), new PainelCadastro<>(EDITORAS));
        add(abas);
    }

    private final class PainelCadastro<T> extends JPanel {
        private final Cadastro<T> cadastro;
        private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{Tradutor.t("value.id"), Tradutor.t("catalog.column_name")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        private final JTable tabela = new JTable(modelo);
        private final JLabel contagem = new JLabel(Tradutor.t("state.loading"));
        private List<T> itens;

        PainelCadastro(Cadastro<T> cadastro) {
            this.cadastro = cadastro;
            setLayout(new BorderLayout(0, 12));
            setBackground(UITheme.BG_APP);
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel titulo = new JLabel(cadastro.titulo());
            titulo.setFont(UITheme.fontPageTitle().deriveFont(20f));
            titulo.setForeground(UITheme.TEXT_PRIMARY);
            contagem.setFont(UITheme.fontSubtitle());
            contagem.setForeground(UITheme.TEXT_SECONDARY);

            JPanel cabecalho = new JPanel(new BorderLayout());
            cabecalho.setOpaque(false);
            JPanel textos = new JPanel();
            textos.setOpaque(false);
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
            textos.add(titulo);
            textos.add(Box.createRigidArea(new Dimension(0, 3)));
            textos.add(contagem);

            ComponenteUI.BotaoPrimario novo =
                new ComponenteUI.BotaoPrimario(Tradutor.t("catalog.new"));
            novo.addActionListener(e -> abrirFormulario(null));
            cabecalho.add(textos, BorderLayout.WEST);
            cabecalho.add(novo, BorderLayout.EAST);
            add(cabecalho, BorderLayout.NORTH);

            ComponenteUI.estilizarTabela(tabela);
            tabela.getColumnModel().getColumn(0).setPreferredWidth(70);
            tabela.getColumnModel().getColumn(1).setPreferredWidth(400);
            add(ComponenteUI.scrollLimpo(tabela), BorderLayout.CENTER);

            JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            acoes.setBackground(UITheme.BG_APP);
            ComponenteUI.BotaoContorno editar =
                new ComponenteUI.BotaoContorno(Tradutor.t("action.edit"));
            ComponenteUI.BotaoPerigo excluir =
                new ComponenteUI.BotaoPerigo(Tradutor.t("action.delete"));
            ComponenteUI.BotaoContorno atualizar =
                new ComponenteUI.BotaoContorno(Tradutor.t("action.refresh"));
            editar.addActionListener(e -> {
                T selecionado = selecionado();
                if (selecionado == null) {
                    aviso(Tradutor.t("catalog.select_edit"));
                } else {
                    abrirFormulario(selecionado);
                }
            });
            excluir.addActionListener(e -> {
                T selecionado = selecionado();
                if (selecionado == null) {
                    aviso(Tradutor.t("catalog.select_delete"));
                } else {
                    confirmarExclusao(selecionado);
                }
            });
            atualizar.addActionListener(e -> carregar());
            acoes.add(atualizar);
            acoes.add(editar);
            acoes.add(excluir);
            add(acoes, BorderLayout.SOUTH);
            carregar();
        }

        private void carregar() {
            new SwingWorker<List<T>, Void>() {
                @Override
                protected List<T> doInBackground() { return cadastro.obterTodos(); }

                @Override
                protected void done() {
                    try {
                        itens = get();
                        modelo.setRowCount(0);
                        for (T item : itens) {
                            modelo.addRow(new Object[]{cadastro.id(item), cadastro.nome(item)});
                        }
                        contagem.setText(Tradutor.formatar("catalog.count", itens.size()));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(PainelCadastro.this,
                            Tradutor.formatar("message.error_detail", ex.getMessage()),
                            Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private T selecionado() {
            int linha = tabela.getSelectedRow();
            if (linha < 0 || itens == null) return null;
            int modeloLinha = tabela.convertRowIndexToModel(linha);
            int id = ((Number) modelo.getValueAt(modeloLinha, 0)).intValue();
            return itens.stream().filter(item -> cadastro.id(item) == id).findFirst().orElse(null);
        }

        private void abrirFormulario(T item) {
            if (itens == null) {
                aviso(Tradutor.t("state.loading"));
                return;
            }
            boolean edicao = item != null;
            String titulo = Tradutor.t(edicao ? "catalog.edit_title" : "catalog.new_title");
            JTextField campoNome = new JTextField(item == null ? "" : cadastro.nome(item), 28);
            campoNome.setFont(UITheme.fontBody());

            JPanel formulario = new JPanel(new BorderLayout(0, 8));
            formulario.add(new JLabel(Tradutor.t("catalog.name_label")), BorderLayout.NORTH);
            formulario.add(campoNome, BorderLayout.CENTER);

            while (true) {
                int resultado = JOptionPane.showConfirmDialog(
                    CatalogoDialog.this, formulario, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (resultado != JOptionPane.OK_OPTION) return;

                String nome = campoNome.getText().trim();
                if (nome.isEmpty()) {
                    aviso(Tradutor.t("catalog.name_required"));
                    continue;
                }
                if (nome.length() > cadastro.tamanhoMaximo()) {
                    aviso(Tradutor.formatar("catalog.name_too_long", cadastro.tamanhoMaximo()));
                    continue;
                }
                boolean duplicado = itens.stream().anyMatch(outro ->
                    cadastro.id(outro) != (item == null ? -1 : cadastro.id(item))
                        && cadastro.nome(outro) != null
                        && cadastro.nome(outro).equalsIgnoreCase(nome));
                if (duplicado) {
                    aviso(Tradutor.t("catalog.name_duplicate"));
                    continue;
                }
                if (item != null && nome.equals(cadastro.nome(item))) return;

                T salvo = item == null ? cadastro.criar(nome) : item;
                if (item != null) cadastro.renomear(salvo, nome);
                boolean sucesso = item == null
                    ? cadastro.inserir(salvo) : cadastro.atualizar(salvo);
                if (!sucesso) {
                    aviso(Tradutor.t("catalog.save_error"));
                    return;
                }
                DadosCache.limpar();
                carregar();
                aoAlterar.run();
                JOptionPane.showMessageDialog(CatalogoDialog.this,
                    Tradutor.t(item == null ? "catalog.created" : "catalog.updated"),
                    Tradutor.t("message.success"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        private void confirmarExclusao(T item) {
            int resposta = JOptionPane.showConfirmDialog(
                CatalogoDialog.this,
                Tradutor.formatar("catalog.delete_prompt", cadastro.nome(item)),
                Tradutor.t("catalog.confirm_delete"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) return;
            if (!cadastro.deletar(cadastro.id(item))) {
                aviso(Tradutor.t("catalog.delete_error"));
                return;
            }
            DadosCache.limpar();
            carregar();
            aoAlterar.run();
            JOptionPane.showMessageDialog(CatalogoDialog.this,
                Tradutor.t("catalog.deleted"),
                Tradutor.t("message.success"), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void aviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem,
            Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
    }
}
