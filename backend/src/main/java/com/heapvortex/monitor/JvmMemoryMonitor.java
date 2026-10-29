package com.heapvortex.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.springframework.stereotype.Component;

@Component
public class JvmMemoryMonitor {

    private final MemoryMXBean memoryMXBean =
            ManagementFactory.getMemoryMXBean();

    public MemoryUsage getHeapMemoryUsage() {
        return memoryMXBean.getHeapMemoryUsage();
    }

    public long getHeapUsed() {
        return getHeapMemoryUsage().getUsed();
    }

    public long getHeapCommitted() {
        return getHeapMemoryUsage().getCommitted();
    }

    public long getHeapMax() {
        return getHeapMemoryUsage().getMax();
    }

    public double getHeapUsagePercentage() {
        MemoryUsage usage = getHeapMemoryUsage();

        long used = usage.getUsed();
        long max = usage.getMax();

        if (max <= 0) {
            return 0.0;
        }

        return (used * 100.0) / max;
    }
}