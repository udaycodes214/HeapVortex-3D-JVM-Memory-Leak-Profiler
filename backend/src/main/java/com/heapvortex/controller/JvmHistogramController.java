package com.heapvortex.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/jvm")
@CrossOrigin(origins = "http://localhost:5173")
public class JvmHistogramController {

    @GetMapping("/histogram")
    public List<ClassHistogramEntry> getHistogram() {

        List<ClassHistogramEntry> result = new ArrayList<>();

        try {

            String pid = getCurrentProcessId();

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "jcmd",
                            pid,
                            "GC.class_histogram"
                    );

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (!line.matches(
                        "^\\d+:\\s+\\d+\\s+\\d+\\s+.+$"
                )) {
                    continue;
                }

                try {

                    String[] parts =
                            line.split("\\s+", 4);

                    int rank =
                            Integer.parseInt(
                                    parts[0].replace(":", "")
                            );

                    long instanceCount =
                            Long.parseLong(parts[1]);

                    long totalBytes =
                            Long.parseLong(parts[2]);

                    String className =
                            parts[3];

                    result.add(
                            new ClassHistogramEntry(
                                    rank,
                                    className,
                                    instanceCount,
                                    totalBytes
                            )
                    );

                } catch (Exception ignored) {
                    // Ignore malformed histogram lines
                }
            }

            process.waitFor();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return result;
    }

    private String getCurrentProcessId() {

        return String.valueOf(
                ProcessHandle.current().pid()
        );
    }

    public record ClassHistogramEntry(
            int rank,
            String className,
            long instanceCount,
            long totalBytes
    ) {
    }
}