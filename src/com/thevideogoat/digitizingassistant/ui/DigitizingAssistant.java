package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.FileReference;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Conversion;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import com.thevideogoat.digitizingassistant.data.Preferences;

public class DigitizingAssistant {

    public static final String CURRENT_DIRECTORY = System.getProperty("user.home");
    public static final File PROJECTS_DIRECTORY;
    public static final String OS = System.getProperty("os.name").toLowerCase();
    public static final String VERSION = "1.5";

    private static DigitizingAssistant instance;

    static {
        if (OS.contains("win")) {
            PROJECTS_DIRECTORY = Paths.get(CURRENT_DIRECTORY, "Documents", ".digitizing-assistant", "projects").toFile();
        } else {
            PROJECTS_DIRECTORY = Paths.get(CURRENT_DIRECTORY, "Documents", ".digitizing-assistant", "projects").toFile();
        }

        // Ensure the directory exists
        if (!PROJECTS_DIRECTORY.exists()) {
            PROJECTS_DIRECTORY.mkdirs();
        }
    }

    public void chooseProject() {
        JFrame projectChooser = new JFrame();
        projectChooser.setUndecorated(true);
        projectChooser.setSize(400, 600);
        projectChooser.setLocationRelativeTo(null);
        projectChooser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIcon(projectChooser);

        // Main panel with dark background
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        Theme.stylePanel(mainPanel);
        mainPanel.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        // Title bar panel
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(Theme.SURFACE);
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // Header with icon and text
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);
        
        ImageIcon icon = new ImageIcon(getIcon().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        JLabel iconLabel = new JLabel(icon);
        
        JLabel header = new JLabel("Digitizing Assistant");
        header.setForeground(Theme.TEXT);
        header.setFont(Theme.HEADER_FONT);
        
        headerPanel.add(iconLabel);
        headerPanel.add(header);
        titleBar.add(headerPanel, BorderLayout.WEST);

        // Close button
        JLabel closeButton = new JLabel("×");
        closeButton.setForeground(Theme.TEXT);
        closeButton.setFont(Theme.HEADER_FONT);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                projectChooser.dispose();
            }
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(new Color(255, 80, 80));
            }
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(Theme.TEXT);
            }
        });
        titleBar.add(closeButton, BorderLayout.EAST);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        Theme.stylePanel(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Project list
        JList<File> projectList = new JList<>();
        Theme.styleList(projectList);
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Scrollpane
        JScrollPane scrollPane = new JScrollPane(projectList);
        Theme.styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(360, 400));

        // Load projects
        File[] allProjects = PROJECTS_DIRECTORY.listFiles();
        DefaultListModel<File> listModel = new DefaultListModel<>();
        if (allProjects != null) {
            for (File f : allProjects) {
                if(f.getName().endsWith(".project") || f.getName().endsWith(".json")) {
                    listModel.addElement(f);
                }
            }
        }
        projectList.setModel(listModel);

        // Set custom cell renderer
        projectList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel && value instanceof File) {
                    JLabel label = (JLabel) c;
                    File file = (File) value;
                    String name = file.getName();
                    String extension = name.substring(name.lastIndexOf("."));
                    name = name.substring(0, name.lastIndexOf("."));
                    
                    if (extension.equals(".project")) {
                        label.setText(name + " (Needs Upgrade)");
                        label.setForeground(isSelected ? Theme.TEXT : new Color(255, 165, 0)); // Orange for .project files
                    } else {
                        label.setText(name);
                        label.setForeground(isSelected ? Theme.TEXT : Theme.TEXT_SECONDARY);
                    }
                    // Add padding to the left
                    label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                }
                setBackground(isSelected ? Theme.ACCENT : Theme.SURFACE);
                return this;
            }
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton newProjectBtn = new JButton("New Project");
        JButton openProjectBtn = new JButton("Open Project");
        JButton menuBtn = new JButton("≡");
        menuBtn.setPreferredSize(new Dimension(30, 30));
        Theme.styleButton(newProjectBtn);
        Theme.styleButton(openProjectBtn);
        Theme.styleButton(menuBtn);

        // Create popup menu
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.SURFACE);
        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        
        JMenuItem importItem = new JMenuItem("Import Project");
        importItem.setForeground(Color.BLACK);

        JMenuItem openProjectFolder = new JMenuItem("Open Project Folder");
        openProjectFolder.setForeground(Color.BLACK);
        
        JMenuItem toolsMenu = new JMenuItem("Tools");
        toolsMenu.setForeground(Color.BLACK);
        
        // Create tools submenu
        JPopupMenu toolsSubmenu = new JPopupMenu();
        toolsSubmenu.setBackground(Theme.SURFACE);
        
        // Common tools
        String[][] tools = {
            {"HandBrake", "HandBrake.exe"},
            {"MakeMKV", "makemkv.exe"},
            {"LosslessCut", "LosslessCut.exe"}
        };
        
        for (String[] tool : tools) {
            JMenuItem toolItem = new JMenuItem(tool[0]);
            toolItem.setForeground(Color.BLACK);
            toolItem.addActionListener(e -> {
                try {
                    // Try to find the tool in common locations
                    String[] searchPaths = {
                        System.getenv("ProgramFiles"),
                        System.getenv("ProgramFiles(x86)"),
                        System.getenv("LOCALAPPDATA")
                    };
                    
                    File toolFile = null;
                    for (String path : searchPaths) {
                        if (path != null) {
                            File possibleFile = new File(path, tool[1]);
                            if (possibleFile.exists()) {
                                toolFile = possibleFile;
                                break;
                            }
                        }
                    }
                    
                    if (toolFile != null) {
                        Desktop.getDesktop().open(toolFile);
                    } else {
                        JOptionPane.showMessageDialog(null,
                            "Could not find " + tool[0] + ". Please make sure it is installed.",
                            "Tool Not Found",
                            JOptionPane.WARNING_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,
                        "Could not launch " + tool[0] + ": " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Add hover effect
            toolItem.addChangeListener(e -> {
                if (toolItem.isArmed()) {
                    toolItem.setBackground(Theme.ACCENT);
                } else {
                    toolItem.setBackground(Theme.SURFACE);
                }
            });
            
            toolsSubmenu.add(toolItem);
        }
        
        // Add tools menu click handler
        toolsMenu.addActionListener(e -> {
            toolsSubmenu.show(toolsMenu, 0, toolsMenu.getHeight());
        });
        
        // Add hover effect for tools menu
        toolsMenu.addChangeListener(e -> {
            if (toolsMenu.isArmed()) {
                toolsMenu.setBackground(Theme.ACCENT);
            } else {
                toolsMenu.setBackground(Theme.SURFACE);
            }
        });
        
        menu.add(importItem);
        menu.add(openProjectFolder);
        menu.addSeparator();
        menu.add(toolsMenu);

        // Add menu button click handler
        menuBtn.addActionListener(e -> {
            menu.show(menuBtn, 0, menuBtn.getHeight());
        });

        newProjectBtn.addActionListener(e -> {
            String projectName = JOptionPane.showInputDialog(projectChooser, "Project Name:");
            if (projectName != null && !projectName.trim().isEmpty()) {
                new ProjectFrame(new Project(projectName));
                projectChooser.dispose();
            }
        });

        openProjectBtn.addActionListener(e -> {
            if (projectList.getSelectedValue() != null) {
                File selectedFile = projectList.getSelectedValue();
                if (selectedFile.getName().endsWith(".project")) {
                    JOptionPane.showMessageDialog(projectChooser,
                        "This project needs to be upgraded to the new format. Please double-click it to upgrade.",
                        "Project Upgrade Required",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    new ProjectFrame(new Project(selectedFile));
                    projectChooser.dispose();
                }
            }
        });

        importItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Import Project");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
            
            // Set the current directory to the last used directory
            String lastDir = Preferences.getInstance().getLastUsedDirectory();
            if (lastDir != null && new File(lastDir).exists()) {
                fileChooser.setCurrentDirectory(new File(lastDir));
            }
            
            if (fileChooser.showOpenDialog(projectChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    // Import the project
                    Project importedProject = new Project(fileChooser.getSelectedFile());
                    
                    // Save it as a .project file
                    importedProject.saveToFile(PROJECTS_DIRECTORY.toPath());
                    
                    // Refresh the project list
                    listModel.clear();
                    File[] updatedProjects = PROJECTS_DIRECTORY.listFiles();
                    if (updatedProjects != null) {
                        for (File f : updatedProjects) {
                            if(f.getName().endsWith(".project")) {
                                listModel.addElement(f);
                            }
                        }
                    }
                    
                    JOptionPane.showMessageDialog(projectChooser,
                        "Project imported successfully.",
                        "Import Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(projectChooser,
                        "Error importing project: " + ex.getMessage(),
                        "Import Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        openProjectFolder.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DigitizingAssistant.PROJECTS_DIRECTORY);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not open project folder: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Enable/disable open button based on selection
        projectList.addListSelectionListener(e -> {
            openProjectBtn.setEnabled(projectList.getSelectedValue() != null);
        });
        openProjectBtn.setEnabled(false);

        // Add double-click functionality
        projectList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int index = projectList.locationToIndex(me.getPoint());
                    if (index != -1) {
                        File selectedFile = listModel.getElementAt(index);
                        if (selectedFile.getName().endsWith(".project")) {
                            int choice = JOptionPane.showConfirmDialog(projectChooser,
                                "This project needs to be upgraded to the new format. Would you like to export it as JSON now?",
                                "Project Upgrade Required",
                                JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                try {
                                    // Load the old project
                                    Project oldProject = new Project(selectedFile.toPath());
                                    // Export as JSON
                                    File jsonFile = new File(PROJECTS_DIRECTORY, oldProject.getName() + ".json");
                                    try (FileWriter writer = new FileWriter(jsonFile)) {
                                        JsonObject projectJson = new JsonObject();
                                        projectJson.addProperty("name", oldProject.getName());
                                        projectJson.addProperty("version", VERSION);
                                        
                                        JsonArray conversionsArray = new JsonArray();
                                        for (Conversion conversion : oldProject.getConversions()) {
                                            JsonObject conversionJson = new JsonObject();
                                            conversionJson.addProperty("name", conversion.name);
                                            conversionJson.addProperty("type", conversion.type.toString());
                                            conversionJson.addProperty("status", conversion.status.toString());
                                            conversionJson.addProperty("note", conversion.note);
                                            conversionJson.addProperty("duration", conversion.duration.toString());
                                            
                                            // Add linked files
                                            JsonArray linkedFilesArray = new JsonArray();
                                            if (conversion.linkedFiles != null) {
                                                for (FileReference fileRef : conversion.linkedFiles) {
                                                    linkedFilesArray.add(fileRef.getPath());
                                                }
                                            }
                                            conversionJson.add("linkedFiles", linkedFilesArray);
                                            
                                            conversionsArray.add(conversionJson);
                                        }
                                        projectJson.add("conversions", conversionsArray);
                                        
                                        new GsonBuilder().setPrettyPrinting().create().toJson(projectJson, writer);
                                    }
                                    
                                    // Delete the old .project file
                                    selectedFile.delete();
                                    
                                    // Refresh the list
                                    listModel.removeElement(selectedFile);
                                    listModel.addElement(jsonFile);
                                    
                                    JOptionPane.showMessageDialog(projectChooser,
                                        "Project has been upgraded to the new format.",
                                        "Upgrade Complete",
                                        JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(projectChooser,
                                        "Error upgrading project: " + ex.getMessage(),
                                        "Upgrade Error",
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        } else {
                            new ProjectFrame(new Project(selectedFile));
                            projectChooser.dispose();
                        }
                    }
                }
            }
        });

        buttonPanel.add(newProjectBtn);
        buttonPanel.add(menuBtn);
        buttonPanel.add(openProjectBtn);

        contentPanel.add(scrollPane);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(buttonPanel);

        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        projectChooser.add(mainPanel);
        projectChooser.setVisible(true);

        // Make window draggable
        titleBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = projectChooser.getLocation();
                projectChooser.setLocation(p.x + e.getX(), p.y + e.getY());
            }
        });
    }

    public static void setIcon(JFrame frame) {
        try {
            BufferedImage icon = ImageIO.read(Objects.requireNonNull(DigitizingAssistant.class.getResourceAsStream("/tvgdigassistappicon0.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            throw new Error("Failed to set icon.");
        }
    }

    public static BufferedImage getIcon() {
        BufferedImage icon = null;
        try {
            icon = ImageIO.read(Objects.requireNonNull(DigitizingAssistant.class.getResourceAsStream("/tvgdigassistappicon0.png")));
        } catch (IOException e) {
            throw new Error("Failed to get icon.");
        }
        return icon;
    }

    public static DigitizingAssistant getInstance() {
        return instance;
    }

    public static void main(String[] args) throws RuntimeException {
        if (!PROJECTS_DIRECTORY.exists()) {
            PROJECTS_DIRECTORY.mkdir();
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.setProperty("sun.java2d.uiScale", "1.0");
        } catch (Exception e) {
            throw new Error("Could not set system look and feel.");
        }

        instance = new DigitizingAssistant();
        
        // Check if we've shown the welcome message before
        File versionFile = new File(PROJECTS_DIRECTORY, ".version");
        boolean showWelcome = false;
        try {
            showWelcome = !versionFile.exists() || !VERSION.equals(new String(Files.readAllBytes(versionFile.toPath())).trim());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (showWelcome) {
            // Show welcome message for version 1.5
            JPanel welcomePanel = new JPanel(new BorderLayout(10, 10));
            welcomePanel.setBackground(Color.WHITE);
            welcomePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Header
            JLabel header = new JLabel("Welcome to Digitizing Assistant v" + VERSION);
            header.setFont(new Font("Segoe UI", Font.BOLD, 18));
            header.setForeground(Color.BLACK);
            welcomePanel.add(header, BorderLayout.NORTH);
            
            // Content
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBackground(Color.WHITE);
            
            // New Features
            JLabel featuresHeader = new JLabel("New Features:");
            featuresHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
            featuresHeader.setForeground(Color.BLACK);
            contentPanel.add(featuresHeader);
            contentPanel.add(Box.createVerticalStrut(10));
            
            String[] features = {
                "• Data-Only Conversions: Mark conversions as pure data storage",
                "• Misc Data Storage Format: For SD cards, hard drives, etc.",
                "• Technician Notes: Internal logging field for observations",
                "• File Map Visualization: Hierarchical view of linked files and contents",
                "• Professional Export System: 'Write to Destination' for client delivery",
                "• Auto-Sort: Conversions automatically sorted on save",
                "• Silent Saves: Removed confirmation dialogs for smoother workflow",
                "• Enhanced File Management: Better bulk operations and relinking"
            };
            
            for (String feature : features) {
                JLabel featureLabel = new JLabel(feature);
                featureLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                featureLabel.setForeground(Color.BLACK);
                contentPanel.add(featureLabel);
                contentPanel.add(Box.createVerticalStrut(5));
            }
            
            contentPanel.add(Box.createVerticalStrut(15));
            
            // Note about new capabilities
            JLabel noteLabel = new JLabel("<html><body style='width: 400px'>" +
                "<b>Professional Workflow:</b> Version 1.5 transforms the app into a complete project delivery system. " +
                "Use the new export feature to create professional client deliveries with proper folder structure and file organization. " +
                "The new File Map feature provides a clear view of your project structure and file contents.</body></html>");
            noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            noteLabel.setForeground(Color.BLACK);
            contentPanel.add(noteLabel);
            
            welcomePanel.add(contentPanel, BorderLayout.CENTER);
            
            // Show dialog with light mode styling
            JOptionPane.showMessageDialog(null, welcomePanel, "Welcome to Digitizing Assistant", JOptionPane.PLAIN_MESSAGE);
            
            // Save the version to prevent showing the message again
            try {
                Files.write(versionFile.toPath(), VERSION.getBytes());
            } catch (IOException e) {
                // If we can't save the version, that's okay - the message will show again next time
            }
        }
        
        instance.chooseProject();
    }
}
