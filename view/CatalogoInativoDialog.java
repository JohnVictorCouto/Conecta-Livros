package view;

import dao.CatalogoInativoDAO;
import model.RegistroInativo;
import util.Tradutor;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.List;

/**
 * Diálogo de consulta aos registros inativados do catálogo.
 * Permite visualizar o histórico preservado sem oferecer exclusão física.
 */
final class CatalogoInativoDialog extends JDialog {
    CatalogoInativoDialog(Frame owner) {
        super(owner, Tradutor.t("catalog.inactive.title"), Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        setSize(620, 380);
        setLocationRelativeTo(owner);

        DefaultTableModel model = new DefaultTableModel(
            new Object[][]{}, new String[]{
                Tradutor.t("catalog.inactive.type"),
                Tradutor.t("catalog.inactive.id"),
                Tradutor.t("catalog.inactive.description"),
                Tradutor.t("catalog.inactive.status")
            }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        ComponenteUI.estilizarTabela(table);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        List<RegistroInativo> registros = CatalogoInativoDAO.obterTodos();
        for (RegistroInativo registro : registros) {
            model.addRow(new Object[]{
                registro.getTipo(), registro.getId(), registro.getDescricao(),
                Tradutor.t("catalog.inactive.value")
            });
        }
    }
}
