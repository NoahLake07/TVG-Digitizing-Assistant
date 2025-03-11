package com.thevideogoat.digitizingassistant.util;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
     * Exports all tasks concurrently, while displaying a modal progress dialog that updates live.
     */
    public void exportAll() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks to export.");
            return;
        }

        // Total number of export tasks to process.
        final int totalTasks = tasks.size();
        final AtomicInteger completedCount = new AtomicInteger(0);
        inProgress = true;

        // Create and configure a progress dialog with a JProgressBar.
        JDialog progressDialog = new JDialog((Frame) null, "Export Progress", true);
        JProgressBar progressBar = new JProgressBar(0, totalTasks);
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

        // For each task, submit a Runnable that performs the export and then updates the progress.
        for (ExportTask task : tasks) {
            executor.submit(() -> {
                // Run the export logic (this call is blocking until the export is complete)
                task.export(exportLoc);

                // Update our count of completed tasks
                int count = completedCount.incrementAndGet();
                // Update the progress bar on the EDT.
                SwingUtilities.invokeLater(() -> progressBar.setValue(count));
                latch.countDown();
            });
        }

        // Start a thread to wait for all exports to complete. Once done, dispose of the progress dialog.
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
        // Optionally update the completedTasks counter if needed elsewhere.
        completedTasks += totalTasks;
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();

        sb.append("Task Queue:\n");
        for(ExportTask task : tasks){
            sb.append("\t" + task.toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * Returns the overall progress as a fraction. (This implementation is simple
     * because the live progress is shown in the dialog; you might add more fine-grained
     * tracking if you wish.)
     */
    public double getProgress() {
        // This method can be adapted if you maintain per-task progress info.
        if (tasks.isEmpty()) {
            return 1.0;
        }
        return (double) completedTasks / (completedTasks + tasks.size());
    }
}