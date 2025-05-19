package org.simplecommerce.ai.commerce.service;

import java.time.Instant;
import java.util.UUID;

public class ServiceInfo {
    private final String id;
    private final String name;
    private final int port;
    private final Instant startTime;
    private final Process process;
    private boolean isRunning;

    public ServiceInfo(String name, int port, Process process) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.port = port;
        this.process = process;
        this.startTime = Instant.now();
        this.isRunning = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Process getProcess() {
        return process;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
