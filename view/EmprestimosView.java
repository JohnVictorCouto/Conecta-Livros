package view;

import dao.EmprestimoDAO;
import model.Emprestimo;
import util.Tradutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lista os empréstimos do acervo e permite acompanhar devoluções e prazos.
 */
public class EmprestimosView extends JPanel {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Runnable aoAlterarLivros;
    private final JComboBox<String> filtro = new JComboBox<>();
    private final DefaultTableModel modelo = new DefaultTableModel(new Object[0][9], new String[0]) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<Emprestimo> emprestimos = List.of();

    /**
     * Cria a listagem e recebe a ação de sincronização do acervo após operações.
     * @param aoAlterarLivros callback para atualizar os livros dependentes
     */
    public EmprestimosView(Runnable aoAlterarLivros) {
        this.aoAlterarLivros = aoAlterarLivros;
        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setOpaque(false);
        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel(Tradutor.t("loans.title"));
        titulo.setFont(UITheme.fontPageTitle());
        titulo.setForeground(UITheme.TEXT_PRIMARY);
        JLabel subtitulo = new JLabel(Tradutor.t("loans.subtitle"));
        subtitulo.setFont(UITheme.fontSubtitle());
        subtitulo.setForeground(UITheme.TEXT_SECONDARY);
        titulos.add(titulo);
        titulos.add(Box.createRigidArea(new Dimension(0, 4)));
        titulos.add(subtitulo);

        filtro.addItem(Tradutor.t("loans.filter_all"));
        filtro.addItem(Tradutor.t("loans.filter_active"));
        filtro.addItem(Tradutor.t("loans.filter_overdue"));
        filtro.addItem(Tradutor.t("loans.filter_returned"));
        filtro.addActionListener(e -> exibirEmprestimos());
        filtro.setFont(UITheme.fontBody());
        Dimension tamanhoFiltro = filtro.getPreferredSize();
        filtro.setPreferredSize(new Dimension(tamanhoFiltro.width, 36));

        JButton atualizar = new ComponenteUI.BotaoContorno(Tradutor.t("action.refresh"));
        atualizar.addActionListener(e -> recarregar());
        JButton novo = new ComponenteUI.BotaoPrimario(Tradutor.t("loans.new_loan"));
        novo.addActionListener(e -> EmprestimoDialog.abrir(
            SwingUtilities.getWindowAncestor(this), 0, this::aposAlteracao));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(filtro);
        acoes.add(atualizar);
        acoes.add(novo);
        topo.add(titulos, BorderLayout.WEST);
        topo.add(acoes, BorderLayout.EAST);

        configurarTabela();
        JButton devolver = new ComponenteUI.BotaoContorno(Tradutor.t("action.return_book"));
        devolver.addActionListener(e -> devolverSelecionado());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.setOpaque(false);
        rodape.add(devolver);

        add(topo, BorderLayout.NORTH);
        add(ComponenteUI.scrollLimpo(tabela), BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        recarregar();
    }

    private void configurarTabela() {
        tabela.setRowHeight(32);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFont(UITheme.fontBody());
        tabela.getTableHeader().setFont(UITheme.fontBodyBold());
        tabela.setFillsViewportHeight(true);
        tabela.setAutoCreateRowSorter(true);
    }

    /** Reconsulta os empréstimos e atualiza a tabela apresentada. */
    public void recarregar() {
        new SwingWorker<List<Emprestimo>, Void>() {
            @Override protected List<Emprestimo> doInBackground() {
                return EmprestimoDAO.obterTodos();
            }

            @Override protected void done() {
                try {
                    emprestimos = get();
                    exibirEmprestimos();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EmprestimosView.this,
                        Tradutor.t("loans.load_error"), Tradutor.t("message.error"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exibirEmprestimos() {
        String[] colunas = {
            Tradutor.t("loans.column_id"), Tradutor.t("loans.column_book"),
            Tradutor.t("loans.column_copy"), Tradutor.t("loans.column_borrower"),
            Tradutor.t("loans.column_contact"), Tradutor.t("loans.column_checkout"),
            Tradutor.t("loans.column_due"), Tradutor.t("loans.column_returned"),
            Tradutor.t("loans.column_status")
        };
        modelo.setColumnIdentifiers(colunas);
        modelo.setRowCount(0);
        String selecionado = (String) filtro.getSelectedItem();
        for (Emprestimo emprestimo : emprestimos) {
            if (Tradutor.t("loans.filter_active").equals(selecionado) && !emprestimo.isAtivo()) continue;
            if (Tradutor.t("loans.filter_overdue").equals(selecionado) && !emprestimo.isAtrasado()) continue;
            if (Tradutor.t("loans.filter_returned").equals(selecionado) && emprestimo.isAtivo()) continue;
            modelo.addRow(new Object[] {
                emprestimo.getId(), emprestimo.getTituloLivro(), emprestimo.getCodigoExemplar(),
                emprestimo.getNomeTomador(), emprestimo.getContatoTomador(),
                formatar(emprestimo.getDataEmprestimo()), formatar(emprestimo.getDataVencimento()),
                emprestimo.getDataDevolucao() == null ? "—" : formatar(emprestimo.getDataDevolucao()),
                Tradutor.t(emprestimo.isAtivo()
                    ? (emprestimo.isAtrasado() ? "loans.status_overdue" : "loans.status_active")
                    : "loans.status_returned")
            });
        }
    }

    private String formatar(java.time.LocalDate data) {
        return data.format(FORMATO_DATA);
    }

    private void devolverSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, Tradutor.t("loans.select_active"),
                Tradutor.t("message.info"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int linhaModelo = tabela.convertRowIndexToModel(linha);
        int id = (Integer) modelo.getValueAt(linhaModelo, 0);
        Emprestimo selecionado = emprestimos.stream()
            .filter(emprestimo -> emprestimo.getId() == id)
            .findFirst().orElse(null);
        if (selecionado == null || !selecionado.isAtivo()) {
            JOptionPane.showMessageDialog(this, Tradutor.t("loans.select_active"),
                Tradutor.t("message.info"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacao = JOptionPane.showConfirmDialog(this,
            Tradutor.formatar("loans.return_prompt", selecionado.getNomeTomador(),
                selecionado.getTituloLivro(), selecionado.getCodigoExemplar()),
            Tradutor.t("message.confirm"), JOptionPane.YES_NO_OPTION);
        if (confirmacao == JOptionPane.YES_OPTION) {
            if (EmprestimoDAO.registrarDevolucao(id)) {
                aposAlteracao();
            } else {
                JOptionPane.showMessageDialog(this, Tradutor.t("loans.return_error"),
                    Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void aposAlteracao() {
        aoAlterarLivros.run();
    }
}
