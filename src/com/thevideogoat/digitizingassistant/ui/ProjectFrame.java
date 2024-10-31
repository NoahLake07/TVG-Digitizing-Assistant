package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Util;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel;
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

    private void setupUI(){
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);

        // sidebar
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // search bar
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField.setMaximumSize(new Dimension(Short.MAX_VALUE, 30));
        searchField.addCaretListener(e -> {
            String search = searchField.getText();
            if(!search.isEmpty()) {
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
            } else {
                for (Component c : conversionListPanel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setVisible(true);
                    }
                }
            }
            conversionListPanel.revalidate();
            conversionListPanel.repaint();
        });
        searchField.addActionListener(e -> {
            String search = searchField.getText();
            if(!search.isEmpty()) {
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
            } else {
                for (Component c : conversionListPanel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setVisible(true);
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
        sortBy.addActionListener(l->{
            conversionListPanel.removeAll();
                ArrayList<Conversion> sortedConversions = Util.sortConversionsBy(project.getConversions(), sortBy.getSelectedItem().toString().toLowerCase());
                for(Conversion c : sortedConversions){
                    addConversionToSidebar(c);
                }
        });
        sidebar.add(sortBy);

        // conversion list (sidebar)
        conversionListPanel = new JPanel();
        conversionListPanel.setLayout(new BoxLayout(conversionListPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for reordering
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
            // create a new conversion
            String conversionName = JOptionPane.showInputDialog("Conversion Name:");
            if(conversionName != null){
                Conversion c = new Conversion(conversionName);
                addConversionToSidebar(c);
                project.addConversion(c);
                splitPane.setRightComponent(new ConversionPanel(c,this));
            }
            saveProject();
        });
        projectMenu.add(newConversionItem);

        JButton newConversion = new JButton("New Conversion");
        newConversion.addActionListener(e -> {
            // create a new conversion
            String conversionName = JOptionPane.showInputDialog("Conversion Name:");
            if(conversionName != null){
                Conversion c = new Conversion(conversionName);
                addConversionToSidebar(c);
                project.addConversion(c);
                splitPane.setRightComponent(new ConversionPanel(c,this));
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

        JMenuItem exitProjectItem = new JMenuItem("Exit Project");
        exitProjectItem.addActionListener(e -> {
            saveProject();
            DigitizingAssistant.getInstance().chooseProject();
            dispose();
        });
        projectMenu.add(exitProjectItem);

        menuBar.add(projectMenu);
        setJMenuBar(menuBar);

        for(Conversion conversion : project.getConversions()){
            addConversionToSidebar(conversion);
        }

        // temporary message panel
        JPanel tempContentPanel = new JPanel(new GridBagLayout());
        JLabel message = new JLabel("Select a conversion from the sidebar to view details.");
        message.setFont(new Font("Arial", Font.BOLD, 13));
        tempContentPanel.add(message);

        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(tempContentPanel);
        add(splitPane);
    }

    private void displayTempContentPanel(){
        // temporary message panel
        JPanel tempContentPanel = new JPanel(new GridBagLayout());
        JLabel message = new JLabel("Select a conversion from the sidebar to view details.");
        message.setFont(new Font("Arial", Font.BOLD, 13));
        tempContentPanel.add(message);
        splitPane.setRightComponent(tempContentPanel);
    }

    private void addConversionToSidebar(Conversion conversion){
        JButton conversionBtn = new JButton(conversion.name);
        conversionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, conversionBtn.getPreferredSize().height));
        conversionBtn.setForeground(conversion.getStatusColor());
        conversionBtn.addActionListener(e -> {
            // show the conversion panel
            splitPane.setRightComponent(new ConversionPanel(conversion, this));
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

    public void remove(ConversionPanel conversion){
        for(Component c : conversionListPanel.getComponents()){
            if(c instanceof JButton){
                JButton btn = (JButton) c;
                if(btn.getText().equals(conversion.conversion.name)){
                    conversionListPanel.remove(btn);
                    conversionListPanel.revalidate();
                    conversionListPanel.repaint();
                    break;
                }
            }
        }

        for(Conversion c : project.getConversions()){
            if(c.name.equals(conversion.conversion.name)){
                project.getConversions().remove(c);
            }
        }
        displayTempContentPanel();
        saveProject();
    }

    public void saveProject() {
        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
        updateButtonColors();
    }

}