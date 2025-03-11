package com.thevideogoat.digitizingassistant.util;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SimpleNLE {

    public static boolean debug = false;
    public static boolean doDialog = true;

    public static boolean trimVideo(double start, double end, String inputPath, String outputPath) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath);
        FFmpegFrameRecorder recorder = null;

        try {
            grabber.start();

            recorder = new FFmpegFrameRecorder(outputPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder.setFormat(grabber.getFormat());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoCodec(grabber.getVideoCodec());
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioCodec(grabber.getAudioCodec());
            recorder.start();

            grabber.setTimestamp((long) start * 1_000_000);

            JProgressBar progressBar = null;
            if (doDialog) {
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Render Progress");
                progressBar = new JProgressBar();
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                progressBar.setString("Preparing...");
                progressDialog.add(progressBar);
                progressDialog.setVisible(true);
                progressDialog.setSize(200, 100);
            }

            Frame f;
            double currentTimestamp, endTimestampInMs = end * 1_000_000;
            while ((f = grabber.grab()) != null && (currentTimestamp = grabber.getTimestamp()) <= endTimestampInMs) {
                recorder.record(f);
                if (doDialog) {
                    progressBar.setValue((int) ((currentTimestamp / endTimestampInMs) * 100));
                    progressBar.setString(String.format("%.2f", (currentTimestamp / endTimestampInMs) * 100) + "% COMPLETE");
                }
            }

            progressBar.setValue(100);
            progressBar.setString("100% COMPLETE");

            if (debug) System.out.println("Video trimming completed successfully.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean joinVideos(String inputPath1, String inputPath2, String outputPath) {
        FFmpegFrameGrabber grabber1 = new FFmpegFrameGrabber(inputPath1), grabber2 = null;
        FFmpegFrameRecorder recorder = null;
        JProgressBar progressBar = null;

        try {
            grabber1.start();

            recorder = new FFmpegFrameRecorder(outputPath, grabber1.getImageWidth(), grabber1.getImageHeight(), grabber1.getAudioChannels());
            recorder.setFormat(grabber1.getFormat());
            recorder.setFrameRate(grabber1.getFrameRate());
            recorder.setVideoCodec(grabber1.getVideoCodec());
            recorder.setVideoBitrate(grabber1.getVideoBitrate());
            recorder.setSampleRate(grabber1.getSampleRate());
            recorder.setAudioCodec(grabber1.getAudioCodec());
            recorder.start();

            if (doDialog) {
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Join Progress");
                progressBar = new JProgressBar();
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                progressDialog.add(progressBar);
                progressDialog.setSize(300, 100);
                progressDialog.setVisible(true);
            }

            Frame f;

            long totalLength1 = grabber1.getLengthInTime();
            while ((f = grabber1.grab()) != null) {
                recorder.record(f);
                if (doDialog) {
                    double currentProgress = (double) grabber1.getTimestamp() / totalLength1;
                    progressBar.setValue((int) (currentProgress * 50));
                    progressBar.setString(String.format("Joining First Video: %.2f%% COMPLETE", currentProgress * 50));
                }
            }

            grabber2 = new FFmpegFrameGrabber(inputPath2);
            grabber2.start();
            long totalLength2 = grabber2.getLengthInTime();
            while ((f = grabber2.grab()) != null) {
                recorder.record(f);
                if (doDialog) {
                    double currentProgress = (double) grabber2.getTimestamp() / totalLength2;
                    progressBar.setValue(50 + (int) (currentProgress * 50));
                    progressBar.setString(String.format("Joining Second Video: %.2f%% COMPLETE", 50 + currentProgress * 50));
                }
            }

            if (doDialog) {
                progressBar.setValue(100);
                progressBar.setString("100% COMPLETE");
            }

            if (debug) System.out.println("Video joining completed successfully.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        } finally {
            try {
                if (grabber1 != null) {
                    grabber1.stop();
                    grabber1.release();
                }
                if (grabber2 != null) {
                    grabber2.stop();
                    grabber2.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setDebug(boolean debug) {
        SimpleNLE.debug = debug;
    }

    public static String getVideoMetadata(String inputPath) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath);
        try {
            grabber.start();
            StringBuilder metadata = new StringBuilder();
            metadata.append("Format: ").append(grabber.getFormat()).append("\n");
            metadata.append("Duration (ms): ").append(grabber.getLengthInTime()).append("\n");
            metadata.append("Frame Rate: ").append(grabber.getFrameRate()).append("\n");
            metadata.append("Width: ").append(grabber.getImageWidth()).append("\n");
            metadata.append("Height: ").append(grabber.getImageHeight()).append("\n");
            metadata.append("Audio Channels: ").append(grabber.getAudioChannels()).append("\n");
            return metadata.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean validateInputFile(String inputPath) {
        File file = new File(inputPath);
        if (!file.exists() || file.isDirectory()) {
            if (debug) System.out.println("Input file does not exist or is a directory: " + inputPath);
            return false;
        }
        return true;
    }

    public static boolean deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            if (debug) System.out.println("Deleted file: " + filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean changeVideoResolution(String inputPath, String outputPath, int newWidth, int newHeight) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath);
        FFmpegFrameRecorder recorder = null;

        try {
            grabber.start();
            recorder = new FFmpegFrameRecorder(outputPath, newWidth, newHeight, grabber.getAudioChannels());
            recorder.setFormat(grabber.getFormat());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoCodec(grabber.getVideoCodec());
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioCodec(grabber.getAudioCodec());
            recorder.start();

            Frame f;
            while ((f = grabber.grab()) != null) {
                recorder.record(f);
            }

            if (debug) System.out.println("Resolution change completed successfully.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
