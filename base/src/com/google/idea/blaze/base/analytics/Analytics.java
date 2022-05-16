package com.google.idea.blaze.base.analytics;

import java.time.Duration;

public interface Analytics {
    /**
     * Initialise the analytics implementation, e.g. find the relevant endpoints, open sockets, etc
     */
    void setup();

    /**
     * Track the duration of running a project sync
     * This call is asynchronous and should not execute the actual work on the calling thread
     * @param duration
     */
    void trackSyncTime(Duration duration);

    /**
     * Flush all events synchronously
     */
    void flush();

    /**
     * Close all underlying resources, e.g. sockets
     */
    void close();
}
