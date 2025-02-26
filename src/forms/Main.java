package forms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class Main extends JFrame {
    private JPanel contentPane;
    private JButton sourceButton;
    private JButton targetButton;
    private JComboBox<String> sourceComboBox;
    private JComboBox<String> targetComboBox;
    private JTable sourceTable;
    private JTable targetTable;
    private JScrollPane sourceScrollPane;
    private JScrollPane targteScrollPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private DefaultTableModel sourceTableModel;
    private DefaultTableModel targetTableModel;

    public Main() {
        setContentPane(contentPane);
        setTitle("Steam Library Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

        // Set the frame location to the center of the screen
        setLocationRelativeTo(null);
    }

    public String selectSourceFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().toString();
        }

        return "";
    }

    public void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void populateComboBoxes(String[] values) {
        sourceComboBox.setModel(new DefaultComboBoxModel<>(values));
        targetComboBox.setModel(new DefaultComboBoxModel<>(values));
    }

    public void addSourceComboBoxItem(String value) {
        sourceComboBox.addItem(value);
    }

    public void setSourceComboBoxListener(ActionListener actionListener) {
        sourceComboBox.addActionListener(actionListener);
    }

    public void setSourceButtonListener(ActionListener actionListener) {
        sourceButton.addActionListener(actionListener);
    }

    public JComboBox<String> getSourceComboBox() {
        return sourceComboBox;
    }

    public void updateSourceTable(ArrayList<Object[]> rows) {
        sourceTableModel.setRowCount(0);
        for (Object[] row : rows) {
            sourceTableModel.addRow(row);
        }
        setupButtonColumn();
    }

    private void createUIComponents() {
        sourceTableModel = new DefaultTableModel(new String[]{"Game Name", "Install Dir", "Size (MB)", "Action"}, 0);
        sourceTable = new JTable(sourceTableModel);
        sourceTable.setFillsViewportHeight(true);
        targetTable = new JTable(targetTableModel);
        targetTable.setFillsViewportHeight(true);
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(_ -> {
                String installDir = (String) sourceTableModel.getValueAt(row, 1);
                StringSelection stringSelection = new StringSelection(installDir);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                JOptionPane.showMessageDialog(getParent(), "Copied to clipboard: " + installDir);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            String label = (value == null) ? "" : value.toString();
            button.setText(label);
            return button;
        }
    }

    private void setupButtonColumn() {
        TableColumn buttonColumn = sourceTable.getColumnModel().getColumn(3);
        buttonColumn.setCellRenderer(new ButtonRenderer());
        buttonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
    }
}

