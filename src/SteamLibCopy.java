import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteamLibCopy extends JFrame {
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

    public SteamLibCopy() {
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
            this.setText("Copy");
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
                JOptionPane.showMessageDialog(getParent(), "Copy: " + installDir);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            //String label = (value == null) ? "" : value.toString();
            button.setText("Copy");
            return button;
        }
    }

    private void setupButtonColumn() {
        TableColumn buttonColumn = sourceTable.getColumnModel().getColumn(3);
        buttonColumn.setCellRenderer(new ButtonRenderer());
        buttonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    public static final String SOURCE_HISTORY_TXT = "source-history.txt";
    private String sourceFolder;

    private String getSourceFolder() {
        return sourceFolder;
    }

    private void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    private java.util.List<String> loadFolderHistory() {
        java.util.List<String> history = new ArrayList<>();
        File historyFile = new File(SOURCE_HISTORY_TXT);
        if (historyFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(historyFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    history.add(line);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return history.isEmpty() ? new ArrayList<>() : history;
    }

    private ArrayList<Object[]> scanLibraryFolder(File libraryFolder) throws Exception {
        ArrayList<Object[]> rows = new ArrayList<>();

        File steamApps = new File(libraryFolder, "steamapps");
        if (!steamApps.exists() || !steamApps.isDirectory()) {
            throw new Exception("Invalid steam library folder: no steamapps directory found");
        }

        File[] files = steamApps.listFiles((_, name) -> name.endsWith(".acf"));
        if (files != null) {
            for (File file : files) {
                rows.add(parseManifest(file));
            }
        }

        return rows;
    }

    private Object[] parseManifest(File file) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            String name = extractValue(content, "\"name\"\\s+\"(.*?)\"");
            String installDir = extractValue(content, "\"installdir\"\\s+\"(.*?)\"");
            String sizeOnDisk = extractValue(content, "\"SizeOnDisk\"\\s+\"(\\d+)\"");

            double sizeMB = sizeOnDisk.isEmpty() ? 0 : Double.parseDouble(sizeOnDisk) / (1024 * 1024);
            return new Object[]{name, installDir, String.format("%.2f", sizeMB), "Copy"};
        } catch (IOException e) {
            e.printStackTrace();
            return new Object[]{};
        }
    }

    private String extractValue(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void saveFolderHistory(java.util.List<String> folderHistory) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(SOURCE_HISTORY_TXT));
            for (String path : folderHistory) {
                writer.write(path);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToFolderHistory(String newPath) {
        java.util.List<String> folderHistory = loadFolderHistory();

        if (!folderHistory.contains(newPath)) {
            folderHistory.addFirst(newPath); // Add to the top of the list
            if (folderHistory.size() > 10) {
                folderHistory.remove(10); // Keep only the latest 10 paths
            }
            saveFolderHistory(folderHistory);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }

            SteamLibCopy steamLibCopy = new SteamLibCopy();

            steamLibCopy.populateComboBoxes(steamLibCopy.loadFolderHistory().toArray(new String[0]));
            steamLibCopy.setSourceComboBoxListener(_ -> {
                steamLibCopy.setSourceFolder((String) steamLibCopy.getSourceComboBox().getSelectedItem());
                ArrayList<Object[]> rows;
                try {
                    rows = steamLibCopy.scanLibraryFolder(new File(steamLibCopy.getSourceFolder()));
                    steamLibCopy.updateSourceTable(rows);
                } catch (Exception e) {
                    steamLibCopy.showError(e);
                }
            });
            steamLibCopy.setSourceButtonListener(_ -> {
                String sourceFolder = steamLibCopy.selectSourceFolder();
                if (!Objects.equals(sourceFolder, "")) {
                    try {
                        steamLibCopy.scanLibraryFolder(new File(sourceFolder));
                        steamLibCopy.addToFolderHistory(sourceFolder);
                        steamLibCopy.addSourceComboBoxItem(sourceFolder); // Add to ComboBox dropdown
                    } catch (Exception e) {
                        steamLibCopy.showError(e);
                    }
                }
            });

            List<String> folderHistory = steamLibCopy.loadFolderHistory();
            // Check if history is not empty and auto-scan on startup
            if (!folderHistory.isEmpty()) {
                // Scan the first history folder
                ArrayList<Object[]> rows;
                try {
                    rows = steamLibCopy.scanLibraryFolder(new File(folderHistory.getFirst()));
                    steamLibCopy.updateSourceTable(rows);
                } catch (Exception e) {
                    steamLibCopy.showError(e);
                }
            }

            steamLibCopy.setVisible(true);
        });
    }
}

