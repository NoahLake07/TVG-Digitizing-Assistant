package com.thevideogoat.digitizingassistant.util;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskQueue implements Serializable {

    private List<ExportTask> tasks;
    private boolean inProgress;
    private int completedTasks;
    private Path exportLoc;

    public TaskQueue() {
        this.tasks = new ArrayList<>();
        this.inProgress = false;
        this.completedTasks = 0;
    }

    public void addTask(ExportTask task) {
        tasks.add(task);
    }

    public List<ExportTask> getTasks() {
        return tasks;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setExportLoc(Path exportLoc) {
        this.exportLoc = exportLoc;
    }

    /**
     * Exports all tasks concurrently using the new TS‑based export method.
     * Displays a modal progress dialog that aggregates each task's progress.
     */
    public void exportAll() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks to export.");
            return;
        }

        final int totalTasks = tasks.size();
        inProgress = true;

        // This map will hold progress for each task (0.0 to 1.0).
        final Map<ExportTask, Double> taskProgress = new ConcurrentHashMap<>();
        // Initialize each task's progress to 0.0.
        for (ExportTask task : tasks) {
            taskProgress.put(task, 0.0);
        }

        // Create and configure a progress dialog with a JProgressBar.
        JDialog progressDialog = new JDialog((Frame) null, "Export Progress", true);
        JProgressBar progressBar = new JProgressBar(0, 100); // 0 to 100%
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressDialog.getContentPane().add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // Create an ExecutorService to run export tasks concurrently.
        int threadCount = Math.min(totalTasks, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Use a CountDownLatch to know when all tasks have finished.
        CountDownLatch latch = new CountDownLatch(totalTasks);

        // For each task, submit a Runnable that runs exportUsingTS and updates progress.
        for (ExportTask task : tasks) {
            executor.submit(() -> {
                // Each task is given its own ProgressListener that updates the shared taskProgress map.
                ExportTask.ProgressListener listener = progress -> {
                    taskProgress.put(task, progress);
                    // Calculate overall progress as the average of all task progress values.
                    double overall = taskProgress.values().stream().mapToDouble(Double::doubleValue).sum() / totalTasks;
                    // Update progress bar on the EDT.
                    SwingUtilities.invokeLater(() -> progressBar.setValue((int) (overall * 100)));
                };

                // Run the exportUsingTS method (this call blocks until export is complete).
                task.export(exportLoc, listener);
                // Mark this task as complete.
                taskProgress.put(task, 1.0);
                latch.countDown();
            });
        }

        // Start a thread to wait for all exports to complete.
        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            executor.shutdown();
            inProgress = false;
            SwingUtilities.invokeLater(() -> {
                progressDialog.dispose();
                JOptionPane.showMessageDialog(null, "Exported " + totalTasks + " tasks.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            });
        }).start();

        // Display the modal progress dialog. This will block until it is disposed.
        progressDialog.setVisible(true);

        // Clear the task list since they've been processed.
        tasks.clear();
        completedTasks += totalTasks;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Task Queue:\n");
        for(ExportTask task : tasks){
            sb.append("\t").append(task.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the overall progress as a fraction.
     * (This method can be adapted if you maintain per-task progress info.)
     */
    public double getProgress() {
        if (tasks.isEmpty()) {
            return 1.0;
        }
        return (double) completedTasks / (completedTasks + tasks.size());
    }
}
