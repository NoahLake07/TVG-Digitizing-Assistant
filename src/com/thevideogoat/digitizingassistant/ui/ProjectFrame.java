package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import com.thevideogoat.digitizingassistant.data.FileReference;
import com.thevideogoat.digitizingassistant.util.FileCacheManager;
import com.thevideogoat.digitizingassistant.util.ExportUtil;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel, detailsPanel;
    JScrollPane conversionScrollPane;
    JSplitPane splitPane;
    Project project;
    JTextField searchField;
    private Point mouseDownCompCoords;
    private JPanel statusBar;
    private JButton newConversion;
    private JLabel saveStatusLabel;
    private Timer saveStatusTimer;
    private boolean hasUnsavedChanges = false;
    private JComboBox<String> sortByDropdown;

    public ProjectFrame(Project project) {
        super();
        this.project = project;
        setUndecorated(true);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DigitizingAssistant.setIcon(this);

        // Initialize save status timer
        saveStatusTimer = new Timer(3000, e -> {
            saveStatusLabel.setText("");
            saveStatusTimer.stop();
        });
        saveStatusTimer.setRepeats(false);

        // Main container with modern styling
        JPanel mainPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(mainPanel);
        mainPanel.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        // Add title bar
        mainPanel.add(createTitleBar(), BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setupUI(contentPanel);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Add resize functionality
        addResizeBorder(mainPanel);
        
        add(mainPanel);
        setVisible(true);
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(Theme.SURFACE);
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        
        ImageIcon icon = new ImageIcon(DigitizingAssistant.getIcon().getScaledInstance(35, 35, Image.SCALE_SMOOTH));
        JLabel iconLabel = new JLabel(icon);
        
        JLabel title = new JLabel(project.getName() + " - TVG Digitizing Assistant v" + DigitizingAssistant.VERSION);
        title.setFont(Theme.HEADER_FONT);
        title.setForeground(Theme.TEXT);
        
        titlePanel.add(iconLabel);
        titlePanel.add(title);
        
        // Window controls with menu
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        controls.setOpaque(false);
        
        // Add menu button
        JButton menuButton = new JButton("≡");
        menuButton.setFont(new Font(menuButton.getFont().getName(), Font.PLAIN, 20));
        menuButton.setForeground(Theme.TEXT);
        menuButton.setBackground(Theme.SURFACE.darker());
        menuButton.setBorderPainted(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        menuButton.setToolTipText("Project menu - Access tools and options");
        menuButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                menuButton.setForeground(Theme.ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                menuButton.setForeground(Theme.TEXT);
            }
        });
        
        // Create popup menu with simple styling
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem refreshList = new JMenuItem("Refresh List");
        JMenuItem exportProject = new JMenuItem("Export Project as JSON");
        JMenuItem exportDigitizingSheet = new JMenuItem("Export Digitizing Sheet");
        JMenuItem exportFileMap = new JMenuItem("Export File Map");
        JMenuItem mediaStats = new JMenuItem("Media Statistics");
        JMenuItem relinkTrimmed = new JMenuItem("Relink to Trimmed Files");
        JMenuItem findAndRelinkTrimmed = new JMenuItem("Find and Relink Trimmed Media");
        JMenuItem renameProjectFiles = new JMenuItem("Rename Project Files");
        JMenuItem openProjectFolder = new JMenuItem("Open Project Folder");
        
        // Add tooltips to menu items
        refreshList.setToolTipText("Refresh the conversion list to show any external changes");
        exportProject.setToolTipText("Export project data as JSON file for backup or sharing");
        exportDigitizingSheet.setToolTipText("Export a detailed digitizing sheet with conversion information");
        exportFileMap.setToolTipText("Export a comprehensive map of all files in the project");
        mediaStats.setToolTipText("View statistics about all media files in the project");
        relinkTrimmed.setToolTipText("Relink files to their trimmed versions in the same directory");
        findAndRelinkTrimmed.setToolTipText("Search subdirectories for trimmed versions of linked files");
        renameProjectFiles.setToolTipText("Bulk rename all linked files in the project");
        openProjectFolder.setToolTipText("Open the project folder in file explorer");
        
        refreshList.addActionListener(e -> refreshConversionList());
        exportProject.addActionListener(e -> exportProjectAsJson());
        exportDigitizingSheet.addActionListener(e -> exportDigitizingSheet());
        exportFileMap.addActionListener(e -> exportFileMap());
        mediaStats.addActionListener(e -> showMediaStatistics());
        relinkTrimmed.addActionListener(e -> Util.relinkToTrimmedFiles(project));
        findAndRelinkTrimmed.addActionListener(e -> {
            ArrayList<FileReference> allFiles = Util.getLinkedFiles(project);
            if (allFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No files are linked to search for trimmed versions.",
                    "No Files",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get all unique parent directories
            Set<File> parentDirs = new HashSet<>();
            for (FileReference fileRef : allFiles) {
                parentDirs.add(fileRef.getParentFile());
            }
            
            // Search each parent directory and its subdirectories
            Map<FileReference, File> trimmedFileMap = new HashMap<>();
            for (File parentDir : parentDirs) {
                if (parentDir != null && parentDir.exists()) {
                    searchForTrimmedFiles(parentDir, allFiles, trimmedFileMap);
                }
            }
            
            if (trimmedFileMap.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No trimmed versions of files were found in any subdirectories.",
                    "No Trimmed Files Found",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Show preview of files to be relinked
            StringBuilder preview = new StringBuilder("Found the following trimmed files:\n\n");
            for (Map.Entry<FileReference, File> entry : trimmedFileMap.entrySet()) {
                preview.append(entry.getKey().getName())
                      .append(" → ")
                      .append(entry.getValue().getName())
                      .append("\n");
            }
            preview.append("\nWould you like to relink to these trimmed files?");
            
            int choice = JOptionPane.showConfirmDialog(this,
                preview.toString(),
                "Relink to Trimmed Files",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                // Log the start of the operation
                logFileOperation("START RELINK OPERATION", "Relinking " + trimmedFileMap.size() + " files to their trimmed versions");
                
                // Relink the files
                for (Map.Entry<FileReference, File> entry : trimmedFileMap.entrySet()) {
                    FileReference originalFileRef = entry.getKey();
                    File trimmedFile = entry.getValue();
                    
                    // Find and update the file in all conversions
                    for (Conversion c : project.getConversions()) {
                        int index = c.linkedFiles.indexOf(originalFileRef);
                        if (index != -1) {
                            c.linkedFiles.set(index, new FileReference(trimmedFile));
                            // Log each relink operation
                            logFileOperation("RELINK", 
                                "Original: " + originalFileRef.getPath() + 
                                " → New: " + trimmedFile.getAbsolutePath() +
                                " (Conversion: " + c.name + ")");
                        }
                    }
                }
                markUnsavedChanges();
                
                // Log the completion
                logFileOperation("END RELINK OPERATION", "Successfully relinked " + trimmedFileMap.size() + " files");
                
                JOptionPane.showMessageDialog(this,
                    "Successfully relinked to trimmed files.\nA log file has been created in the logs directory.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        renameProjectFiles.addActionListener(e -> {
            // Create options dialog
            String[] options = {
                "Rename to conversion names",
                "Rename to conversion notes",
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
            
            int renameChoice = JOptionPane.showOptionDialog(
                this,
                dialogPanel,
                "Project Rename Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (renameChoice == 3 || renameChoice == JOptionPane.CLOSED_OPTION) return; // User cancelled
            
            // Get all files in the project
            ArrayList<FileReference> allFiles = Util.getLinkedFiles(project);
            if (allFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No files are linked to rename.",
                    "No Files",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show one confirmation dialog for the entire operation
            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("This will rename %d file(s) in the project. Continue?", allFiles.size()),
                "Confirm Bulk Rename",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                String operationType = "";
                if (renameChoice == 0) {
                    operationType = "conversion names";
                } else if (renameChoice == 1) {
                    operationType = "conversion notes";
                } else if (renameChoice == 2) {
                    String newName = JOptionPane.showInputDialog(this,
                        "Enter custom name:",
                        "Custom Filename",
                        JOptionPane.PLAIN_MESSAGE);
                    if (newName == null || newName.trim().isEmpty()) {
                        return;
                    }
                    operationType = "custom name: " + newName;
                }
                
                logFileOperation("START RENAME OPERATION", 
                    "Renaming " + allFiles.size() + " files using option: " + operationType);
                
                if (renameChoice == 0) {
                    // Rename to conversion names
                    for (Conversion c : project.getConversions()) {
                        if (!c.linkedFiles.isEmpty()) {
                            ArrayList<FileReference> originalFiles = new ArrayList<>(c.linkedFiles);
                                                    Util.renameFilesWithOptionsFromReferences(
                            c.linkedFiles,
                            c.name,
                            includeSubdirs.isSelected(),
                            preserveNumbering.isSelected()
                        );
                            updateLinkedFilesAfterRename(c, c.name);
                            for (int i = 0; i < originalFiles.size(); i++) {
                                logFileOperation("RENAME", 
                                    "Original: " + originalFiles.get(i).getPath() + 
                                    " → New: " + c.linkedFiles.get(i).getPath() +
                                    " (Conversion: " + c.name + ")");
                            }
                        }
                    }
                } else if (renameChoice == 1) {
                    // Rename to conversion notes
                    for (Conversion c : project.getConversions()) {
                        if (!c.linkedFiles.isEmpty() && !c.note.isEmpty()) {
                            ArrayList<FileReference> originalFiles = new ArrayList<>(c.linkedFiles);
                            Util.renameFilesWithOptionsFromReferences(
                                c.linkedFiles,
                                c.note,
                                includeSubdirs.isSelected(),
                                preserveNumbering.isSelected()
                            );
                            updateLinkedFilesAfterRename(c, c.note);
                            for (int i = 0; i < originalFiles.size(); i++) {
                                logFileOperation("RENAME", 
                                    "Original: " + originalFiles.get(i).getPath() + 
                                    " → New: " + c.linkedFiles.get(i).getPath() +
                                    " (Conversion: " + c.name + ")");
                            }
                        }
                    }
                } else if (renameChoice == 2) {
                    // Custom name
                    String newName = JOptionPane.showInputDialog(this,
                        "Enter custom name:",
                        "Custom Filename",
                        JOptionPane.PLAIN_MESSAGE);
                    if (newName != null && !newName.trim().isEmpty()) {
                        ArrayList<FileReference> originalFiles = new ArrayList<>(allFiles);
                        // Convert FileReferences to Files for the rename operation
                        ArrayList<File> filesToRename = new ArrayList<>();
                        for (FileReference fileRef : allFiles) {
                            filesToRename.add(fileRef.getFile());
                        }
                        Util.renameFilesWithOptions(
                            filesToRename,
                            newName,
                            includeSubdirs.isSelected(),
                            preserveNumbering.isSelected()
                        );
                        // Update all conversions' linkedFiles
                        for (Conversion c : project.getConversions()) {
                            updateLinkedFilesAfterRename(c, newName);
                        }
                        for (int i = 0; i < originalFiles.size(); i++) {
                            logFileOperation("RENAME", 
                                "Original: " + originalFiles.get(i).getPath() + 
                                " → New: " + allFiles.get(i).getPath());
                        }
                    }
                }
                
                logFileOperation("END RENAME OPERATION", "Successfully renamed " + allFiles.size() + " files");
                
                JOptionPane.showMessageDialog(this,
                    "Successfully renamed files.\nA log file has been created in the logs directory.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        // Add menu item for relinking by conversion note
        JMenuItem relinkByNote = new JMenuItem("Relink by Conversion Note");
        relinkByNote.addActionListener(e -> {
            ArrayList<FileReference> allFiles = getAllFilesInProjectDirectories();
            int relinked = 0;
            for (Conversion c : project.getConversions()) {
                String noteNorm = normalizeFilename(c.note);
                for (FileReference fileRef : allFiles) {
                    String fileNorm = normalizeFilename(fileRef.getName());
                    if (!noteNorm.isEmpty() && fileNorm.contains(noteNorm)) {
                        c.linkedFiles.clear();
                        c.linkedFiles.add(fileRef);
                        relinked++;
                        logFileOperation("RELINK BY NOTE", "Linked conversion '" + c.name + "' to file: " + fileRef.getPath());
                        break;
                    }
                }
            }
            JOptionPane.showMessageDialog(this,
                "Relinked " + relinked + " conversions by note.",
                "Relink by Note",
                JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(relinkByNote);
        
        openProjectFolder.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DigitizingAssistant.PROJECTS_DIRECTORY);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not open project folder: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        menu.add(refreshList);
        menu.add(exportProject);
        menu.add(exportDigitizingSheet);
        menu.add(exportFileMap);
        
        // Add Smart Renaming menu item
        JMenuItem smartRename = new JMenuItem("Smart Renaming Analysis");
        smartRename.setToolTipText("Analyze content and automatically choose the best renaming strategy");
        smartRename.addActionListener(e -> showSmartRenameDialog());
        menu.add(smartRename);
        menu.addSeparator();
        menu.add(mediaStats);
        menu.add(relinkTrimmed);
        menu.add(findAndRelinkTrimmed);
        menu.add(renameProjectFiles);
        
        // Add Write Conversions To Destination menu item
        JMenuItem writeToDestination = new JMenuItem("Write Conversions To Destination");
        writeToDestination.setToolTipText("Export all conversions to a destination folder for client delivery");
        writeToDestination.addActionListener(e -> showWriteToDestinationDialog());
        menu.add(writeToDestination);
        
        menu.addSeparator();
        menu.add(openProjectFolder);
        
        menuButton.addActionListener(e -> {
            menu.show(menuButton, 0, menuButton.getHeight());
        });
        
        // Window control buttons
        JLabel minimizeBtn = new JLabel("−");
        JLabel maximizeBtn = new JLabel("□");
        JLabel closeBtn = new JLabel("×");
        
        for (JLabel btn : new JLabel[]{minimizeBtn, maximizeBtn, closeBtn}) {
            btn.setForeground(Theme.TEXT);
            btn.setFont(Theme.HEADER_FONT);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        minimizeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setState(Frame.ICONIFIED);
            }
            public void mouseEntered(MouseEvent e) {
                minimizeBtn.setForeground(Theme.ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                minimizeBtn.setForeground(Theme.TEXT);
            }
        });

        maximizeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ((getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
                    setExtendedState(Frame.NORMAL);
                } else {
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                }
            }
            public void mouseEntered(MouseEvent e) {
                maximizeBtn.setForeground(Theme.ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                maximizeBtn.setForeground(Theme.TEXT);
            }
        });

        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                saveProject();
                dispose();
                DigitizingAssistant.getInstance().chooseProject();
            }
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 80, 80));
            }
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Theme.TEXT);
            }
        });

        controls.add(menuButton);
        controls.add(minimizeBtn);
        controls.add(maximizeBtn);
        controls.add(closeBtn);

        titleBar.add(titlePanel, BorderLayout.WEST);
        titleBar.add(controls, BorderLayout.EAST);

        // Make window draggable from title bar
        MouseAdapter dragAdapter = new MouseAdapter() {
            private Point clickPoint;

            public void mousePressed(MouseEvent e) {
                clickPoint = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                if (clickPoint != null && (getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    Point dragPoint = e.getLocationOnScreen();
                    setLocation(dragPoint.x - clickPoint.x, dragPoint.y - clickPoint.y);
                }
            }
        };
        
        titleBar.addMouseListener(dragAdapter);
        titleBar.addMouseMotionListener(dragAdapter);
        
        return titleBar;
    }

    private void setupUI(JPanel contentPanel) {
        // Create split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setBackground(Theme.SURFACE);

        // Custom divider styling
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {}
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(Theme.BORDER);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });

        // Create sidebar
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        Theme.stylePanel(sidebar);
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add search field
        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        Theme.styleTextField(searchField);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search conversions...");
        searchField.setToolTipText("Type to filter conversions by name (Ctrl+F to focus)");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterConversions(searchField.getText());
            }
        });
        sidebar.add(searchField);
        sidebar.add(Box.createVerticalStrut(10));

        // Add sort dropdown with simple styling
        sortByDropdown = new JComboBox<>(new String[]{"Name", "Natural Sort", "Status", "Type"});
        sortByDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        sortByDropdown.setFont(Theme.NORMAL_FONT);
        sortByDropdown.setForeground(Color.BLACK);
        sortByDropdown.setSelectedItem("Natural Sort"); // Set Natural Sort as default
        sortByDropdown.setToolTipText("Sort conversions by different criteria");
        
        sortByDropdown.addActionListener(e -> {
            conversionListPanel.removeAll();
            ArrayList<Conversion> sortedConversions = Util.sortConversionsBy(
                project.getConversions(), 
                sortByDropdown.getSelectedItem().toString()
            );
            for (Conversion c : sortedConversions) {
                addConversionToSidebar(c);
            }
            conversionListPanel.revalidate();
            conversionListPanel.repaint();
        });
        sidebar.add(sortByDropdown);
        sidebar.add(Box.createVerticalStrut(10));

        // Add New Conversion button
        newConversion = new JButton("+ New Conversion");
        newConversion.setFont(Theme.NORMAL_FONT.deriveFont(Font.BOLD, 16f));
        newConversion.setForeground(Color.WHITE);
        newConversion.setBackground(new Color(40, 167, 69)); // Green accent for creation
        newConversion.setFocusPainted(false);
        newConversion.setBorderPainted(false);
        newConversion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); // Match conversion button height
        newConversion.setHorizontalAlignment(SwingConstants.LEFT); // Match conversion button alignment
        newConversion.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Match conversion button padding
        newConversion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newConversion.setToolTipText("Create a new media conversion (Ctrl+N)");
        newConversion.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        newConversion.setPreferredSize(new Dimension(100, 40));
        
        // Add hover effects similar to conversion buttons
        newConversion.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                newConversion.setBackground(new Color(50, 177, 79)); // Slightly brighter green on hover
            }
            public void mouseExited(MouseEvent e) {
                newConversion.setBackground(new Color(40, 167, 69)); // Back to original green
            }
        });
        
        newConversion.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter a name for the new conversion:", "New Conversion", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                Conversion newConv = new Conversion(name.trim());
                // Set the conversion type to the last used type from preferences
                newConv.type = Preferences.getInstance().getLastUsedConversionType();
                project.addConversion(newConv);
                addConversionToSidebar(newConv);
                showConversionDetails(newConv);
                markUnsavedChanges();
            }
        });
        sidebar.add(newConversion);
        sidebar.add(Box.createVerticalStrut(10));

        // Add quick action bar
        addQuickActionBar(sidebar);
        sidebar.add(Box.createVerticalStrut(10));

        // Create conversion list panel
        conversionListPanel = new JPanel();
        conversionListPanel.setLayout(new BoxLayout(conversionListPanel, BoxLayout.Y_AXIS));
        Theme.stylePanel(conversionListPanel);
        
        // Add scroll pane for conversions
        conversionScrollPane = new JScrollPane(conversionListPanel);
        conversionScrollPane.setBorder(null);
        conversionScrollPane.setBackground(Theme.SURFACE);
        conversionScrollPane.getViewport().setBackground(Theme.SURFACE);
        conversionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        conversionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(conversionScrollPane);

        // Create details panel
        detailsPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(detailsPanel);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add panels to split pane
        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(detailsPanel);

        // Add split pane to content panel
        contentPanel.add(splitPane, BorderLayout.CENTER);

        // Add status bar
        addStatusBar(contentPanel);

        // Add all conversions to the sidebar
        for (Conversion conversion : project.getConversions()) {
            addConversionToSidebar(conversion);
        }

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        // Register keyboard shortcuts using key bindings
        JRootPane rootPane = getRootPane();
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        
        // Ctrl+F for search
        im.put(KeyStroke.getKeyStroke("control F"), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });
        
        // Ctrl+N for new conversion
        im.put(KeyStroke.getKeyStroke("control N"), "newConversion");
        am.put("newConversion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newConversion.doClick();
            }
        });

        // Ctrl+S for save
        im.put(KeyStroke.getKeyStroke("control S"), "saveProject");
        am.put("saveProject", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveProject();
            }
        });

        // [ and ] keys for navigation
        im.put(KeyStroke.getKeyStroke('['), "selectPrevious");
        am.put("selectPrevious", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateConversions(-1);
            }
        });

        im.put(KeyStroke.getKeyStroke(']'), "selectNext");
        am.put("selectNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateConversions(1);
            }
        });

        // Add global key listener to root pane
        rootPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '[') {
                    navigateConversions(-1);
                } else if (e.getKeyChar() == ']') {
                    navigateConversions(1);
                }
            }
        });
    }

    private void navigateConversions(int direction) {
        // Get all visible conversion buttons
        ArrayList<JButton> visibleButtons = new ArrayList<>();
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton && c.isVisible()) {
                visibleButtons.add((JButton) c);
            }
        }

        if (visibleButtons.isEmpty()) return;

        // Find currently selected button
        JButton selectedButton = null;
        int selectedIndex = -1;
        for (int i = 0; i < visibleButtons.size(); i++) {
            JButton btn = visibleButtons.get(i);
            if (btn.getBackground().equals(Theme.ACCENT.darker())) {
                selectedButton = btn;
                selectedIndex = i;
                break;
            }
        }

        // If no button is selected, select the first/last one
        if (selectedButton == null) {
            if (direction > 0) {
                selectedIndex = 0;
            } else {
                selectedIndex = visibleButtons.size() - 1;
            }
        } else {
            // Move to next/previous button
            selectedIndex += direction;
            if (selectedIndex < 0) selectedIndex = visibleButtons.size() - 1;
            if (selectedIndex >= visibleButtons.size()) selectedIndex = 0;
        }

        // Click the selected button
        visibleButtons.get(selectedIndex).doClick();

        // Ensure the selected button is visible in the scroll pane
        Rectangle buttonBounds = visibleButtons.get(selectedIndex).getBounds();
        Rectangle viewportBounds = conversionScrollPane.getViewport().getViewRect();
        if (!viewportBounds.contains(buttonBounds)) {
            visibleButtons.get(selectedIndex).scrollRectToVisible(buttonBounds);
        }
    }

    private void addConversionToSidebar(Conversion conversion) {
        JButton conversionBtn = new JButton(conversion.name);
        conversionBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
        conversionBtn.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Enhanced styling
        Theme.styleButton(conversionBtn);
        conversionBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Add padding
        updateConversionButtonStyle(conversionBtn, conversion, false);
        
        // Add tooltip with status and file information
        String tooltipText = "Status: " + conversion.status.toString();
        if (conversion.linkedFiles != null && !conversion.linkedFiles.isEmpty()) {
            tooltipText += "\nFiles: " + conversion.linkedFiles.size() + " linked";
            if (conversion.duration != null && !conversion.duration.isZero()) {
                tooltipText += "\nDuration: " + formatDuration(conversion.duration);
            }
        } else {
            tooltipText += "\nNo files linked";
        }
        if (!conversion.note.isEmpty()) {
            tooltipText += "\nNote: " + conversion.note;
        }
        tooltipText += "\nRight-click for more options";
        conversionBtn.setToolTipText(tooltipText);
        
        // Add click handler with selection state
        conversionBtn.addActionListener(e -> {
            // Reset all other buttons to unselected state
            for (Component c : conversionListPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton btn = (JButton) c;
                    // Find the corresponding conversion for this button
                    for (Conversion conv : project.getConversions()) {
                        if (btn.getText().equals(conv.name)) {
                            updateConversionButtonStyle(btn, conv, btn == conversionBtn);
                            break;
                        }
                    }
                }
            }
            showConversionDetails(conversion);
        });
        
        // Add right-click menu
        addContextMenu(conversionBtn, conversion);
        
        conversionListPanel.add(conversionBtn);
        conversionListPanel.add(Box.createVerticalStrut(5));
    }

    private void updateConversionButtonStyle(JButton button, Conversion conversion, boolean isSelected) {
        Color statusColor = conversion.getStatusColor();
        
        if (isSelected) {
            // Selected state
            button.setBackground(Theme.ACCENT.darker());
            button.setForeground(Color.WHITE); // White text for better contrast on dark background
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, statusColor),
                BorderFactory.createEmptyBorder(0, 7, 0, 10)
            ));
        } else {
            // Normal state
            button.setBackground(Theme.SURFACE);
            button.setForeground(statusColor.brighter()); // Status color for text
            button.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    button.setBackground(Theme.SURFACE.brighter());
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    button.setBackground(Theme.SURFACE);
                }
            }
        });
    }

    private void updateButtonColors() {
        Component selectedComponent = null;
        
        // Find currently selected button
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton && c.getBackground().equals(Theme.ACCENT.darker())) {
                selectedComponent = c;
                break;
            }
        }
        
        // Update all buttons
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                // Find matching conversion for this button
                for (Conversion conv : project.getConversions()) {
                    if (btn.getText().equals(conv.name)) {
                        updateConversionButtonStyle(btn, conv, c == selectedComponent);
                        break; // Found the matching conversion, no need to continue inner loop
                    }
                }
            }
        }
    }

    public void remove(ConversionPanel conversionPanel) {
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                if (btn.getText().equals(conversionPanel.conversion.name)) {
                    conversionListPanel.remove(btn);
                    conversionListPanel.revalidate();
                    conversionListPanel.repaint();
                    break;
                }
            }
        }

        project.getConversions().removeIf(c -> c.name.equals(conversionPanel.conversion.name));

        detailsPanel.removeAll();
        displayTempContentPanel();
        saveProject();
    }

    private void displayTempContentPanel() {
        detailsPanel.removeAll();
        JPanel tempPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(tempPanel);
        tempPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel message = new JLabel("Select a conversion or create a new one");
        message.setFont(Theme.NORMAL_FONT);
        message.setForeground(Theme.TEXT);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        tempPanel.add(message, BorderLayout.CENTER);

        detailsPanel.add(tempPanel, BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    public void saveProject() {
        // Save the current conversion if one is selected
        if (detailsPanel.getComponentCount() > 0 && 
            detailsPanel.getComponent(0) instanceof JScrollPane &&
            ((JScrollPane)detailsPanel.getComponent(0)).getViewport().getView() instanceof ConversionPanel) {
            ConversionPanel currentPanel = (ConversionPanel)((JScrollPane)detailsPanel.getComponent(0)).getViewport().getView();
            // Instead of clicking the save button, directly update the conversion
            currentPanel.updateConversion();
        }

        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
        
        // Auto-sort conversions after saving using current dropdown selection
        String currentSortBy = sortByDropdown.getSelectedItem().toString();
        project.setConversions(Util.sortConversionsBy(project.getConversions(), currentSortBy));
        refreshConversionList();
        
        updateButtonColors();
        updateStatusBar();
        
        // Show save status in green
        saveStatusLabel.setForeground(new Color(40, 167, 69)); // Bootstrap success green
        saveStatusLabel.setText("Saved " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        saveStatusTimer.restart();
        hasUnsavedChanges = false;
    }

    private void addResizeBorder(JPanel mainPanel) {
        final int BORDER_SIZE = 5;
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        MouseAdapter resizeAdapter = new MouseAdapter() {
            private int cursor;
            private Point startPos = null;
            private Rectangle startBounds = null;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (getExtendedState() != Frame.MAXIMIZED_BOTH) {
                    cursor = getCursor(e);
                    setCursor(Cursor.getPredefinedCursor(cursor));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (cursor != Cursor.DEFAULT_CURSOR) {
                    startPos = e.getPoint();
                    startBounds = getBounds();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPos != null) {
                    int dx = e.getX() - startPos.x;
                    int dy = e.getY() - startPos.y;
                    
                    Rectangle bounds = new Rectangle(startBounds);
                    switch (cursor) {
                        case Cursor.N_RESIZE_CURSOR:
                            bounds.y += dy;
                            bounds.height -= dy;
                            break;
                        case Cursor.S_RESIZE_CURSOR:
                            bounds.height += dy;
                            break;
                        case Cursor.W_RESIZE_CURSOR:
                            bounds.x += dx;
                            bounds.width -= dx;
                            break;
                        case Cursor.E_RESIZE_CURSOR:
                            bounds.width += dx;
                            break;
                        case Cursor.NW_RESIZE_CURSOR:
                            bounds.x += dx;
                            bounds.y += dy;
                            bounds.width -= dx;
                            bounds.height -= dy;
                            break;
                        case Cursor.NE_RESIZE_CURSOR:
                            bounds.y += dy;
                            bounds.width += dx;
                            bounds.height -= dy;
                            break;
                        case Cursor.SW_RESIZE_CURSOR:
                            bounds.x += dx;
                            bounds.width -= dx;
                            bounds.height += dy;
                            break;
                        case Cursor.SE_RESIZE_CURSOR:
                            bounds.width += dx;
                            bounds.height += dy;
                            break;
                    }
                    
                    // Enforce minimum size
                    bounds.width = Math.max(bounds.width, 400);
                    bounds.height = Math.max(bounds.height, 300);
                    
                    setBounds(bounds);
                    revalidate();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                startPos = null;
                startBounds = null;
            }

            private int getCursor(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int w = getWidth();
                int h = getHeight();

                if (x < BORDER_SIZE && y < BORDER_SIZE) return Cursor.NW_RESIZE_CURSOR;
                if (x > w - BORDER_SIZE && y < BORDER_SIZE) return Cursor.NE_RESIZE_CURSOR;
                if (x < BORDER_SIZE && y > h - BORDER_SIZE) return Cursor.SW_RESIZE_CURSOR;
                if (x > w - BORDER_SIZE && y > h - BORDER_SIZE) return Cursor.SE_RESIZE_CURSOR;
                if (x < BORDER_SIZE) return Cursor.W_RESIZE_CURSOR;
                if (x > w - BORDER_SIZE) return Cursor.E_RESIZE_CURSOR;
                if (y < BORDER_SIZE) return Cursor.N_RESIZE_CURSOR;
                if (y > h - BORDER_SIZE) return Cursor.S_RESIZE_CURSOR;
                return Cursor.DEFAULT_CURSOR;
            }
        };

        addMouseListener(resizeAdapter);
        addMouseMotionListener(resizeAdapter);
    }

    private void addStatusBar(JPanel contentPanel) {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setPreferredSize(new Dimension(-1, 25));
        Theme.stylePanel(statusBar);
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusBar.setToolTipText("Project progress and file statistics");
        
        JLabel progressLabel = new JLabel();
        progressLabel.setForeground(Theme.TEXT_SECONDARY);
        progressLabel.setFont(Theme.SMALL_FONT);
        
        JLabel durationLabel = new JLabel();
        durationLabel.setForeground(Theme.TEXT_SECONDARY);
        durationLabel.setFont(Theme.SMALL_FONT);

        saveStatusLabel = new JLabel();
        saveStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        saveStatusLabel.setFont(Theme.SMALL_FONT);
        
        // Create a panel for the left side labels with spacing
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(progressLabel);
        leftPanel.add(Box.createHorizontalStrut(20)); // Add spacing
        leftPanel.add(saveStatusLabel);
        
        statusBar.add(leftPanel, BorderLayout.WEST);
        statusBar.add(durationLabel, BorderLayout.EAST);
        
        contentPanel.add(statusBar, BorderLayout.SOUTH);
        
        // Initial update
        updateStatusBar();
    }

    private void updateStatusBar() {
        if (statusBar != null) {
            int total = project.getConversions().size();
            long completed = project.getConversions().stream()
                .filter(c -> c.status == ConversionStatus.COMPLETED)
                .count();
            
            // Create progress info
            String progressText = String.format("Completed: %d/%d (%.1f%%)", 
                completed, total, (completed * 100.0) / Math.max(1, total));
            
            // Calculate total size asynchronously with caching
            calculateTotalSizeAsync().thenAccept(totalSize -> {
                String sizeText = String.format("Total Size: %s", formatSize(totalSize));
                
                // Update status bar labels on EDT
                SwingUtilities.invokeLater(() -> {
                    Component[] components = statusBar.getComponents();
                    if (components.length >= 2) {
                        JPanel leftPanel = (JPanel) components[0];
                        JLabel progressLabel = (JLabel) leftPanel.getComponent(0);
                        JLabel saveLabel = (JLabel) leftPanel.getComponent(2);
                        
                        progressLabel.setText(progressText);
                        if (hasUnsavedChanges) {
                            saveLabel.setForeground(new Color(255, 100, 100));
                            saveLabel.setText("Changes not saved");
                        } else {
                            saveLabel.setForeground(Theme.TEXT_SECONDARY);
                        }
                        ((JLabel)components[1]).setText(sizeText);
                    }
                });
            });
        }
    }
    
    /**
     * Calculate total size asynchronously using the cache manager
     */
    private CompletableFuture<Long> calculateTotalSizeAsync() {
        FileCacheManager cacheManager = FileCacheManager.getInstance();
        ArrayList<FileReference> allFiles = new ArrayList<>();
        
        for (Conversion conversion : project.getConversions()) {
            if (conversion.linkedFiles != null) {
                allFiles.addAll(conversion.linkedFiles);
            }
        }
        
        return cacheManager.calculateTotalSizeAsync(allFiles);
    }

    /**
     * Calculate total size of all linked files and their subdirectories
     */
    private long calculateTotalSizeIncludingSubdirectories() {
        long totalSize = 0;
        
        for (Conversion conversion : project.getConversions()) {
            if (conversion.linkedFiles != null) {
                for (FileReference file : conversion.linkedFiles) {
                    if (file.exists()) {
                        if (file.getFile().isFile()) {
                            // Add size of the file itself
                            totalSize += file.length();
                        } else if (file.getFile().isDirectory()) {
                            // Recursively calculate size of all files in directory
                            totalSize += calculateDirectorySize(file.getFile());
                        }
                    }
                }
            }
        }
        
        return totalSize;
    }

    /**
     * Recursively calculate the total size of all files in a directory
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        // Recursively calculate subdirectory size
                        size += calculateDirectorySize(file);
                    }
                }
            }
        }
        
        return size;
    }

    /**
     * Recursively process a directory for statistics (file count, size, and type breakdown)
     */
    private void processDirectoryForStatisticsRecursive(File directory, int totalFiles, long totalSize, 
                                                       Map<String, Integer> typeCount, Map<String, Long> typeSize) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        totalFiles++;
                        long size = file.length();
                        totalSize += size;

                        String type = getExtension(new FileReference(file)).toLowerCase();
                        typeCount.merge(type, 1, Integer::sum);
                        typeSize.merge(type, size, Long::sum);
                    } else if (file.isDirectory()) {
                        // Recursively process subdirectories
                        processDirectoryForStatisticsRecursive(file, totalFiles, totalSize, typeCount, typeSize);
                    }
                }
            }
        }
    }

    private void addQuickActionBar(JPanel sidebar) {

    }

    private void enableDragAndDrop() {
        conversionListPanel.setTransferHandler(new TransferHandler() {
            // Implementation for drag and drop reordering of conversions
            // This would require more extensive code to handle the actual
            // drag and drop operations
        });
    }

    private void exportProjectAsJson() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Project");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
            
            // Set the current directory to the last used directory
            String lastDir = Preferences.getInstance().getLastUsedDirectory();
            if (lastDir != null && new File(lastDir).exists()) {
                fileChooser.setCurrentDirectory(new File(lastDir));
            }
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getParentFile(), file.getName() + ".json");
                }
                
                // Save the parent directory as the last used directory
                File parentDir = file.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    Preferences.getInstance().setLastUsedDirectory(parentDir.getAbsolutePath());
                }
                
                JsonObject projectJson = new JsonObject();
                projectJson.addProperty("name", project.getName());
                projectJson.addProperty("version", DigitizingAssistant.VERSION);
                
                JsonArray conversionsArray = new JsonArray();
                for (Conversion conversion : project.getConversions()) {
                    JsonObject conversionJson = new JsonObject();
                    conversionJson.addProperty("name", conversion.name);
                    conversionJson.addProperty("type", conversion.type.toString());
                    conversionJson.addProperty("status", conversion.status.toString());
                    conversionJson.addProperty("note", conversion.note);
                    conversionJson.addProperty("duration", conversion.duration.toString());
                    
                    // Add linked files
                    JsonArray linkedFilesArray = new JsonArray();
                    if (conversion.linkedFiles != null) {
                        for (FileReference file1 : conversion.linkedFiles) {
                            linkedFilesArray.add(file1.getPath());
                        }
                    }
                    conversionJson.add("linkedFiles", linkedFilesArray);
                    
                    conversionsArray.add(conversionJson);
                }
                projectJson.add("conversions", conversionsArray);
                
                try (FileWriter writer = new FileWriter(file)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(projectJson, writer);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error exporting project: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportDigitizingSheet() {
        try {
            // Create export options dialog
            JDialog optionsDialog = new JDialog(this, "Export Digitizing Sheet", true);
            optionsDialog.setSize(400, 300);
            optionsDialog.setLocationRelativeTo(this);
            optionsDialog.setResizable(false);

            JPanel panel = new JPanel(new GridBagLayout());
            Theme.stylePanel(panel);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Export type selection
            JLabel typeLabel = new JLabel("Export Type:");
            typeLabel.setForeground(Theme.TEXT);
            panel.add(typeLabel, gbc);

            JComboBox<ExportUtil.ExportType> typeCombo = new JComboBox<>(ExportUtil.ExportType.values());
            Theme.styleComboBox(typeCombo);
            typeCombo.setBackground(Theme.SURFACE);
            typeCombo.setForeground(Theme.TEXT);
            panel.add(typeCombo, gbc);

            // Format selection
            JLabel formatLabel = new JLabel("Format:");
            formatLabel.setForeground(Theme.TEXT);
            panel.add(formatLabel, gbc);

            JCheckBox csvCheck = new JCheckBox("CSV", true);
            JCheckBox jsonCheck = new JCheckBox("JSON", true);
            csvCheck.setForeground(Theme.TEXT);
            jsonCheck.setForeground(Theme.TEXT);

            JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            formatPanel.setOpaque(false);
            formatPanel.add(csvCheck);
            formatPanel.add(jsonCheck);
            panel.add(formatPanel, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setOpaque(false);

            JButton exportButton = new JButton("Export");
            JButton cancelButton = new JButton("Cancel");
            Theme.styleButton(exportButton);
            Theme.styleButton(cancelButton);

            exportButton.addActionListener(e -> {
                if (!csvCheck.isSelected() && !jsonCheck.isSelected()) {
                    JOptionPane.showMessageDialog(optionsDialog, 
                        "Please select at least one format.",
                        "No Format Selected",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                optionsDialog.dispose();
                
                // Choose output directory
                JFileChooser dirChooser = new JFileChooser();
                dirChooser.setDialogTitle("Select Export Directory");
                dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    dirChooser.setCurrentDirectory(new File(lastDir));
                }
                
                if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File outputDir = dirChooser.getSelectedFile();
                    
                    // Save the directory as the last used directory
                    Preferences.getInstance().setLastUsedDirectory(outputDir.getAbsolutePath());
                    
                    // Export the digitizing sheet
                    ExportUtil.exportDigitizingSheet(
                        project, 
                        outputDir.toPath(), 
                        (ExportUtil.ExportType) typeCombo.getSelectedItem(),
                        csvCheck.isSelected(),
                        jsonCheck.isSelected()
                    );
                    
                    // Mark all conversions as exported
                    for (Conversion conversion : project.getConversions()) {
                        conversion.hasBeenExported = true;
                        conversion.lastExportTime = java.time.LocalDateTime.now();
                        conversion.lastExportType = "digitizing_sheet";
                    }
                    saveProject();
                }
            });

            cancelButton.addActionListener(e -> optionsDialog.dispose());

            buttonPanel.add(exportButton);
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, gbc);

            optionsDialog.add(panel);
            optionsDialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error exporting digitizing sheet: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportFileMap() {
        try {
            // Create export options dialog
                    JDialog optionsDialog = new JDialog(this, "Export File Map", true);
        optionsDialog.setSize(350, 250);
            optionsDialog.setLocationRelativeTo(this);
            optionsDialog.setResizable(false);

            JPanel panel = new JPanel(new GridBagLayout());
            Theme.stylePanel(panel);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Include checksums option
            JCheckBox includeChecksums = new JCheckBox("Include file checksums (slower but more detailed)");
            includeChecksums.setForeground(Theme.TEXT);
            panel.add(includeChecksums, gbc);
            
            // Directory depth option
            JLabel depthLabel = new JLabel("Max directory depth:");
            depthLabel.setForeground(Theme.TEXT);
            panel.add(depthLabel, gbc);
            
            JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
            depthSpinner.setPreferredSize(new Dimension(80, 25));
            Theme.styleSpinner(depthSpinner);
            depthSpinner.getEditor().setBackground(Theme.SURFACE);
            depthSpinner.getEditor().setForeground(Theme.TEXT);
            panel.add(depthSpinner, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setOpaque(false);

            JButton exportButton = new JButton("Export");
            JButton cancelButton = new JButton("Cancel");
            Theme.styleButton(exportButton);
            Theme.styleButton(cancelButton);

            exportButton.addActionListener(e -> {
                optionsDialog.dispose();
                
                // Choose output file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save File Map");
                fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
                
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    fileChooser.setCurrentDirectory(new File(lastDir));
                }
                
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".json")) {
                        file = new File(file.getParentFile(), file.getName() + ".json");
                    }
                    
                    // Save the parent directory as the last used directory
                    File parentDir = file.getParentFile();
                    if (parentDir != null && parentDir.exists()) {
                        Preferences.getInstance().setLastUsedDirectory(parentDir.getAbsolutePath());
                    }
                    
                    // Export the file map
                    int maxDepth = (Integer) depthSpinner.getValue();
                    ExportUtil.exportFileMap(project, file.toPath(), includeChecksums.isSelected(), maxDepth);
                    
                    // Mark all conversions as exported
                    for (Conversion conversion : project.getConversions()) {
                        conversion.hasBeenExported = true;
                        conversion.lastExportTime = java.time.LocalDateTime.now();
                        conversion.lastExportType = "file_map";
                    }
                    saveProject();
                }
            });

            cancelButton.addActionListener(e -> optionsDialog.dispose());

            buttonPanel.add(exportButton);
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, gbc);

            optionsDialog.add(panel);
            optionsDialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error exporting file map: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMediaStatistics() {
        JDialog dialog = new JDialog(this, "Media Statistics", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        Theme.stylePanel(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Header
        JLabel header = new JLabel("Media Statistics");
        header.setFont(Theme.HEADER_FONT);
        header.setForeground(Theme.TEXT);
        panel.add(header, gbc);
        panel.add(Box.createVerticalStrut(20), gbc);

        // Calculate statistics for linked files only
        int totalFiles = 0;
        long totalSize = 0;
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Long> typeSize = new HashMap<>();

        // Process all linked files and their subdirectories
        for (Conversion conversion : project.getConversions()) {
            if (conversion.linkedFiles != null) {
                for (FileReference file : conversion.linkedFiles) {
                    if (file.exists()) {
                        if (file.getFile().isFile()) {
                            totalFiles++;
                            long size = file.length();
                            totalSize += size;

                            String type = getExtension(file).toLowerCase();
                            typeCount.merge(type, 1, Integer::sum);
                            typeSize.merge(type, size, Long::sum);
                        } else if (file.getFile().isDirectory()) {
                            // Process all files in directory recursively
                            processDirectoryForStatisticsRecursive(file.getFile(), totalFiles, totalSize, typeCount, typeSize);
                        }
                    }
                }
            }
        }

        // Display statistics
        addStatRow(panel, gbc, "Total Files", String.valueOf(totalFiles));
        addStatRow(panel, gbc, "Total Size", formatSize(totalSize));
        panel.add(Box.createVerticalStrut(20), gbc);

        // File types
        JLabel typesHeader = new JLabel("File Types");
        typesHeader.setFont(Theme.NORMAL_FONT.deriveFont(Font.BOLD));
        typesHeader.setForeground(Theme.TEXT);
        panel.add(typesHeader, gbc);
        panel.add(Box.createVerticalStrut(10), gbc);

        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            long size = typeSize.get(type);
            
            StringBuilder typeStats = new StringBuilder();
            typeStats.append(count).append(" files");
            typeStats.append(" (").append(formatSize(size)).append(")");
            
            addStatRow(panel, gbc, type.toUpperCase(), typeStats.toString());
        }

        // Add close button
        JButton closeButton = new JButton("Close");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dialog.dispose());
        panel.add(Box.createVerticalStrut(20), gbc);
        panel.add(closeButton, gbc);

        // Make the panel scrollable
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Theme.SURFACE);
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Style the scrollbar
        scrollPane.getVerticalScrollBar().setBackground(Theme.SURFACE);
        scrollPane.getVerticalScrollBar().setForeground(Theme.TEXT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private boolean isVideoFile(String extension) {
        return extension.matches("(mp4|mkv|avi|mov|wmv|flv|webm)");
    }

    private Duration getVideoDuration(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.getAbsolutePath()
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output != null) {
                double seconds = Double.parseDouble(output);
                return Duration.ofSeconds((long) seconds);
            }
        } catch (Exception e) {
            // If ffprobe fails, try to get duration from the file name
            String name = file.getName();
            if (name.matches(".*\\d{2}-\\d{2}-\\d{2}.*")) {
                try {
                    String[] parts = name.split("\\d{2}-\\d{2}-\\d{2}")[1].split("\\.")[0].trim().split("-");
                    if (parts.length == 2) {
                        String[] start = parts[0].trim().split(":");
                        String[] end = parts[1].trim().split(":");
                        if (start.length == 3 && end.length == 3) {
                            int startSeconds = Integer.parseInt(start[0]) * 3600 + 
                                             Integer.parseInt(start[1]) * 60 + 
                                             Integer.parseInt(start[2]);
                            int endSeconds = Integer.parseInt(end[0]) * 3600 + 
                                           Integer.parseInt(end[1]) * 60 + 
                                           Integer.parseInt(end[2]);
                            return Duration.ofSeconds(endSeconds - startSeconds);
                        }
                    }
                } catch (Exception ex) {
                    // Ignore parsing errors
                }
            }
        }
        return null;
    }

    private String formatSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void showConversionDetails(Conversion conversion) {
        detailsPanel.removeAll();
        ConversionPanel conversionPanel = new ConversionPanel(conversion, this);
        JScrollPane scrollPane = new JScrollPane(conversionPanel);
        Theme.styleScrollPane(scrollPane);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        detailsPanel.add(scrollPane, BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private void refreshConversionList() {
        conversionListPanel.removeAll();
        for (Conversion conversion : project.getConversions()) {
            addConversionToSidebar(conversion);
        }
        conversionListPanel.revalidate();
        conversionListPanel.repaint();
    }

    private void addContextMenu(JButton conversionBtn, Conversion conversion) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem duplicate = new JMenuItem("Duplicate");
        JMenuItem delete = new JMenuItem("Delete");
        
        // Add tooltips to context menu items
        duplicate.setToolTipText("Create a copy of this conversion with the same settings");
        delete.setToolTipText("Permanently delete this conversion (cannot be undone)");
        
        duplicate.addActionListener(e -> {
            Conversion newConversion = new Conversion(conversion.name + " (Copy)");
            newConversion.type = conversion.type;
            newConversion.note = conversion.note;
            newConversion.status = conversion.status;
            newConversion.duration = conversion.duration;
            project.getConversions().add(newConversion);
            addConversionToSidebar(newConversion);
            saveProject();
        });
        
        delete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this conversion?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                conversionListPanel.remove(conversionBtn);
                project.getConversions().remove(conversion);
                conversionListPanel.revalidate();
                conversionListPanel.repaint();
                saveProject();
                
                // Clear details panel if the deleted conversion was selected
                if (detailsPanel.getComponentCount() > 0 && 
                    detailsPanel.getComponent(0) instanceof JScrollPane &&
                    ((JScrollPane)detailsPanel.getComponent(0)).getViewport().getView() instanceof ConversionPanel &&
                    ((ConversionPanel)((JScrollPane)detailsPanel.getComponent(0)).getViewport().getView()).conversion == conversion) {
                    displayTempContentPanel();
                }
            }
        });
        
        contextMenu.add(duplicate);
        contextMenu.add(delete);
        
        conversionBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    contextMenu.show(conversionBtn, me.getX(), me.getY());
                }
            }
            public void mouseReleased(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    contextMenu.show(conversionBtn, me.getX(), me.getY());
                }
            }
        });
    }

    // Add this method to mark changes as unsaved
    public void markUnsavedChanges() {
        hasUnsavedChanges = true;
        updateStatusBar();
    }

    // Helper method to normalize filenames for matching
    private String normalizeFilename(String name) {
        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot != -1) name = name.substring(0, dot);
        // Remove spaces, parentheses, dashes, underscores, and non-alphanumeric
        return name.replaceAll("[\\s\\(\\)\\-_.]", "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    // Helper method to extract numeric part from filename
    private String extractNumber(String name) {
        return name.replaceAll("\\D", "");
    }

    // Improved helper method to recursively search for trimmed files
    private void searchForTrimmedFiles(File directory, ArrayList<FileReference> originalFiles, Map<FileReference, File> trimmedFileMap) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively search subdirectories
                    searchForTrimmedFiles(file, originalFiles, trimmedFileMap);
                } else {
                    String trimmedName = file.getName();
                    if (trimmedName.contains("_trimmed")) {
                        String trimmedNorm = normalizeFilename(trimmedName.substring(0, trimmedName.indexOf("_trimmed")));
                        String trimmedNum = extractNumber(trimmedName);
                        ArrayList<FileReference> possibleMatches = new ArrayList<>();
                        for (FileReference originalFileRef : originalFiles) {
                            String origName = originalFileRef.getName();
                            String origNorm = normalizeFilename(origName);
                            String origNum = extractNumber(origName);
                            // Match if normalized base matches or number matches
                            if (origNorm.equals(trimmedNorm) || (!origNum.isEmpty() && origNum.equals(trimmedNum))) {
                                possibleMatches.add(originalFileRef);
                            }
                        }
                        if (possibleMatches.size() == 1) {
                            trimmedFileMap.put(possibleMatches.get(0), file);
                        } else if (possibleMatches.size() > 1) {
                            // Ambiguous, log for user review
                            logFileOperation("AMBIGUOUS MATCH", "Trimmed file '" + file.getName() + "' matches multiple originals: " + possibleMatches);
                        }
                    }
                }
            }
        }
    }

    // Add this method to create and write to log files
    private void logFileOperation(String operation, String details) {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File(DigitizingAssistant.PROJECTS_DIRECTORY, "logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            // Create timestamped log file
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File logFile = new File(logsDir, project.getName() + "_" + timestamp + ".log");
            
            // Write to log file
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ");
                writer.write(operation + ": " + details + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error writing to log file: " + e.getMessage(),
                "Log Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper to gather all files in project directories
    private ArrayList<FileReference> getAllFilesInProjectDirectories() {
        HashSet<File> dirs = new HashSet<>();
        for (Conversion c : project.getConversions()) {
            for (FileReference f : c.linkedFiles) {
                if (f.getParentFile() != null) {
                    dirs.add(f.getParentFile());
                }
            }
        }
        ArrayList<FileReference> allFiles = new ArrayList<>();
        for (File dir : dirs) {
            gatherFilesRecursive(dir, allFiles);
        }
        return allFiles;
    }

    private void gatherFilesRecursive(File dir, ArrayList<FileReference> allFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    gatherFilesRecursive(f, allFiles);
                } else {
                    allFiles.add(new FileReference(f));
                }
            }
        }
    }

    // Add these helper methods at the class level
    private void updateLinkedFilesAfterRename(Conversion c, String baseName) {
        ArrayList<FileReference> updated = new ArrayList<>();
        for (FileReference oldFile : c.linkedFiles) {
            File dir = oldFile.getParentFile();
            if (dir != null && dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String norm = normalizeFilename(f.getName());
                        String baseNorm = normalizeFilename(baseName);
                        if (norm.contains(baseNorm) && f.getName().endsWith(getExtension(oldFile))) {
                            updated.add(new FileReference(f));
                            break;
                        }
                    }
                }
            }
        }
        if (!updated.isEmpty()) {
            c.linkedFiles.clear();
            c.linkedFiles.addAll(updated);
        }
    }

    private String getExtension(FileReference file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot != -1) ? name.substring(dot) : "";
    }

    private void openProjectFolder() {
        try {
            Desktop.getDesktop().open(DigitizingAssistant.PROJECTS_DIRECTORY);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not open project folder: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterConversions(String searchText) {
        String search = searchText.toLowerCase();
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setVisible(btn.getText().toLowerCase().contains(search));
            }
        }
        conversionListPanel.revalidate();
        conversionListPanel.repaint();
    }

    private void addStatRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(rowPanel);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(Theme.NORMAL_FONT);
        labelComponent.setForeground(Theme.TEXT);
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(Theme.NORMAL_FONT);
        valueComponent.setForeground(Theme.TEXT);
        
        rowPanel.add(labelComponent, BorderLayout.WEST);
        rowPanel.add(valueComponent, BorderLayout.EAST);
        panel.add(rowPanel, gbc);
    }
    
    private void showWriteToDestinationDialog() {
        // Create destination selection dialog
        JFileChooser destinationChooser = new JFileChooser();
        destinationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        destinationChooser.setDialogTitle("Select Destination Folder for Client Delivery");
        
        // Set the current directory to the last used directory
        String lastDir = Preferences.getInstance().getLastUsedDirectory();
        if (lastDir != null && new File(lastDir).exists()) {
            destinationChooser.setCurrentDirectory(new File(lastDir));
        }
        
        int result = destinationChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File destinationFolder = destinationChooser.getSelectedFile();
        
        // Create options dialog
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Folder structure options
        JCheckBox createConversionFolders = new JCheckBox("Create separate folder for each conversion", true);
        createConversionFolders.setToolTipText("Each conversion will get its own folder named after the conversion note");
        
        JCheckBox renameFiles = new JCheckBox("Rename files to conversion names", false);
        renameFiles.setToolTipText("Rename files to match their conversion names (ignored for data-only conversions)");
        
        JCheckBox includeSubdirectories = new JCheckBox("Include files in subdirectories", false);
        includeSubdirectories.setToolTipText("Also copy files found in subdirectories of linked files");
        
        JCheckBox preserveOriginalNames = new JCheckBox("Preserve original filenames for data-only conversions", true);
        preserveOriginalNames.setToolTipText("Data-only conversions will keep original filenames (recommended)");
        
        optionsPanel.add(new JLabel("Export Options:"));
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(createConversionFolders);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(renameFiles);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(includeSubdirectories);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(preserveOriginalNames);
        
        int optionsResult = JOptionPane.showConfirmDialog(this,
            optionsPanel,
            "Export Options",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (optionsResult != JOptionPane.OK_OPTION) {
            return;
        }
        
        // Generate preview
        StringBuilder preview = new StringBuilder();
        preview.append("Destination: ").append(destinationFolder.getAbsolutePath()).append("\n");
        preview.append("Project Folder: ").append(project.getName()).append("\n\n");
        
        int totalFiles = 0;
        for (Conversion c : project.getConversions()) {
            if (c.linkedFiles.isEmpty()) continue;
            
            String folderName = createConversionFolders.isSelected() ? 
                (c.note.isEmpty() ? c.name : c.note) : "";
            
            preview.append("Conversion: ").append(c.name);
            if (!folderName.isEmpty()) {
                preview.append(" → Folder: ").append(project.getName()).append("/").append(folderName);
            } else {
                preview.append(" → Folder: ").append(project.getName());
            }
            preview.append("\n");
            
            if (c.isDataOnly) {
                preview.append("  (Data-only: preserving original filenames)\n");
                for (FileReference fileRef : c.linkedFiles) {
                    preview.append("    ").append(fileRef.getName()).append("\n");
                    totalFiles++;
                }
            } else {
                for (FileReference fileRef : c.linkedFiles) {
                    String newName = renameFiles.isSelected() ? 
                        (c.note.isEmpty() ? c.name : c.note) : fileRef.getName();
                    preview.append("    ").append(fileRef.getName()).append(" → ").append(newName).append("\n");
                    totalFiles++;
                }
            }
            preview.append("\n");
        }
        
        preview.append("Total files to copy: ").append(totalFiles);
        
        // Show preview and confirm
        int confirmResult = JOptionPane.showConfirmDialog(this,
            preview.toString(),
            "Preview Export",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
            
        if (confirmResult != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Execute the export
        executeExport(destinationFolder, createConversionFolders.isSelected(), 
                     renameFiles.isSelected(), includeSubdirectories.isSelected(), 
                     preserveOriginalNames.isSelected());
    }
    
    private void executeExport(File destinationFolder, boolean createFolders, boolean renameFiles, 
                              boolean includeSubdirs, boolean preserveDataNames) {
        int totalCopied = 0;
        int totalErrors = 0;
        
        // Create project folder
        String projectFolderName = project.getName().replaceAll("[<>:\"/\\|?*]", "_");
        File projectFolder = new File(destinationFolder, projectFolderName);
        if (!projectFolder.exists()) {
            projectFolder.mkdirs();
        }
        
        logFileOperation("START EXPORT", "Exporting to: " + projectFolder.getAbsolutePath());
        
        for (Conversion c : project.getConversions()) {
            if (c.linkedFiles.isEmpty()) continue;
            
            // Create conversion folder if requested
            File conversionFolder = projectFolder;
            if (createFolders) {
                String folderName = c.note.isEmpty() ? c.name : c.note;
                // Sanitize folder name
                folderName = folderName.replaceAll("[<>:\"/\\|?*]", "_");
                conversionFolder = new File(projectFolder, folderName);
                if (!conversionFolder.exists()) {
                    conversionFolder.mkdirs();
                }
            }
            
            for (FileReference fileRef : c.linkedFiles) {
                try {
                    File sourceFile = fileRef.getFile();
                    if (!sourceFile.exists()) {
                        logFileOperation("ERROR", "Source file not found: " + sourceFile.getAbsolutePath());
                        totalErrors++;
                        continue;
                    }
                    
                    // Determine destination filename
                    String destFileName;
                    if (c.isDataOnly && preserveDataNames) {
                        // For data-only conversions, preserve original names
                        destFileName = sourceFile.getName();
                    } else if (renameFiles && !c.note.isEmpty()) {
                        // Rename to conversion note
                        String extension = "";
                        int dot = sourceFile.getName().lastIndexOf('.');
                        if (dot > 0) {
                            extension = sourceFile.getName().substring(dot);
                        }
                        destFileName = c.note + extension;
                    } else {
                        // Keep original name
                        destFileName = sourceFile.getName();
                    }
                    
                    File destFile = new File(conversionFolder, destFileName);
                    
                    // Handle duplicate filenames
                    int counter = 1;
                    while (destFile.exists()) {
                        String baseName = destFileName;
                        String extension = "";
                        int dot = destFileName.lastIndexOf('.');
                        if (dot > 0) {
                            baseName = destFileName.substring(0, dot);
                            extension = destFileName.substring(dot);
                        }
                        destFileName = baseName + " (" + counter + ")" + extension;
                        destFile = new File(conversionFolder, destFileName);
                        counter++;
                    }
                    
                    // Copy the file
                    java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), 
                                           java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    logFileOperation("COPY", sourceFile.getAbsolutePath() + " → " + destFile.getAbsolutePath());
                    totalCopied++;
                    
                } catch (Exception e) {
                    logFileOperation("ERROR", "Failed to copy " + fileRef.getPath() + ": " + e.getMessage());
                    totalErrors++;
                }
            }
        }
        
        logFileOperation("END EXPORT", "Copied " + totalCopied + " files, " + totalErrors + " errors");
        
        // Show completion message
        String message = String.format("Export completed!\n\nCopied: %d files\nErrors: %d\n\nFiles exported to: %s", 
                                      totalCopied, totalErrors, projectFolder.getAbsolutePath());
        JOptionPane.showMessageDialog(this, message, "Export Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSmartRenameDialog() {
        try {
            // Analyze all conversions to categorize them
            Map<String, ConversionCategory> conversionCategories = analyzeConversions();
            
            // Create analysis summary
            StringBuilder analysis = new StringBuilder();
            analysis.append("Content Analysis Results:\n\n");
            
            int videoCount = 0, dataCount = 0, mixedCount = 0;
            for (ConversionCategory category : conversionCategories.values()) {
                switch (category) {
                    case VIDEO: videoCount++; break;
                    case DATA: dataCount++; break;
                    case MIXED: mixedCount++; break;
                }
            }
            
            analysis.append("• Video CDs: ").append(videoCount).append(" (need HandBrake processing)\n");
            analysis.append("• Data/Photo CDs: ").append(dataCount).append(" (direct renaming)\n");
            analysis.append("• Mixed CDs: ").append(mixedCount).append(" (hybrid approach)\n\n");
            
            // Show detailed breakdown with export status
            analysis.append("Detailed Breakdown:\n");
            analysis.append("Format: [Conversion] → [Category] → [Recommendation] [Export Status]\n\n");
            
            List<Conversion> hybridConversions = new ArrayList<>();
            
            for (Conversion conversion : project.getConversions()) {
                ConversionCategory category = conversionCategories.get(conversion.name);
                String recommendation = getRecommendation(category, conversion);
                String exportStatus = getExportStatus(conversion);
                
                analysis.append("• ").append(conversion.name).append(": ").append(category.toString())
                       .append(" → ").append(recommendation).append(" ").append(exportStatus).append("\n");
                
                // Track hybrid conversions for special attention
                if (category == ConversionCategory.MIXED) {
                    hybridConversions.add(conversion);
                }
            }
            
            // Special section for hybrid conversions that need manual attention
            if (!hybridConversions.isEmpty()) {
                analysis.append("\n⚠️  HYBRID CONVERSIONS - MANUAL ATTENTION REQUIRED:\n");
                analysis.append("These conversions contain both video and data files.\n");
                analysis.append("Consider processing video files through HandBrake separately.\n\n");
                
                for (Conversion conversion : hybridConversions) {
                    analysis.append("• ").append(conversion.name).append(":\n");
                    
                    // Analyze what types of files are present
                    List<String> videoFiles = new ArrayList<>();
                    List<String> dataFiles = new ArrayList<>();
                    
                    if (conversion.linkedFiles != null) {
                        for (FileReference fileRef : conversion.linkedFiles) {
                            if (fileRef.exists()) {
                                if (fileRef.getFile().isFile()) {
                                    String extension = getExtension(fileRef).toLowerCase();
                                    if (isVideoFile(extension)) {
                                        videoFiles.add(fileRef.getPath());
                                    } else if (isDataFile(extension)) {
                                        dataFiles.add(fileRef.getPath());
                                    }
                                } else if (fileRef.getFile().isDirectory()) {
                                    boolean[] dirResult = analyzeDirectory(fileRef.getFile());
                                    if (dirResult[0]) {
                                        videoFiles.add(fileRef.getPath() + " (directory)");
                                    }
                                    if (dirResult[1]) {
                                        dataFiles.add(fileRef.getPath() + " (directory)");
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!videoFiles.isEmpty()) {
                        analysis.append("  - Video files: ").append(videoFiles.size()).append(" files\n");
                    }
                    if (!dataFiles.isEmpty()) {
                        analysis.append("  - Data files: ").append(dataFiles.size()).append(" files\n");
                    }
                    analysis.append("  - Recommendation: Process video files through HandBrake, then rename\n\n");
                }
            }
            
            // Create options panel
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            
            // Analysis text area
            JTextArea analysisArea = new JTextArea(analysis.toString());
            analysisArea.setEditable(false);
            analysisArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(analysisArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            panel.add(scrollPane, BorderLayout.CENTER);
            
            // Options panel
            JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            optionsPanel.setBorder(BorderFactory.createTitledBorder("Rename Options"));
            
            // Smart rename option
            JCheckBox smartRename = new JCheckBox("Use Smart Renaming (recommended)", true);
            smartRename.setToolTipText("Automatically choose the best renaming strategy for each conversion");
            
            // Manual override options
            JCheckBox videoOnly = new JCheckBox("Force all to video naming (1.mp4, 2.mp4...)");
            JCheckBox dataOnly = new JCheckBox("Force all to descriptive naming (conversion notes)");
            JCheckBox customOnly = new JCheckBox("Use custom naming for all");
            
            // Custom name field
            JTextField customNameField = new JTextField("Custom_Base_Name");
            customNameField.setEnabled(false);
            
            // Add listeners for mutual exclusivity
            smartRename.addActionListener(e -> {
                boolean enabled = !smartRename.isSelected();
                videoOnly.setEnabled(enabled);
                dataOnly.setEnabled(enabled);
                customOnly.setEnabled(enabled);
                customNameField.setEnabled(customOnly.isSelected() && enabled);
            });
            
            videoOnly.addActionListener(e -> {
                if (videoOnly.isSelected()) {
                    dataOnly.setSelected(false);
                    customOnly.setSelected(false);
                }
                customNameField.setEnabled(customOnly.isSelected());
            });
            
            dataOnly.addActionListener(e -> {
                if (dataOnly.isSelected()) {
                    videoOnly.setSelected(false);
                    customOnly.setSelected(false);
                }
                customNameField.setEnabled(customOnly.isSelected());
            });
            
            customOnly.addActionListener(e -> {
                if (customOnly.isSelected()) {
                    videoOnly.setSelected(false);
                    dataOnly.setSelected(false);
                }
                customNameField.setEnabled(customOnly.isSelected());
            });
            
            optionsPanel.add(smartRename);
            optionsPanel.add(videoOnly);
            optionsPanel.add(dataOnly);
            optionsPanel.add(customOnly);
            optionsPanel.add(new JLabel("Custom base name:"));
            optionsPanel.add(customNameField);
            
            panel.add(optionsPanel, BorderLayout.SOUTH);
            
            // Show dialog
            int result = JOptionPane.showConfirmDialog(this, 
                panel, 
                "Smart Renaming Analysis", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE);
            
            if (result == JOptionPane.OK_OPTION) {
                // Execute the chosen renaming strategy
                if (smartRename.isSelected()) {
                    executeSmartRenaming(conversionCategories);
                } else if (videoOnly.isSelected()) {
                    executeVideoRenaming();
                } else if (dataOnly.isSelected()) {
                    executeDataRenaming();
                } else if (customOnly.isSelected()) {
                    executeCustomRenaming(customNameField.getText());
                }
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error during smart renaming analysis: " + ex.getMessage(),
                "Analysis Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private enum ConversionCategory {
        VIDEO("Video Content"),
        DATA("Data/Photo Content"), 
        MIXED("Mixed Content");
        
        private final String description;
        
        ConversionCategory(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    private Map<String, ConversionCategory> analyzeConversions() {
        Map<String, ConversionCategory> categories = new HashMap<>();
        
        for (Conversion conversion : project.getConversions()) {
            if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
                categories.put(conversion.name, ConversionCategory.DATA); // Default for empty
                continue;
            }
            
            boolean hasVideo = false;
            boolean hasData = false;
            
            for (FileReference fileRef : conversion.linkedFiles) {
                if (fileRef.exists()) {
                    if (fileRef.getFile().isFile()) {
                        String extension = getExtension(fileRef).toLowerCase();
                        if (isVideoFile(extension)) {
                            hasVideo = true;
                        } else if (isDataFile(extension)) {
                            hasData = true;
                        }
                    } else if (fileRef.getFile().isDirectory()) {
                        // Analyze directory contents
                        boolean[] dirResult = analyzeDirectory(fileRef.getFile());
                        hasVideo = hasVideo || dirResult[0];
                        hasData = hasData || dirResult[1];
                    }
                }
            }
            
            // Categorize based on content
            if (hasVideo && hasData) {
                categories.put(conversion.name, ConversionCategory.MIXED);
            } else if (hasVideo) {
                categories.put(conversion.name, ConversionCategory.VIDEO);
            } else {
                categories.put(conversion.name, ConversionCategory.DATA);
            }
        }
        
        return categories;
    }
    
    private boolean isDataFile(String extension) {
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "pdf", "doc", "docx", "txt", "rtf").contains(extension);
    }
    
        private boolean[] analyzeDirectory(File directory) {
        boolean[] result = {false, false}; // [hasVideo, hasData]
        try {
            Files.walk(directory.toPath(), 3) // Limit depth for performance
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String fileName = file.getFileName().toString();
                    if (fileName.contains(".")) {
                        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                        if (isVideoFile(extension)) {
                            result[0] = true;
                        } else if (isDataFile(extension)) {
                            result[1] = true;
                        }
                    }
                });
        } catch (IOException e) {
            // Ignore directory access errors
        }
        return result;
    }
    
    private String getRecommendation(ConversionCategory category, Conversion conversion) {
        switch (category) {
            case VIDEO:
                return "Rename to: " + conversion.name + ".mp4 (after HandBrake)";
            case DATA:
                return "Rename to: " + (conversion.note.isEmpty() ? conversion.name : conversion.note);
            case MIXED:
                return "Rename to: " + conversion.name + "_Mixed";
            default:
                return "Use conversion name";
        }
    }
    
    private String getExportStatus(Conversion conversion) {
        if (!conversion.hasBeenExported) {
            return "[Not Exported]";
        }
        
        String status = "[Exported: " + conversion.lastExportType;
        if (conversion.lastExportTime != null) {
            status += " - " + conversion.lastExportTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        }
        status += "]";
        return status;
    }
    
    private void executeSmartRenaming(Map<String, ConversionCategory> categories) {
        // Group conversions by category
        List<Conversion> videoConversions = new ArrayList<>();
        List<Conversion> dataConversions = new ArrayList<>();
        List<Conversion> mixedConversions = new ArrayList<>();
        
        for (Conversion conversion : project.getConversions()) {
            ConversionCategory category = categories.get(conversion.name);
            switch (category) {
                case VIDEO: videoConversions.add(conversion); break;
                case DATA: dataConversions.add(conversion); break;
                case MIXED: mixedConversions.add(conversion); break;
            }
        }
        
        // Execute renaming for each category
        if (!videoConversions.isEmpty()) {
            executeVideoRenaming(videoConversions);
        }
        
        if (!dataConversions.isEmpty()) {
            executeDataRenaming(dataConversions);
        }
        
        if (!mixedConversions.isEmpty()) {
            executeMixedRenaming(mixedConversions);
        }
        
        // Mark all conversions as exported
        for (Conversion conversion : project.getConversions()) {
            conversion.hasBeenExported = true;
            conversion.lastExportTime = java.time.LocalDateTime.now();
            conversion.lastExportType = "smart_rename";
        }
        saveProject();
        
        JOptionPane.showMessageDialog(this,
            "Smart renaming completed!\n\n" +
            "• Video conversions: " + videoConversions.size() + "\n" +
            "• Data conversions: " + dataConversions.size() + "\n" +
            "• Mixed conversions: " + mixedConversions.size() + "\n\n" +
            "All conversions have been marked as exported.",
            "Renaming Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void executeVideoRenaming() {
        executeVideoRenaming(project.getConversions());
    }
    
    private void executeVideoRenaming(List<Conversion> conversions) {
        for (int i = 0; i < conversions.size(); i++) {
            Conversion conversion = conversions.get(i);
            String newName = (i + 1) + ".mp4";
            updateLinkedFilesAfterRename(conversion, newName);
        }
    }
    
    private void executeDataRenaming() {
        executeDataRenaming(project.getConversions());
    }
    
    private void executeDataRenaming(List<Conversion> conversions) {
        for (Conversion conversion : conversions) {
            String newName = conversion.note.isEmpty() ? conversion.name : conversion.note;
            updateLinkedFilesAfterRename(conversion, newName);
        }
    }
    
    private void executeCustomRenaming(String baseName) {
        for (int i = 0; i < project.getConversions().size(); i++) {
            Conversion conversion = project.getConversions().get(i);
            String newName = baseName + "_" + (i + 1);
            updateLinkedFilesAfterRename(conversion, newName);
        }
    }
    
    private void executeMixedRenaming(List<Conversion> conversions) {
        for (Conversion conversion : conversions) {
            String newName = conversion.name + "_Mixed";
            updateLinkedFilesAfterRename(conversion, newName);
        }
    }
}
