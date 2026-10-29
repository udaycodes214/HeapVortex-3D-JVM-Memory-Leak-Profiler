package com.heapvortex.simulator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoryLeakSimulator {

    private final List<byte[]> leakedMemory = new ArrayList<>();

    private volatile boolean running = false;

    private Thread leakThread;

    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB

    private static final int MAX_CHUNKS = 500; // 500 MB maximum

    public synchronized void start() {

        // Already running
        if (running) {
            return;
        }

        // Start the simulator
        running = true;

        leakThread = new Thread(() -> {

            System.out.println(
                    "HeapVortex Leak Simulator STARTED"
            );

            while (running) {

                try {

                    // Prevent allocation beyond the limit
                    if (leakedMemory.size() >= MAX_CHUNKS) {

                        System.out.println(
                                "Leak Simulator reached "
                                        + MAX_CHUNKS
                                        + " MB limit."
                        );

                        break;
                    }

                    // Allocate 1 MB
                    leakedMemory.add(
                            new byte[CHUNK_SIZE]
                    );

                    System.out.println(
                            "Leak Simulator: "
                                    + leakedMemory.size()
                                    + " MB retained"
                    );

                    // Wait 1 second
                    Thread.sleep(1000);

                } catch (InterruptedException e) {

                    System.out.println(
                            "Leak Simulator thread interrupted."
                    );

                    Thread.currentThread().interrupt();

                    break;

                } catch (OutOfMemoryError e) {

                    System.out.println(
                            "Leak Simulator stopped: "
                                    + "JVM is out of memory."
                    );

                    break;
                }
            }

            // Thread finished
            running = false;

            System.out.println(
                    "HeapVortex Leak Simulator STOPPED"
            );

        }, "heapvortex-leak-simulator");

        leakThread.setDaemon(true);

        leakThread.start();
    }

    public synchronized void stop() {

        if (!running) {
            return;
        }

        System.out.println(
                "Stopping HeapVortex Leak Simulator..."
        );

        running = false;

        if (leakThread != null) {

            leakThread.interrupt();

            leakThread = null;
        }
    }

    public synchronized void clear() {

        System.out.println(
                "Clearing simulated leaked memory..."
        );

        running = false;

        if (leakThread != null) {

            leakThread.interrupt();

            leakThread = null;
        }

        leakedMemory.clear();

        System.gc();

        System.out.println(
                "Leak Simulator memory cleared."
        );
    }

    public synchronized int getRetainedMB() {

        return leakedMemory.size();
    }

    public boolean isRunning() {

        return running;
    }
}