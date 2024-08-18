package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;

import javax.swing.*;
import java.awt.*;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel;
    JScrollPane conversionScrollPane;
    JSplitPane splitPane;
    Project project;

    public ProjectFrame(Project project) {
        super(project.getName() + " - TVG Digitizing Assistant" + " v" + DigitizingAssistant.VERSION);
        this.project = project;
        setSize(625, 450);
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

        conversionListPanel = new JPanel();
        conversionListPanel.setLayout(new GridLayout(0, 1, 5, 5)); // Uniform button sizes
        conversionScrollPane = new JScrollPane(conversionListPanel);
        conversionScrollPane.setBorder(BorderFactory.createTitledBorder("Conversions"));
        conversionScrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 315));
        sidebar.add(conversionScrollPane);

        JButton newConversionBtn = new JButton("New Conversion");
        newConversionBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        newConversionBtn.addActionListener(e -> {
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
        sidebar.add(newConversionBtn);

        JButton projectManagementBtn = new JButton("Exit Project");
        projectManagementBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        projectManagementBtn.addActionListener(e -> {
            saveProject();
            DigitizingAssistant.getInstance().chooseProject();
            dispose();
        });
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(projectManagementBtn);

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
        conversionBtn.addActionListener(e -> {
            // show the conversion panel
            splitPane.setRightComponent(new ConversionPanel(conversion,this));
        });

        conversionListPanel.add(conversionBtn);
        conversionListPanel.revalidate();
        conversionListPanel.repaint();
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

    public void saveProject(){
        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
    }

}