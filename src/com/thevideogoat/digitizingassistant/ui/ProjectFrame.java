package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;

import javax.swing.*;
import java.awt.*;

public class ProjectFrame extends JFrame {

    JPanel sidebar, conversionListPanel;
    JSplitPane splitPane;
    Project project;

    public ProjectFrame(Project project) {
        super(project.getName() + " - TVG Digitizing Assistant" + " v" + DigitizingAssistant.VERSION);
        this.project = project;
        setSize(625, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
            conversionListPanel.setLayout(new BoxLayout(conversionListPanel, BoxLayout.Y_AXIS));
            conversionListPanel.setBorder(BorderFactory.createTitledBorder("Conversions"));
            conversionListPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 315));
            sidebar.add(conversionListPanel);

            JButton newConversionBtn = new JButton("New Conversion");
            newConversionBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            newConversionBtn.addActionListener(e -> {
                // create a new conversion
                String conversionName = JOptionPane.showInputDialog("Conversion Name:");
                if(conversionName != null){
                    Conversion c = new Conversion(conversionName);
                    addConversionToSidebar(c);
                    project.addConversion(c);
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

    public void saveProject(){
        project.saveToFile(DigitizingAssistant.PROJECTS_DIRECTORY.toPath());
    }

}
