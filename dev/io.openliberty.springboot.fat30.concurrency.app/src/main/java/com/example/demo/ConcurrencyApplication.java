package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import({ConcurrencyApplicationConfig.class, MyTask.class, AppRunner.class})
public class ConcurrencyApplication extends SpringBootServletInitializer{
	
	@Override
	protected SpringApplicationBuilder configure (SpringApplicationBuilder application) {
        return application.sources(ConcurrencyApplication.class);
	}
	
	public static void main(String[] args) {
        SpringApplication.run(ConcurrencyApplication.class, args);
    }

}
