package com.catticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// CatticketProperties（catticket.*）を読み込むため
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatticketApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatticketApplication.class, args);
	}

}
