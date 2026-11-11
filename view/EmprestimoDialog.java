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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Diálogo de cadastro de empréstimo, com seleção de exemplar disponível
 * e coleta dos dados de quem o recebe.
 */
final class EmprestimoDialog extends JDialog {

    // Separa a seleção da obra da cópia física para registrar o empréstimo no exemplar correto.
    private final JComboBox<Livro> comboLivro = new JComboBox<>();
    private final JComboBox<Exemplar> comboExemplar = new JComboBox<>();
    private final JTextField campoTomador = new JTextField(24);
    private final JTextField campoContato = new JTextField(24);
    private final JSpinner campoRetirada = new JSpinner(new SpinnerDateModel());
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
        setMinimumSize(new Dimension(520, 390));
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

        Date agora = new Date();
        configurarCampoDataHora(campoRetirada, agora);
        configurarCampoDataHora(campoVencimento, agora);
        adicionarCampo(campos, c, 4, Tradutor.t("loans.checkout_date"), campoRetirada);
        adicionarCampo(campos, c, 5, Tradutor.t("loans.due_date"), campoVencimento);

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
                    setText(exemplar.getCodigo());
                } else {
                    setText("");
                }
                return this;
            }
        });
        comboLivro.addActionListener(e -> carregarExemplares());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelar = new ComponenteUI.BotaoContorno(Tradutor.t("action.cancel"));
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

    private void configurarCampoDataHora(JSpinner campo, Date valorInicial) {
        campo.setEditor(new JSpinner.DateEditor(campo, "dd/MM/yyyy HH:mm"));
        campo.setValue(valorInicial);
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
        Date retirada;
        try {
            campoRetirada.commitEdit();
            campoVencimento.commitEdit();
            retirada = (Date) campoRetirada.getValue();
        } catch (java.text.ParseException e) {
            erro("loans.date_time_invalid");
            return;
        }
        LocalDateTime dataRetirada = paraDataHora(retirada);
        LocalDateTime vencimento = paraDataHora((Date) campoVencimento.getValue());
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
        if (dataRetirada.isAfter(LocalDateTime.now())) {
            erro("loans.checkout_date_invalid");
            return;
        }
        if (!vencimento.isAfter(dataRetirada)) {
            erro("loans.due_date_invalid");
            return;
        }

        if (!EmprestimoDAO.registrar(exemplar.getId(), nome, contato, dataRetirada, vencimento)) {
            erro("loans.save_error");
            return;
        }
        salvo = true;
        aoAtualizar.run();
        dispose();
    }

    private LocalDateTime paraDataHora(Date valor) {
        return valor.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            .withSecond(0).withNano(0);
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
