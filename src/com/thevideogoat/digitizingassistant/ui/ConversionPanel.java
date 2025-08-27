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
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNullElse;
import com.thevideogoat.digitizingassistant.data.FileReference;
import com.thevideogoat.digitizingassistant.util.FileCacheManager;

public class ConversionPanel extends JPanel {

    ProjectFrame projectFrame;
    Conversion conversion;

    JPanel typeRow, noteRow, dataOnlyRow, technicianNotesRow, filesPanel, filenamePanel, dateRow, timeRow, buttonRow, statusRow, tapeDurationRow, damagePanel;
    JLabel header, type, note, technicianNotes;
    JList<FileReference> filesList;
    JComboBox<Type> typeSelector;
    JComboBox<ConversionStatus> statusSelector;
    JComboBox<String> amPmSelector;
    JTextField noteField, technicianNotesField;
    JCheckBox dataOnlyCheckbox;
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
        note = new JLabel("Conversion Note");
        note.setForeground(Color.WHITE);
        note.setFont(Theme.NORMAL_FONT);
        
        noteField = new JTextField(conversion.note);
        noteField.setPreferredSize(new Dimension(400, 30));
        Theme.styleTextField(noteField);
        noteField.setFont(Theme.NORMAL_FONT.deriveFont(14f));
        
        noteRow.add(note);
        noteRow.add(Box.createHorizontalStrut(10));
        noteRow.add(noteField);

        // data-only checkbox
        dataOnlyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataOnlyRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        dataOnlyRow.setBackground(Theme.BACKGROUND);
        
        dataOnlyCheckbox = new JCheckBox("Mark As Data-Only");
        dataOnlyCheckbox.setSelected(conversion.isDataOnly);
        dataOnlyCheckbox.setForeground(Color.WHITE);
        dataOnlyCheckbox.setFont(Theme.NORMAL_FONT);
        dataOnlyCheckbox.setOpaque(false);
        dataOnlyCheckbox.addActionListener(e -> {
            conversion.isDataOnly = dataOnlyCheckbox.isSelected();
            projectFrame.markUnsavedChanges();
        });
        
        dataOnlyRow.add(dataOnlyCheckbox);

        // technician notes
        technicianNotesRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        technicianNotesRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
        technicianNotesRow.setBackground(Theme.BACKGROUND);
        technicianNotes = new JLabel("Technician Notes");
        technicianNotes.setForeground(Color.WHITE);
        technicianNotes.setFont(Theme.NORMAL_FONT);
        
        technicianNotesField = new JTextField(conversion.technicianNotes);
        technicianNotesField.setPreferredSize(new Dimension(400, 30));
        Theme.styleTextField(technicianNotesField);
        technicianNotesField.setFont(Theme.NORMAL_FONT.deriveFont(14f));
        technicianNotesField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
            public void removeUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
            public void insertUpdate(DocumentEvent e) { projectFrame.markUnsavedChanges(); }
        });
        
        technicianNotesRow.add(technicianNotes);
        technicianNotesRow.add(Box.createHorizontalStrut(10));
        technicianNotesRow.add(technicianNotesField);

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
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                File selectedFile = selectedFileRef.getFile();
                if (!selectedFileRef.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "File not found: " + selectedFileRef.getPath() + "\n\nWould you like to relink this file?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        fileChooser.setDialogTitle("Select new location for " + selectedFileRef.getName());
                        
                        // Set the current directory to the last used directory
                        String lastDir = Preferences.getInstance().getLastUsedDirectory();
                        if (lastDir != null && new File(lastDir).exists()) {
                            fileChooser.setCurrentDirectory(new File(lastDir));
                        }
                        
                        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File newLocation = fileChooser.getSelectedFile();
                            File newFile = new File(newLocation, selectedFileRef.getName());
                            
                            if (newFile.exists()) {
                                int index = conversion.linkedFiles.indexOf(selectedFileRef);
                                if (index != -1) {
                                    conversion.linkedFiles.set(index, new FileReference(newFile));
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
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                if (!selectedFileRef.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "File not found: " + selectedFileRef.getPath() + "\n\nWould you like to relink this file?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        fileChooser.setDialogTitle("Select new location for " + selectedFileRef.getName());
                        
                        // Set the current directory to the last used directory
                        String lastDir = Preferences.getInstance().getLastUsedDirectory();
                        if (lastDir != null && new File(lastDir).exists()) {
                            fileChooser.setCurrentDirectory(new File(lastDir));
                        }
                        
                        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File newLocation = fileChooser.getSelectedFile();
                            File newFile = new File(newLocation, selectedFileRef.getName());
                            
                            if (newFile.exists()) {
                                int index = conversion.linkedFiles.indexOf(selectedFileRef);
                                if (index != -1) {
                                    conversion.linkedFiles.set(index, new FileReference(newFile));
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
                        Desktop.getDesktop().open(selectedFileRef.getParentFile());
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
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                StringSelection stringSelection = new StringSelection(selectedFileRef.getPath());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        
        JMenuItem renameFile = new JMenuItem("Rename File");
        renameFile.addActionListener(e -> {
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                String newName = JOptionPane.showInputDialog(this,
                    "Enter new name:",
                    "Rename File",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    selectedFileRef.getName()).toString();
                
                if (newName != null && !newName.trim().isEmpty()) {
                    File selectedFile = selectedFileRef.getFile();
                    File newFile = new File(selectedFile.getParentFile(), newName);
                    if (selectedFile.renameTo(newFile)) {
                        int index = conversion.linkedFiles.indexOf(selectedFileRef);
                        if (index != -1) {
                            conversion.linkedFiles.set(index, new FileReference(newFile));
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
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Select new location for " + selectedFileRef.getName());
                
                // Set the current directory to the last used directory
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    fileChooser.setCurrentDirectory(new File(lastDir));
                }
                
                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File newLocation = fileChooser.getSelectedFile();
                    File newFile = new File(newLocation, selectedFileRef.getName());
                    
                    if (newFile.exists()) {
                        int index = conversion.linkedFiles.indexOf(selectedFileRef);
                        if (index != -1) {
                            conversion.linkedFiles.set(index, new FileReference(newFile));
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
            FileReference selectedFileRef = filesList.getSelectedValue();
            if (selectedFileRef != null) {
                conversion.linkedFiles.remove(selectedFileRef);
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
        
        DefaultListModel<FileReference> listModel = new DefaultListModel<>();
        if(conversion.linkedFiles != null) {
            for (FileReference fileRef : conversion.linkedFiles) {
                listModel.addElement(fileRef);
            }
        } else {
            listModel.addElement(new FileReference("No files attached"));
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
                // Convert FileReferences to Files for the rename operation
                ArrayList<File> filesToRename = new ArrayList<>();
                for (FileReference fileRef : conversion.linkedFiles) {
                    filesToRename.add(fileRef.getFile());
                }
                
                Util.renameFilesWithOptions(
                    filesToRename, 
                    newName,
                    includeSubdirs.isSelected(),
                    preserveNumbering.isSelected()
                );
                updateLinkedFiles();
            }
        });

        addFileBtn = new JButton("Attach File");
        Theme.styleButton(addFileBtn);

        // Show File Map button
        JButton showFileMapBtn = new JButton("Show File Map");
        Theme.styleButton(showFileMapBtn);
        showFileMapBtn.addActionListener(e -> showFileMapDialog());

        filesButtonRow.add(renameOptionsBtn);
        filesButtonRow.add(Box.createHorizontalStrut(10));
        filesButtonRow.add(showFileMapBtn);
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
            
            // Update damage panel visibility
            updateDamagePanelVisibility();
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
        add(Box.createVerticalStrut(1));
        add(noteRow);
        add(Box.createVerticalStrut(1));
        add(technicianNotesRow);
        add(Box.createVerticalStrut(1));
        add(dataOnlyRow);
        add(Box.createVerticalStrut(15));
        add(filesPanel);
        add(Box.createVerticalStrut(15));
        add(statusRow);
        add(Box.createVerticalStrut(15));
        
        // Create damage management panel but don't add it yet
        damagePanel = createDamageManagementPanel();
        
        // Only show damage panel if conversion has damage history or is in a damaged status
        boolean showDamagePanel = (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) ||
                                  conversion.status == ConversionStatus.DAMAGED ||
                                  conversion.status == ConversionStatus.DAMAGE_FIXED ||
                                  conversion.status == ConversionStatus.DAMAGE_IRREVERSIBLE;
        
        if (showDamagePanel) {
            add(damagePanel);
            add(Box.createVerticalStrut(15));
        }
        
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
                    FileReference fileRef = new FileReference(file);
                    if (!conversion.linkedFiles.contains(fileRef)) {
                        conversion.linkedFiles.add(fileRef);
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
        DefaultListModel<FileReference> listModel = new DefaultListModel<>();
        if(conversion.linkedFiles != null) {
            for (FileReference fileRef : conversion.linkedFiles) {
                listModel.addElement(fileRef);
            }
        } else {
            listModel.addElement(new FileReference("No files attached"));
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
        conversion.technicianNotes = technicianNotesField.getText();
        conversion.isDataOnly = dataOnlyCheckbox.isSelected();
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

    private void showFileMapDialog() {
        if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No files are linked to this conversion.",
                "No Files",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create the file map dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "File Map - " + conversion.name, true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Create tree model for file structure with conversion as root
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(conversion.name);
        
        // Add linked files directly as children of the conversion
        for (FileReference fileRef : conversion.linkedFiles) {
            File file = fileRef.getFile();
            if (file.exists()) {
                // Add the linked file directly as a child of the conversion
                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(fileRef.getName());
                fileNode.setUserObject(new FileData(fileRef.getName(), file.getAbsolutePath(), getFileSize(file)));
                rootNode.add(fileNode);
                
                // If it's a directory, explore its contents
                if (file.isDirectory()) {
                    exploreDirectory(file, fileNode);
                }
            } else {
                // Show missing files
                DefaultMutableTreeNode missingNode = new DefaultMutableTreeNode(fileRef.getName() + " (Missing)");
                missingNode.setUserObject(new FileData(fileRef.getName(), fileRef.getPath(), "File not found"));
                rootNode.add(missingNode);
            }
        }

        JTree fileTree = new JTree(rootNode);
        fileTree.setRootVisible(true);
        fileTree.expandRow(0); // Expand root node
        
        // Style the tree with light mode
        fileTree.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileTree.setBackground(Color.WHITE);
        fileTree.setForeground(Color.BLACK);
        fileTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem openFileItem = new JMenuItem("Open File");
        JMenuItem openLocationItem = new JMenuItem("Open File Location");
        JMenuItem viewPropertiesItem = new JMenuItem("View Properties");
        
        contextMenu.add(openFileItem);
        contextMenu.add(openLocationItem);
        contextMenu.add(viewPropertiesItem);
        
        // Add context menu listeners
        openFileItem.addActionListener(e -> {
            FileData fileData = getSelectedFileData(fileTree);
            if (fileData != null && !fileData.getPath().contains("(Missing)")) {
                try {
                    Desktop.getDesktop().open(new File(fileData.getPath()));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Unable to open file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        openLocationItem.addActionListener(e -> {
            FileData fileData = getSelectedFileData(fileTree);
            if (fileData != null && !fileData.getPath().contains("(Missing)")) {
                try {
                    Desktop.getDesktop().open(new File(fileData.getPath()).getParentFile());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Unable to open file location: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        viewPropertiesItem.addActionListener(e -> {
            FileData fileData = getSelectedFileData(fileTree);
            if (fileData != null) {
                String message = "File: " + fileData.getName() + "\nPath: " + fileData.getPath() + "\nSize: " + fileData.getSize();
                JOptionPane.showMessageDialog(dialog, message, "File Properties", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        // Add mouse listener for context menu
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = fileTree.getClosestRowForLocation(e.getX(), e.getY());
                    fileTree.setSelectionRow(row);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileTree);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.setBackground(new Color(240, 240, 240));
        closeButton.setForeground(Color.BLACK);
        closeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        closeButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(closeButton);
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private String getFileSize(File file) {
        try {
            if (file.isDirectory()) {
                return "Directory";
            } else {
                long size = Files.size(file.toPath());
                if (size < 1024) {
                    return size + " bytes";
                } else if (size < 1024 * 1024) {
                    return String.format("%.1f KB", size / 1024.0);
                } else if (size < 1024 * 1024 * 1024) {
                    return String.format("%.1f MB", size / (1024.0 * 1024.0));
                } else {
                    return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
                }
            }
        } catch (IOException e) {
            return "Size unknown";
        }
    }
    
    private void exploreDirectory(File directory, DefaultMutableTreeNode parentNode) {
        try {
            File[] contents = directory.listFiles();
            if (contents != null) {
                for (File item : contents) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(item.getName());
                    childNode.setUserObject(new FileData(item.getName(), item.getAbsolutePath(), getFileSize(item)));
                    parentNode.add(childNode);
                    
                    // Recursively explore subdirectories (but limit depth to avoid performance issues)
                    if (item.isDirectory() && parentNode.getLevel() < 3) { // Limit to 3 levels deep
                        exploreDirectory(item, childNode);
                    }
                }
            }
        } catch (Exception e) {
            // Add error node if we can't read directory
            DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode("Error reading directory");
            parentNode.add(errorNode);
        }
    }
    

    
    private FileData getSelectedFileData(JTree tree) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof FileData) {
            return (FileData) selectedNode.getUserObject();
        }
        return null;
    }
    
    // Helper class for file data
    private static class FileData {
        private final String name;
        private final String path;
        private final String size;
        
        public FileData(String name, String path, String size) {
            this.name = name;
            this.path = path;
            this.size = size;
        }
        
        public String getName() { return name; }
        public String getPath() { return path; }
        public String getSize() { return size; }
        
        @Override
        public String toString() {
            return name;
        }
    }

    private JPanel createDamageManagementPanel() {
        JPanel damagePanel = new JPanel(new BorderLayout());
        damagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        damagePanel.setBackground(Theme.BACKGROUND);
        damagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Damage Management",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.NORMAL_FONT,
                Theme.TEXT
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Damage status buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setOpaque(false);

        JButton markDamagedBtn = new JButton("Mark as Damaged");
        JButton markFixedBtn = new JButton("Mark as Fixed");
        JButton markIrreversibleBtn = new JButton("Mark as Irreversible");
        JButton addDamageEventBtn = new JButton("Add Damage Event");

        Theme.styleButton(markDamagedBtn);
        Theme.styleButton(markFixedBtn);
        Theme.styleButton(markIrreversibleBtn);
        Theme.styleButton(addDamageEventBtn);

        // Set button colors based on status
        markDamagedBtn.setBackground(Color.RED);
        markFixedBtn.setBackground(new Color(255, 165, 0)); // Orange
        markIrreversibleBtn.setBackground(new Color(128, 0, 128)); // Purple

        // Add action listeners
        markDamagedBtn.addActionListener(e -> {
            conversion.status = ConversionStatus.DAMAGED;
            statusSelector.setSelectedItem(ConversionStatus.DAMAGED);
            projectFrame.markUnsavedChanges();
            updateDamagePanelVisibility();
        });

        markFixedBtn.addActionListener(e -> {
            conversion.status = ConversionStatus.DAMAGE_FIXED;
            statusSelector.setSelectedItem(ConversionStatus.DAMAGE_FIXED);
            projectFrame.markUnsavedChanges();
            updateDamagePanelVisibility();
        });

        markIrreversibleBtn.addActionListener(e -> {
            conversion.status = ConversionStatus.DAMAGE_IRREVERSIBLE;
            statusSelector.setSelectedItem(ConversionStatus.DAMAGE_IRREVERSIBLE);
            projectFrame.markUnsavedChanges();
            updateDamagePanelVisibility();
        });

        addDamageEventBtn.addActionListener(e -> showAddDamageEventDialog());

        buttonPanel.add(markDamagedBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(markFixedBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(markIrreversibleBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(addDamageEventBtn);

        // Damage history display
        JTextArea damageHistoryArea = new JTextArea();
        damageHistoryArea.setEditable(false);
        damageHistoryArea.setBackground(Theme.SURFACE);
        damageHistoryArea.setForeground(Theme.TEXT);
        damageHistoryArea.setFont(Theme.NORMAL_FONT);
        damageHistoryArea.setRows(3);

        updateDamageHistoryDisplay(damageHistoryArea);

        JScrollPane scrollPane = new JScrollPane(damageHistoryArea);
        scrollPane.setPreferredSize(new Dimension(400, 80));
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        damagePanel.add(buttonPanel, BorderLayout.NORTH);
        damagePanel.add(scrollPane, BorderLayout.CENTER);

        return damagePanel;
    }

    private void showAddDamageEventDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Damage Event", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        Theme.stylePanel(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Description field
        JLabel descLabel = new JLabel("Damage Description:");
        descLabel.setForeground(Theme.TEXT);
        panel.add(descLabel, gbc);

        JTextArea descArea = new JTextArea(3, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(Theme.SURFACE);
        descArea.setForeground(Theme.TEXT);
        descArea.setFont(Theme.NORMAL_FONT);
        JScrollPane descScroll = new JScrollPane(descArea);
        panel.add(descScroll, gbc);

        // Technician notes field
        JLabel notesLabel = new JLabel("Technician Notes:");
        notesLabel.setForeground(Theme.TEXT);
        panel.add(notesLabel, gbc);

        JTextArea notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(Theme.SURFACE);
        notesArea.setForeground(Theme.TEXT);
        notesArea.setFont(Theme.NORMAL_FONT);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        panel.add(notesScroll, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        JButton addButton = new JButton("Add Event");
        JButton cancelButton = new JButton("Cancel");
        Theme.styleButton(addButton);
        Theme.styleButton(cancelButton);

        addButton.addActionListener(e -> {
            String description = descArea.getText().trim();
            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter a damage description.",
                    "Missing Description",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            conversion.addDamageEvent(description, notesArea.getText().trim());
            projectFrame.markUnsavedChanges();
            dialog.dispose();
            
            // Update the damage history display
            updateDamageHistoryDisplay();
            
            // Show the damage panel if it was hidden
            updateDamagePanelVisibility();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void updateDamageHistoryDisplay() {
        // Find the damage history text area and update it
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (Component subComp : panel.getComponents()) {
                    if (subComp instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) subComp;
                        Component view = scrollPane.getViewport().getView();
                        if (view instanceof JTextArea) {
                            updateDamageHistoryDisplay((JTextArea) view);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateDamageHistoryDisplay(JTextArea textArea) {
        if (conversion.damageHistory == null || conversion.damageHistory.isEmpty()) {
            textArea.setText("No damage events recorded.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Conversion.DamageEvent event : conversion.damageHistory) {
                sb.append(String.format("[%s] %s\n", 
                    event.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    event.description));
                if (!event.technicianNotes.isEmpty()) {
                    sb.append("  Notes: ").append(event.technicianNotes).append("\n");
                }
                sb.append("\n");
            }
            textArea.setText(sb.toString());
        }
    }

    private void updateDamagePanelVisibility() {
        if (damagePanel != null) {
            ConversionStatus currentStatus = (ConversionStatus) statusSelector.getSelectedItem();
            boolean shouldShow = (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) ||
                                currentStatus == ConversionStatus.DAMAGED ||
                                currentStatus == ConversionStatus.DAMAGE_FIXED ||
                                currentStatus == ConversionStatus.DAMAGE_IRREVERSIBLE;
            
            damagePanel.setVisible(shouldShow);
            revalidate();
            repaint();
        }
    }

}
