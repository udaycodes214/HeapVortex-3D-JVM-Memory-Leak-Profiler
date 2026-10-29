package com.heapvortex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HeapVortexApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeapVortexApplication.class, args);
	}

}