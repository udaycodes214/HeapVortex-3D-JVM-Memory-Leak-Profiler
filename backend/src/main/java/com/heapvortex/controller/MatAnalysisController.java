package com.heapvortex.controller;

import com.heapvortex.service.MatAnalysisService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Comparator;
import java.util.Map;

@RestController
@RequestMapping("/api/jvm")
@CrossOrigin(origins = "http://localhost:5173")
public class MatAnalysisController {

    private final MatAnalysisService matAnalysisService;

    public MatAnalysisController(
            MatAnalysisService matAnalysisService
    ) {

        this.matAnalysisService =
                matAnalysisService;
    }

    @PostMapping("/mat-analyze")
    public Map<String, Object> analyzeLatestHeapDump() {

        try {

            Path heapDumpDirectory =
                    Paths.get(
                            "heap-dumps"
                    )
                    .toAbsolutePath()
                    .normalize();

            if (!Files.exists(heapDumpDirectory)) {

                return Map.of(
                        "success",
                        false,

                        "message",
                        "Heap dump directory does not exist."
                );
            }

            Path latestHeapDump;

            try (
                    var stream =
                            Files.list(
                                    heapDumpDirectory
                            )
            ) {

                latestHeapDump =
                        stream
                                .filter(
                                        path ->
                                                path.getFileName()
                                                        .toString()
                                                        .endsWith(".hprof")
                                )
                                .max(
                                        Comparator.comparing(
                                                path -> {

                                                    try {

                                                        return Files
                                                                .getLastModifiedTime(
                                                                        path
                                                                );

                                                    } catch (Exception e) {

                                                        return null;
                                                    }
                                                },
                                                Comparator.nullsFirst(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                )
                                .orElse(null);
            }

            if (latestHeapDump == null) {

                return Map.of(
                        "success",
                        false,

                        "message",
                        "No HPROF heap dump was found."
                );
            }

            return matAnalysisService
                    .analyzeHeapDump(
                            latestHeapDump
                    );

        } catch (Exception e) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Unable to start MAT analysis.",

                    "error",
                    e.getMessage()
            );
        }
    }

    @PostMapping("/mat-analyze/file")
    public Map<String, Object> analyzeSpecificHeapDump(
            @RequestParam String fileName
    ) {

        try {

            Path heapDumpDirectory =
                    Paths.get(
                            "heap-dumps"
                    )
                    .toAbsolutePath()
                    .normalize();

            Path heapDump =
                    heapDumpDirectory
                            .resolve(
                                    fileName
                            )
                            .normalize();

            if (
                    !heapDump.startsWith(
                            heapDumpDirectory
                    )
            ) {

                return Map.of(
                        "success",
                        false,

                        "message",
                        "Invalid heap dump path."
                );
            }

            if (!Files.exists(heapDump)) {

                return Map.of(
                        "success",
                        false,

                        "message",
                        "Heap dump not found."
                );
            }

            return matAnalysisService
                    .analyzeHeapDump(
                            heapDump
                    );

        } catch (Exception e) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Unable to analyze heap dump.",

                    "error",
                    e.getMessage()
            );
        }
    }

    @GetMapping("/mat-status")
    public Map<String, Object> status() {

        Path matPath =
                Paths.get(
                        System.getProperty("user.home"),
                        "Downloads",
                        "MemoryAnalyzer-1.17.0.20260601-win32.win32.x86_64",
                        "mat",
                        "ParseHeapDump.bat"
                );

        return Map.of(
                "matAvailable",
                Files.exists(matPath),

                "matPath",
                matPath.toString()
        );
    }
}