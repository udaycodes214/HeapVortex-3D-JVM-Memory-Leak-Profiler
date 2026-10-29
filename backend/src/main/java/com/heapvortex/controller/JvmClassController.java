package com.heapvortex.controller;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jvm")
@CrossOrigin(origins = "http://localhost:5173")
public class JvmClassController {

    private final ClassLoadingMXBean classLoadingMXBean =
            ManagementFactory.getClassLoadingMXBean();

    @GetMapping("/classes")
    public Map<String, Object> getClassStatistics() {

        return Map.of(
                "loadedClassCount",
                classLoadingMXBean.getLoadedClassCount(),

                "totalLoadedClassCount",
                classLoadingMXBean.getTotalLoadedClassCount(),

                "unloadedClassCount",
                classLoadingMXBean.getUnloadedClassCount()
        );
    }
}