package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Util;
import com.thevideogoat.digitizingassistant.util.ExportTask;
import com.thevideogoat.digitizingassistant.util.TaskQueue;
import com.thevideogoat.digitizingassistant.util.TrimPanel;
import com.thevideogoat.digitizingassistant.data.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class TrimWindow extends JFrame {

    // region Fields
    JSplitPane splitPane;
    JButton viewQueue, exportAll;
    ArrayList<File> videoFiles = new ArrayList<>();
    TaskQueue queue;
    public static boolean useNoteAsNewFilename, appendToAttachedFiles;
    HashMap<File,Conversion> conversionMap = new HashMap<>();
    // endregion

    // region Constructor
    public TrimWindow(Project project) {
        super("TVG Digitizing Assistant: Conversion Trim Tool");
        setSize(1100, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.queue = new TaskQueue();

        // region Main Split Pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        // endregion

        // region Key Bindings
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        KeyStroke nextKey = KeyStroke.getKeyStroke(']');
        KeyStroke prevKey = KeyStroke.getKeyStroke('[');

        inputMap.put(nextKey, "nextFile");
        inputMap.put(prevKey, "prevFile");

        actionMap.put("nextFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextFile();
            }
        });

        actionMap.put("prevFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousFile();
            }
        });
        // endregion

        // region Left Panel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel selectorLabel = new JLabel("All Video Files");
        selectorLabel.setFont(new Font("Arial", Font.BOLD, 14));
        leftPanel.add(selectorLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JScrollPane selectorScrollPane = new JScrollPane(buttonPanel);
        selectorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        leftPanel.add(selectorScrollPane, BorderLayout.CENTER);

        // Scan for video files
        for (Conversion c : project.getConversions()) {
            for (File videoFile : c.linkedFiles) {
                videoFiles.add(videoFile);
                conversionMap.put(videoFile, c);
                JButton videoButton = new JButton(videoFile.getName());
                videoButton.addActionListener(e -> {
                    splitPane.setRightComponent(new TrimInterface(videoFile, queue, c));
                });
                buttonPanel.add(videoButton);
            }
        }

        JPanel queuePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Queue Controls"));

        viewQueue = new JButton("View Queue");
        viewQueue.addActionListener(e -> {
            updateQueue(project);

            JFrame queueFrame = new JFrame("Task Queue");
            queueFrame.setSize(400, 600);
            queueFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            queueFrame.setLayout(new BorderLayout(5, 5));

            // Create a list model and populate it with tasks from the queue.
            DefaultListModel<ExportTask> listModel = new DefaultListModel<>();
            for (ExportTask task : queue.getTasks()) {
                listModel.addElement(task);
            }

            // Create the JList that uses the model.
            JList<ExportTask> taskList = new JList<>(listModel);
            taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(taskList);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            queueFrame.add(scrollPane, BorderLayout.CENTER);

            // Panel for action buttons.
            JPanel buttonPanel1 = new JPanel(new FlowLayout());

            // Button to delete the selected task.
            JButton deleteButton = new JButton("Delete Selected Task");
            deleteButton.addActionListener(ev -> {
                int selectedIndex = taskList.getSelectedIndex();
                if (selectedIndex != -1) {
                    ExportTask selectedTask = listModel.getElementAt(selectedIndex);
                    int confirm = JOptionPane.showConfirmDialog(queueFrame,
                            "Delete task: " + selectedTask + " ?",
                            "Confirm Delete",
                            JOptionPane.OK_CANCEL_OPTION);
                    if (confirm == JOptionPane.OK_OPTION) {
                        listModel.remove(selectedIndex);
                        queue.getTasks().remove(selectedTask);
                    }
                }
            });
            buttonPanel1.add(deleteButton);

            // Button to clear the entire queue.
            JButton clearButton = new JButton("Clear Queue");
            clearButton.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(queueFrame,
                        "Are you sure you want to clear the entire queue?",
                        "Confirm Clear",
                        JOptionPane.OK_CANCEL_OPTION);
                if (confirm == JOptionPane.OK_OPTION) {
                    listModel.clear();
                    queue.getTasks().clear();
                }
            });
            buttonPanel1.add(clearButton);

            queueFrame.add(buttonPanel1, BorderLayout.SOUTH);
            queueFrame.setVisible(true);
        });

        queuePanel.add(viewQueue);

        exportAll = new JButton("Export All");
        exportAll.setFont(new Font("Arial", Font.BOLD, 12));
        exportAll.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, "Export all " + queue.getTasks().size() + " task(s)?");
            if (result == JOptionPane.OK_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                // Set the current directory to the last used directory
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    chooser.setCurrentDirectory(new File(lastDir));
                }
                
                int chooserResult = chooser.showOpenDialog(null);
                if (chooserResult != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File selectedDir = chooser.getSelectedFile();
                queue.setExportLoc(selectedDir.toPath());
                
                // Save the selected directory as the last used directory
                Preferences.getInstance().setLastUsedDirectory(selectedDir.getAbsolutePath());
                
                queue.exportAll();
                initExport();
            }
        });
        queuePanel.add(exportAll);

        JButton serializeQueue = new JButton("Save Queue");
        serializeQueue.addActionListener(e -> {
            // serialize object to file
            boolean createNew = false, savedSucessfully = false;
            String projectQueuePath = Util.getProjectQueuePath(project);
            if (Files.exists(new File(projectQueuePath).toPath())) {
                int result = JOptionPane.showConfirmDialog(null, "A queue file already exists for this project. Overwrite?");
                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            } else {
                try {
                    Files.createFile(new File(projectQueuePath).toPath());
                    createNew = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(projectQueuePath);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(queue);
                objectOutputStream.flush();
                objectOutputStream.close();
                savedSucessfully = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            String dialogMessage = createNew ? "Queue saved successfully." : "Queue updated successfully.";
            if (savedSucessfully) {
                JOptionPane.showMessageDialog(null, dialogMessage);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to serialize queue to file.");
            }
        });
        queuePanel.add(serializeQueue);

        leftPanel.add(queuePanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);
        // endregion

        // region Right Panel
        TrimInterface trimInterface = new TrimInterface(
                videoFiles.get(0),
                queue,
                project.getConversions().get(0)
        );
        splitPane.setRightComponent(trimInterface);
        // endregion

        add(splitPane);
        this.setVisible(true);
    }
    // endregion

    // region Navigation Methods
    private int currentFileIndex = 0;

    private void showNextFile() {
        if (videoFiles.isEmpty()) return;
        currentFileIndex = (currentFileIndex + 1) % videoFiles.size();
        File nextFile = videoFiles.get(currentFileIndex);
        splitPane.setRightComponent(new TrimInterface(nextFile, queue, conversionMap.get(nextFile)));
    }

    private void showPreviousFile() {
        if (videoFiles.isEmpty()) return;
        currentFileIndex = (currentFileIndex - 1 + videoFiles.size()) % videoFiles.size();
        File prevFile = videoFiles.get(currentFileIndex);
        splitPane.setRightComponent(new TrimInterface(prevFile, queue, conversionMap.get(prevFile)));
    }
    // endregion

    // region Queue Methods
    public void updateQueue(Project project) {
        String projectQueuePath = Util.getProjectQueuePath(project);
        if (Files.exists(new File(projectQueuePath).toPath())) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(projectQueuePath);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                queue = (TaskQueue) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    // endregion

    void initExport() {
    }

    // region TrimInterface Class
    class TrimInterface extends JPanel {

        JLabel title, note, linkedConversionLbl;
        TrimPanel trimmer;
        JCheckBox useNoteAsNewFilename, appendToAttachedFiles;
        JButton addToQueue, exportNow;

        public TrimInterface(File conversionFile, TaskQueue queue, Conversion linkedConversion) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            // region Header Panel
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            title = new JLabel("Trimming File " + conversionFile.getName());
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
            note = new JLabel("Note:" + linkedConversion.note);
            note.setFont(new Font("Arial", Font.ITALIC, 14));
            linkedConversionLbl = new JLabel("Linked Conversion: " + linkedConversion.name);
            linkedConversionLbl.setFont(new Font("Arial", Font.PLAIN, 14));
            headerPanel.add(title);
            headerPanel.add(note);
            headerPanel.add(linkedConversionLbl);
            add(headerPanel);
            // endregion

            // region Trimmer Panel
            trimmer = new TrimPanel(conversionFile, queue);
            add(trimmer);
            // endregion

            // region Options Panel
            JPanel optionsPanel = new JPanel();
            optionsPanel.setBorder(BorderFactory.createTitledBorder("Export Options"));
            useNoteAsNewFilename = new JCheckBox("Use note as new filename");
            useNoteAsNewFilename.addActionListener(e -> {
                TrimWindow.useNoteAsNewFilename = useNoteAsNewFilename.isSelected();
            });
            optionsPanel.add(useNoteAsNewFilename);
            appendToAttachedFiles = new JCheckBox("Append to attached files");
            appendToAttachedFiles.addActionListener(e -> {
                TrimWindow.appendToAttachedFiles = appendToAttachedFiles.isSelected();
            });
            optionsPanel.add(appendToAttachedFiles);
            add(optionsPanel);
            // endregion

            // region Action Bar
            JPanel actionBar = new JPanel();
            actionBar.setLayout(new FlowLayout(FlowLayout.LEFT));
            actionBar.setBorder(BorderFactory.createTitledBorder("Actions"));
            addToQueue = new JButton("Add to Queue");
            addToQueue.addActionListener(e -> {
                queue.addTask(trimmer.getTask(linkedConversion));
            });
            actionBar.add(addToQueue);
            exportNow = new JButton("Export Now");
            exportNow.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                // Set the current directory to the last used directory
                String lastDir = Preferences.getInstance().getLastUsedDirectory();
                if (lastDir != null && new File(lastDir).exists()) {
                    chooser.setCurrentDirectory(new File(lastDir));
                }
                
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File exportLoc = chooser.getSelectedFile();
                    
                    // Save the selected directory as the last used directory
                    Preferences.getInstance().setLastUsedDirectory(exportLoc.getAbsolutePath());
                    
                    trimmer.getTask(linkedConversion).export(exportLoc.toPath(),null); // ! MAKE PROG. LIST. NOT NULL WHEN RE-ENABLING EXPORT NOW
                }
            });
            actionBar.add(exportNow);
            exportNow.setEnabled(false); // TEMP UNTIL FIXED PROG. LIST. PARAM
            add(actionBar);
            // endregion
        }
    }
    // endregion
}
