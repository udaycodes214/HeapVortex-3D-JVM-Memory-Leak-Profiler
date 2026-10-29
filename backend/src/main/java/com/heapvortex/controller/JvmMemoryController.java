package com.heapvortex.controller;

import com.heapvortex.monitor.JvmMemoryMonitor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jvm")
@CrossOrigin(origins = "http://localhost:5173")
public class JvmMemoryController {

    private final JvmMemoryMonitor memoryMonitor;

    public JvmMemoryController(JvmMemoryMonitor memoryMonitor) {
        this.memoryMonitor = memoryMonitor;
    }

    @GetMapping("/memory")
    public Map<String, Object> getMemoryStats() {

       
        MemoryUsage usage = memoryMonitor.getHeapMemoryUsage();

        long used = usage.getUsed();
        long committed = usage.getCommitted();
        long max = usage.getMax();

        double usagePercentage = 0.0;

        if (max > 0) {
            usagePercentage = (used * 100.0) / max;
        }

        Map<String, Object> memory = new LinkedHashMap<>();

        memory.put("heapUsed", used);
        memory.put("heapCommitted", committed);
        memory.put("heapMax", max);
        memory.put("heapUsagePercentage", usagePercentage);

        return memory;
    }
}