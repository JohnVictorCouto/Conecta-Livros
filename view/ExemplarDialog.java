package view;

import dao.ExemplarDAO;
import dao.EstanteDAO;
import model.Exemplar;
import model.Estante;
import model.Livro;
import util.DadosCache;
import util.Tradutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Diálogo para consultar e editar exemplares físicos de um livro,
 * inclusive situação, conservação e localização.
 */
public class ExemplarDialog extends JDialog {

    private final Livro livro;
    private final Runnable aoAlterar;
    private final DefaultTableModel modelo = new DefaultTableModel();
    private final JTable tabela = new JTable(modelo);
    private List<Exemplar> exemplares;
    private boolean alterado;

    /**
     * Abre os exemplares associados ao livro selecionado.
     */
    public ExemplarDialog(Frame owner, Livro livro, Runnable aoAlterar) {
        super(owner, Tradutor.formatar("copies.title", livro.getTitle()), true);
        this.livro = livro;
        this.aoAlterar = aoAlterar;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setSize(760, 420);
        setLocationRelativeTo(owner);

        modelo.setColumnIdentifiers(new Object[] {
            Tradutor.t("copies.column_code"),
            Tradutor.t("copies.column_shelf"),
            Tradutor.t("copies.column_location"),
            Tradutor.t("copies.column_condition"),
            Tradutor.t("copies.column_status")
        });
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton adicionar = new JButton(Tradutor.t("copies.add"));
        JButton editar = new JButton(Tradutor.t("copies.edit"));
        JButton excluir = new JButton(Tradutor.t("copies.delete"));
        JButton fechar = new JButton(Tradutor.t("action.close"));
        adicionar.addActionListener(e -> editar(null));
        editar.addActionListener(e -> editarSelecionado());
        excluir.addActionListener(e -> excluirSelecionado());
        fechar.addActionListener(e -> dispose());
        acoes.add(adicionar);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.add(fechar);
        add(acoes, BorderLayout.SOUTH);

        carregar();
    }

    public boolean isAlterado() {
        return alterado;
    }

    private void carregar() {
        exemplares = ExemplarDAO.obterPorLivro(livro.getId());
        modelo.setRowCount(0);
        for (Exemplar exemplar : exemplares) {
            modelo.addRow(new Object[] {
                exemplar.getCodigo(),
                exemplar.getEstanteId() > 0
                    ? DadosCache.getNomeEstante(exemplar.getEstanteId()) : Tradutor.t("value.no_shelf"),
                exemplar.getLocalizacao(),
                exemplar.getConservacao(),
                Tradutor.t("copies.status." + exemplar.getSituacao().name().toLowerCase(java.util.Locale.ROOT))
            });
        }
    }

    private void editarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, Tradutor.t("copies.select_edit"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        editar(exemplares.get(tabela.convertRowIndexToModel(linha)));
    }

    private void editar(Exemplar exemplar) {
        JTextField codigo = new JTextField(exemplar == null ? "" : exemplar.getCodigo());
        JTextField localizacao = new JTextField(exemplar == null ? "" : valor(exemplar.getLocalizacao()));
        JTextField conservacao = new JTextField(exemplar == null ? "" : valor(exemplar.getConservacao()));
        JComboBox<Estante> estante = new JComboBox<>();
        estante.addItem(new Estante(0, Tradutor.t("value.no_shelf"), 0));
        for (Estante opcao : EstanteDAO.obterTodos()) {
            estante.addItem(opcao);
            if (exemplar != null && opcao.getId() == exemplar.getEstanteId()) {
                estante.setSelectedItem(opcao);
            }
        }
        Exemplar.Situacao[] situacoes = java.util.Arrays.stream(Exemplar.Situacao.values())
            .filter(status -> status != Exemplar.Situacao.EMPRESTADO
                || (exemplar != null && exemplar.getSituacao() == Exemplar.Situacao.EMPRESTADO))
            .toArray(Exemplar.Situacao[]::new);
        JComboBox<Exemplar.Situacao> situacao = new JComboBox<>(situacoes);
        situacao.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                Component componente = super.getListCellRendererComponent(
                    list, value, index, selected, focus);
                if (value instanceof Exemplar.Situacao) {
                    Exemplar.Situacao status = (Exemplar.Situacao) value;
                    setText(Tradutor.t("copies.status." +
                        status.name().toLowerCase(java.util.Locale.ROOT)));
                }
                return componente;
            }
        });
        situacao.setSelectedItem(exemplar == null
            ? Exemplar.Situacao.DISPONIVEL : exemplar.getSituacao());
        if (exemplar != null && exemplar.getSituacao() == Exemplar.Situacao.EMPRESTADO) {
            situacao.setEnabled(false);
            situacao.setToolTipText(Tradutor.t("copies.status_managed_by_loan"));
        }

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.add(new JLabel(Tradutor.t("copies.code")));
        formulario.add(codigo);
        formulario.add(new JLabel(Tradutor.t("copies.shelf")));
        formulario.add(estante);
        formulario.add(new JLabel(Tradutor.t("copies.location")));
        formulario.add(localizacao);
        formulario.add(new JLabel(Tradutor.t("copies.condition")));
        formulario.add(conservacao);
        formulario.add(new JLabel(Tradutor.t("copies.status")));
        formulario.add(situacao);

        int resposta = JOptionPane.showConfirmDialog(this, formulario,
            Tradutor.t(exemplar == null ? "copies.add_title" : "copies.edit_title"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resposta != JOptionPane.OK_OPTION) {
            return;
        }
        if (codigo.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, Tradutor.t("copies.code_required"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        Exemplar salvo = exemplar == null ? new Exemplar() : exemplar;
        salvo.setLivroId(livro.getId());
        Estante estanteSelecionada = (Estante) estante.getSelectedItem();
        salvo.setEstanteId(estanteSelecionada == null ? 0 : estanteSelecionada.getId());
        salvo.setCodigo(codigo.getText().trim());
        salvo.setLocalizacao(localizacao.getText().trim());
        salvo.setConservacao(conservacao.getText().trim());
        salvo.setSituacao((Exemplar.Situacao) situacao.getSelectedItem());

        boolean sucesso = exemplar == null
            ? ExemplarDAO.inserir(salvo) : ExemplarDAO.atualizar(salvo);
        if (!sucesso) {
            JOptionPane.showMessageDialog(this, Tradutor.t("copies.save_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        alterado = true;
        carregar();
        aoAlterar.run();
    }

    private void excluirSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, Tradutor.t("copies.select_delete"),
                Tradutor.t("message.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        Exemplar exemplar = exemplares.get(tabela.convertRowIndexToModel(linha));
        int resposta = JOptionPane.showConfirmDialog(this,
            Tradutor.formatar("copies.delete_prompt", exemplar.getCodigo()),
            Tradutor.t("message.confirm"), JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        if (!ExemplarDAO.deletar(exemplar.getId(), livro.getId())) {
            JOptionPane.showMessageDialog(this, Tradutor.t("copies.save_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        alterado = true;
        carregar();
        aoAlterar.run();
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
