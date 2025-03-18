package com.thevideogoat.digitizingassistant.ui;

public interface ProgressListener {
    /**
     * Called with progress value between 0.0 and 1.0.
     */
    void update(double progress);
}
