package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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

import static com.thevideogoat.digitizingassistant.data.Util.isSystemLevelFile;
import static java.util.Objects.requireNonNullElse;

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
        header.setFont(new Font(Theme.HEADER_FONT.getFamily(), Font.BOLD, 18));
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
                // Keep the selection on this conversion after list refresh
                projectFrame.refreshConversionListAndReselect(conversion);
                projectFrame.markUnsavedChanges();
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
        typeSelector.setFont(Theme.NORMAL_FONT.deriveFont(14f));
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
        noteField.setFont(Theme.NORMAL_FONT);
        
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
        technicianNotesField.setFont(Theme.NORMAL_FONT);
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
        
        JMenuItem quickRelink = new JMenuItem("Quick Relink");
        quickRelink.addActionListener(e -> {
            performQuickRelink();
        });
        
        fileMenu.add(openFile);
        fileMenu.add(showInExplorer);
        fileMenu.add(copyPath);
        fileMenu.addSeparator();
        fileMenu.add(renameFile);
        fileMenu.add(relinkMedia);
        fileMenu.add(quickRelink);
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
        renameOptionsBtn.addActionListener(e -> showAdvancedRenameDialog());

        // Relink files button
        JButton relinkBtn = new JButton("Relink Files");
        Theme.styleButton(relinkBtn);
        relinkBtn.addActionListener(e -> showRelinkDialog());

        // Quick Rename to Note button
        JButton renameToNoteBtn = new JButton("Rename to Note");
        Theme.styleButton(renameToNoteBtn);
        renameToNoteBtn.addActionListener(e -> quickRenameToNote());

        addFileBtn = new JButton("Attach File");
        Theme.styleButton(addFileBtn);

        // Show File Map button
        JButton showFileMapBtn = new JButton("Show File Map");
        Theme.styleButton(showFileMapBtn);
        showFileMapBtn.addActionListener(e -> showFileMapDialog());

        filesButtonRow.add(renameToNoteBtn);
        filesButtonRow.add(Box.createHorizontalStrut(10));
        filesButtonRow.add(renameOptionsBtn);
        filesButtonRow.add(Box.createHorizontalStrut(10));
        filesButtonRow.add(showFileMapBtn);
        filesButtonRow.add(Box.createHorizontalStrut(10));
        filesButtonRow.add(relinkBtn);
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
            if (conversion.timeOfConversion == null) {
                // Create spinners with sensible defaults, but do not set conversion's value
                SpinnerNumberModel hhModel = new SpinnerNumberModel(12, 1, 12, 1);
                hhSpinner = new JSpinner(hhModel);
                SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 59, 1);
                minSpinner = new JSpinner(minModel);
                amPmSelector = new JComboBox<>(new String[]{"AM", "PM"});
            } else {
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
            }
            timeRow.add(hhSpinner);
            timeRow.add(minSpinner);
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
            amPmSelector.setFont(Theme.NORMAL_FONT.deriveFont(14f));
            timeRow.add(amPmSelector);
        }
        hhSpinner.setPreferredSize(new Dimension(70, 30));
        minSpinner.setPreferredSize(new Dimension(70, 30));
        
        amPmSelector.setPreferredSize(new Dimension(70, 30));
        amPmSelector.setFont(Theme.NORMAL_FONT.deriveFont(14f));

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
        tapeDurationSpinner.setFont(Theme.NORMAL_FONT.deriveFont(16f));

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
        statusSelector.setFont(Theme.NORMAL_FONT.deriveFont(14f));

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
        // Ensure we have non-null date/time objects to avoid NPEs when loading older JSONs
        if (conversion.dateOfConversion == null) {
            conversion.dateOfConversion = new Date();
        }
        if (conversion.timeOfConversion == null) {
            conversion.timeOfConversion = new Time();
        }
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
                case DAMAGE_FIXED:
                    setBackground(new Color(255, 165, 0)); // Orange for fixed damage
                    break;
                case DAMAGE_IRREVERSIBLE:
                    setBackground(new Color(128, 0, 128)); // Purple for irreversible damage
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
        
        // Auto-expand all nodes to show the complete file structure
        for (int i = 0; i < fileTree.getRowCount(); i++) {
            fileTree.expandRow(i);
        }
        
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

        // Use default button colors for a streamlined look
        markDamagedBtn.setBackground(UIManager.getColor("Button.background"));
        markDamagedBtn.setForeground(UIManager.getColor("Button.foreground"));
        markFixedBtn.setBackground(UIManager.getColor("Button.background"));
        markFixedBtn.setForeground(UIManager.getColor("Button.foreground"));
        markIrreversibleBtn.setBackground(UIManager.getColor("Button.background"));
        markIrreversibleBtn.setForeground(UIManager.getColor("Button.foreground"));

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

    private void showAdvancedRenameDialog() {
        if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No files are linked to rename.",
                "No Files",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

                        // Create main dialog with modern styling
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Advanced Rename Options", true);
                dialog.setSize(950, 600);
                dialog.setLocationRelativeTo(this);
                dialog.setResizable(true);
                dialog.setMinimumSize(new Dimension(650, 550));

                        // Main content panel with scroll capability
                JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
                contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        contentPanel.setBackground(Color.WHITE);

                        // Create main content area with new layout
                JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
                mainContentPanel.setBackground(Color.WHITE);

                // Top row: Strategy and Options panels side by side
                JPanel topRowPanel = new JPanel(new GridLayout(1, 2, 10, 0));
                topRowPanel.setBackground(Color.WHITE);

                // Strategy selection panel with modern styling
                JPanel strategyPanel = createModernPanel("Rename Strategy", new Color(240, 248, 255));
                strategyPanel.setLayout(new BoxLayout(strategyPanel, BoxLayout.Y_AXIS));
                strategyPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 1), 
                        "Rename Strategy", TitledBorder.LEFT, TitledBorder.TOP, 
                        new Font("Segoe UI", Font.BOLD, 12), new Color(60, 60, 60)),
                    BorderFactory.createEmptyBorder(8, 12, 10, 12)
                ));

        ButtonGroup strategyGroup = new ButtonGroup();
        JRadioButton prefixNameBtn = createStyledRadioButton("🏷️ Prefix with conversion name", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratPrefixName());
        JRadioButton prefixNoteBtn = createStyledRadioButton("📝 Prefix with conversion note", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratPrefixNote());
        JRadioButton suffixNameBtn = createStyledRadioButton("🏷️➡️ Suffix with conversion name", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratSuffixName());
        JRadioButton suffixNoteBtn = createStyledRadioButton("📝➡️ Suffix with conversion note", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratSuffixNote());
        JRadioButton replaceNoteBtn = createStyledRadioButton("📝 Rename to conversion note", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratReplaceNote());
        JRadioButton noteNumberBtn = createStyledRadioButton("📝🔢 Note + Number", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratNoteNumber());
        JRadioButton replaceBtn = createStyledRadioButton("🔄 Replace entire filename (classic)", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratReplace());
        JRadioButton smartReplaceBtn = createStyledRadioButton("🧠 Smart replace (generic names only)", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratSmartReplace());
        JRadioButton customBtn = createStyledRadioButton("⚙️ Custom format...", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isStratCustom());

        // Enhanced tooltips with better formatting
        prefixNameBtn.setToolTipText("<html><b>Prefix with Conversion Name</b><br/>Example: 'Smith Family - IMG001.jpg'<br/>Best for: Most digitization workflows</html>");
        prefixNoteBtn.setToolTipText("<html><b>Prefix with Conversion Note</b><br/>Example: 'Beach Trip 2023 - DSC001.jpg'<br/>Requires: Conversion must have a note</html>");
        suffixNameBtn.setToolTipText("<html><b>Suffix with Conversion Name</b><br/>Example: 'IMG001 - Smith Family.jpg'<br/>Best for: When original names are meaningful</html>");
        suffixNoteBtn.setToolTipText("<html><b>Suffix with Conversion Note</b><br/>Example: 'DSC001 - Beach Trip 2023.jpg'<br/>Requires: Conversion must have a note</html>");
        replaceNoteBtn.setToolTipText("<html><b>Rename to Conversion Note</b><br/>Example: 'Beach Trip 2023.jpg'<br/>Best for: Quick directory/file renaming</html>");
        noteNumberBtn.setToolTipText("<html><b>Note + Number</b><br/>Example: 'Beach Trip 2023 001.jpg'<br/>Best for: Preserving order with meaningful names</html>");
        replaceBtn.setToolTipText("<html><b>Replace Entire Filename</b><br/>Example: 'Smith Family.jpg'<br/>Warning: Original information is lost</html>");
        smartReplaceBtn.setToolTipText("<html><b>Smart Replace</b><br/>Only renames generic files like IMG_, DSC_, MOV_<br/>Best for: Mixed collections with meaningful names</html>");
        customBtn.setToolTipText("<html><b>Custom Format</b><br/>Use variables for custom patterns<br/>Advanced: Full control over naming</html>");

        strategyGroup.add(prefixNameBtn);
        strategyGroup.add(prefixNoteBtn);
        strategyGroup.add(suffixNameBtn);
        strategyGroup.add(suffixNoteBtn);
        strategyGroup.add(replaceNoteBtn);
        strategyGroup.add(noteNumberBtn);
        strategyGroup.add(replaceBtn);
        strategyGroup.add(smartReplaceBtn);
        strategyGroup.add(customBtn);

                        strategyPanel.add(prefixNameBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(prefixNoteBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(suffixNameBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(suffixNoteBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(replaceNoteBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(noteNumberBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(replaceBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(smartReplaceBtn);
                strategyPanel.add(Box.createVerticalStrut(4));
                strategyPanel.add(customBtn);

        // Custom format field with modern styling
        JTextField customFormatField = new JTextField(com.thevideogoat.digitizingassistant.data.Preferences.getInstance().getStratCustomFormat());
        customFormatField.setEnabled(customBtn.isSelected());
        customFormatField.setFont(new Font("Consolas", Font.PLAIN, 12));
        customFormatField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        customFormatField.setToolTipText("<html><b>Custom Format Variables:</b><br/>" +
            "• {conversion_name} - Name of the conversion<br/>" +
            "• {conversion_note} - Note text from conversion<br/>" +
            "• {original_name} - Original filename without extension<br/>" +
            "• {original_number} - Numbers extracted from original filename</html>");
        
        JPanel customFormatPanel = new JPanel(new BorderLayout(10, 5));
        customFormatPanel.setBackground(Color.WHITE);
        customFormatPanel.add(new JLabel("Custom Format:"), BorderLayout.WEST);
        customFormatPanel.add(customFormatField, BorderLayout.CENTER);
        strategyPanel.add(Box.createVerticalStrut(10));
        strategyPanel.add(customFormatPanel);

        // Options panel with modern grid layout
        JPanel optionsPanel = createModernPanel("Additional Options", new Color(248, 255, 248));
                        optionsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 200, 100), 1), 
                        "Additional Options", TitledBorder.LEFT, TitledBorder.TOP, 
                        new Font("Segoe UI", Font.BOLD, 12), new Color(60, 60, 60)),
                    BorderFactory.createEmptyBorder(8, 12, 10, 12)
                ));
        optionsPanel.setLayout(new GridBagLayout());
        GridBagConstraints ogbc = new GridBagConstraints();
        ogbc.anchor = GridBagConstraints.WEST;
        // slightly tighter spacing to prevent cropping
        ogbc.insets = new Insets(6, 4, 6, 8);

        JCheckBox includeSubdirs = createStyledCheckBox("📁 Include files in subdirectories", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isRenameIncludeSubdirs());
        includeSubdirs.setToolTipText("<html><b>Include Subdirectories</b><br/>Also rename files inside linked folders<br/>Use when: Conversions link to directories</html>");

        JCheckBox useSequential = createStyledCheckBox("🔢 Use sequential numbers (001, 002, 003...)", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isRenameUseSequential());
        useSequential.setToolTipText("<html><b>Sequential Numbering</b><br/>Replace original numbering with sequence<br/>Result: Consistent numbering regardless of original names</html>");

        JCheckBox addDate = createStyledCheckBox("📅 Add date prefix", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isRenameAddDate());
        addDate.setToolTipText("<html><b>Date Prefix</b><br/>Add current date to beginning of filenames<br/>Example: '2024-01-15 - Smith Family - IMG001.jpg'</html>");

        // New: ignore/delete system files options
        JCheckBox ignoreSystemFiles = createStyledCheckBox("🛡️ Ignore system files (.DS_Store, Thumbs.db, etc.)", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isRenameIgnoreSystemFiles());
        JCheckBox deleteIgnored = createStyledCheckBox("🗑️ Delete ignored system files", com.thevideogoat.digitizingassistant.data.Preferences.getInstance().isRenameDeleteIgnored());

        JLabel separatorLabel = new JLabel("Separator:");
        separatorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JComboBox<String> separatorCombo = new JComboBox<>(new String[]{" - ", "_", " ", "."});
        separatorCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        separatorCombo.setToolTipText("<html><b>Separator Character</b><br/>Character(s) to separate conversion info from filename<br/>Example: 'Smith Family - IMG001.jpg' vs 'Smith Family_IMG001.jpg'</html>");
        separatorCombo.setSelectedItem(com.thevideogoat.digitizingassistant.data.Preferences.getInstance().getRenameSeparator());

        ogbc.gridx = 0; ogbc.gridy = 0; ogbc.gridwidth = 2;
        optionsPanel.add(includeSubdirs, ogbc);
        ogbc.gridy++;
        optionsPanel.add(useSequential, ogbc);
        ogbc.gridy++;
        optionsPanel.add(addDate, ogbc);
        ogbc.gridy++; 
        optionsPanel.add(ignoreSystemFiles, ogbc);
        ogbc.gridy++;
        optionsPanel.add(deleteIgnored, ogbc);
        ogbc.gridy++; ogbc.gridwidth = 1;
        optionsPanel.add(separatorLabel, ogbc);
        ogbc.gridx = 1;
        optionsPanel.add(separatorCombo, ogbc);

        // Preview panel with tree view for directory structure
        JPanel previewPanel = createModernPanel("Preview", new Color(255, 248, 240));
                        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 150, 100), 1), 
                        "📋 Live Preview", TitledBorder.LEFT, TitledBorder.TOP, 
                        new Font("Segoe UI", Font.BOLD, 12), new Color(60, 60, 60)),
                    BorderFactory.createEmptyBorder(8, 12, 10, 12)
                ));
        previewPanel.setLayout(new BorderLayout());
        
        // Create tree for showing file structure
        DefaultMutableTreeNode previewRootNode = new DefaultMutableTreeNode("Preview");
        JTree previewTree = new JTree(previewRootNode);
        previewTree.setRootVisible(false);
        previewTree.setShowsRootHandles(true);
        previewTree.setFont(new Font("Consolas", Font.PLAIN, 11));
        previewTree.setBackground(new Color(252, 252, 252));
        previewTree.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane previewScroll = new JScrollPane(previewTree);
        previewScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewPanel.add(previewScroll, BorderLayout.CENTER);

                                        // Add panels to top row
                topRowPanel.add(strategyPanel);
                topRowPanel.add(optionsPanel);

                // Add top row and preview to main content
                mainContentPanel.add(topRowPanel, BorderLayout.NORTH);
                mainContentPanel.add(previewPanel, BorderLayout.CENTER);

                // Create scroll pane for main content
                JScrollPane mainScrollPane = new JScrollPane(mainContentPanel);
                mainScrollPane.setBorder(null);
                mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);

                contentPanel.add(mainScrollPane, BorderLayout.CENTER);

        // Update preview when options change
        Runnable updatePreview = () -> {
            // Clear the tree
            previewRootNode.removeAllChildren();
            
            String separator = (String) separatorCombo.getSelectedItem();
            String prefix = addDate.isSelected() ? java.time.LocalDate.now().toString() + separator : "";
            boolean includeSubdirectories = includeSubdirs.isSelected();
            
            int fileCount = 0;
            int maxPreview = 50; // Limit to prevent UI slowdown
            
            // Process each linked file/directory
            for (FileReference fileRef : conversion.linkedFiles) {
                if (fileCount >= maxPreview) break;
                
                File file = fileRef.getFile();
                if (file.isDirectory()) {
                    if (includeSubdirectories) {
                        // Show directory contents (files inside will be renamed)
                        DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode("📁 " + file.getName() + "/ (contents will be renamed)");
                        previewRootNode.add(dirNode);
                        fileCount += addDirectoryPreview(dirNode, file, separator, prefix,
                            prefixNameBtn.isSelected(), prefixNoteBtn.isSelected(),
                            suffixNameBtn.isSelected(), suffixNoteBtn.isSelected(),
                            replaceNoteBtn.isSelected(), noteNumberBtn.isSelected(), replaceBtn.isSelected(), smartReplaceBtn.isSelected(),
                            customBtn.isSelected(), customFormatField.getText(),
                            useSequential.isSelected(), fileCount + 1, maxPreview - fileCount,
                            includeSubdirectories);
                    } else {
                        // Show directory rename preview
                        String originalName = file.getName();
                        String newName = generatePreviewName(originalName, separator, prefix,
                            prefixNameBtn.isSelected(), prefixNoteBtn.isSelected(),
                            suffixNameBtn.isSelected(), suffixNoteBtn.isSelected(),
                            replaceNoteBtn.isSelected(), noteNumberBtn.isSelected(), replaceBtn.isSelected(), smartReplaceBtn.isSelected(),
                            customBtn.isSelected(), customFormatField.getText(),
                            useSequential.isSelected(), fileCount + 1);
                        
                        // Handle sequential numbering preview for note-based renaming
                        if ((replaceNoteBtn.isSelected() || noteNumberBtn.isSelected()) && fileCount > 0) {
                            // Extract base name and extension for sequential numbering
                            String baseName = newName;
                            String extension = "";
                            int dotIndex = newName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                baseName = newName.substring(0, dotIndex);
                                extension = newName.substring(dotIndex);
                            }
                            newName = baseName + (fileCount > 0 ? " (" + fileCount + ")" : "") + extension;
                        }
                        
                        DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(
                            "📁 " + originalName + "/ → " + newName + "/");
                        previewRootNode.add(dirNode);
                        fileCount++;
                    }
                } else {
                    // Single file preview
                    String originalName = fileRef.getName();
                    if (ignoreSystemFiles.isSelected() && isSystemLevelFile(originalName)) {
                        continue;
                    }
                    String newName = generatePreviewName(originalName, separator, prefix,
                        prefixNameBtn.isSelected(), prefixNoteBtn.isSelected(),
                        suffixNameBtn.isSelected(), suffixNoteBtn.isSelected(),
                        replaceNoteBtn.isSelected(), noteNumberBtn.isSelected(), replaceBtn.isSelected(), smartReplaceBtn.isSelected(),
                        customBtn.isSelected(), customFormatField.getText(),
                        useSequential.isSelected(), fileCount + 1);
                    
                    // Handle sequential numbering preview for note-based renaming
                    if ((replaceNoteBtn.isSelected() || noteNumberBtn.isSelected()) && fileCount > 0) {
                        // Extract base name and extension for sequential numbering
                        String baseName = newName;
                        String extension = "";
                        int dotIndex = newName.lastIndexOf('.');
                        if (dotIndex > 0) {
                            baseName = newName.substring(0, dotIndex);
                            extension = newName.substring(dotIndex);
                        }
                        newName = baseName + (fileCount > 0 ? " (" + fileCount + ")" : "") + extension;
                    }
                    
                    String icon = "📄";
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(
                        icon + " " + originalName + " → " + newName);
                    previewRootNode.add(fileNode);
                    fileCount++;
                }
            }
            
            if (conversion.linkedFiles.size() > maxPreview || fileCount >= maxPreview) {
                DefaultMutableTreeNode moreNode = new DefaultMutableTreeNode(
                    "... and more files (showing first " + Math.min(fileCount, maxPreview) + ")");
                previewRootNode.add(moreNode);
            }
            
            // Refresh tree display
            ((DefaultTreeModel) previewTree.getModel()).reload();
            
            // Expand all nodes for better visibility
            for (int i = 0; i < previewTree.getRowCount(); i++) {
                previewTree.expandRow(i);
            }
        };

        // Add listeners for preview updates (live)
        prefixNameBtn.addActionListener(e -> updatePreview.run());
        prefixNoteBtn.addActionListener(e -> updatePreview.run());
        suffixNameBtn.addActionListener(e -> updatePreview.run());
        suffixNoteBtn.addActionListener(e -> updatePreview.run());
        replaceNoteBtn.addActionListener(e -> updatePreview.run());
        noteNumberBtn.addActionListener(e -> updatePreview.run());
        replaceBtn.addActionListener(e -> updatePreview.run());
        smartReplaceBtn.addActionListener(e -> updatePreview.run());
        customBtn.addActionListener(e -> {
            customFormatField.setEnabled(customBtn.isSelected());
            if (customBtn.isSelected()) {
                customFormatField.setBackground(Color.WHITE);
            } else {
                customFormatField.setBackground(new Color(240, 240, 240));
            }
            updatePreview.run();
        });
        customFormatField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updatePreview.run(); }
            public void removeUpdate(DocumentEvent e) { updatePreview.run(); }
            public void insertUpdate(DocumentEvent e) { updatePreview.run(); }
        });
        separatorCombo.addActionListener(e -> updatePreview.run());
        useSequential.addActionListener(e -> updatePreview.run());
        addDate.addActionListener(e -> updatePreview.run());
        includeSubdirs.addActionListener(e -> updatePreview.run());
        ignoreSystemFiles.addActionListener(e -> updatePreview.run());
        deleteIgnored.addActionListener(e -> updatePreview.run());

        // Regular button panel with standard styling
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                buttonPanel.setBackground(new Color(248, 248, 248));
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 10, 15));

        JButton previewButton = new JButton("🔄 Refresh Preview");
        JButton renameButton = new JButton("✅ Rename Files");
        JButton cancelButton = new JButton("❌ Cancel");

        previewButton.addActionListener(e -> updatePreview.run());
        
        renameButton.addActionListener(e -> {
            // Validate that at least one rename strategy is selected
            boolean hasStrategy = prefixNameBtn.isSelected() || prefixNoteBtn.isSelected() || 
                                suffixNameBtn.isSelected() || suffixNoteBtn.isSelected() || 
                                replaceNoteBtn.isSelected() || noteNumberBtn.isSelected() || 
                                replaceBtn.isSelected() || smartReplaceBtn.isSelected() || 
                                customBtn.isSelected() || addDate.isSelected();
            
            if (!hasStrategy) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select at least one rename strategy (prefix, suffix, replace, custom, etc.).",
                    "No Strategy Selected",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Validate inputs
            if (prefixNoteBtn.isSelected() || suffixNoteBtn.isSelected() || replaceNoteBtn.isSelected() || noteNumberBtn.isSelected()) {
                if (conversion.note.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                        "Conversion note is empty. Please add a note first or choose a different strategy.",
                        "Missing Note",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            if (customBtn.isSelected() && customFormatField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Please enter a custom format pattern.",
                    "Missing Format",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Persist chosen options
            com.thevideogoat.digitizingassistant.data.Preferences prefs = com.thevideogoat.digitizingassistant.data.Preferences.getInstance();
            prefs.setStratPrefixName(prefixNameBtn.isSelected());
            prefs.setStratPrefixNote(prefixNoteBtn.isSelected());
            prefs.setStratSuffixName(suffixNameBtn.isSelected());
            prefs.setStratSuffixNote(suffixNoteBtn.isSelected());
            prefs.setStratReplaceNote(replaceNoteBtn.isSelected());
            prefs.setStratNoteNumber(noteNumberBtn.isSelected());
            prefs.setStratReplace(replaceBtn.isSelected());
            prefs.setStratSmartReplace(smartReplaceBtn.isSelected());
            prefs.setStratCustom(customBtn.isSelected());
            prefs.setStratCustomFormat(customFormatField.getText());
            prefs.setRenameIncludeSubdirs(includeSubdirs.isSelected());
            prefs.setRenameUseSequential(useSequential.isSelected());
            prefs.setRenameAddDate(addDate.isSelected());
            prefs.setRenameIgnoreSystemFiles(ignoreSystemFiles.isSelected());
            prefs.setRenameDeleteIgnored(deleteIgnored.isSelected());
            prefs.setRenameSeparator((String) separatorCombo.getSelectedItem());

            // Confirm rename operation
            int confirm = JOptionPane.showConfirmDialog(dialog,
                String.format("Rename %d file(s) using the selected strategy?\n\nThis action cannot be undone.", 
                    conversion.linkedFiles.size()),
                "Confirm Rename",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                // Execute rename with selected strategy
                executeAdvancedRename(
                    (String) separatorCombo.getSelectedItem(),
                    addDate.isSelected(),
                    prefixNameBtn.isSelected(), prefixNoteBtn.isSelected(),
                    suffixNameBtn.isSelected(), suffixNoteBtn.isSelected(),
                    replaceNoteBtn.isSelected(), noteNumberBtn.isSelected(), replaceBtn.isSelected(), smartReplaceBtn.isSelected(),
                    customBtn.isSelected(), customFormatField.getText(),
                    includeSubdirs.isSelected(), useSequential.isSelected(),
                    ignoreSystemFiles.isSelected(), deleteIgnored.isSelected()
                );
                dialog.dispose();
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(previewButton);
        buttonPanel.add(renameButton);
        buttonPanel.add(cancelButton);

        // Initial preview
        updatePreview.run();

        // Layout dialog
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String generatePreviewName(String originalName, String separator, String prefix,
            boolean prefixName, boolean prefixNote, boolean suffixName, boolean suffixNote,
            boolean replaceNote, boolean noteNumber, boolean replace, boolean smartReplace, boolean custom, String customFormat,
            boolean useSequential, int sequenceNumber) {
        
        String baseName = originalName;
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        String conversionInfo = conversion.name;
        if ((prefixNote || suffixNote) && !conversion.note.trim().isEmpty()) {
            conversionInfo = conversion.note;
        }

        String result = prefix;

        // Handle "Rename to conversion note" option
        if (replaceNote) {
            custom = true;
            customFormat = "{conversion_note}";
        }
        
        // Handle "Note + Number" option
        if (noteNumber) {
            custom = true;
            customFormat = "{conversion_note} {original_number}";
        }

        if (custom) {
            result += customFormat
                .replace("{conversion_name}", conversion.name)
                .replace("{conversion_note}", conversion.note)
                .replace("{original_name}", baseName)
                .replace("{original_number}", useSequential ? String.format("%03d", sequenceNumber) : extractNumbers(baseName));
        } else if (prefixName || prefixNote) {
            result += conversionInfo + separator + baseName;
        } else if (suffixName || suffixNote) {
            result += baseName + separator + conversionInfo;
        } else if (replace) {
            result += conversionInfo + (useSequential ? String.format(" (%03d)", sequenceNumber) : "");
        } else if (smartReplace) {
            if (isGenericFilename(baseName)) {
                result += conversionInfo + separator + (useSequential ? String.format("%03d", sequenceNumber) : extractNumbers(baseName));
            } else {
                result += originalName; // Keep original if not generic
                return result; // Return early to avoid adding extension again
            }
        }

        return result + extension;
    }

    private boolean isGenericFilename(String filename) {
        String lower = filename.toLowerCase();
        return lower.startsWith("img_") || lower.startsWith("dsc_") || lower.startsWith("mov_") ||
               lower.startsWith("vid_") || lower.startsWith("pic_") || lower.startsWith("p") ||
               lower.matches("\\d+") || lower.matches("track\\d+") || lower.matches("audio_\\d+") ||
               lower.startsWith("dsc") || lower.startsWith("img") || lower.startsWith("mov") ||
               lower.startsWith("vid") || lower.startsWith("pic") ||
               lower.endsWith("_img") || lower.endsWith("_dsc") || lower.endsWith("_mov") ||
               lower.endsWith("_vid") || lower.endsWith("_pic") || lower.endsWith("_photo") ||
               lower.endsWith("_image") || lower.endsWith("_audio") || lower.endsWith("_track") ||
               lower.matches(".*\\d{3,4}.*") || // Contains 3-4 digit numbers
               lower.matches(".*\\d{2,3}\\..*"); // Contains 2-3 digits before extension
    }

    private String extractNumbers(String filename) {
        return filename.replaceAll("[^0-9]", "");
    }

    private int addDirectoryPreview(DefaultMutableTreeNode parentNode, File directory, String separator, String prefix,
            boolean prefixName, boolean prefixNote, boolean suffixName, boolean suffixNote,
            boolean replaceNote, boolean noteNumber, boolean replace, boolean smartReplace, boolean custom, String customFormat,
            boolean useSequential, int startSequence, int maxFiles, boolean includeSubdirectories) {
        
        File[] files = directory.listFiles();
        if (files == null || maxFiles <= 0) return 0;
        
        int fileCount = 0;
        int sequenceNumber = startSequence;
        
        // Sort files for consistent preview (directories first, then files)
        java.util.Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        
        for (File file : files) {
            if (fileCount >= maxFiles) break;
            
            String originalName = file.getName();
            String icon = file.isDirectory() ? "📁" : "📄";
            
            if (file.isDirectory()) {
                // Show directory (not renamed, just listed)
                DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(icon + " " + originalName + "/");
                parentNode.add(dirNode);
                
                // Only recurse into subdirectories if includeSubdirectories is enabled
                // and we haven't exceeded depth limits
                if (includeSubdirectories && parentNode.getLevel() < 2) { // Limit to 2 levels deep
                    int subFileCount = addDirectoryPreview(dirNode, file, separator, prefix,
                        prefixName, prefixNote, suffixName, suffixNote,
                        replaceNote, noteNumber, replace, smartReplace, custom, customFormat,
                        useSequential, sequenceNumber, Math.min(10, maxFiles - fileCount - 1), includeSubdirectories);
                    fileCount += subFileCount;
                    sequenceNumber += subFileCount;
                }
                fileCount++;
            } else {
                // Show file with rename preview
                String newName = generatePreviewName(originalName, separator, prefix,
                    prefixName, prefixNote, suffixName, suffixNote,
                    replaceNote, noteNumber, replace, smartReplace, custom, customFormat,
                    useSequential, sequenceNumber);

                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(
                    icon + " " + originalName + " → " + newName);
                // Store original and proposed names for future overrides
                fileNode.setUserObject(new FileData(originalName, newName, ""));
                parentNode.add(fileNode);
                fileCount++;
                
                if (useSequential) {
                    sequenceNumber++;
                }
            }
        }
        
        if (files.length > fileCount) {
            DefaultMutableTreeNode moreNode = new DefaultMutableTreeNode(
                "... (" + (files.length - fileCount) + " more items)");
            parentNode.add(moreNode);
        }
        
        return fileCount;
    }

    private void quickRenameToNote() {
        if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No files are linked to rename.",
                "No Files",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (conversion.note.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Conversion note is empty. Please add a note first.",
                "Missing Note",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm rename operation
        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Rename linked directories to \"%s\"?\n\nThis action cannot be undone.", 
                conversion.note),
            "Confirm Quick Rename",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Convert FileReferences to Files for the rename operation
                ArrayList<File> filesToRename = new ArrayList<>();
                for (FileReference fileRef : conversion.linkedFiles) {
                    filesToRename.add(fileRef.getFile());
                }
                
                // Use custom format with just the conversion note
                int renamedCount = Util.renameFilesWithAdvancedOptions(
                    filesToRename,
                    conversion.name,
                    conversion.note,
                    " - ", // Default separator
                    false, // No date prefix
                    false, false, false, false, // No prefix/suffix
                    false, false, // No replace/smart replace
                    true, "{conversion_note}", // Custom format with conversion note
                    false, true, // No subdirectories, but use sequential for conflicts
                    conversion
                );
                
                if (renamedCount > 0) {
                    // References are automatically updated by the rename method
                    updateLinkedFiles();
                    projectFrame.markUnsavedChanges();
                }
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error during rename operation: " + ex.getMessage(),
                    "Rename Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void executeAdvancedRename(String separator, boolean addDate,
            boolean prefixName, boolean prefixNote, boolean suffixName, boolean suffixNote,
            boolean replaceNote, boolean noteNumber, boolean replace, boolean smartReplace, boolean custom, String customFormat,
            boolean includeSubdirs, boolean useSequential, boolean ignoreSystemFiles, boolean deleteIgnored) {
        
        try {
            // Validate that at least one rename strategy is selected
            boolean hasStrategy = prefixName || prefixNote || suffixName || suffixNote || 
                                replaceNote || noteNumber || replace || smartReplace || custom || addDate;
            
            if (!hasStrategy) {
                JOptionPane.showMessageDialog(this,
                    "Please select at least one rename strategy (prefix, suffix, replace, custom, etc.).",
                    "No Strategy Selected",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Convert FileReferences to Files for the rename operation
            ArrayList<File> filesToRename = new ArrayList<>();
            for (FileReference fileRef : conversion.linkedFiles) {
                // Optionally skip system-level files
                if (ignoreSystemFiles && isSystemLevelFile(fileRef.getName())) {
                    continue;
                }
                filesToRename.add(fileRef.getFile());
            }
            
            // Check if we have any files to rename after filtering
            if (filesToRename.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No files available to rename (all files may have been filtered out).",
                    "No Files to Rename",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Handle "Rename to conversion note" option
            if (replaceNote) {
                custom = true;
                customFormat = "{conversion_note}";
                // Force sequential numbering to prevent conflicts when renaming multiple files
                useSequential = true;
            }
            
            // Handle "Note + Number" option
            if (noteNumber) {
                custom = true;
                customFormat = "{conversion_note} {original_number}";
                // Force sequential numbering to prevent conflicts when renaming multiple files
                useSequential = true;
            }
            
            // Debug: Show what strategy is being used
            String strategyInfo = "Strategy: ";
            if (replaceNote) strategyInfo += "Replace with Note ";
            else if (noteNumber) strategyInfo += "Note + Number ";
            else if (custom) strategyInfo += "Custom (" + customFormat + ") ";
            else if (prefixName) strategyInfo += "Prefix Name ";
            else if (prefixNote) strategyInfo += "Prefix Note ";
            else if (suffixName) strategyInfo += "Suffix Name ";
            else if (suffixNote) strategyInfo += "Suffix Note ";
            else if (replace) strategyInfo += "Replace ";
            else if (smartReplace) strategyInfo += "Smart Replace ";
            else if (addDate) strategyInfo += "Add Date ";
            else strategyInfo += "Unknown ";
            
            System.out.println("Advanced Rename Debug: " + strategyInfo + "| Files: " + filesToRename.size());
            
            int renamedCount = Util.renameFilesWithAdvancedOptions(
                filesToRename,
                conversion.name,
                conversion.note,
                separator,
                addDate,
                prefixName, prefixNote, suffixName, suffixNote,
                replace, smartReplace, custom, customFormat,
                includeSubdirs, useSequential,
                conversion
            );
            
            if (renamedCount > 0) {
                // References are automatically updated by the rename method
                updateLinkedFiles();
                projectFrame.markUnsavedChanges();
            }

            // Optionally delete ignored system files from disk
            if (ignoreSystemFiles && deleteIgnored) {
                for (FileReference fileRef : conversion.linkedFiles) {
                    if (isSystemLevelFile(fileRef.getName())) {
                        try { fileRef.getFile().delete(); } catch (Exception ignore) {}
                    }
                }
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error during rename operation: " + ex.getMessage(),
                "Rename Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRelinkDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose folder to search for media");
        String lastDir = com.thevideogoat.digitizingassistant.data.Preferences.getInstance().getLastUsedDirectory();
        if (lastDir != null && new java.io.File(lastDir).exists()) {
            chooser.setCurrentDirectory(new java.io.File(lastDir));
        }
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File root = chooser.getSelectedFile();
        if (root != null && root.exists()) {
            com.thevideogoat.digitizingassistant.data.Preferences.getInstance().setLastUsedDirectory(root.getAbsolutePath());
        }

        // Load saved preferences for checkbox states
        com.thevideogoat.digitizingassistant.data.Preferences prefs = com.thevideogoat.digitizingassistant.data.Preferences.getInstance();
        JCheckBox byNote = new JCheckBox("Match conversion note", prefs.isRelinkByNote());
        JCheckBox byTitle = new JCheckBox("Match conversion title", prefs.isRelinkByTitle());
        JCheckBox byTrimmed = new JCheckBox("Match trimmed filenames (_trimmed)", prefs.isRelinkByTrimmed());

        JPanel opts = new JPanel(new GridLayout(0,1));
        opts.add(byNote); opts.add(byTitle); opts.add(byTrimmed);

        int ok = JOptionPane.showConfirmDialog(this, opts, "Relink Search Options", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        
        // Save the checkbox states to preferences
        prefs.setRelinkByNote(byNote.isSelected());
        prefs.setRelinkByTitle(byTitle.isSelected());
        prefs.setRelinkByTrimmed(byTrimmed.isSelected());

        java.util.List<File> matches = new java.util.ArrayList<>();
        java.util.function.Predicate<File> predicate = f -> {
            if (f.isDirectory()) return false;
            String lowerName = f.getName().toLowerCase();
            if (lowerName.endsWith(".llc")) return false; // ignore LosslessCut project files

            boolean matched = false;

            // Exact match on note (base name equals conversion note), any extension
            if (byNote.isSelected() && conversion.note != null && !conversion.note.isBlank()) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                matched |= base.equals(conversion.note.toLowerCase());
            }

            // Exact match on title with .mp4 extension
            if (!matched && byTitle.isSelected()) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                String ext = "";
                if (dot > 0) {
                    ext = base.substring(dot);
                    base = base.substring(0, dot);
                }
                matched |= base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4");
            }

            // Trimmed match: title_... with "trimmed" somewhere
            if (!matched && byTrimmed.isSelected()) {
                String baseTitle = conversion.name.toLowerCase();
                matched |= lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed");
            }
            return matched;
        };

        java.util.Deque<File> stack = new java.util.ArrayDeque<>();
        stack.push(root);
        while(!stack.isEmpty()){
            File dir = stack.pop();
            File[] list = dir.listFiles();
            if (list == null) continue;
            for (File f : list) {
                if (f.isDirectory()) { stack.push(f); continue; }
                if (predicate.test(f)) matches.add(f);
            }
        }

        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching files found.", "Relink", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Prioritize results: 1) note exact, 2) trimmed, 3) title exact mp4
        java.util.function.ToIntFunction<File> rank = f -> {
            String lowerName = f.getName().toLowerCase();
            String base = lowerName;
            int dot = base.lastIndexOf('.');
            String ext = "";
            if (dot > 0) { ext = base.substring(dot); base = base.substring(0, dot); }

            // note exact
            if (byNote.isSelected() && conversion.note != null && !conversion.note.isBlank()) {
                if (base.equals(conversion.note.toLowerCase())) return 0;
            }
            // trimmed
            if (byTrimmed.isSelected()) {
                String baseTitle = conversion.name.toLowerCase();
                if (lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed")) return 1;
            }
            // title exact mp4
            if (byTitle.isSelected()) {
                if (base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4")) return 2;
            }
            return 3;
        };
        matches.sort(java.util.Comparator.comparingInt(rank).thenComparing(File::getName));

        JList<File> list = new JList<>(matches.toArray(new File[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(600, 300));
        int choose = JOptionPane.showConfirmDialog(this, sp, "Select files to link", JOptionPane.OK_CANCEL_OPTION);
        if (choose != JOptionPane.OK_OPTION) return;

        java.util.List<File> selected = list.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) return;
        if (conversion.linkedFiles == null) conversion.linkedFiles = new ArrayList<>();
        for (File f : selected) {
            conversion.linkedFiles.add(new FileReference(f.getAbsolutePath()));
        }
        updateLinkedFiles();
        projectFrame.markUnsavedChanges();
    }
    
    private void performQuickRelink() {
        // Get the last used directory from preferences
        String lastDir = com.thevideogoat.digitizingassistant.data.Preferences.getInstance().getLastUsedDirectory();
        if (lastDir == null || !new File(lastDir).exists()) {
            JOptionPane.showMessageDialog(this,
                "No previous relink directory found. Please use the regular 'Relink Media' option first.",
                "Quick Relink",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        File root = new File(lastDir);
        
        // Get saved relink preferences
        com.thevideogoat.digitizingassistant.data.Preferences prefs = com.thevideogoat.digitizingassistant.data.Preferences.getInstance();
        boolean byNote = prefs.isRelinkByNote();
        boolean byTitle = prefs.isRelinkByTitle();
        boolean byTrimmed = prefs.isRelinkByTrimmed();
        
        // Use the same search logic as showRelinkDialog()
        java.util.List<File> matches = new java.util.ArrayList<>();
        java.util.function.Predicate<File> predicate = f -> {
            if (f.isDirectory()) return false;
            String lowerName = f.getName().toLowerCase();
            if (lowerName.endsWith(".llc")) return false; // ignore LosslessCut project files

            boolean matched = false;

            // Exact match on note (base name equals conversion note), any extension
            if (byNote && conversion.note != null && !conversion.note.isBlank()) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                matched |= base.equals(conversion.note.toLowerCase());
            }

            // Exact match on title with .mp4 extension
            if (!matched && byTitle) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                String ext = "";
                if (dot > 0) {
                    ext = base.substring(dot);
                    base = base.substring(0, dot);
                }
                matched |= base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4");
            }

            // Trimmed match: title_... with "trimmed" somewhere
            if (!matched && byTrimmed) {
                String baseTitle = conversion.name.toLowerCase();
                matched |= lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed");
            }
            return matched;
        };

        java.util.Deque<File> stack = new java.util.ArrayDeque<>();
        stack.push(root);
        while(!stack.isEmpty()){
            File dir = stack.pop();
            File[] list = dir.listFiles();
            if (list == null) continue;
            for (File f : list) {
                if (f.isDirectory()) { stack.push(f); continue; }
                if (predicate.test(f)) matches.add(f);
            }
        }

        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No matching files found using saved settings.", 
                "Quick Relink", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Prioritize results: 1) note exact, 2) trimmed, 3) title exact mp4
        java.util.function.ToIntFunction<File> rank = f -> {
            String lowerName = f.getName().toLowerCase();
            String base = lowerName;
            int dot = base.lastIndexOf('.');
            String ext = "";
            if (dot > 0) { ext = base.substring(dot); base = base.substring(0, dot); }

            // note exact
            if (byNote && conversion.note != null && !conversion.note.isBlank()) {
                if (base.equals(conversion.note.toLowerCase())) return 0;
            }
            // trimmed
            if (byTrimmed) {
                String baseTitle = conversion.name.toLowerCase();
                if (lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed")) return 1;
            }
            // title exact mp4
            if (byTitle) {
                if (base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4")) return 2;
            }
            return 3;
        };
        matches.sort(java.util.Comparator.comparingInt(rank).thenComparing(File::getName));

        // Auto-select the best match (first in sorted list) for quick relink
        File bestMatch = matches.get(0);
        
        // Clear existing linked files and add the best match
        if (conversion.linkedFiles == null) conversion.linkedFiles = new ArrayList<>();
        conversion.linkedFiles.clear();
        conversion.linkedFiles.add(new FileReference(bestMatch.getAbsolutePath()));
        
        updateLinkedFiles();
        projectFrame.markUnsavedChanges();
        
        // Show success message
        JOptionPane.showMessageDialog(this,
            "Quick relink completed. Linked to: " + bestMatch.getName(),
            "Quick Relink",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // Helper methods for modern UI styling
    private JPanel createModernPanel(String title, Color backgroundColor) {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        return panel;
    }

    private JRadioButton createStyledRadioButton(String text, boolean selected) {
        JRadioButton button = new JRadioButton(text, selected);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(60, 60, 60));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return button;
    }

    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        checkBox.setBackground(Color.WHITE);
        checkBox.setForeground(new Color(60, 60, 60));
        checkBox.setFocusPainted(false);
        checkBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return checkBox;
    }



}
