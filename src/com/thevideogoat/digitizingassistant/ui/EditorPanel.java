package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class EditorPanel extends JPanel {

    private Project project;
    private Conversion currentConversion;
    private int conversionIndex; // To keep track of the current conversion index

    // UI components
    private JLabel conversionIndicatorLabel;
    private JList<File> linkedFilesList;
    private JLabel startLabel, endLabel;
    private JSlider startSlider, endSlider;
    private JPanel startPreviewPlaceholder, endPreviewPlaceholder;
    private JButton exportButton, prevButton, nextButton;

    // Currently selected file
    private File selectedFile;

    public EditorPanel(Project project) {
        this.project = project;
        this.setBackground(new Color(173, 164, 161));

        if (project.getConversions().size() > 0) {
            conversionIndex = 0; // Start with the first conversion
            currentConversion = project.getConversions().get(conversionIndex);
        } else {
            JOptionPane.showMessageDialog(this, "No conversions available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        // Set the layout to BorderLayout
        this.setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        JLabel headerLabel = new JLabel("Trim Conversion", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        conversionIndicatorLabel = new JLabel(getConversionIndicatorText(), SwingConstants.CENTER);
        conversionIndicatorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        headerPanel.add(headerLabel);
        headerPanel.add(conversionIndicatorLabel);
        this.add(headerPanel, BorderLayout.NORTH);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Linked Files List
        linkedFilesList = new JList<>(currentConversion.linkedFiles.toArray(new File[0]));
        linkedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(linkedFilesList);
        scrollPane.setPreferredSize(new Dimension(200, 400));
        mainPanel.add(scrollPane, BorderLayout.WEST);

        // Previews and Sliders Panel
        JPanel previewAndSlidersPanel = new JPanel();
        previewAndSlidersPanel.setLayout(new BoxLayout(previewAndSlidersPanel, BoxLayout.Y_AXIS));

        // Start Preview Placeholder
        startPreviewPlaceholder = new JPanel();
        startPreviewPlaceholder.setPreferredSize(new Dimension(400, 200));
        startPreviewPlaceholder.setBackground(Color.GRAY);
        startPreviewPlaceholder.add(new JLabel("Start Preview"));
        previewAndSlidersPanel.add(startPreviewPlaceholder);

        // Start Slider and Label
        JPanel startPanel = new JPanel(new BorderLayout());
        startSlider = new JSlider();
        startLabel = new JLabel("Start: 00:00:00");
        startPanel.add(startSlider, BorderLayout.CENTER);
        startPanel.add(startLabel, BorderLayout.EAST);
        previewAndSlidersPanel.add(startPanel);

        // End Preview Placeholder
        endPreviewPlaceholder = new JPanel();
        endPreviewPlaceholder.setPreferredSize(new Dimension(400, 200));
        endPreviewPlaceholder.setBackground(Color.GRAY);
        endPreviewPlaceholder.add(new JLabel("End Preview"));
        previewAndSlidersPanel.add(endPreviewPlaceholder);

        // End Slider and Label
        JPanel endPanel = new JPanel(new BorderLayout());
        endSlider = new JSlider();
        endLabel = new JLabel("End: 00:00:00");
        endPanel.add(endSlider, BorderLayout.CENTER);
        endPanel.add(endLabel, BorderLayout.EAST);
        previewAndSlidersPanel.add(endPanel);

        mainPanel.add(previewAndSlidersPanel, BorderLayout.CENTER);
        this.add(mainPanel, BorderLayout.CENTER);

        // Bottom Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        exportButton = new JButton("Export");
        prevButton = new JButton("Previous");
        nextButton = new JButton("Next");
        buttonPanel.add(prevButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(nextButton);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // List selection listener for linked files
        linkedFilesList.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                selectedFile = linkedFilesList.getSelectedValue();
                if (selectedFile != null) {
                    if (selectedFile.exists()) {
                        updateSliders(); // Directly call updateSliders()
                    } else {
                        JOptionPane.showMessageDialog(EditorPanel.this, "Selected file not found: " + selectedFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Action listener for Previous button
        prevButton.addActionListener(e -> navigateToPreviousConversion());

        // Action listener for Next button
        nextButton.addActionListener(e -> navigateToNextConversion());

        // Action listener for Export button
        exportButton.addActionListener(e -> exportTrimmedVideo());

        // Change listener for Start Slider
        startSlider.addChangeListener(e -> {
            if (startSlider.getValue() >= endSlider.getValue()) {
                startSlider.setValue(endSlider.getValue() - 1); // Prevent start time from being equal or after end time
            }
            updateStartLabel();
        });

        // Change listener for End Slider
        endSlider.addChangeListener(e -> {
            if (endSlider.getValue() <= startSlider.getValue()) {
                endSlider.setValue(startSlider.getValue() + 1); // Prevent end time from being equal or before start time
            }
            updateEndLabel();
        });
    }

    private void loadMedia() {
        if (currentConversion.linkedFiles != null && !currentConversion.linkedFiles.isEmpty()) {
            // Select the first file by default
            linkedFilesList.setSelectedIndex(0);
        } else {
            JOptionPane.showMessageDialog(this, "No linked files available.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSliders() {
        int duration = 60000; // 1 minute in milliseconds
        startSlider.setMinimum(0);
        startSlider.setMaximum(duration);
        startSlider.setValue(0);

        endSlider.setMinimum(0);
        endSlider.setMaximum(duration);
        endSlider.setValue(duration);

        updateStartLabel();
        updateEndLabel();
    }

    private void updateStartLabel() {
        String time = formatTime(startSlider.getValue());
        startLabel.setText("Start: " + time);
    }

    private void updateEndLabel() {
        String time = formatTime(endSlider.getValue());
        endLabel.setText("End: " + time);
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getConversionIndicatorText() {
        return "Conversion " + (conversionIndex + 1) + " of " + project.getConversions().size();
    }

    private void updateUIForCurrentConversion() {
        // Update the conversion indicator text
        conversionIndicatorLabel.setText(getConversionIndicatorText());

        // Update linked files list
        linkedFilesList.setListData(currentConversion.linkedFiles.toArray(new File[0]));

        // Clear current selection
        linkedFilesList.clearSelection();
        selectedFile = null;

        // Refresh the panel to apply changes
        this.revalidate();
        this.repaint();

        // Load media for the new conversion
        loadMedia();
    }

    private void navigateToPreviousConversion() {
        if (conversionIndex > 0) {
            conversionIndex--;
            currentConversion = project.getConversions().get(conversionIndex);
            updateUIForCurrentConversion();
        }
    }

    private void navigateToNextConversion() {
        if (conversionIndex < project.getConversions().size() - 1) {
            conversionIndex++;
            currentConversion = project.getConversions().get(conversionIndex);
            updateUIForCurrentConversion();
        }
    }

    private void exportTrimmedVideo() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "No file selected for export.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Selected file not found: " + selectedFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get start and end times from sliders (in milliseconds)
        double start = startSlider.getValue() / 1000.0;
        double end = endSlider.getValue() / 1000.0;

        if (start >= end) {
            JOptionPane.showMessageDialog(this, "Start time must be less than end time.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prompt user to select the output file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Export Location");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            try {
                // For demo purposes, we skip actual video trimming
                JOptionPane.showMessageDialog(this, "Export completed successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
