package com.system.megacityCab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MegacityCabApplication {

	public static void main(String[] args) {
		SpringApplication.run(MegacityCabApplication.class, args);
	}

}
