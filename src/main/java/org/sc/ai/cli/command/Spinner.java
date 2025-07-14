package org.sc.ai.cli.command;

import java.io.PrintWriter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple terminal spinner for indicating background processing.
 * The spinner only starts after a delay to avoid flashing for quick operations.
 * 
 * @author Julius Krah
 */
public class Spinner {
    private static final String[] SPINNER_CHARS = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int SPINNER_DELAY = 80; // milliseconds
    private static final int START_DELAY = 100; // milliseconds before spinner starts
    
    private final PrintWriter writer;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean shouldStart = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> spinnerTask;
    private ScheduledFuture<?> startTask;
    private final String message;
    private volatile boolean hasStarted = false;
    
    public Spinner(PrintWriter writer, String message) {
        this.writer = writer;
        this.message = message != null ? message : "Processing";
    }
    
    /**
     * Start the spinner animation after a delay.
     */
    public void start() {
        if (shouldStart.compareAndSet(false, true)) {
            scheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "spinner-thread");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule the spinner to start after a delay
            startTask = scheduler.schedule(() -> {
                if (shouldStart.get() && isRunning.compareAndSet(false, true)) {
                    hasStarted = true;
                    startSpinnerAnimation();
                }
            }, START_DELAY, TimeUnit.MILLISECONDS);
        }
    }
    
    private void startSpinnerAnimation() {
        spinnerTask = scheduler.scheduleAtFixedRate(() -> {
            if (isRunning.get()) {
                String spinnerChar = SPINNER_CHARS[(int) (System.currentTimeMillis() / SPINNER_DELAY) % SPINNER_CHARS.length];
                writer.print("\r" + spinnerChar + " " + message);
                writer.flush();
            }
        }, 0, SPINNER_DELAY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stop the spinner animation.
     */
    public void stop() {
        shouldStart.set(false);
        
        if (startTask != null) {
            startTask.cancel(true);
        }
        
        if (isRunning.compareAndSet(true, false)) {
            if (spinnerTask != null) {
                spinnerTask.cancel(true);
            }
            
            if (hasStarted) {
                clearLine();
            }
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException _) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void clearLine() {
        // Move cursor to beginning of line and clear it
        writer.print("\r");
        // Write spaces to overwrite the spinner text
        writer.print(" ".repeat(message.length() + 10)); // Extra spaces to ensure clearing
        // Move cursor back to beginning
        writer.print("\r");
        writer.flush();
    }
    
    /**
     * Check if the spinner is currently running.
     * 
     * @return true if the spinner is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Check if the spinner should start (may not have started yet due to delay).
     * 
     * @return true if the spinner should start, false otherwise
     */
    public boolean shouldStart() {
        return shouldStart.get();
    }
}
