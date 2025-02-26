import javax.swing.*;

import com.formdev.flatlaf.FlatDarkLaf;
import forms.Main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

public class SteamLibraryScanner {
    public static final String SOURCE_HISTORY_TXT = "source-history.txt";
    private String sourceFolder;

    private String getSourceFolder() {
        return sourceFolder;
    }

    private void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    private List<String> loadFolderHistory() {
        List<String> history = new ArrayList<>();
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

    private void saveFolderHistory(List<String> folderHistory) {
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
        List<String> folderHistory = loadFolderHistory();

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

            SteamLibraryScanner scanner = new SteamLibraryScanner();
            Main mainFrame = new Main();

            mainFrame.populateComboBoxes(scanner.loadFolderHistory().toArray(new String[0]));
            mainFrame.setSourceComboBoxListener(_ -> {
                scanner.setSourceFolder((String) mainFrame.getSourceComboBox().getSelectedItem());
                ArrayList<Object[]> rows;
                try {
                    rows = scanner.scanLibraryFolder(new File(scanner.getSourceFolder()));
                    mainFrame.updateSourceTable(rows);
                } catch (Exception e) {
                    mainFrame.showError(e);
                }
            });
            mainFrame.setSourceButtonListener(_ -> {
                String sourceFolder = mainFrame.selectSourceFolder();
                if (!Objects.equals(sourceFolder, "")) {
                    try {
                        scanner.scanLibraryFolder(new File(sourceFolder));
                        scanner.addToFolderHistory(sourceFolder);
                        mainFrame.addSourceComboBoxItem(sourceFolder); // Add to ComboBox dropdown
                    } catch (Exception e) {
                        mainFrame.showError(e);
                    }
                }
            });

            List<String> folderHistory = scanner.loadFolderHistory();
            // Check if history is not empty and auto-scan on startup
            if (!folderHistory.isEmpty()) {
                // Scan the first history folder
                ArrayList<Object[]> rows;
                try {
                    rows = scanner.scanLibraryFolder(new File(folderHistory.getFirst()));
                    mainFrame.updateSourceTable(rows);
                } catch (Exception e) {
                    mainFrame.showError(e);
                }
            }

            mainFrame.setVisible(true);
        });
    }
}
