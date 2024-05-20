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
    public static final String VERSION = "1.1";

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
        JFrame projectChooser = new JFrame("Choose a project");
        projectChooser.setSize(300, 400);
        projectChooser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIcon(projectChooser);

        // project chooser panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // header
            JPanel headerPanel = new JPanel();
                JLabel header = new JLabel("Digitizing Assistant");
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                header.setFont(new Font("Arial", Font.BOLD, 20));
                headerPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

                ImageIcon icon = new ImageIcon(DigitizingAssistant.getIcon().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
                JLabel iconLabel = new JLabel(icon);
                headerPanel.add(iconLabel);
                headerPanel.add(header);
            panel.add(headerPanel);

        // project list
        JList<File> projectList = new JList<>();
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setLayoutOrientation(JList.VERTICAL);
        projectList.setMaximumSize(new Dimension(Short.MAX_VALUE, 500));

            // Set a custom cell renderer
            projectList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel && value instanceof File) {
                        // Set the text of the label to the name of the file
                        ((JLabel) c).setText(((File) value).getName());
                    }
                    return c;
                }
            });

            File[] allProjects = PROJECTS_DIRECTORY.listFiles();
            DefaultListModel<File> listModel = new DefaultListModel<>();
            if(allProjects != null){
                for(File f : allProjects) {
                    listModel.addElement(f);
                }
            }
            projectList.setModel(listModel);

            // Create a popup menu
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteMenuItem = new JMenuItem("Delete");
            deleteMenuItem.addActionListener(e -> {
                int selectedIndex = projectList.getSelectedIndex();
                if (selectedIndex != -1) {
                    // Delete the project file
                    File selectedFile = listModel.getElementAt(selectedIndex);
                    boolean deleted = selectedFile.delete();
                    if (deleted) {
                        // Remove the item from the list
                        listModel.remove(selectedIndex);
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to delete the project file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            popupMenu.add(deleteMenuItem);

            // Add a mouse listener to the list
            projectList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent me) {
                    if (SwingUtilities.isRightMouseButton(me) && !projectList.isSelectionEmpty() && projectList.locationToIndex(me.getPoint()) == projectList.getSelectedIndex()) {
                        popupMenu.show(projectList, me.getX(), me.getY());
                    }
                }
            });
            panel.add(projectList);


        // button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
            Dimension buttonBounds = new Dimension(150, 50);

            // new project button
            JButton newProjectBtn = new JButton("New Project");
            newProjectBtn.setMaximumSize(buttonBounds);
            newProjectBtn.addActionListener(e -> {
                // create a new project
                String projectName = JOptionPane.showInputDialog("Project Name:");
                if(projectName != null){
                    new ProjectFrame(new Project(projectName));
                    projectChooser.setVisible(false);
                }
            });
            buttonPanel.add(newProjectBtn);

            // open project button
            JButton openProjectBtn = new JButton("Open Project");
            openProjectBtn.setMaximumSize(buttonBounds);
            openProjectBtn.addActionListener(e -> {
                // open the project that the user selected
                new ProjectFrame(new Project(projectList.getSelectedValue().toPath()));
                projectChooser.setVisible(false);
            });
            projectList.addListSelectionListener(e -> {
                if(projectList.getSelectedValue() != null){
                    openProjectBtn.setEnabled(true);
                }
            });
            if(projectList.getSelectedValue() == null){
                openProjectBtn.setEnabled(false);
            }
            buttonPanel.add(openProjectBtn);
        panel.add(buttonPanel);

        projectChooser.add(panel);
        projectChooser.setVisible(true);
    }

    public static void setIcon(JFrame frame){
        try {
            BufferedImage icon = ImageIO.read(Objects.requireNonNull(DigitizingAssistant.class.getResourceAsStream("/tvgdigassistappicon.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            throw new Error("Failed to set icon.");
        }
    }

    public static BufferedImage getIcon(){
        BufferedImage icon = null;
        try {
            icon = ImageIO.read(Objects.requireNonNull(DigitizingAssistant.class.getResourceAsStream("/tvgdigassistappicon.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return icon;
    }

    public static DigitizingAssistant getInstance() {
        return instance;
    }

    public static void main(String[] args) throws RuntimeException {
        if(!PROJECTS_DIRECTORY.exists()){
            PROJECTS_DIRECTORY.mkdir();
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            throw new Error("Could not set system look and feel.");
        }

        instance = new DigitizingAssistant();
        instance.chooseProject();
    }

}
