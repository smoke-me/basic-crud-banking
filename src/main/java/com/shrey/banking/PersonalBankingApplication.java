package com.shrey.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonalBankingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalBankingApplication.class, args);
	}

}
