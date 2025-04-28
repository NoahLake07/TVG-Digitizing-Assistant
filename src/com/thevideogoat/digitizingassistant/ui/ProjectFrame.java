package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.*;
import com.google.gson.Gson;
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
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel, detailsPanel;
    JScrollPane conversionScrollPane;
    JSplitPane splitPane;
    Project project;
    JTextField searchField;
    private Point mouseDownCompCoords;
    private JPanel statusBar;
    private JButton newConversion;

    public ProjectFrame(Project project) {
        super();
        this.project = project;
        setUndecorated(true);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DigitizingAssistant.setIcon(this);

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
        JMenuItem mediaStats = new JMenuItem("Media Statistics");
        JMenuItem relinkTrimmed = new JMenuItem("Relink to Trimmed Files");
        
        refreshList.addActionListener(e -> refreshConversionList());
        exportProject.addActionListener(e -> exportProjectAsJson());
        mediaStats.addActionListener(e -> showMediaStatistics());
        relinkTrimmed.addActionListener(e -> Util.relinkToTrimmedFiles(project));
        
        menu.add(refreshList);
        menu.add(exportProject);
        menu.addSeparator();
        menu.add(mediaStats);
        menu.add(relinkTrimmed);
        
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
        // Style the split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setBorder(null);
        splitPane.setDividerSize(4);
        
        // Remove the default split pane borders
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Custom divider styling
        splitPane.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {}
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(Theme.BORDER);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });

        // Sidebar setup
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        Theme.stylePanel(sidebar);
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search field with modern styling
        searchField = new JTextField();
        Theme.styleTextField(searchField);
        searchField.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
        searchField.putClientProperty("JTextField.placeholderText", "Search conversions...");
        searchField.addCaretListener(e -> {
            String search = searchField.getText().toLowerCase();
            for (Component c : conversionListPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton btn = (JButton) c;
                    btn.setVisible(btn.getText().toLowerCase().contains(search));
                }
            }
            conversionListPanel.revalidate();
            conversionListPanel.repaint();
        });
        
        // Sort dropdown with modern styling
        JComboBox<String> sortBy = new JComboBox<>(new String[]{"Name", "Natural Sort", "Status", "Duration", "Type"});
        sortBy.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
        
        sortBy.addActionListener(e -> {
            conversionListPanel.removeAll();
            ArrayList<Conversion> sortedConversions = Util.sortConversionsBy(
                project.getConversions(), 
                sortBy.getSelectedItem().toString()
            );
            for (Conversion c : sortedConversions) {
                addConversionToSidebar(c);
            }
            conversionListPanel.revalidate();
            conversionListPanel.repaint();
        });

        // Conversion list panel
        conversionListPanel = new JPanel();
        conversionListPanel.setLayout(new BoxLayout(conversionListPanel, BoxLayout.Y_AXIS));
        Theme.stylePanel(conversionListPanel);
        
        // Scrollpane with modern styling
        conversionScrollPane = new JScrollPane(conversionListPanel);
        Theme.styleScrollPane(conversionScrollPane);
        conversionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        conversionScrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        conversionScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // New conversion button with modern styling
        newConversion = new JButton("New Conversion");
        Theme.styleButton(newConversion);
        newConversion.setMaximumSize(new Dimension(Short.MAX_VALUE, Theme.BUTTON_SIZE.height));
        
        newConversion.addActionListener(e -> {
            // Get the most recent conversion's type
            String defaultType = "DEFAULT"; // Set a default type if none exists
            ArrayList<Conversion> conversions = project.getConversions();
            if (!conversions.isEmpty()) {
                defaultType = conversions.get(conversions.size() - 1).type.toString();
            }
            
            // Prompt user for conversion name
            String name = JOptionPane.showInputDialog(
                ProjectFrame.this,
                "Enter conversion name:",
                "New Conversion",
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (name != null && !name.trim().isEmpty()) {
                Conversion conversion = new Conversion(name.trim());
                conversion.type = com.thevideogoat.digitizingassistant.data.Type.valueOf(defaultType);
                project.getConversions().add(conversion);
                addConversionToSidebar(conversion);
                
                // Select and show the new conversion
                detailsPanel.removeAll();
                ConversionPanel conversionPanel = new ConversionPanel(conversion, ProjectFrame.this);
                JScrollPane scrollPane = new JScrollPane(conversionPanel);
                Theme.styleScrollPane(scrollPane);
                scrollPane.setBorder(null);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                detailsPanel.add(scrollPane, BorderLayout.CENTER);
                detailsPanel.revalidate();
                detailsPanel.repaint();
                
                saveProject();
            }
        });

        // Add components to sidebar
        sidebar.add(searchField);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(sortBy);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(conversionScrollPane);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(newConversion);

        // Add quick action buttons
        addQuickActionBar(sidebar);

        // Details panel
        detailsPanel = new JPanel(new BorderLayout());
        Theme.stylePanel(detailsPanel);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder());
        displayTempContentPanel();

        // Load existing conversions
        for (Conversion conversion : project.getConversions()) {
            addConversionToSidebar(conversion);
        }

        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(detailsPanel);
        contentPanel.add(splitPane);

        // Add existing action listeners and functionality...
        // (keep existing action listener code for searchField, sortBy, newConversion, etc.)

        // Add keyboard shortcuts
        setupKeyboardShortcuts();

        // Add status bar
        addStatusBar(contentPanel);

        // Add drag and drop functionality
        enableDragAndDrop();
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
    }

    private void addConversionToSidebar(Conversion conversion) {
        JButton conversionBtn = new JButton(conversion.name);
        conversionBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
        conversionBtn.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Enhanced styling
        Theme.styleButton(conversionBtn);
        conversionBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Add padding
        updateConversionButtonStyle(conversionBtn, conversion, false);
        
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
        JLabel message = new JLabel("Select a conversion from the sidebar", SwingConstants.CENTER);
        message.setFont(Theme.NORMAL_FONT);
        message.setForeground(Theme.TEXT_SECONDARY);
        detailsPanel.add(message, BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    public void saveProject() {
        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
        updateButtonColors();
        updateStatusBar();
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
        
        JLabel progressLabel = new JLabel();
        progressLabel.setForeground(Theme.TEXT_SECONDARY);
        progressLabel.setFont(Theme.SMALL_FONT);
        
        JLabel durationLabel = new JLabel();
        durationLabel.setForeground(Theme.TEXT_SECONDARY);
        durationLabel.setFont(Theme.SMALL_FONT);
        
        statusBar.add(progressLabel, BorderLayout.WEST);
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
            
            // Calculate total duration
            Duration totalDuration = project.getConversions().stream()
                .map(c -> c.duration)
                .reduce(Duration.ZERO, Duration::plus);
                
            String durationText = String.format("Total Duration: %s", formatDuration(totalDuration));
            
            // Update status bar labels
            Component[] components = statusBar.getComponents();
            if (components.length >= 2) {
                ((JLabel)components[0]).setText(progressText);
                ((JLabel)components[1]).setText(durationText);
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
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getParentFile(), file.getName() + ".json");
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

    private void showMediaStatistics() {
        // Create scrollable statistics panel
        JPanel statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Project Overview Section
        JLabel overviewHeader = new JLabel("Media Statistics");
        overviewHeader.setFont(new Font(Theme.HEADER_FONT.getFamily(), Font.BOLD, 16));
        overviewHeader.setForeground(Color.BLACK);
        statsPanel.add(overviewHeader, gbc);
        
        // Calculate statistics
        int totalConversions = project.getConversions().size();
        long completedCount = project.getConversions().stream()
            .filter(c -> c.status == ConversionStatus.COMPLETED)
            .count();
        Duration totalDuration = project.getConversions().stream()
            .map(c -> c.duration)
            .reduce(Duration.ZERO, Duration::plus);
        
        // Count files and directories
        int totalFiles = 0;
        int totalDirs = 0;
        int totalSubDirs = 0;
        
        for (Conversion conversion : project.getConversions()) {
            if (conversion.linkedFiles != null) {
                for (File file : conversion.linkedFiles) {
                    if (file.isDirectory()) {
                        totalDirs++;
                        // Count subdirectories
                        totalSubDirs += countSubdirectories(file);
                    } else {
                        totalFiles++;
                    }
                }
            }
        }
        
        // Media Type Statistics
        Map<com.thevideogoat.digitizingassistant.data.Type, Integer> mediaTypeStats = new HashMap<>();
        Map<com.thevideogoat.digitizingassistant.data.Type, Duration> typeDurations = new HashMap<>();
        for (Conversion conversion : project.getConversions()) {
            mediaTypeStats.merge(conversion.type, 1, Integer::sum);
            typeDurations.merge(conversion.type, conversion.duration, Duration::plus);
        }
        
        // Add Overview Statistics
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        addStatRow(statsPanel, gbc, "Total Conversions", String.format("%d (%d completed)", 
            totalConversions, completedCount));
        addStatRow(statsPanel, gbc, "Total Duration", formatDuration(totalDuration));
        addStatRow(statsPanel, gbc, "Linked Files", String.format("%d files, %d directories", 
            totalFiles, totalDirs));
        
        if (totalSubDirs > 0) {
            addStatRow(statsPanel, gbc, "Subdirectories", String.valueOf(totalSubDirs));
        }
        
        // Media Type Breakdown
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel typeHeader = new JLabel("Media Type Breakdown");
        typeHeader.setFont(new Font(Theme.HEADER_FONT.getFamily(), Font.BOLD, 14));
        typeHeader.setForeground(Color.BLACK);
        statsPanel.add(typeHeader, gbc);
        
        // Add type statistics
        for (com.thevideogoat.digitizingassistant.data.Type type : com.thevideogoat.digitizingassistant.data.Type.values()) {
            int count = mediaTypeStats.getOrDefault(type, 0);
            if (count > 0) {
                Duration typeDuration = typeDurations.getOrDefault(type, Duration.ZERO);
                String statValue = String.format("%d items, %s", count, formatDuration(typeDuration));
                addStatRow(statsPanel, gbc, type.toString(), statValue);
            }
        }
        
        // Create scrollable container
        JScrollPane scrollPane = new JScrollPane(statsPanel);
        scrollPane.setPreferredSize(new Dimension(400, Math.min(500, statsPanel.getPreferredSize().height + 50)));
        scrollPane.setBorder(null);
        
        // Show dialog
        JOptionPane.showMessageDialog(this,
            scrollPane,
            "Media Statistics",
            JOptionPane.PLAIN_MESSAGE);
    }

    private void addStatRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 20, 5, 20);
        
        JLabel statLabel = new JLabel(label + ":");
        statLabel.setForeground(Color.BLACK);
        panel.add(statLabel, gbc);
        
        gbc.gridx = 1;
        JLabel statValue = new JLabel(value);
        statValue.setForeground(Color.BLACK);
        panel.add(statValue, gbc);
    }

    private int countSubdirectories(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count++;
                    count += countSubdirectories(file);
                }
            }
        }
        return count;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%d:%02d", hours, minutes);
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
}
