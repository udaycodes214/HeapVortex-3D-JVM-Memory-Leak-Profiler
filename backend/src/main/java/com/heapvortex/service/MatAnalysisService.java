 package com.heapvortex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MatAnalysisService {

    private final Path matHome;

    private final Path heapDumpDirectory;

    public MatAnalysisService(
            @Value("${heapvortex.mat-home:}") String configuredMatHome
    ) {

        this.heapDumpDirectory =
                Paths.get(
                        "heap-dumps"
                )
                .toAbsolutePath()
                .normalize();

        if (
                configuredMatHome != null
                && !configuredMatHome.isBlank()
        ) {

            this.matHome =
                    Paths.get(
                            configuredMatHome
                    )
                    .toAbsolutePath()
                    .normalize();

        } else {

            this.matHome =
                    Paths.get(
                            System.getProperty("user.home"),
                            "Downloads",
                            "MemoryAnalyzer-1.17.0.20260601-win32.win32.x86_64",
                            "mat"
                    )
                    .toAbsolutePath()
                    .normalize();
        }
    }

    public Map<String, Object> analyzeHeapDump(
            Path heapDumpPath
    ) {

        Map<String, Object> response =
                new HashMap<>();

        try {

            /*
             * ============================================================
             * 1. VERIFY HEAP DUMP
             * ============================================================
             */

            if (!Files.exists(heapDumpPath)) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Heap dump file does not exist."
                );

                return response;
            }

            /*
             * ============================================================
             * 2. LOCATE ECLIPSE MAT
             * ============================================================
             */

            Path parseHeapDump =
                    matHome.resolve(
                            "ParseHeapDump.bat"
                    );

            if (!Files.exists(parseHeapDump)) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Eclipse MAT ParseHeapDump.bat was not found."
                );

                response.put(
                        "matPath",
                        parseHeapDump.toString()
                );

                return response;
            }

            /*
             * ============================================================
             * 3. PREPARE DIRECTORIES
             * ============================================================
             */

            Files.createDirectories(
                    heapDumpDirectory
            );

            String baseName =
                    removeExtension(
                            heapDumpPath
                                    .getFileName()
                                    .toString()
                    );

            Path reportZip =
                    heapDumpDirectory.resolve(
                            baseName
                                    + "_Leak_Suspects.zip"
                    );

            Files.deleteIfExists(
                    reportZip
            );

            /*
             * ============================================================
             * 4. RUN ECLIPSE MAT
             * ============================================================
             */

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "cmd.exe",
                            "/c",
                            parseHeapDump.toString(),
                            heapDumpPath
                                    .toAbsolutePath()
                                    .toString(),
                            "org.eclipse.mat.api:suspects"
                    );

            processBuilder.directory(
                    matHome.toFile()
            );

            processBuilder.redirectErrorStream(
                    true
            );

            Process process =
                    processBuilder.start();

            StringBuilder commandOutput =
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

                    commandOutput
                            .append(line)
                            .append(
                                    System.lineSeparator()
                            );
                }
            }

            int exitCode =
                    process.waitFor();

            /*
             * ============================================================
             * 5. VERIFY MAT OUTPUT
             * ============================================================
             */

            if (
                    exitCode != 0
                    || !Files.exists(reportZip)
            ) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Eclipse MAT analysis failed."
                );

                response.put(
                        "exitCode",
                        exitCode
                );

                response.put(
                        "commandOutput",
                        commandOutput.toString()
                );

                return response;
            }

            /*
             * ============================================================
             * 6. CREATE REPORT DIRECTORY
             * ============================================================
             */

            String reportId =
                    baseName
                            + "-"
                            + LocalDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyyMMdd-HHmmss"
                                    )
                            );

            Path reportDirectory =
                    heapDumpDirectory
                            .resolve(
                                    "mat-reports"
                            )
                            .resolve(
                                    reportId
                            );

            Files.createDirectories(
                    reportDirectory
            );

            /*
             * ============================================================
             * 7. EXTRACT MAT ZIP REPORT
             * ============================================================
             */

            unzipReport(
                    reportZip,
                    reportDirectory
            );

            /*
             * ============================================================
             * 8. LOCATE MAT INDEX PAGE
             * ============================================================
             */

            Path indexFile =
                    reportDirectory.resolve(
                            "index.html"
                    );

            /*
             * ============================================================
             * 9. EXTRACT MAT SUMMARY
             * ============================================================
             */

            Map<String, Object> matSummary =
                    extractMatSummary(
                            indexFile
                    );

            /*
             * ============================================================
             * 10. BUILD RESPONSE
             * ============================================================
             */

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Eclipse MAT leak analysis completed."
            );

            response.put(
                    "heapDump",
                    heapDumpPath
                            .getFileName()
                            .toString()
            );

            response.put(
                    "reportZip",
                    reportZip
                            .getFileName()
                            .toString()
            );

            response.put(
                    "reportDirectory",
                    reportDirectory
                            .toString()
            );

            response.put(
                    "reportAvailable",
                    Files.exists(indexFile)
            );

            response.put(
                    "reportUrl",
                    "/mat-reports/"
                            + reportId
                            + "/index.html"
            );

            response.put(
                    "exitCode",
                    exitCode
            );

            response.put(
                    "matSummary",
                    matSummary
            );

            return response;

        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "MAT analysis error."
            );

            response.put(
                    "error",
                    e.getMessage()
            );

            return response;
        }
    }

    /*
     * ====================================================================
     * MAT SUMMARY EXTRACTION
     * ====================================================================
     *
     * Reads the actual Eclipse MAT Leak Suspects index.html generated
     * by the current MAT version.
     *
     * The parser is based on the real HTML structure generated by
     * HeapVortex's MAT report.
     */

    private Map<String, Object> extractMatSummary(
            Path indexFile
    ) {

        Map<String, Object> summary =
                new HashMap<>();

        /*
         * Default values
         */

        summary.put(
                "available",
                false
        );

        try {

            if (
                    indexFile == null
                    || !Files.exists(indexFile)
            ) {

                return summary;
            }

            String html =
                    Files.readString(
                            indexFile,
                            StandardCharsets.UTF_8
                    );

            /*
             * ============================================================
             * PROBLEM SUSPECT 1
             * ============================================================
             *
             * Example from the actual MAT report:
             *
             * One instance of
             * <strong><q>
             * com.heapvortex.simulator.MemoryLeakSimulator
             * </q></strong>
             * ...
             * occupies
             * <strong>
             * 127,928,944 (90.63%)
             * </strong>
             * bytes.
             */

            Pattern suspectPattern =
                    Pattern.compile(
                            "Problem Suspect 1.*?"
                                    + "One instance of\\s*"
                                    + "<strong><q>(.*?)</q></strong>"
                                    + ".*?"
                                    + "occupies\\s*"
                                    + "<strong>"
                                    + "([0-9,]+)"
                                    + "\\s*\\(([0-9.]+)%\\)"
                                    + "</strong>"
                                    + "\\s*bytes",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher suspectMatcher =
                    suspectPattern.matcher(
                            html
                    );

            if (suspectMatcher.find()) {

                String suspectClass =
                        cleanHtmlText(
                                suspectMatcher.group(1)
                        );

                long retainedBytes =
                        parseLong(
                                suspectMatcher.group(2)
                        );

                double retainedPercentage =
                        parseDouble(
                                suspectMatcher.group(3)
                        );

                summary.put(
                        "available",
                        true
                );

                summary.put(
                        "suspectClass",
                        suspectClass
                );

                summary.put(
                        "retainedBytes",
                        retainedBytes
                );

                summary.put(
                        "retainedMB",
                        retainedBytes
                                / 1024.0
                                / 1024.0
                );

                summary.put(
                        "retainedPercentage",
                        retainedPercentage
                );
            }

            /*
             * ============================================================
             * TOP MEMORY CONSUMER
             * ============================================================
             *
             * Example:
             *
             * byte[] (122 instances totaling 127,928,224)
             */

            Pattern topConsumerPattern =
                    Pattern.compile(
                            "top consumers of its minimum retained heap"
                                    + ".*?"
                                    + "<strong><q>(.*?)</q></strong>"
                                    + "\\s*\\("
                                    + "\\s*([0-9,]+)"
                                    + "\\s*instances"
                                    + "\\s*totaling"
                                    + "\\s*([0-9,]+)"
                                    + "\\s*\\)",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher topConsumerMatcher =
                    topConsumerPattern.matcher(
                            html
                    );

            if (topConsumerMatcher.find()) {

                String topConsumerClass =
                        cleanHtmlText(
                                topConsumerMatcher.group(1)
                        );

                long instanceCount =
                        parseLong(
                                topConsumerMatcher.group(2)
                        );

                long totalBytes =
                        parseLong(
                                topConsumerMatcher.group(3)
                        );

                summary.put(
                        "topConsumerClass",
                        topConsumerClass
                );

                summary.put(
                        "topConsumerInstances",
                        instanceCount
                );

                summary.put(
                        "topConsumerBytes",
                        totalBytes
                );

                summary.put(
                        "topConsumerMB",
                        totalBytes
                                / 1024.0
                                / 1024.0
                );
            }

            /*
             * ============================================================
             * LEAK THREAD
             * ============================================================
             *
             * Example:
             *
             * Thread
             * <strong><q>
             * java.lang.Thread @ ... heapvortex-leak-simulator
             * </q></strong>
             */

            Pattern threadPattern =
                    Pattern.compile(
                            "Thread\\s*"
                                    + "<strong><q>"
                                    + "(.*?)"
                                    + "</q></strong>"
                                    + "\\s*has a local variable",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher threadMatcher =
                    threadPattern.matcher(
                            html
                    );

            if (threadMatcher.find()) {

                String thread =
                        cleanHtmlText(
                                threadMatcher.group(1)
                        );

                summary.put(
                        "leakThread",
                        thread
                );
            }

            /*
             * ============================================================
             * STACK FRAME
             * ============================================================
             *
             * Example:
             *
             * MemoryLeakSimulator.lambda$start$0()V
             * (MemoryLeakSimulator.java:65)
             */

            Pattern stackPattern =
                    Pattern.compile(
                            "Significant stack frames and local variables"
                                    + ".*?"
                                    + "<li>"
                                    + "(.*?)"
                                    + "</li>",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher stackMatcher =
                    stackPattern.matcher(
                            html
                    );

            if (stackMatcher.find()) {

                String stackFrame =
                        cleanHtmlText(
                                stackMatcher.group(1)
                        );

                summary.put(
                        "stackFrame",
                        stackFrame
                );

                Pattern sourceLinePattern =
                        Pattern.compile(
                                "\\(([^()]*)\\)"
                        );

                Matcher sourceMatcher =
                        sourceLinePattern.matcher(
                                stackFrame
                        );

                if (sourceMatcher.find()) {

                    summary.put(
                            "sourceLine",
                            sourceMatcher.group(1)
                    );
                }
            }

            /*
             * ============================================================
             * OVERVIEW RETAINED SIZE
             * ============================================================
             *
             * Example:
             *
             * Retained Size: 122 MB
             */

            Pattern overviewPattern =
                    Pattern.compile(
                            "Problem Suspect 1:.*?"
                                    + "Retained Size:\\s*"
                                    + "([0-9.]+)"
                                    + "\\s*MB",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher overviewMatcher =
                    overviewPattern.matcher(
                            html
                    );

            if (overviewMatcher.find()) {

                double overviewRetainedMB =
                        parseDouble(
                                overviewMatcher.group(1)
                        );

                summary.put(
                        "overviewRetainedMB",
                        overviewRetainedMB
                );
            }

        } catch (Exception ignored) {

            /*
             * MAT analysis itself succeeded.
             *
             * If summary extraction fails, we keep the main
             * MAT report available instead of failing the entire
             * analysis.
             */
        }

        return summary;
    }

    /*
     * ====================================================================
     * HTML TEXT CLEANUP
     * ====================================================================
     */

    private String cleanHtmlText(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replaceAll(
                        "<[^>]*>",
                        ""
                )
                .replace(
                        "&lt;",
                        "<"
                )
                .replace(
                        "&gt;",
                        ">"
                )
                .replace(
                        "&amp;",
                        "&"
                )
                .replace(
                        "&quot;",
                        "\""
                )
                .trim();
    }

    /*
     * ====================================================================
     * NUMBER PARSERS
     * ====================================================================
     */

    private long parseLong(
            String value
    ) {

        if (value == null) {

            return 0L;
        }

        try {

            return Long.parseLong(
                    value.replace(
                            ",",
                            ""
                    )
            );

        } catch (Exception e) {

            return 0L;
        }
    }

    private double parseDouble(
            String value
    ) {

        if (value == null) {

            return 0.0;
        }

        try {

            return Double.parseDouble(
                    value.replace(
                            ",",
                            ""
                    )
            );

        } catch (Exception e) {

            return 0.0;
        }
    }

    /*
     * ====================================================================
     * ZIP EXTRACTION
     * ====================================================================
     */

    private void unzipReport(
            Path zipFile,
            Path destination
    )
            throws IOException {

        try (
                ZipInputStream zipInputStream =
                        new ZipInputStream(
                                Files.newInputStream(
                                        zipFile
                                )
                        )
        ) {

            ZipEntry entry;

            while (
                    (entry =
                            zipInputStream.getNextEntry())
                            != null
            ) {

                Path outputPath =
                        destination
                                .resolve(
                                        entry.getName()
                                )
                                .normalize();

                /*
                 * Prevent ZIP path traversal.
                 */

                if (
                        !outputPath.startsWith(
                                destination
                        )
                ) {

                    throw new IOException(
                            "Invalid ZIP entry: "
                                    + entry.getName()
                    );
                }

                if (entry.isDirectory()) {

                    Files.createDirectories(
                            outputPath
                    );

                } else {

                    Path parent =
                            outputPath.getParent();

                    if (parent != null) {

                        Files.createDirectories(
                                parent
                        );
                    }

                    Files.copy(
                            zipInputStream,
                            outputPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                zipInputStream.closeEntry();
            }
        }
    }

    /*
     * ====================================================================
     * FILE NAME HELPER
     * ====================================================================
     */

    private String removeExtension(
            String fileName
    ) {

        int dot =
                fileName.lastIndexOf('.');

        if (dot <= 0) {

            return fileName;
        }

        return fileName.substring(
                0,
                dot
        );
    }
}