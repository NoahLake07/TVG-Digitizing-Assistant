package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.ConversionStatus;
import com.thevideogoat.digitizingassistant.data.Type;
import com.thevideogoat.digitizingassistant.data.Util;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Objects.requireNonNullElse;

public class ConversionPanel extends JPanel {

    ProjectFrame projectFrame;
    Conversion conversion;

    JPanel typeRow, noteRow, filesPanel, filenamePanel, dateRow, timeRow, buttonRow, statusRow, tapeDurationRow;
    JLabel header, type, note;
    JList<File> filesList;
    JComboBox<Type> typeSelector;
    JComboBox<ConversionStatus> statusSelector;
    JComboBox<String> amPmSelector;
    JTextField noteField;
    JSpinner mmSpinner, ddSpinner, yyyySpinner, hhSpinner, minSpinner, tapeDurationSpinner;

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

        // rename conversion
        JPopupMenu renameMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("Rename");
        renameMenuItem.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog("Rename Conversion", conversion.name);
            if(newName != null){
                conversion.name = newName;
                header.setText(newName);
            }
        });
        renameMenu.add(renameMenuItem);
        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    renameMenu.show(header, me.getX(), me.getY());
                }
            }
        });

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
        filesList = new JList<>();
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
        JMenuItem renameFile = new JMenuItem("Auto Rename");
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
        renameFile.addActionListener(e -> {
            Util.renameFile(listModel.getElementAt(filesList.getSelectedIndex()), conversion.name);
            updateLinkedFiles();
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

        // filename panel
        filenamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filenamePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        filenamePanel.setBorder(BorderFactory.createTitledBorder("Filenames"));
        JButton renameAll = new JButton("Rename Linked Files");
        renameAll.addActionListener(e->{
            Util.renameLinkedFiles(conversion);
            updateLinkedFiles();
        });
        filenamePanel.add(renameAll);

        // date
        dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateRow.setBorder(BorderFactory.createTitledBorder("Date of Conversion"));
        dateRow.setMaximumSize(basicRowMaxSize);
        setupDateTimeSpinners(); // setting time to now
        JSpinner.NumberEditor yyyyEditor = new JSpinner.NumberEditor(yyyySpinner, "0000");
        yyyySpinner.setEditor(yyyyEditor);
        dateRow.add(mmSpinner);
        dateRow.add(ddSpinner);
        dateRow.add(yyyySpinner);

        // Add button to update date to current date
        JButton updateDateBtn = new JButton("Update to Current Date");
        updateDateBtn.addActionListener(e -> {
            LocalDate currentDate = LocalDate.now();
            mmSpinner.setValue(currentDate.getMonthValue());
            ddSpinner.setValue(currentDate.getDayOfMonth());
            yyyySpinner.setValue(currentDate.getYear());
        });
        dateRow.add(updateDateBtn);

        // time
        timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeRow.setBorder(BorderFactory.createTitledBorder("Time of Conversion"));
        timeRow.setMaximumSize(basicRowMaxSize);

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

        // Add button to update time to current time
        JButton updateTimeBtn = new JButton("Update to Current Time");
        updateTimeBtn.addActionListener(e -> {
            LocalTime currentTime = LocalTime.now();
            int hour = currentTime.getHour();
            int minute = currentTime.getMinute();
            String amPm = hour >= 12 ? "PM" : "AM";
            hour = hour % 12;
            if (hour == 0) hour = 12;
            hhSpinner.setValue(hour);
            minSpinner.setValue(minute);
            amPmSelector.setSelectedItem(amPm);
        });
        timeRow.add(updateTimeBtn);

        // tape duration
        tapeDurationRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tapeDurationRow.setMaximumSize(basicRowMaxSize);
        JLabel tapeDurationLabel = new JLabel("Tape Duration (minutes)");
        SpinnerNumberModel tapeDurationModel = new SpinnerNumberModel((int) conversion.duration.toMinutes(), 0, Integer.MAX_VALUE, 1);
        tapeDurationSpinner = new JSpinner(tapeDurationModel);
        tapeDurationRow.add(tapeDurationLabel);
        tapeDurationRow.add(tapeDurationSpinner);

        // status
        statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.setMaximumSize(basicRowMaxSize);
        statusRow.setBorder(BorderFactory.createTitledBorder("Current Status"));
        statusSelector = new JComboBox<>(ConversionStatus.values());
        statusSelector.setSelectedItem(requireNonNullElse(conversion.status, ConversionStatus.NOT_STARTED));
        Dimension prefSize = new Dimension(150,20);
        statusSelector.setPreferredSize(prefSize);

        StatusIndicator statusIndicator = new StatusIndicator();
        statusSelector.addActionListener(e -> {
            ConversionStatus selectedStatus = (ConversionStatus) statusSelector.getSelectedItem();
            statusIndicator.updateColor(selectedStatus);
            if(selectedStatus == ConversionStatus.COMPLETED || selectedStatus == ConversionStatus.BASIC_EDITING){
                tapeDurationRow.setVisible(true);
            } else {
                tapeDurationRow.setVisible(false);
            }
            if(selectedStatus == ConversionStatus.NOT_STARTED){
                dateRow.setVisible(false);
                timeRow.setVisible(false);
            } else {
                dateRow.setVisible(true);
                timeRow.setVisible(true);
                // Removed call to setupDateTimeSpinners()
            }
            statusSelector.setPreferredSize(prefSize);
        });
        statusRow.add(statusIndicator);
        statusRow.add(statusSelector);

        ConversionStatus selectedStatus = (ConversionStatus) statusSelector.getSelectedItem();
        if(selectedStatus == ConversionStatus.COMPLETED || selectedStatus == ConversionStatus.BASIC_EDITING){
            tapeDurationRow.setVisible(true);
        } else {
            tapeDurationRow.setVisible(false);
        }

        if(selectedStatus == ConversionStatus.NOT_STARTED){
            dateRow.setVisible(false);
            timeRow.setVisible(false);
        } else {
            dateRow.setVisible(true);
            timeRow.setVisible(true);
        }

        // button row
        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.setMaximumSize(basicRowMaxSize);
        saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            // save the conversion
            conversion.name = header.getText();
            conversion.note = noteField.getText();
            conversion.type = (Type) typeSelector.getSelectedItem();
            conversion.dateOfConversion.month = mmSpinner.getValue().toString();
            conversion.dateOfConversion.day = ddSpinner.getValue().toString();
            conversion.dateOfConversion.year = yyyySpinner.getValue().toString();
            conversion.timeOfConversion.hour = hhSpinner.getValue().toString();
            conversion.timeOfConversion.minute = minSpinner.getValue().toString();
            conversion.timeOfConversion.am_pm = (String) amPmSelector.getSelectedItem();
            conversion.status = (ConversionStatus) statusSelector.getSelectedItem();
            conversion.duration = Duration.ofMinutes((Integer) tapeDurationSpinner.getValue());

            // save the project (serialize it)
            projectFrame.saveProject();
        });
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> {
            // delete the conversion
            projectFrame.remove(this);
            projectFrame.saveProject();
        });

        buttonRow.add(deleteBtn);
        buttonRow.add(saveBtn);

        // add components to the panel
        add(typeRow);
        add(noteRow);
        add(filesPanel);
        add(filenamePanel);
        add(statusRow);
        add(dateRow);
        add(timeRow);
        add(tapeDurationRow);
        add(buttonRow);
    }

    private void updateLinkedFiles(){
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
    }

    private void setupDateTimeSpinners(){
        try {
            // Month Spinner
            if(mmSpinner == null) {
                SpinnerNumberModel mmModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getMonth()), 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
            } else {
                mmSpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getMonth()));
            }
            // Day Spinner
            if(ddSpinner == null) {
                SpinnerNumberModel ddModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getDay()), 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
            } else {
                ddSpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getDay()));
            }
            // Year Spinner
            if(yyyySpinner == null) {
                SpinnerNumberModel yyyyModel = new SpinnerNumberModel(Integer.parseInt(conversion.dateOfConversion.getYear()), 1900, 2100, 1);
                yyyySpinner = new JSpinner(yyyyModel);
            } else {
                yyyySpinner.setValue(Integer.parseInt(conversion.dateOfConversion.getYear()));
            }
        } catch (NumberFormatException e) {
            // Handle exceptions appropriately
            if(mmSpinner == null) {
                SpinnerNumberModel mmModel = new SpinnerNumberModel(1, 1, 12, 1);
                mmSpinner = new JSpinner(mmModel);
            } else {
                mmSpinner.setValue(1);
            }
            if(ddSpinner == null) {
                SpinnerNumberModel ddModel = new SpinnerNumberModel(1, 1, 31, 1);
                ddSpinner = new JSpinner(ddModel);
            } else {
                ddSpinner.setValue(1);
            }
            if(yyyySpinner == null) {
                SpinnerNumberModel yyyyModel = new SpinnerNumberModel(2000, 1900, 2100, 1);
                yyyySpinner = new JSpinner(yyyyModel);
            } else {
                yyyySpinner.setValue(2000);
            }
        }
    }

    public class StatusIndicator extends JPanel {
        public StatusIndicator() {
            setPreferredSize(new Dimension(20, 20));
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            updateColor(requireNonNullElse(conversion.status, ConversionStatus.NOT_STARTED));
        }

        public void updateColor(ConversionStatus status) {
            switch (status) {
                case NOT_STARTED:
                    setBackground(new Color(59, 59, 59));
                    break;
                case DAMAGED:
                    setBackground(new Color(204, 25, 25));
                    break;
                case IN_PROGRESS:
                    setBackground(new Color(255, 165, 0));
                    break;
                case BASIC_EDITING:
                    setBackground(new Color(83, 25, 194));
                    break;
                case COMPLETED:
                    setBackground(new Color(0, 128, 0));
                    break;
            }
        }
    }

}
