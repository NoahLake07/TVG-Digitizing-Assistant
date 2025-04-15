package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Type;
import com.thevideogoat.digitizingassistant.data.Util;
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
        JMenuItem settings = new JMenuItem("Project Settings");
        
        refreshList.addActionListener(e -> refreshConversionList());
        exportProject.addActionListener(e -> exportProjectAsJson());
        mediaStats.addActionListener(e -> showMediaStatistics());
        settings.addActionListener(e -> showProjectSettings());
        
        menu.add(refreshList);
        menu.add(exportProject);
        menu.addSeparator();
        menu.add(mediaStats);
        menu.add(settings);
        
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
        
        // Simplified styling
        Theme.styleButton(conversionBtn);
        conversionBtn.setForeground(conversion.getStatusColor());
        
        // Add click handler
        conversionBtn.addActionListener(e -> showConversionDetails(conversion));
        
        // Add right-click menu
        addContextMenu(conversionBtn, conversion);
        
        conversionListPanel.add(conversionBtn);
        conversionListPanel.add(Box.createVerticalStrut(5));
    }

    private void addProgressIndicator(JButton conversionBtn, Conversion conversion) {
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setPreferredSize(new Dimension(conversionBtn.getWidth(), 3));
        progress.setForeground(Theme.ACCENT);
        progress.setBackground(Theme.SURFACE);
        progress.setBorderPainted(false);
        
        // Use existing status property to determine progress
        int progressValue = 0;
        switch (conversion.status) {
            case COMPLETED:
                progressValue = 100;
                break;
            case IN_PROGRESS:
                progressValue = 50;
                break;
            case BASIC_EDITING:
                progressValue = 75;
                break;
            case DAMAGED:
                progressValue = 0;
                break;
            case NOT_STARTED:
                progressValue = 0;
                break;
            default:
                progressValue = 0;
                break;
        }
        progress.setValue(progressValue);
        
        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(conversionBtn, BorderLayout.CENTER);
        btnPanel.add(progress, BorderLayout.SOUTH);
        
        conversionListPanel.add(btnPanel);
    }

    private void addContextMenu(JButton conversionBtn, Conversion conversion) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem duplicate = new JMenuItem("Duplicate");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem export = new JMenuItem("Export Details");
        
        duplicate.addActionListener(e -> {
            // Duplicate conversion logic
        });
        
        delete.addActionListener(e -> {
            // Delete conversion logic
        });
        
        export.addActionListener(e -> {
            // Export conversion details logic
        });
        
        contextMenu.add(duplicate);
        contextMenu.add(delete);
        contextMenu.addSeparator();
        contextMenu.add(export);
        
        conversionBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void updateButtonColors() {
        for (Component c : conversionListPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                for (Conversion conversion : project.getConversions()) {
                    if (btn.getText().equals(conversion.name)) {
                        btn.setForeground(conversion.getStatusColor());
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
        
        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Theme.TEXT_SECONDARY);
        statusLabel.setFont(Theme.SMALL_FONT);
        
        JLabel statsLabel = new JLabel();
        statsLabel.setForeground(Theme.TEXT_SECONDARY);
        statsLabel.setFont(Theme.SMALL_FONT);
        updateStatsLabel(statsLabel);
        
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(statsLabel, BorderLayout.EAST);
        
        contentPanel.add(statusBar, BorderLayout.SOUTH);
    }

    private void updateStatsLabel(JLabel statsLabel) {
        int total = project.getConversions().size();
        long completed = project.getConversions().stream()
            .filter(c -> c.status.equals("Completed"))
            .count();
        statsLabel.setText(String.format("Completed: %d/%d", completed, total));
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
        Map<com.thevideogoat.digitizingassistant.data.Type, Integer> mediaCount = new HashMap<>();
        Map<com.thevideogoat.digitizingassistant.data.Type, Duration> totalDuration = new HashMap<>();
        
        // Calculate statistics
        for (Conversion conversion : project.getConversions()) {
            mediaCount.merge(conversion.type, 1, Integer::sum);
            totalDuration.merge(conversion.type, conversion.duration, Duration::plus);
        }
        
        // Create statistics panel
        JPanel statsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add headers
        statsPanel.add(new JLabel("Media Type"), gbc);
        gbc.gridx++;
        statsPanel.add(new JLabel("Quantity"), gbc);
        gbc.gridx++;
        statsPanel.add(new JLabel("Total Duration"), gbc);
        
        // Add data rows
        for (Type type : Type.values()) {
            gbc.gridy++;
            gbc.gridx = 0;
            statsPanel.add(new JLabel(type.toString()), gbc);
            gbc.gridx++;
            statsPanel.add(new JLabel(String.valueOf(mediaCount.getOrDefault(type, 0))), gbc);
            gbc.gridx++;
            Duration duration = totalDuration.getOrDefault(type, Duration.ZERO);
            statsPanel.add(new JLabel(formatDuration(duration)), gbc);
        }
        
        JOptionPane.showMessageDialog(this,
            statsPanel,
            "Media Statistics",
            JOptionPane.PLAIN_MESSAGE);
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

    private void showProjectSettings() {
        JDialog settingsDialog = new JDialog(this, "Project Settings", true);
        settingsDialog.setLayout(new BorderLayout());
        
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add read-only project info
        settingsPanel.add(new JLabel("Project Name:"), gbc);
        gbc.gridx = 1;
        JLabel projectNameLabel = new JLabel(project.getName());
        settingsPanel.add(projectNameLabel, gbc);
        
        // Add close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> settingsDialog.dispose());
        buttonPanel.add(closeButton);
        
        settingsDialog.add(settingsPanel, BorderLayout.CENTER);
        settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }
}
