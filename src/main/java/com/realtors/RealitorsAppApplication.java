package com.realtors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.realtors")
@EnableCaching
@EnableAsync
public class RealitorsAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealitorsAppApplication.class, args);
	}

}
