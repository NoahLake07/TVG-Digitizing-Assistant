package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.ConversionStatus;
import com.thevideogoat.digitizingassistant.data.Type;
import com.thevideogoat.digitizingassistant.data.Util;
import com.thevideogoat.digitizingassistant.data.Preferences;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Objects.requireNonNullElse;

public class ConversionPanel extends JPanel {

    ProjectFrame projectFrame;
    Conversion conversion;

    JPanel typeRow, noteRow, filesPanel, filenamePanel, dateRow, timeRow, buttonRow, statusRow, tapeDurationRow;
    JLabel header, type, note;
    JList<File> filesList;
    JComboBox<Type> typeSelector;
    JComboBox<ConversionStatus> statusSelector;
    JComboBox<String> amPmSelector;
    JTextField noteField;
    JSpinner mmSpinner, ddSpinner, yyyySpinner, hhSpinner, minSpinner, tapeDurationSpinner;

    JButton addFileBtn, saveBtn;

    public ConversionPanel(Conversion conversion, ProjectFrame projectFrame){
        super();
        this.conversion = conversion;
        this.projectFrame = projectFrame;
        setupUI();
    }

    private void setupUI(){
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Dimension basicRowMaxSize = new Dimension(Short.MAX_VALUE, 50);

        // header
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        headerRow.setBackground(Theme.BACKGROUND);
        header = new JLabel(conversion.name);
        header.setFont(new Font(Theme.HEADER_FONT.getFamily(), Font.BOLD, 24));
        header.setForeground(Theme.TEXT);
        headerRow.add(header);

        // rename conversion to conversion name
        JPopupMenu renameMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("Rename");
        renameMenuItem.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog("Rename Conversion", conversion.name);
            if(newName != null){
                conversion.name = newName;
                header.setText(newName);
            }
        });
        renameMenu.add(renameMenuItem);
        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    renameMenu.show(header, me.getX(), me.getY());
                }
            }
        });

        // type
        typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
        typeRow.setBackground(Theme.BACKGROUND);
        type = new JLabel("Format Type");
        type.setForeground(Color.WHITE);
        type.setFont(Theme.NORMAL_FONT);
        
        typeSelector = new JComboBox<>(Type.values());
        if (conversion.type != null) typeSelector.setSelectedItem(conversion.type);
        typeSelector.setPreferredSize(new Dimension(150, 30));
        typeSelector.setFont(Theme.NORMAL_FONT.deriveFont(18f));
        typeRow.add(type);
        typeRow.add(Box.createHorizontalStrut(10));
        typeRow.add(typeSelector);

        // note
        noteRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        noteRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
        noteRow.setBackground(Theme.BACKGROUND);
        note = new JLabel("Conversion Notes");
        note.setForeground(Color.WHITE);
        note.setFont(Theme.NORMAL_FONT);
        
        noteField = new JTextField(conversion.note);
        noteField.setPreferredSize(new Dimension(400, 30));
        Theme.styleTextField(noteField);
        noteField.setFont(Theme.NORMAL_FONT.deriveFont(18f));
        
        noteRow.add(note);
        noteRow.add(Box.createHorizontalStrut(10));
        noteRow.add(noteField);

        // linked files
        filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(Theme.BACKGROUND);
        filesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Linked Files",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                Theme.NORMAL_FONT,
                Theme.TEXT
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        filesPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));

        filesList = new JList<>();
        Theme.styleList(filesList);
        
        // Add right-click menu for files
        JPopupMenu fileMenu = new JPopupMenu();
        
        JMenuItem openFile = new JMenuItem("Open File");
        openFile.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                if (!selectedFile.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "File not found: " + selectedFile.getAbsolutePath() + "\n\nWould you like to relink this file?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        fileChooser.setDialogTitle("Select new location for " + selectedFile.getName());
                        
                        // Set the current directory to the last used directory
                        String lastDir = Preferences.getInstance().getLastUsedDirectory();
                        if (lastDir != null && new File(lastDir).exists()) {
                            fileChooser.setCurrentDirectory(new File(lastDir));
                        }
                        
                        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File newLocation = fileChooser.getSelectedFile();
                            File newFile = new File(newLocation, selectedFile.getName());
                            
                            if (newFile.exists()) {
                                int index = conversion.linkedFiles.indexOf(selectedFile);
                                if (index != -1) {
                                    conversion.linkedFiles.set(index, newFile);
                                    updateLinkedFiles();
                                    projectFrame.markUnsavedChanges();
                                    JOptionPane.showMessageDialog(this,
                                        "File relinked successfully.",
                                        "Success",
                                        JOptionPane.INFORMATION_MESSAGE);
                                    
                                    // Try opening the file again after relinking
                                    try {
                                        Desktop.getDesktop().open(newFile);
                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(this,
                                            "Could not open file: " + ex.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            } else {
                                JOptionPane.showMessageDialog(this,
                                    "Could not find file at new location.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                } else {
                    try {
                        Desktop.getDesktop().open(selectedFile);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this,
                            "Could not open file: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        JMenuItem showInExplorer = new JMenuItem("Show in Explorer");
        showInExplorer.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                if (!selectedFile.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "File not found: " + selectedFile.getAbsolutePath() + "\n\nWould you like to relink this file?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        fileChooser.setDialogTitle("Select new location for " + selectedFile.getName());
                        
                        // Set the current directory to the last used directory
                        String lastDir = Preferences.getInstance().getLastUsedDirectory();
                        if (lastDir != null && new File(lastDir).exists()) {
                            fileChooser.setCurrentDirectory(new File(lastDir));
                        }
                        
                        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File newLocation = fileChooser.getSelectedFile();
                            File newFile = new File(newLocation, selectedFile.getName());
                            
                            if (newFile.exists()) {
                                int index = conversion.linkedFiles.indexOf(selectedFile);
                                if (index != -1) {
                                    conversion.linkedFiles.set(index, newFile);
                                    updateLinkedFiles();
                                    projectFrame.markUnsavedChanges();
                                    JOptionPane.showMessageDialog(this,
                                        "File relinked successfully.",
                                        "Success",
                                        JOptionPane.INFORMATION_MESSAGE);
                                    
                                    // Try showing in explorer again after relinking
                                    try {
                                        Desktop.getDesktop().open(newFile.getParentFile());
                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(this,
                                            "Could not open file location: " + ex.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            } else {
                                JOptionPane.showMessageDialog(this,
                                    "Could not find file at new location.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                } else {
                    try {
                        Desktop.getDesktop().open(selectedFile.getParentFile());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this,
                            "Could not open file location: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        JMenuItem copyPath = new JMenuItem("Copy Path");
        copyPath.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                StringSelection stringSelection = new StringSelection(selectedFile.getAbsolutePath());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        
        JMenuItem renameFile = new JMenuItem("Rename File");
        renameFile.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                String newName = JOptionPane.showInputDialog(this,
                    "Enter new name:",
                    "Rename File",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    selectedFile.getName()).toString();
                
                if (newName != null && !newName.trim().isEmpty()) {
                    File newFile = new File(selectedFile.getParentFile(), newName);
                    if (selectedFile.renameTo(newFile)) {
                        int index = conversion.linkedFiles.indexOf(selectedFile);
                        if (index != -1) {
                            conversion.linkedFiles.set(index, newFile);
                            updateLinkedFiles();
                            projectFrame.markUnsavedChanges();
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Could not rename file. Make sure the file is not in use.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        JMenuItem relinkMedia = new JMenuItem("Relink Media");
        relinkMedia.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Select new location for " + selectedFile.getName());
                
                // Set the current directory to the last used directory
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    fileChooser.setCurrentDirectory(new File(lastDir));
                }
                
                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File newLocation = fileChooser.getSelectedFile();
                    File newFile = new File(newLocation, selectedFile.getName());
                    
                    if (newFile.exists()) {
                        int index = conversion.linkedFiles.indexOf(selectedFile);
                        if (index != -1) {
                            conversion.linkedFiles.set(index, newFile);
                            updateLinkedFiles();
                            projectFrame.markUnsavedChanges();
                            JOptionPane.showMessageDialog(this,
                                "File relinked successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Could not find file at new location.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        JMenuItem removeFile = new JMenuItem("Remove File");
        removeFile.addActionListener(e -> {
            File selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                conversion.linkedFiles.remove(selectedFile);
                updateLinkedFiles();
                projectFrame.markUnsavedChanges();
            }
        });
        
        fileMenu.add(openFile);
        fileMenu.add(showInExplorer);
        fileMenu.add(copyPath);
        fileMenu.addSeparator();
        fileMenu.add(renameFile);
        fileMenu.add(relinkMedia);
        fileMenu.add(removeFile);
        
        filesList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    int index = filesList.locationToIndex(me.getPoint());
                    if (index != -1) {
                        filesList.setSelectedIndex(index);
                        fileMenu.show(filesList, me.getX(), me.getY());
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(filesList);
        Theme.styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        
        DefaultListModel<File> listModel = new DefaultListModel<>();
        if(conversion.linkedFiles != null) {
            for (File f : conversion.linkedFiles) {
                listModel.addElement(f);
            }
        } else {
            listModel.addElement(new File("No files attached"));
        }
        filesList.setModel(listModel);

        // Files button panel with both Attach and Rename buttons
        JPanel filesButtonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filesButtonRow.setBackground(Theme.BACKGROUND);

        // Rename Options button
        JButton renameOptionsBtn = new JButton("Rename Options");
        Theme.styleButton(renameOptionsBtn);
        renameOptionsBtn.addActionListener(e -> {
            if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No files are linked to rename.",
                    "No Files",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] options = {
                "Rename to conversion name",
                "Rename to conversion note",
                "Custom name...",
                "Cancel"
            };
            
            // Create checkbox for subdirectories
            JCheckBox includeSubdirs = new JCheckBox("Include files in subdirectories", false);
            includeSubdirs.setToolTipText("If checked, files in subdirectories will also be renamed.");
            
            // Create preserve numbering checkbox
            JCheckBox preserveNumbering = new JCheckBox("Preserve existing numbering", false);
            preserveNumbering.setToolTipText("If checked, will try to maintain any existing numbers in filenames.");
            
            // Create custom dialog
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            dialogPanel.add(new JLabel("Select rename option:"));
            dialogPanel.add(Box.createVerticalStrut(10));
            dialogPanel.add(includeSubdirs);
            dialogPanel.add(Box.createVerticalStrut(5));
            dialogPanel.add(preserveNumbering);
            
            int choice = JOptionPane.showOptionDialog(
                this,
                dialogPanel,
                "Rename Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            String newName = null;
            switch (choice) {
                case 0: // Conversion name
                    newName = conversion.name;
                    break;
                case 1: // Conversion note
                    if (conversion.note.isEmpty()) {
                        JOptionPane.showMessageDialog(this, 
                            "Conversion note is empty. Please add a note first.", 
                            "Warning", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    newName = conversion.note;
                    break;
                case 2: // Custom name
                    newName = JOptionPane.showInputDialog(this,
                        "Enter custom name:",
                        "Custom Filename",
                        JOptionPane.PLAIN_MESSAGE);
                    if (newName == null || newName.trim().isEmpty()) {
                        return;
                    }
                    break;
                default:
                    return;
            }
            
            // Confirm rename operation
            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Rename %d file(s) to \"%s\"?", 
                    conversion.linkedFiles.size(), newName),
                "Confirm Rename",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                Util.renameFilesWithOptions(
                    conversion.linkedFiles, 
                    newName,
                    includeSubdirs.isSelected(),
                    preserveNumbering.isSelected()
                );
                updateLinkedFiles();
            }
        });

        addFileBtn = new JButton("Attach File");
        Theme.styleButton(addFileBtn);

        filesButtonRow.add(renameOptionsBtn);
        filesButtonRow.add(Box.createHorizontalStrut(10));
        filesButtonRow.add(addFileBtn);

        filesPanel.add(scrollPane);
        filesPanel.add(Box.createVerticalStrut(10));
        filesPanel.add(filesButtonRow);

        // date
        dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateRow.setBackground(Theme.BACKGROUND);
        dateRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Date of Conversion",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                Theme.NORMAL_FONT,
                Theme.TEXT
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        dateRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));

        setupDateTimeSpinners();
        
        // Style spinners with explicit colors
        Component[] spinners = {mmSpinner, ddSpinner, yyyySpinner};
        for (Component spinner : spinners) {
            ((JSpinner)spinner).setPreferredSize(new Dimension(70, 30));
        }

        JButton updateDateBtn = new JButton("Update to Current Date");
        Theme.styleButton(updateDateBtn);
        
        dateRow.add(mmSpinner);
        dateRow.add(Box.createHorizontalStrut(5));
        dateRow.add(ddSpinner);
        dateRow.add(Box.createHorizontalStrut(5));
        dateRow.add(yyyySpinner);
        dateRow.add(Box.createHorizontalStrut(10));
        dateRow.add(updateDateBtn);

        // time
        timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeRow.setBackground(Theme.BACKGROUND);
        timeRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Time of Conversion",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                Theme.NORMAL_FONT,
                Theme.TEXT
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        timeRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));

        try {
            SpinnerNumberModel hhModel = new SpinnerNumberModel(Integer.parseInt(conversion.timeOfConversion.getHour()), 1, 12, 1);
            hhSpinner = new JSpinner(hhModel);
            if (conversion.timeOfConversion.hour != null)
                hhSpinner.setValue(Integer.parseInt(conversion.timeOfConversion.getHour()));
            SpinnerNumberModel minModel = new SpinnerNumberModel(Integer.parseInt(conversion.timeOfConversion.getMinute()), 0, 59, 1);
            minSpinner = new JSpinner(minModel);

            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(minSpinner, "00");
            minSpinner.setEditor(editor);
            if (conversion.timeOfConversion.minute != null)
                minSpinner.setValue(Integer.parseInt(conversion.timeOfConversion.getMinute()));
            timeRow.add(hhSpinner);
            timeRow.add(minSpinner);
            amPmSelector = new JComboBox<>(new String[]{"AM", "PM"});
            amPmSelector.setPreferredSize(new Dimension(70, 30));
            amPmSelector.setBackground(Color.WHITE);
            amPmSelector.setForeground(Color.BLACK);
            if (conversion.timeOfConversion.getAmPm() != null) {
                amPmSelector.setSelectedItem(conversion.timeOfConversion.getAmPm());
            }
            timeRow.add(amPmSelector);
        } catch (NumberFormatException e) {
            SpinnerNumberModel hhModel = new SpinnerNumberModel(12, 1, 12, 1);
            hhSpinner = new JSpinner(hhModel);
            SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 59, 1);
            minSpinner = new JSpinner(minModel);
            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(minSpinner, "00");
            minSpinner.setEditor(editor);
            timeRow.add(hhSpinner);
            timeRow.add(minSpinner);
            amPmSelector = new JComboBox<>(new String[]{"AM", "PM"});
            amPmSelector.setSelectedItem("AM");
            timeRow.add(amPmSelector);
        }
        hhSpinner.setPreferredSize(new Dimension(70, 30));
        minSpinner.setPreferredSize(new Dimension(70, 30));
        
        amPmSelector.setPreferredSize(new Dimension(70, 30));

        JButton updateTimeBtn = new JButton("Update to Current Time");
        Theme.styleButton(updateTimeBtn);

        timeRow.add(hhSpinner);
        timeRow.add(Box.createHorizontalStrut(5));
        timeRow.add(minSpinner);
        timeRow.add(Box.createHorizontalStrut(5));
        timeRow.add(amPmSelector);
        timeRow.add(Box.createHorizontalStrut(10));
        timeRow.add(updateTimeBtn);

        // tape duration
        tapeDurationRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tapeDurationRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
        tapeDurationRow.setBackground(Theme.BACKGROUND);
        
        JLabel tapeDurationLabel = new JLabel("Tape Duration (minutes)");
        tapeDurationLabel.setForeground(Color.WHITE);
        tapeDurationLabel.setFont(Theme.NORMAL_FONT);
        
        tapeDurationSpinner = new JSpinner(new SpinnerNumberModel(
            (int) conversion.duration.toMinutes(), 0, Integer.MAX_VALUE, 1));
        tapeDurationSpinner.setPreferredSize(new Dimension(100, 30));
        tapeDurationSpinner.setFont(Theme.NORMAL_FONT.deriveFont(18f));

        // status
        statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        statusRow.setBackground(Theme.BACKGROUND);
        statusRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Current Status",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                Theme.NORMAL_FONT,
                Theme.TEXT
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        statusSelector = new JComboBox<>(ConversionStatus.values());
        statusSelector.setSelectedItem(requireNonNullElse(conversion.status, ConversionStatus.NOT_STARTED));
        statusSelector.setPreferredSize(new Dimension(150, 30));
        statusSelector.setFont(Theme.NORMAL_FONT.deriveFont(18f));

        StatusIndicator statusIndicator = new StatusIndicator();
        statusSelector.addActionListener(e -> {
            ConversionStatus selectedStatus = (ConversionStatus) statusSelector.getSelectedItem();
            statusIndicator.updateColor(selectedStatus);
            if(selectedStatus == ConversionStatus.COMPLETED || selectedStatus == ConversionStatus.BASIC_EDITING){
                tapeDurationRow.setVisible(true);
            } else {
                tapeDurationRow.setVisible(false);
            }
            if(selectedStatus == ConversionStatus.NOT_STARTED){
                dateRow.setVisible(false);
                timeRow.setVisible(false);
            } else {
                dateRow.setVisible(true);
                timeRow.setVisible(true);
            }
        });

        statusRow.add(statusIndicator);
        statusRow.add(Box.createHorizontalStrut(10));
        statusRow.add(statusSelector);

        // button row
        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.setMaximumSize(basicRowMaxSize);
        buttonRow.setBackground(Theme.BACKGROUND);
        
        saveBtn = new JButton("Save");
        JButton deleteBtn = new JButton("Delete");
        Theme.styleButton(saveBtn);
        Theme.styleButton(deleteBtn);
        
        buttonRow.add(deleteBtn);
        buttonRow.add(Box.createHorizontalStrut(10));
        buttonRow.add(saveBtn);

        // Add components with consistent spacing
        add(headerRow);
        add(Box.createVerticalStrut(15));
        add(typeRow);
        add(Box.createVerticalStrut(15));
        add(noteRow);
        add(Box.createVerticalStrut(15));
        add(filesPanel);
        add(Box.createVerticalStrut(15));
        add(statusRow);
        add(Box.createVerticalStrut(15));
        add(dateRow);
        add(Box.createVerticalStrut(15));
        add(timeRow);
        add(Box.createVerticalStrut(15));
        add(tapeDurationRow);
        add(Box.createVerticalStrut(15));
        add(buttonRow);

        // Add File button
        addFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            
            // Set the current directory to the last used directory
            String lastDir = Preferences.getInstance().getLastUsedDirectory();
            if (lastDir != null && new File(lastDir).exists()) {
                fileChooser.setCurrentDirectory(new File(lastDir));
            }
            
            int result = fileChooser.showOpenDialog(this);
            
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    if (!conversion.linkedFiles.contains(file)) {
                        conversion.linkedFiles.add(file);
                    }
                }
                
                // Save the parent directory of the first selected file as the last used directory
                if (selectedFiles.length > 0) {
                    File firstFile = selectedFiles[0];
                    File parentDir = firstFile.getParentFile();
                    if (parentDir != null && parentDir.exists()) {
                        Preferences.getInstance().setLastUsedDirectory(parentDir.getAbsolutePath());
                    }
                }
                
                updateLinkedFiles();
                projectFrame.markUnsavedChanges();
            }
        });

        // Update Date button
        updateDateBtn.addActionListener(e -> {
            LocalDate now = LocalDate.now();
            mmSpinner.setValue(now.getMonthValue());
            ddSpinner.setValue(now.getDayOfMonth());
            yyyySpinner.setValue(now.getYear());
            projectFrame.markUnsavedChanges();
        });

        // Update Time button
        updateTimeBtn.addActionListener(e -> {
            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            String amPm = hour >= 12 ? "PM" : "AM";
            hour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            
            hhSpinner.setValue(hour);
            minSpinner.setValue(now.getMinute());
            amPmSelector.setSelectedItem(amPm);
            projectFrame.markUnsavedChanges();
        });

        // Save button
        saveBtn.addActionListener(e -> {
            updateConversion();
            projectFrame.saveProject();
            JOptionPane.showMessageDialog(this, 
                "Changes saved successfully.", 
                "Save Success", 
                JOptionPane.INFORMATION_MESSAGE);
        });

        // Delete button
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this conversion?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                projectFrame.remove(this);
            }
        });

        // type selector
        typeSelector.addActionListener(e -> {
            projectFrame.markUnsavedChanges();
            // Save the selected type to preferences
            Type selectedType = (Type) typeSelector.getSelectedItem();
            if (selectedType != null) {
                Preferences.getInstance().setLastUsedConversionType(selectedType);
            }
        });

        // note field
        noteField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
            public void removeUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
            public void insertUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
        });

        // status selector
        statusSelector.addActionListener(e -> {
            projectFrame.markUnsavedChanges();
        });

        // date spinners
        mmSpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());
        ddSpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());
        yyyySpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());

        // time spinners
        hhSpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());
        minSpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());
        amPmSelector.addActionListener(e -> projectFrame.markUnsavedChanges());

        // tape duration spinner
        tapeDurationSpinner.addChangeListener(e -> projectFrame.markUnsavedChanges());
    }

    private void updateLinkedFiles(){
        DefaultListModel<File> listModel = new DefaultListModel<>();
        if(conversion.linkedFiles != null) {
            for (File f : conversion.linkedFiles) {
                listModel.addElement(f);
            }
        } else {
            listModel.addElement(new File("No files attached"));
        }
        filesList.setModel(listModel);
    }

    private void setupDateTimeSpinners(){
        try {
            // Month Spinner
            if(mmSpinner == null) {
                SpinnerNumberModel mmModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getMonth()), 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
            } else {
                mmSpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getMonth()));
            }
            // Day Spinner
            if(ddSpinner == null) {
                SpinnerNumberModel ddModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getDay()), 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
            } else {
                ddSpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getDay()));
            }
            // Year Spinner
            if(yyyySpinner == null) {
                SpinnerNumberModel yyyyModel = new SpinnerNumberModel(
                    Integer.parseInt(conversion.dateOfConversion.getYear()), 
                    1900, 
                    2100, 
                    1
                );
                yyyySpinner = new JSpinner(yyyyModel);
                JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yyyySpinner, "#");
                yyyySpinner.setEditor(editor);
            } else {
                yyyySpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getYear()));
            }
        } catch (NumberFormatException e) {
            // Handle exceptions appropriately
            if(mmSpinner == null) {
                SpinnerNumberModel mmModel = new SpinnerNumberModel(1, 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
            } else {
                mmSpinner.setValue(1);
            }
            if(ddSpinner == null) {
                SpinnerNumberModel ddModel = new SpinnerNumberModel(1, 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
            } else {
                ddSpinner.setValue(1);
            }
            if(yyyySpinner == null) {
                SpinnerNumberModel yyyyModel = new SpinnerNumberModel(2000, 1900, 2100, 1);
                yyyySpinner = new JSpinner(yyyyModel);
            } else {
                yyyySpinner.setValue(2000);
            }
        }
    }

    public class StatusIndicator extends JPanel {
        public StatusIndicator() {
            setPreferredSize(new Dimension(20, 20));
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            updateColor(requireNonNullElse(conversion.status, ConversionStatus.NOT_STARTED));
        }

        public void updateColor(ConversionStatus status) {
            switch (status) {
                case NOT_STARTED:
                    setBackground(new Color(158, 158, 158)); // Lighter gray for better visibility
                    break;
                case DAMAGED:
                    setBackground(new Color(220, 53, 69)); // Bootstrap danger red
                    break;
                case IN_PROGRESS:
                    setBackground(new Color(255, 193, 7)); // Bootstrap warning yellow
                    break;
                case BASIC_EDITING:
                    setBackground(new Color(0, 123, 255)); // Bootstrap primary blue
                    break;
                case COMPLETED:
                    setBackground(new Color(40, 167, 69)); // Bootstrap success green
                    break;
            }
        }
    }

    // Add this method to update conversion data
    public void updateConversion() {
        // Update conversion properties
        conversion.type = (Type) typeSelector.getSelectedItem();
        conversion.note = noteField.getText();
        conversion.status = (ConversionStatus) statusSelector.getSelectedItem();
        
        // Update date - format numbers to ensure two digits
        conversion.dateOfConversion.month = String.format("%02d", (Integer)mmSpinner.getValue());
        conversion.dateOfConversion.day = String.format("%02d", (Integer)ddSpinner.getValue());
        conversion.dateOfConversion.year = String.format("%04d", (Integer)yyyySpinner.getValue());
        
        // Update time - format numbers to ensure two digits
        conversion.timeOfConversion.hour = String.format("%02d", (Integer)hhSpinner.getValue());
        conversion.timeOfConversion.minute = String.format("%02d", (Integer)minSpinner.getValue());
        conversion.timeOfConversion.am_pm = (String) amPmSelector.getSelectedItem();
        
        // Update duration if visible
        if (tapeDurationRow.isVisible()) {
            int minutes = (Integer) tapeDurationSpinner.getValue();
            conversion.duration = Duration.ofMinutes(minutes);
        }
    }

}
