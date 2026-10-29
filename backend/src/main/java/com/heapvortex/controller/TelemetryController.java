package com.heapvortex.controller;

import com.heapvortex.telemetry.MemoryTelemetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "http://localhost:5173")
public class TelemetryController {

    private final MemoryTelemetryService telemetryService;

    public TelemetryController(MemoryTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/history")
    public List<MemoryTelemetryService.MemorySample> getHistory() {
        return telemetryService.getHistory();
    }
}