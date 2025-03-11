package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.Conversion;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;

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
     * Exports (trims) the original file to the specified time range.
     *
     * @param exportDir The directory into which the trimmed file should be saved.
     */
    public void export(Path exportDir) {
        timeOfExport = LocalDateTime.now();
        // Create input and output file objects
        File inputFile = new File(originalFilePath);
        File outputFile = new File(exportDir.toFile(), exportFileName);

        // Ensure the output file has a .mp4 extension
        String lowerName = outputFile.getName().toLowerCase();
        if (!lowerName.endsWith(".mp4")) {
            outputFile = new File(outputFile.getParent(), lowerName + ".mp4");
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            // 1) Start the grabber to read metadata and frames
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int channels = grabber.getAudioChannels();
            double frameRate = grabber.getFrameRate();
            int sampleRate = grabber.getSampleRate();

            // Get the original video bitrate; if unavailable, use a fallback
            int origVideoBitrate = grabber.getVideoBitrate();
            if (origVideoBitrate <= 0) {
                origVideoBitrate = 10_000_000; // e.g., 10 Mbps fallback
            }

            // 2) Set up the recorder for the trimmed output
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, channels)) {
                recorder.setFormat("mp4");
                recorder.setVideoCodec(AV_CODEC_ID_H264);
                recorder.setAudioCodec(AV_CODEC_ID_AAC);
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
                recorder.setVideoBitrate(origVideoBitrate);
                recorder.setFrameRate(frameRate);
                recorder.setSampleRate(sampleRate);
                recorder.start();

                // 3) Seek to the starting point (in microseconds)
                long startMicros = (long) (timeStart * 1_000_000);
                grabber.setTimestamp(startMicros);
                long endMicros = (long) (timeEnd * 1_000_000);

                // 4) Read and record frames until the end time is reached
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long t = grabber.getTimestamp();
                    if (t > endMicros) {
                        break;
                    }
                    // Adjust timestamp so that the new file starts at 0
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
}
