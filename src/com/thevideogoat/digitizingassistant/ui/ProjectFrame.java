package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Util;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel, detailsPanel;
    JTabbedPane tabbedPane;
    transient EditorPanel editorPanel;

    JScrollPane conversionScrollPane;
    JSplitPane splitPane;
    Project project;
    JTextField searchField;

    public ProjectFrame(Project project) {
        super(project.getName() + " - TVG Digitizing Assistant" + " v" + DigitizingAssistant.VERSION);
        this.project = project;
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DigitizingAssistant.setIcon(this);

        setupUI();
        setVisible(true);
    }

    private void setupUI() {
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);

        tabbedPane = new JTabbedPane();

        // Initialize detailsPanel
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BorderLayout());

        tabbedPane.addTab("Details", detailsPanel);
        tabbedPane.addTab("Editor", new EditorPanel(project));

        // sidebar
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // search bar
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        searchField.addCaretListener(e -> {
            String search = searchField.getText();
            for (Component c : conversionListPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton btn = (JButton) c;
                    if (btn.getText().toLowerCase().contains(search.toLowerCase())) {
                        btn.setVisible(true);
                    } else {
                        btn.setVisible(false);
                    }
                }
            }
            conversionListPanel.revalidate();
            conversionListPanel.repaint();
        });
        sidebar.add(searchField);

        // sort by dropdown
        JComboBox<String> sortBy = new JComboBox<>();
        sortBy.addItem("Name");
        sortBy.addItem("Status");
        sortBy.setBorder(BorderFactory.createTitledBorder("Sort By"));
        sortBy.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        sortBy.addActionListener(l -> {
            conversionListPanel.removeAll();
            ArrayList<Conversion> sortedConversions = Util.sortConversionsBy(project.getConversions(), sortBy.getSelectedItem().toString().toLowerCase());
            for (Conversion c : sortedConversions) {
                addConversionToSidebar(c);
            }
        });
        sidebar.add(sortBy);

        // conversion list (sidebar)
        conversionListPanel = new JPanel();
        conversionListPanel.setLayout(new BoxLayout(conversionListPanel, BoxLayout.Y_AXIS));
        conversionScrollPane = new JScrollPane(conversionListPanel);
        conversionScrollPane.setBorder(BorderFactory.createTitledBorder("Conversions"));
        conversionScrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 315));
        conversionScrollPane.setMinimumSize(new Dimension(150, 315));
        conversionScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(conversionScrollPane);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu projectMenu = new JMenu("Project");

        JMenuItem newConversionItem = new JMenuItem("New Conversion");
        newConversionItem.addActionListener(e -> {
            String conversionName = JOptionPane.showInputDialog("Conversion Name:");
            if (conversionName != null) {
                Conversion c = new Conversion(conversionName);
                addConversionToSidebar(c);
                project.addConversion(c);

                // Update detailsPanel content
                detailsPanel.removeAll();
                detailsPanel.add(new ConversionPanel(c, this), BorderLayout.CENTER);
                detailsPanel.revalidate();
                detailsPanel.repaint();
                tabbedPane.setSelectedComponent(detailsPanel);
            }
            saveProject();
        });
        projectMenu.add(newConversionItem);

        JButton newConversion = new JButton("New Conversion");
        newConversion.addActionListener(e -> {
            String conversionName = JOptionPane.showInputDialog("Conversion Name:");
            if (conversionName != null) {
                Conversion c = new Conversion(conversionName);
                addConversionToSidebar(c);
                project.addConversion(c);

                // Update detailsPanel content
                detailsPanel.removeAll();
                detailsPanel.add(new ConversionPanel(c, this), BorderLayout.CENTER);
                detailsPanel.revalidate();
                detailsPanel.repaint();
                tabbedPane.setSelectedComponent(detailsPanel);
            }
            saveProject();
        });
        sidebar.add(newConversion);

        JMenuItem renameAllFilesItem = new JMenuItem("Rename All Linked Files");
        renameAllFilesItem.addActionListener(e -> {
            for (Conversion conversion : project.getConversions()) {
                Util.renameLinkedFiles(conversion);
            }
            saveProject();
        });
        projectMenu.add(renameAllFilesItem);

        JMenuItem relinkFilesItem = new JMenuItem("Relink Files");
        relinkFilesItem.addActionListener(e -> {
            Util.relinkFiles(project);
            saveProject();
        });
        projectMenu.add(relinkFilesItem);

        JMenuItem openTrimWindow = new JMenuItem("Open File Trim Tool");
        openTrimWindow.addActionListener(e -> {
            new TrimWindow(project);
        });
        projectMenu.add(openTrimWindow);

        JMenuItem exitProjectItem = new JMenuItem("Exit Project");
        exitProjectItem.addActionListener(e -> {
            saveProject();
            DigitizingAssistant.getInstance().chooseProject();
            dispose();
        });
        projectMenu.add(exitProjectItem);

        menuBar.add(projectMenu);
        setJMenuBar(menuBar);

        for (Conversion conversion : project.getConversions()) {
            addConversionToSidebar(conversion);
        }

        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(tabbedPane);
        add(splitPane);
    }

    private void addConversionToSidebar(Conversion conversion) {
        JButton conversionBtn = new JButton(conversion.name);
        conversionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, conversionBtn.getPreferredSize().height));
        conversionBtn.setForeground(conversion.getStatusColor());
        conversionBtn.addActionListener(e -> {
            // Update detailsPanel content
            detailsPanel.removeAll();
            detailsPanel.add(new ConversionPanel(conversion, this), BorderLayout.CENTER);
            detailsPanel.revalidate();
            detailsPanel.repaint();
            tabbedPane.setSelectedComponent(detailsPanel);
        });

        conversionListPanel.add(conversionBtn);
        conversionListPanel.revalidate();
        conversionListPanel.repaint();
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

        // Clear detailsPanel content
        detailsPanel.removeAll();
        detailsPanel.revalidate();
        detailsPanel.repaint();
        displayTempContentPanel();
        saveProject();
    }

    private void displayTempContentPanel() {
        detailsPanel.removeAll();
        JLabel message = new JLabel("Select a conversion from the sidebar to view details.");
        message.setFont(new Font("Arial", Font.BOLD, 13));
        detailsPanel.add(message, BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    public void saveProject() {
        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
        updateButtonColors();
    }
}
