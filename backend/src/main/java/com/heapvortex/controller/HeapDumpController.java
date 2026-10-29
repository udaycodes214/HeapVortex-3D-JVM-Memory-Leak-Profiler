package com.heapvortex.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jvm")
@CrossOrigin(origins = "http://localhost:5173")
public class HeapDumpController {

    @PostMapping("/heap-dump")
    public Map<String, Object> createHeapDump() {

        Map<String, Object> response =
                new HashMap<>();

        try {

            String pid =
                    String.valueOf(
                            ProcessHandle.current().pid()
                    );

            Path dumpDirectory =
                    Paths.get(
                            "heap-dumps"
                    );

            Files.createDirectories(
                    dumpDirectory
            );

            String fileName =
                    "heapvortex-"
                            + Instant.now()
                            .toString()
                            .replace(":", "-")
                            .replace(".", "-")
                            + ".hprof";

            Path dumpPath =
                    dumpDirectory.resolve(
                            fileName
                    );

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "jcmd",
                            pid,
                            "GC.heap_dump",
                            dumpPath
                                    .toAbsolutePath()
                                    .toString()
                    );

            processBuilder.redirectErrorStream(
                    true
            );

            Process process =
                    processBuilder.start();

            StringBuilder output =
                    new StringBuilder();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream()
                                    )
                            )
            ) {

                String line;

                while (
                        (line =
                                reader.readLine())
                                != null
                ) {

                    output.append(
                            line
                    );

                    output.append(
                            System.lineSeparator()
                    );
                }
            }

            int exitCode =
                    process.waitFor();

            File dumpFile =
                    dumpPath.toFile();

            if (
                    exitCode != 0
                    || !dumpFile.exists()
            ) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Heap dump creation failed."
                );

                response.put(
                        "commandOutput",
                        output.toString()
                );

                return response;
            }

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Heap dump created successfully."
            );

            response.put(
                    "fileName",
                    fileName
            );

            response.put(
                    "path",
                    dumpFile
                            .getAbsolutePath()
            );

            response.put(
                    "sizeBytes",
                    dumpFile.length()
            );

            response.put(
                    "sizeMB",
                    String.format(
                            "%.2f",
                            dumpFile.length()
                                    / 1024.0
                                    / 1024.0
                    )
            );

            response.put(
                    "pid",
                    pid
            );

            return response;

        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Error creating heap dump."
            );

            response.put(
                    "error",
                    e.getMessage()
            );

            return response;
        }
    }
}