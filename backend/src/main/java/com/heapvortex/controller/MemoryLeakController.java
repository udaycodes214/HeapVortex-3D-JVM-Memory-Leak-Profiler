package com.heapvortex.controller;

import com.heapvortex.simulator.MemoryLeakSimulator;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leak")
@CrossOrigin(origins = "http://localhost:5173")
public class MemoryLeakController {

    private final MemoryLeakSimulator simulator;

    public MemoryLeakController(
            MemoryLeakSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {

        simulator.start();

        return Map.of(
                "status", "started",
                "retainedMB", simulator.getRetainedMB(),
                "running", simulator.isRunning()
        );
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {

        simulator.stop();

        return Map.of(
                "status", "stopped",
                "retainedMB", simulator.getRetainedMB(),
                "running", simulator.isRunning()
        );
    }

    @PostMapping("/clear")
    public Map<String, Object> clear() {

        simulator.clear();

        return Map.of(
                "status", "cleared",
                "retainedMB", simulator.getRetainedMB(),
                "running", simulator.isRunning()
        );
    }

    @GetMapping("/status")
    public Map<String, Object> status() {

        return Map.of(
                "status",
                simulator.isRunning()
                        ? "running"
                        : "stopped",

                "retainedMB",
                simulator.getRetainedMB(),

                "running",
                simulator.isRunning()
        );
    }
}