package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.ui.TrimWindow;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

public class TrimPanel extends JPanel {

    // STATIC TRACKING VARS
    static double progress = 0.00d;
    static int completedTasks = 0;
    static boolean debug = false;

    //  SLIDER SETTINGS
    private static final double SLIDER_INCREMENT = 0.1;       // 0.1s per slider step
    private static final double LABEL_STEP_SECONDS = 1.0;     // label every 1s

    //  PREVIEW SETTINGS
    private static final int PREVIEW_WIDTH  = 480;
    private static final int PREVIEW_HEIGHT = 270;
    private static final int DECODE_WIDTH   = 320;  // downscale decode
    private static final int DECODE_HEIGHT  = 180;

    //  FIELDS
    private final File mediaFile;
    private double totalDuration;  // in seconds

    // Sliders
    private JSlider startSlider;
    private JSlider endSlider;

    // UI labels
    private JLabel startLabel;
    private JLabel endLabel;
    private JLabel startPreview;
    private JLabel endPreview;
    private JButton exportButton;

    // Background single grabber
    private FFmpegFrameGrabber sharedGrabber;
    private final Object grabberLock = new Object();

    // SwingWorkers
    private SwingWorker<BufferedImage, Void> startPreviewWorker;
    private SwingWorker<BufferedImage, Void> endPreviewWorker;

    TaskQueue queue;

    public TrimPanel(File mediaFile, TaskQueue queue) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));

        this.mediaFile = mediaFile;
        this.totalDuration = fetchMediaDuration(mediaFile);

        this.queue = queue;

        if (this.totalDuration <= 0.001) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not read a valid duration from the media file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            this.totalDuration = 60; // fallback
        }

        // Initialize the shared grabber
        try {
            initSharedGrabber();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error initializing grabber:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        initComponents();
        layoutComponents();
        registerListeners();
    }

    private void onExit(){
        closeSharedGrabber();
    }

    /**
     * Use a temporary grabber to read the total duration in seconds
     */
    private double fetchMediaDuration(File file) {
        double durationSec = -1;
        try (FFmpegFrameGrabber temp = new FFmpegFrameGrabber(file)) {
            temp.start();
            long lengthInMicroseconds = temp.getLengthInTime();
            durationSec = lengthInMicroseconds / 1_000_000.0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return durationSec;
    }

    /**
     * Initialize a SINGLE grabber for the entire session,
     * set downscaled decode resolution, then start it.
     */
    private void initSharedGrabber() throws Exception {
        sharedGrabber = new FFmpegFrameGrabber(mediaFile);
        // Downscale
        sharedGrabber.setImageWidth(DECODE_WIDTH);
        sharedGrabber.setImageHeight(DECODE_HEIGHT);
        sharedGrabber.start();
    }

    /**
     * Create UI components
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.WHITE);

        int maxSliderVal = (int) Math.ceil(totalDuration / SLIDER_INCREMENT);

        startSlider = new JSlider(0, maxSliderVal, 0);
        endSlider   = new JSlider(0, maxSliderVal, maxSliderVal);

        configureSlider(startSlider, maxSliderVal);
        configureSlider(endSlider, maxSliderVal);

        startLabel = new JLabel("Start: 0.0s");
        endLabel   = new JLabel("End: " + (maxSliderVal * SLIDER_INCREMENT) + "s");

        startPreview = createPreviewLabel("Start Preview");
        endPreview   = createPreviewLabel("End Preview");

        exportButton = new JButton("Export Trimmed Media");
    }

    private void configureSlider(JSlider slider, int maxValue) {
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);

        slider.setPreferredSize(new Dimension(600, 50));

        int sliderStepsPerSecond = (int) Math.round(1.0 / SLIDER_INCREMENT);
        slider.setMajorTickSpacing(sliderStepsPerSecond);
        slider.setMinorTickSpacing(Math.max(1, sliderStepsPerSecond / 2));

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        double totalSeconds = maxValue * SLIDER_INCREMENT;

        for (double sec = 0.0; sec <= totalSeconds + 0.0001; sec += LABEL_STEP_SECONDS) {
            int sliderVal = (int) Math.round(sec / SLIDER_INCREMENT);
            if (sliderVal <= maxValue) {
                labelTable.put(sliderVal, new JLabel(String.format("%.1fs", sec)));
            }
        }
        double remainder = totalSeconds % LABEL_STEP_SECONDS;
        if (remainder > 0.0001) {
            labelTable.put(maxValue, new JLabel(String.format("%.1fs", totalSeconds)));
        }

        slider.setLabelTable(labelTable);
    }

    private JLabel createPreviewLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return label;
    }

    private void layoutComponents() {
        JPanel slidersPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Start row
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        startPanel.add(new JLabel("Trim Start:"));
        startPanel.add(startSlider);
        startPanel.add(startLabel);

        // End row
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        endPanel.add(new JLabel("Trim End:"));
        endPanel.add(endSlider);
        endPanel.add(endLabel);

        slidersPanel.add(startPanel);
        slidersPanel.add(endPanel);

        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        previewPanel.add(startPreview);
        previewPanel.add(endPreview);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(exportButton);

        add(slidersPanel, BorderLayout.NORTH);
        add(previewPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void registerListeners() {
        ChangeListener startListener = e -> {
            if (startSlider.getValue() > endSlider.getValue()) {
                startSlider.setValue(endSlider.getValue());
            }
            updateStartControls();
        };
        startSlider.addChangeListener(startListener);

        ChangeListener endListener = e -> {
            if (endSlider.getValue() < startSlider.getValue()) {
                endSlider.setValue(startSlider.getValue());
            }
            updateEndControls();
        };
        endSlider.addChangeListener(endListener);

        // Initialize
        updateStartControls();
        updateEndControls();

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTrimmed();
            }
        });
    }

    private void updateStartControls() {
        int sliderVal = startSlider.getValue();
        double startSec = sliderVal * SLIDER_INCREMENT;
        startLabel.setText(String.format("Start: %.1fs", startSec));
        updatePreviewAsync(startPreview, startSec, true);
    }

    private void updateEndControls() {
        int sliderVal = endSlider.getValue();
        double endSec = sliderVal * SLIDER_INCREMENT;
        endLabel.setText(String.format("End: %.1fs", endSec));
        updatePreviewAsync(endPreview, endSec, false);
    }

    /**
     * We re-use the single sharedGrabber.
     * So each preview request just does a "seek + grabImage" on that instance.
     */
    private void updatePreviewAsync(JLabel previewLabel, double second, boolean isStart) {
        // Cancel any old worker for that label
        if (isStart) {
            if (startPreviewWorker != null && !startPreviewWorker.isDone()) {
                startPreviewWorker.cancel(true);
            }
        } else {
            if (endPreviewWorker != null && !endPreviewWorker.isDone()) {
                endPreviewWorker.cancel(true);
            }
        }

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                if (isCancelled()) return null;
                if (second < 0 || second > totalDuration) {
                    return null;
                }
                // Use the shared grabber
                return grabSharedFrame(second);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    BufferedImage frame = get();
                    if (frame != null) {
                        Image scaled = frame.getScaledInstance(
                                PREVIEW_WIDTH, PREVIEW_HEIGHT, Image.SCALE_SMOOTH
                        );
                        previewLabel.setIcon(new ImageIcon(scaled));
                        previewLabel.setText("");
                    } else {
                        previewLabel.setIcon(null);
                        previewLabel.setText("No Frame");
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                    previewLabel.setIcon(null);
                    previewLabel.setText("Err grabbing frame");
                }
            }
        };

        if (isStart) {
            startPreviewWorker = worker;
        } else {
            endPreviewWorker = worker;
        }
        worker.execute();
    }

    /**
     * Actual code to do "seek + grab" from the single sharedGrabber.
     * We must synchronize so only one thread uses the grabber at a time.
     */
    private BufferedImage grabSharedFrame(double second) {
        synchronized (grabberLock) {
            try {
                // Seek
                long targetMicros = (long) (second * 1_000_000);
                sharedGrabber.setTimestamp(targetMicros);

                // Attempt to grab an image
                for (int i = 0; i < 5; i++) {
                    Frame frame = sharedGrabber.grabImage();
                    if (frame != null) {
                        Java2DFrameConverter converter = new Java2DFrameConverter();
                        return converter.getBufferedImage(frame);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Export the trimmed portion at full resolution.
     */
    private void exportTrimmed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Trimmed Media");
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // user canceled
        }

        File outFile = fileChooser.getSelectedFile();
        if (outFile == null) {
            return;
        }

        double startSec = startSlider.getValue() * SLIDER_INCREMENT;
        double endSec   = endSlider.getValue() * SLIDER_INCREMENT;

        if (startSec >= endSec) {
            JOptionPane.showMessageDialog(this,
                    "End time must be greater than start time!",
                    "Invalid Range",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            trimMedia(mediaFile, outFile, startSec, endSec);
            JOptionPane.showMessageDialog(this,
                    "Export complete!\nSaved to: " + outFile.getAbsolutePath(),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error exporting media:\n" + ex.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Trim the input file from startSec to endSec, preserving A/V sync.
     * We do not use the sharedGrabber for trimming, so we don't mess up the preview state.
     * Instead, we create a new local grabber for the full-res export.
     */
    private static void trimMedia(File inputFile, File outputFile, double startSec, double endSec) throws Exception {
        // Make sure the outputFile has .mp4 extension
        String fileName = outputFile.getName().toLowerCase();
        if (!fileName.endsWith(".mp4")) {
            outputFile = new File(outputFile.getParent(), fileName + ".mp4");
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            // 1) Start the grabber so we can read metadata
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int channels = grabber.getAudioChannels();
            double frameRate = grabber.getFrameRate();
            int sampleRate = grabber.getSampleRate();

            // Try to get the original video bitrate
            int origVideoBitrate = grabber.getVideoBitrate();  // bits/sec
            // If the grabber doesn’t report a valid bitrate, pick a fallback
            if (origVideoBitrate <= 0) {
                origVideoBitrate = 10_000_000; // e.g., 10 Mbps fallback
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, channels)) {
                recorder.setFormat("mp4");
                recorder.setVideoCodec(AV_CODEC_ID_H264);
                recorder.setAudioCodec(AV_CODEC_ID_AAC);

                // Force pixel format if desired
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);

                // Use the *original* video bitrate from the input
                recorder.setVideoBitrate(origVideoBitrate);

                // Retain original frame rate, sample rate
                recorder.setFrameRate(frameRate);
                recorder.setSampleRate(sampleRate);

                recorder.start();

                // 2) Seek the grabber to startSec
                long startMicros = (long) (startSec * 1_000_000);
                grabber.setTimestamp(startMicros);
                long endMicros = (long) (endSec * 1_000_000);
                long totalMicro = endMicros - startMicros;

                // 3) Read frames until endSec
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long t = grabber.getTimestamp();
                    long deltaMicro = t - startMicros;
                    progress = ((double) deltaMicro / (double) totalMicro);
                    if (t % 30 == 0 && deltaMicro > 0 && debug) System.out.println("EXPORT PROGRESS:\t" + String.format("%.2f", progress*100) + "%");
                    if (t > endMicros) {
                        progress = 1.00;
                        completedTasks++;
                        if (t % 30 == 0 && deltaMicro > 0 && debug) System.out.println("EXPORT PROGRESS:\t" + String.format("%.2f", progress*100) + "%");
                        break;
                    }
                    // Adjust so new file timestamps start at 0
                    recorder.setTimestamp(t - startMicros);
                    recorder.record(frame);
                }

                recorder.stop();
            }

            grabber.stop();
        }
    }

    private static void trimMedia(File inputFile, Path outputDir, String newFilename, double startSec, double endSec) throws Exception {
        if (!newFilename.endsWith(".mp4")) {
            newFilename = newFilename + ".mp4";
        }

        File outputFile = outputDir.resolve(newFilename).toFile();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            // 1) Start the grabber so we can read metadata
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int channels = grabber.getAudioChannels();
            double frameRate = grabber.getFrameRate();
            int sampleRate = grabber.getSampleRate();

            // Try to get the original video bitrate
            int origVideoBitrate = grabber.getVideoBitrate();  // bits/sec
            // If the grabber doesn’t report a valid bitrate, pick a fallback
            if (origVideoBitrate <= 0) {
                origVideoBitrate = 10_000_000; // e.g., 10 Mbps fallback
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, channels)) {
                recorder.setFormat("mp4");
                recorder.setVideoCodec(AV_CODEC_ID_H264);
                recorder.setAudioCodec(AV_CODEC_ID_AAC);

                // Force pixel format if desired
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);

                // Use the *original* video bitrate from the input
                recorder.setVideoBitrate(origVideoBitrate);

                // Retain original frame rate, sample rate
                recorder.setFrameRate(frameRate);
                recorder.setSampleRate(sampleRate);

                recorder.start();

                // 2) Seek the grabber to startSec
                long startMicros = (long) (startSec * 1_000_000);
                grabber.setTimestamp(startMicros);
                long endMicros = (long) (endSec * 1_000_000);
                long totalMicro = endMicros - startMicros;

                // 3) Read frames until endSec
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long t = grabber.getTimestamp();
                    long deltaMicro = t - startMicros;
                    progress = ((double) deltaMicro / (double) totalMicro);
                    if (t % 30 == 0 && deltaMicro > 0 && debug) System.out.println("EXPORT PROGRESS:\t" + String.format("%.2f", progress*100) + "%");
                    if (t > endMicros) {
                        progress = 1.00;
                        completedTasks++;
                        if (t % 30 == 0 && deltaMicro > 0 && debug) System.out.println("EXPORT PROGRESS:\t" + String.format("%.2f", progress*100) + "%");
                        break;
                    }
                    // Adjust so new file timestamps start at 0
                    recorder.setTimestamp(t - startMicros);
                    recorder.record(frame);
                }

                recorder.stop();
            }

            grabber.stop();
        }
    }

    public static int getCompletedTasks(){
        return completedTasks;
    }

    public void resetCompletedTasks(){
        completedTasks = 0;
    }

    public static double getProgress(){
        return progress;
    }

    public double getTrimStartInSeconds() {
        return startSlider.getValue() * SLIDER_INCREMENT;
    }

    public double getTrimEndInSeconds() {
        return endSlider.getValue() * SLIDER_INCREMENT;
    }

    public void closeSharedGrabber() {
        if (sharedGrabber != null) {
            try {
                sharedGrabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ExportTask getTask(Conversion linkedConversion){
        String exportFileName;
        String originalFilePath = mediaFile.getAbsolutePath();

        double timeStart = getTrimStartInSeconds();
        double timeEnd = getTrimEndInSeconds();

        // add modifiers to the export task parameters (i.e. filename settings)
        if(TrimWindow.useNoteAsNewFilename){
            exportFileName = linkedConversion.note;
        } else {
            exportFileName = mediaFile.getName();
        }

        ExportTask task = new ExportTask(exportFileName, originalFilePath, timeStart, timeEnd, linkedConversion);
        return task;
    }
}