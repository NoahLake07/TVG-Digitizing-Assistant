package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Project;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

public class DigitizingAssistant {

    public static final String CURRENT_DIRECTORY = System.getProperty("user.home");
    public static final File PROJECTS_DIRECTORY;
    public static final String OS = System.getProperty("os.name").toLowerCase();
    public static final String VERSION = "1.3";

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

        // Set custom cell renderer
        projectList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel && value instanceof File) {
                    JLabel label = (JLabel) c;
                    String name = ((File) value).getName();
                    name = name.substring(0, name.lastIndexOf(".project"));
                    label.setText(name);
                    // Add padding to the left
                    label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                }
                setForeground(isSelected ? Theme.TEXT : Theme.TEXT_SECONDARY);
                setBackground(isSelected ? Theme.ACCENT : Theme.SURFACE);
                return this;
            }
        });

        // Load projects
        File[] allProjects = PROJECTS_DIRECTORY.listFiles();
        DefaultListModel<File> listModel = new DefaultListModel<>();
        if (allProjects != null) {
            for (File f : allProjects) {
                if(f.getName().endsWith(".project")) {
                    listModel.addElement(f);
                }
            }
        }
        projectList.setModel(listModel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton newProjectBtn = new JButton("New Project");
        JButton openProjectBtn = new JButton("Open Project");
        Theme.styleButton(newProjectBtn);
        Theme.styleButton(openProjectBtn);

        newProjectBtn.addActionListener(e -> {
            String projectName = JOptionPane.showInputDialog(projectChooser, "Project Name:");
            if (projectName != null && !projectName.trim().isEmpty()) {
                new ProjectFrame(new Project(projectName));
                projectChooser.dispose();
            }
        });

        openProjectBtn.addActionListener(e -> {
            if (projectList.getSelectedValue() != null) {
                new ProjectFrame(new Project(projectList.getSelectedValue().toPath()));
                projectChooser.dispose();
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
                        new ProjectFrame(new Project(selectedFile.toPath()));
                        projectChooser.dispose();
                    }
                }
            }
        });

        buttonPanel.add(newProjectBtn);
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
        instance.chooseProject();
    }
}
