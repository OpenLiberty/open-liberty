package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;

@SpringBootApplication
@EnableScheduling
public class ConcurrencyApplication extends SpringBootServletInitializer{
	
	@Override
	protected SpringApplicationBuilder configure (SpringApplicationBuilder application) {
        return application.sources(ConcurrencyApplication.class);
	}
	
	public static void main(String[] args) {
        SpringApplication.run(ConcurrencyApplication.class, args);
    }
	
	@Bean
    public DefaultManagedTaskScheduler defaultManagedTaskScheduler() throws InterruptedException {
    	DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    	scheduler.setConcurrentExecutor(taskExecutor());
        return scheduler;
    }
    
    @Bean
    public DefaultManagedTaskExecutor taskExecutor() throws InterruptedException {
    	DefaultManagedTaskExecutor executor = new DefaultManagedTaskExecutor();
        return executor;
    }
    

}
