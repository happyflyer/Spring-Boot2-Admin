package org.example.spring_boot2.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @author lifei
 */
@ServletComponentScan
@SpringBootApplication
public class SpringBoot2AdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBoot2AdminApplication.class, args);
	}

}
