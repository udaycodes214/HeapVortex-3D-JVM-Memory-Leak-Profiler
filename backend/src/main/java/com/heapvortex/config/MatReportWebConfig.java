package com.heapvortex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MatReportWebConfig
        implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        Path reportDirectory =
                Paths.get(
                        "heap-dumps",
                        "mat-reports"
                )
                .toAbsolutePath()
                .normalize();

        registry
                .addResourceHandler(
                        "/mat-reports/**"
                )
                .addResourceLocations(
                        reportDirectory
                                .toUri()
                                .toString()
                );
    }
}