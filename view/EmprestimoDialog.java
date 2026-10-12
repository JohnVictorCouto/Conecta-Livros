package view;

import dao.EmprestimoDAO;
import dao.ExemplarDAO;
import dao.LivroDAO;
import model.Emprestimo;
import model.Exemplar;
import model.Livro;
import util.Tradutor;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Diálogo de cadastro de empréstimo, com seleção de exemplar disponível
 * e coleta dos dados de quem o recebe.
 */
final class EmprestimoDialog extends JDialog {

    // Mantém a escolha da obra separada da cópia física selecionada para circulação.
    private final JComboBox<Livro> comboLivro = new JComboBox<>();
    private final JComboBox<Exemplar> comboExemplar = new JComboBox<>();
    private final JTextField campoTomador = new JTextField(24);
    private final JTextField campoContato = new JTextField(24);
    private final JSpinner campoVencimento = new JSpinner(new SpinnerDateModel());
    private final Runnable aoAtualizar;
    private final int livroInicialId;
    private boolean salvo;

    private EmprestimoDialog(Window owner, int livroId, Runnable aoAtualizar) {
        super(owner, Tradutor.t("loans.new_title"), ModalityType.APPLICATION_MODAL);
        this.aoAtualizar = aoAtualizar;
        this.livroInicialId = livroId;
        construirInterface();
        carregarLivros();
        setMinimumSize(new Dimension(470, 340));
        setLocationRelativeTo(owner);
    }

    static boolean abrir(Window owner, int livroId, Runnable aoAtualizar) {
        EmprestimoDialog dialog = new EmprestimoDialog(owner, livroId, aoAtualizar);
        dialog.setVisible(true);
        return dialog.salvo;
    }

    private void construirInterface() {
        setLayout(new BorderLayout(12, 12));
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        adicionarCampo(campos, c, 0, Tradutor.t("loans.book"), comboLivro);
        adicionarCampo(campos, c, 1, Tradutor.t("loans.copy"), comboExemplar);
        adicionarCampo(campos, c, 2, Tradutor.t("loans.borrower"), campoTomador);
        adicionarCampo(campos, c, 3, Tradutor.t("loans.contact"), campoContato);

        campoVencimento.setEditor(new JSpinner.DateEditor(campoVencimento, "yyyy-MM-dd"));
        campoVencimento.setValue(Date.from(LocalDate.now().plusDays(14)
            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        adicionarCampo(campos, c, 4, Tradutor.t("loans.due_date"), campoVencimento);

        comboLivro.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Livro ? ((Livro) value).getTitle() : "");
                return this;
            }
        });
        comboExemplar.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Exemplar) {
                    Exemplar exemplar = (Exemplar) value;
                    setText(exemplar.getCodigo() + " — " +
                        (exemplar.getLocalizacao() == null || exemplar.getLocalizacao().isBlank()
                            ? Tradutor.t("value.no_shelf") : exemplar.getLocalizacao()));
                } else {
                    setText("");
                }
                return this;
            }
        });
        comboLivro.addActionListener(e -> carregarExemplares());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelar = new JButton(Tradutor.t("action.cancel"));
        JButton registrar = new ComponenteUI.BotaoPrimario(Tradutor.t("loans.register"));
        cancelar.addActionListener(e -> dispose());
        registrar.addActionListener(e -> registrar());
        botoes.add(cancelar);
        botoes.add(registrar);
        botoes.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 16));

        add(campos, BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints c, int linha, String rotulo,
                                JComponent campo) {
        c.gridx = 0;
        c.gridy = linha;
        c.weightx = 0;
        painel.add(new JLabel(rotulo), c);
        c.gridx = 1;
        c.weightx = 1;
        painel.add(campo, c);
    }

    private void carregarLivros() {
        List<Livro> livros = LivroDAO.obterTodos();
        comboLivro.removeAllItems();
        for (Livro livro : livros) comboLivro.addItem(livro);
        if (livroInicialId > 0) {
            for (int i = 0; i < comboLivro.getItemCount(); i++) {
                if (comboLivro.getItemAt(i).getId() == livroInicialId) {
                    comboLivro.setSelectedIndex(i);
                    break;
                }
            }
        }
        carregarExemplares();
    }

    private void carregarExemplares() {
        comboExemplar.removeAllItems();
        Livro livro = (Livro) comboLivro.getSelectedItem();
        if (livro == null) return;
        for (Exemplar exemplar : ExemplarDAO.obterPorLivro(livro.getId())) {
            if (exemplar.getSituacao() == Exemplar.Situacao.DISPONIVEL) {
                comboExemplar.addItem(exemplar);
            }
        }
    }

    private void registrar() {
        Exemplar exemplar = (Exemplar) comboExemplar.getSelectedItem();
        String nome = campoTomador.getText().trim();
        String contato = campoContato.getText().trim();
        try {
            campoVencimento.commitEdit();
        } catch (java.text.ParseException e) {
            erro("loans.due_date_invalid");
            return;
        }
        LocalDate vencimento = ((Date) campoVencimento.getValue()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        if (exemplar == null) {
            erro("loans.no_available_copy");
            return;
        }
        if (nome.isBlank() || contato.isBlank()) {
            erro("loans.borrower_required");
            return;
        }
        if (nome.length() > 150 || contato.length() > 255) {
            erro("loans.field_too_long");
            return;
        }
        if (vencimento.isBefore(LocalDate.now())) {
            erro("loans.due_date_invalid");
            return;
        }

        if (!EmprestimoDAO.registrar(exemplar.getId(), nome, contato, vencimento)) {
            erro("loans.save_error");
            return;
        }
        salvo = true;
        aoAtualizar.run();
        dispose();
    }

    static boolean devolver(Window owner, int livroId, Runnable aoAtualizar) {
        List<Emprestimo> ativos;
        try {
            ativos = EmprestimoDAO.obterAtivosPorLivro(livroId);
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(owner, Tradutor.t("loans.load_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (ativos.isEmpty()) {
            JOptionPane.showMessageDialog(owner, Tradutor.t("loans.no_active_for_book"),
                Tradutor.t("message.info"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        Emprestimo selecionado;
        if (ativos.size() == 1) {
            selecionado = ativos.get(0);
        } else {
            Emprestimo[] opcoes = ativos.toArray(new Emprestimo[0]);
            selecionado = (Emprestimo) JOptionPane.showInputDialog(owner,
                Tradutor.t("loans.select_return"), Tradutor.t("loans.return_title"),
                JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);
            if (selecionado == null) return false;
        }
        int confirmacao = JOptionPane.showConfirmDialog(owner,
            Tradutor.formatar("loans.return_prompt", selecionado.getNomeTomador(),
                selecionado.getTituloLivro(), selecionado.getCodigoExemplar()),
            Tradutor.t("message.confirm"), JOptionPane.YES_NO_OPTION);
        if (confirmacao != JOptionPane.YES_OPTION) return false;
        if (!EmprestimoDAO.registrarDevolucao(selecionado.getId())) {
            JOptionPane.showMessageDialog(owner, Tradutor.t("loans.return_error"),
                Tradutor.t("message.error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        aoAtualizar.run();
        return true;
    }

    private void erro(String chave) {
        JOptionPane.showMessageDialog(this, Tradutor.t(chave), Tradutor.t("message.error"),
            JOptionPane.ERROR_MESSAGE);
    }
}
