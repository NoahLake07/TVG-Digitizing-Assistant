package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Type;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class ConversionPanel extends JPanel {

    ProjectFrame projectFrame;
    Conversion conversion;

    JPanel typeRow, noteRow, filesPanel, dateRow, timeRow, buttonRow;
    JLabel header, type, note;
    JComboBox<Type> typeSelector;
    JComboBox<String> amPmSelector;
    JTextField noteField;
    JSpinner mmField, ddField, yyyyField, hhField;

    JButton addFileBtn, saveBtn;

    public ConversionPanel(Conversion conversion, ProjectFrame projectFrame){
        super();
        this.conversion = conversion;
        this.projectFrame = projectFrame;
        setupUI();
    }

    private void setupUI(){
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Dimension basicRowMaxSize = new Dimension(Short.MAX_VALUE, 50);

        // header
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerRow.setMaximumSize(basicRowMaxSize);
        header = new JLabel(conversion.name);
        header.setFont(new Font("Arial", Font.BOLD, 20));
        headerRow.add(header);
        add(headerRow);

        // type
        typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeRow.setMaximumSize(basicRowMaxSize);
        type = new JLabel("Tape Format Type");
        typeSelector = new JComboBox<>(Type.values());
        if (conversion.type != null) typeSelector.setSelectedItem(conversion.type);
        typeSelector.setPreferredSize(new Dimension(100, 20));
        typeRow.add(type);
        typeRow.add(typeSelector);

        // note
        noteRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        noteRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        note = new JLabel("Conversion Notes");
        noteField = new JTextField(conversion.note);
        noteField.setPreferredSize(new Dimension(200, 30));
        noteRow.add(note);
        noteRow.add(noteField);

        // linked files
        filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBorder(BorderFactory.createTitledBorder("Linked Files"));
        filesPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 150));
            JList<File> filesList = new JList<>();
            filesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filesList.setLayoutOrientation(JList.VERTICAL);
            filesList.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
            DefaultListModel<File> listModel = new DefaultListModel<>();
            if(conversion.linkedFiles != null) {
                for (File f : conversion.linkedFiles) {
                    listModel.addElement(f);
                }
            } else {
                listModel.addElement(new File("No files attached"));
            }
            filesList.setModel(listModel);
            filesPanel.add(filesList);

            JPanel filesButtonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                addFileBtn = new JButton("Attach File");
                addFileBtn.addActionListener(e -> {
                    // open a file chooser dialog
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setMultiSelectionEnabled(true);
                    int result = fileChooser.showOpenDialog(this);
                    if(result == JFileChooser.APPROVE_OPTION){
                        for(File f : fileChooser.getSelectedFiles()){
                            listModel.addElement(f);
                            conversion.linkedFiles.add(f);
                        }
                    }
                });
            filesButtonRow.add(addFileBtn);
            filesPanel.add(filesButtonRow);

            // Create a popup menu
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem openMenuItem = new JMenuItem("Open File");
            JMenuItem openFileLocationItem = new JMenuItem("Open File Location");
            JMenuItem removeMenuItem = new JMenuItem("Remove");
            openMenuItem.addActionListener(e -> {
                int selectedIndex = filesList.getSelectedIndex();
                if (selectedIndex != -1) {
                    File selectedFile = listModel.getElementAt(selectedIndex);
                    try {
                        Desktop.getDesktop().open(selectedFile);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Failed to open the file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            openFileLocationItem.addActionListener(e -> {
                int selectedIndex = filesList.getSelectedIndex();
                if (selectedIndex != -1) {
                    File selectedFile = listModel.getElementAt(selectedIndex);
                    try {
                        Desktop.getDesktop().open(selectedFile.getParentFile());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Failed to open the file location.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            removeMenuItem.addActionListener(e -> {
                int selectedIndex = filesList.getSelectedIndex();
                if (selectedIndex != -1) {
                    listModel.remove(selectedIndex);
                    conversion.linkedFiles.remove(selectedIndex);
                }
            });
            popupMenu.add(openMenuItem);
            popupMenu.add(openFileLocationItem);
            popupMenu.add(removeMenuItem);

            // Add a mouse listener to the list
            filesList.addMouseListener(new MouseAdapter()    {
                public void mousePressed(MouseEvent me) {
                    if (SwingUtilities.isRightMouseButton(me) && !filesList.isSelectionEmpty() && filesList.locationToIndex(me.getPoint()) == filesList.getSelectedIndex()) {
                        popupMenu.show(filesList, me.getX(), me.getY());
                    }
                }
            });

            // date
            dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            dateRow.setBorder(BorderFactory.createTitledBorder("Date of Conversion"));
            dateRow.setMaximumSize(basicRowMaxSize);
            SpinnerNumberModel mmModel, ddModel, yyyyModel;
            JSpinner mmSpinner, ddSpinner, yyyySpinner;
            try {
                mmModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getMonth()), 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
                ddModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getDay()), 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
                yyyyModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getYear()), 1900, 2100, 1);
                yyyySpinner = new JSpinner(yyyyModel);
            } catch (NumberFormatException e) {
                mmModel = new SpinnerNumberModel(1, 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
                ddModel = new SpinnerNumberModel(1, 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
                yyyyModel = new SpinnerNumberModel(2000, 1900, 2100, 1);
                yyyySpinner = new JSpinner(yyyyModel);
            }
            JSpinner.NumberEditor yyyyEditor = new JSpinner.NumberEditor(yyyySpinner, "0000");
            yyyySpinner.setEditor(yyyyEditor);
            dateRow.add(mmSpinner);
            dateRow.add(ddSpinner);
            dateRow.add(yyyySpinner);

            // time
            timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            timeRow.setBorder(BorderFactory.createTitledBorder("Time of Conversion"));
            timeRow.setMaximumSize(basicRowMaxSize);

            JSpinner hhSpinner, minSpinner;
            try {
                SpinnerNumberModel hhModel = new SpinnerNumberModel(Integer.parseInt(conversion.timeOfConversion.getHour()), 1, 12, 1);
                hhSpinner = new JSpinner(hhModel);
                if (conversion.timeOfConversion.hour != null)
                    hhSpinner.setValue(Integer.parseInt(conversion.timeOfConversion.getHour()));
                SpinnerNumberModel minModel = new SpinnerNumberModel(Integer.parseInt(conversion.timeOfConversion.getMinute()), 0, 59, 1);
                minSpinner = new JSpinner(minModel);
                JSpinner.NumberEditor editor = new JSpinner.NumberEditor(minSpinner, "00");
                minSpinner.setEditor(editor);
                if (conversion.timeOfConversion.minute != null)
                    minSpinner.setValue(Integer.parseInt(conversion.timeOfConversion.getMinute()));
                timeRow.add(hhSpinner);
                timeRow.add(minSpinner);
                amPmSelector = new JComboBox<>(new String[]{"AM", "PM"});
                if (conversion.timeOfConversion.getAmPm() != null)
                    amPmSelector.setSelectedItem(conversion.timeOfConversion.getAmPm());
                timeRow.add(amPmSelector);
            } catch (NumberFormatException e) {
                SpinnerNumberModel hhModel = new SpinnerNumberModel(12, 1, 12, 1);
                hhSpinner = new JSpinner(hhModel);
                SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 59, 1);
                minSpinner = new JSpinner(minModel);
                JSpinner.NumberEditor editor = new JSpinner.NumberEditor(minSpinner, "00");
                minSpinner.setEditor(editor);
                timeRow.add(hhSpinner);
                timeRow.add(minSpinner);
                amPmSelector = new JComboBox<>(new String[]{"AM", "PM"});
                amPmSelector.setSelectedItem("AM");
                timeRow.add(amPmSelector);
            }
            amPmSelector.setPreferredSize(new Dimension(50, 20));

        // button row
        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.setMaximumSize(basicRowMaxSize);
        saveBtn = new JButton("Save");
        JSpinner finalMmSpinner = mmSpinner;
        JSpinner finalDdSpinner = ddSpinner;
        JSpinner finalYyyySpinner = yyyySpinner;
        JSpinner finalHhSpinner = hhSpinner;
        saveBtn.addActionListener(e -> {
            // save the conversion
            conversion.name = header.getText();
            conversion.note = noteField.getText();
            conversion.type = (Type) typeSelector.getSelectedItem();
            conversion.dateOfConversion.month = finalMmSpinner.getValue().toString();
            conversion.dateOfConversion.day = finalDdSpinner.getValue().toString();
            conversion.dateOfConversion.year = finalYyyySpinner.getValue().toString();
            conversion.timeOfConversion.hour = finalHhSpinner.getValue().toString();
            conversion.timeOfConversion.minute = finalMmSpinner.getValue().toString();
            conversion.timeOfConversion.am_pm = (String) amPmSelector.getSelectedItem();

            // save the project (serialize it)
            projectFrame.saveProject();
        });
        buttonRow.add(saveBtn);

        // add components to the panel
        add(typeRow);
        add(noteRow);
        add(filesPanel);
        add(dateRow);
        add(timeRow);
        add(buttonRow);
    }

}