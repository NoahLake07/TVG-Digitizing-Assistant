package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.Conversion;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

public class ExportTask implements Serializable {

    private String exportFileName;
    private String originalFilePath;
    private double timeStart, timeEnd; // the actual start/stop times of the trim (in seconds)
    private LocalDateTime timeOfExport; // time of export start (log purposes only)
    private Conversion linkedConversion;
    private boolean failed;

    public ExportTask(String exportFileName, String originalFilePath, double timeStart, double timeEnd, Conversion linkedConversion) {
        this.exportFileName = exportFileName;
        this.originalFilePath = originalFilePath;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.linkedConversion = linkedConversion;
        this.failed = false;
    }

    public String getExportFileName() {
        return exportFileName;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public double getTimeStart() {
        return timeStart;
    }

    public double getTimeEnd(){
        return timeEnd;
    }

    public Conversion getLinkedConversion() {
        return linkedConversion;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setLinkedConversion(Conversion c) {
        this.linkedConversion = c;
    }

    public boolean addNewFilename(String newFilename) {
        if (newFilename == null || newFilename.isEmpty()) {
            return false;
        }
        this.exportFileName = newFilename;
        return true;
    }

    public String toString(){
        return "ExportTask: " + exportFileName + " (" + timeStart + "s - " + timeEnd + "s) -- " + originalFilePath;
    }

    /**
     * Original export method (for reference).
     */
    public void exportOld(Path exportDir) {
        timeOfExport = LocalDateTime.now();
        File inputFile = new File(originalFilePath);
        File outputFile = new File(exportDir.toFile(), exportFileName);

        // Ensure the output file has a .mp4 extension
        String lowerName = outputFile.getName().toLowerCase();
        if (!lowerName.endsWith(".mp4")) {
            outputFile = new File(outputFile.getParent(), lowerName + ".mp4");
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int channels = grabber.getAudioChannels();
            double frameRate = grabber.getFrameRate();
            int sampleRate = grabber.getSampleRate();

            int origVideoBitrate = grabber.getVideoBitrate();
            if (origVideoBitrate <= 0) {
                origVideoBitrate = 10_000_000; // fallback
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, channels)) {
                recorder.setFormat("mp4");
                recorder.setVideoCodec(AV_CODEC_ID_H264);
                recorder.setAudioCodec(AV_CODEC_ID_AAC);
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
                recorder.setVideoBitrate(origVideoBitrate);
                recorder.setFrameRate(frameRate);
                recorder.setSampleRate(sampleRate);
                recorder.start();

                long startMicros = (long) (timeStart * 1_000_000);
                grabber.setTimestamp(startMicros);
                long endMicros = (long) (timeEnd * 1_000_000);

                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long t = grabber.getTimestamp();
                    if (t > endMicros) {
                        break;
                    }
                    recorder.setTimestamp(t - startMicros);
                    recorder.record(frame);
                }

                recorder.stop();
            }

            grabber.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            failed = true;
        }
    }

    /**
     * Interface to allow progress updates.
     */
    public interface ProgressListener {
        /**
         * Called with progress value between 0.0 and 1.0.
         */
        void update(double progress);
    }

    /**
     * New two-step export method that first converts the original file into a .ts file, then
     * trims the .ts file into an MP4 using FFmpeg's command-line.
     *
     * @param exportDir The directory into which the trimmed file should be saved.
     * @param progressListener A listener for progress updates (0.0 to 1.0).
     */
    public void export(Path exportDir, ProgressListener progressListener) {
        timeOfExport = LocalDateTime.now();
        File inputFile = new File(originalFilePath);
        // Final output file: ensure .mp4 extension.
        File outputFile = new File(exportDir.toFile(), exportFileName);
        String lowerName = outputFile.getName().toLowerCase();
        if (!lowerName.endsWith(".mp4")) {
            outputFile = new File(outputFile.getParent(), lowerName + ".mp4");
        }
        // Temporary .ts file for intermediate conversion.
        File tsFile = new File(exportDir.toFile(), "temp_output.ts");

        // ----- Stage 1: Convert original file to a .ts file -----
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            // Generate presentation timestamps if missing.
            grabber.setOption("fflags", "genpts");
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int channels = grabber.getAudioChannels();
            double frameRate = grabber.getFrameRate();
            int sampleRate = grabber.getSampleRate();

            int origVideoBitrate = grabber.getVideoBitrate();
            if (origVideoBitrate <= 0) {
                origVideoBitrate = 10_000_000; // fallback bitrate
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tsFile, width, height, channels)) {
                // Use MPEG-TS format which tolerates irregular timestamps.
                recorder.setFormat("mpegts");
                recorder.setVideoCodec(AV_CODEC_ID_H264);
                recorder.setAudioCodec(AV_CODEC_ID_AAC);
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
                recorder.setVideoBitrate(origVideoBitrate);
                recorder.setFrameRate(frameRate);
                recorder.setSampleRate(sampleRate);
                recorder.start();

                // Total duration of input (microseconds)
                long totalDuration = grabber.getLengthInTime();
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    recorder.record(frame);
                    if (progressListener != null && totalDuration > 0) {
                        // Conversion stage: update progress from 0% to 50%
                        double convProgress = (double) grabber.getTimestamp() / totalDuration;
                        progressListener.update(convProgress * 0.5);
                    }
                }

                recorder.stop();
            }
            grabber.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            failed = true;
            return;
        }

        // ----- Stage 2: Trim the .ts file into final MP4 using FFmpeg command-line -----
        double duration = timeEnd - timeStart;  // duration in seconds
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y"); // Overwrite output if exists.
        command.add("-i");
        command.add(tsFile.getAbsolutePath());
        command.add("-ss");
        command.add(String.valueOf(timeStart));  // Start time (seconds)
        command.add("-t");
        command.add(String.valueOf(duration));     // Duration (seconds)
        command.add("-c");
        command.add("copy");  // Use stream copy to avoid re-encoding.
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process process = pb.start();
            // (Optional) You could parse process.getInputStream() for detailed progress updates.
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg trimming process failed with exit code " + exitCode);
                failed = true;
            }
            // Mark progress as complete (100%)
            if (progressListener != null) {
                progressListener.update(1.0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            failed = true;
            return;
        }

        // Clean up: Delete temporary .ts file
        if (tsFile.exists()) {
            tsFile.delete();
        }
    }
}
