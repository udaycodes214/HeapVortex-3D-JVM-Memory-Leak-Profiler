package com.heapvortex.telemetry;

import com.heapvortex.monitor.JvmMemoryMonitor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class MemoryTelemetryService {

    private final JvmMemoryMonitor memoryMonitor;

    private final Deque<MemorySample> history = new ArrayDeque<>();

    private static final int MAX_SAMPLES = 60;

    public MemoryTelemetryService(JvmMemoryMonitor memoryMonitor) {
        this.memoryMonitor = memoryMonitor;
    }

    @Scheduled(fixedRate = 1000)
    public void collectSample() {

        MemoryUsage usage = memoryMonitor.getHeapMemoryUsage();

        MemorySample sample = new MemorySample(
                Instant.now(),
                usage.getUsed(),
                usage.getCommitted(),
                usage.getMax()
        );

        synchronized (history) {

            history.addLast(sample);

            while (history.size() > MAX_SAMPLES) {
                history.removeFirst();
            }
        }
    }

    public List<MemorySample> getHistory() {

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public record MemorySample(
            Instant timestamp,
            long heapUsed,
            long heapCommitted,
            long heapMax
    ) {
    }
}