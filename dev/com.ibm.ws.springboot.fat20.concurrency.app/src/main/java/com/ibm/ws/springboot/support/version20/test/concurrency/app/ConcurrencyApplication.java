/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.concurrency.app;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;

@SpringBootApplication
@EnableScheduling
@Configuration
public class ConcurrencyApplication extends SpringBootServletInitializer{
	
	@Override
	protected SpringApplicationBuilder configure (SpringApplicationBuilder application) {
        return application.sources(ConcurrencyApplication.class);
	}

	public static void main(String[] args) {
        SpringApplication.run(ConcurrencyApplication.class, args);
    }

	@Bean
	@Primary
    public DefaultManagedTaskScheduler defaultManagedTaskScheduler() throws InterruptedException {
    	DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    	scheduler.setConcurrentExecutor(taskExecutor());
        return scheduler;
    }

    @Bean
    @Primary
    public DefaultManagedTaskExecutor taskExecutor() throws InterruptedException {
    	DefaultManagedTaskExecutor executor = new DefaultManagedTaskExecutor();
        return executor;
    }
    
	@Bean
	@Qualifier(value="taskScheduler1")
    public DefaultManagedTaskScheduler taskScheduler1() throws InterruptedException {
    	DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    	scheduler.setResourceRef(true);
    	scheduler.setJndiName("taskScheduler1");
    	scheduler.setConcurrentExecutor(taskExecutor1());
        return scheduler;
    }
    
    @Bean
    @Qualifier(value="taskExecutor1")
    public DefaultManagedTaskExecutor taskExecutor1() throws InterruptedException {
    	DefaultManagedTaskExecutor executor = new DefaultManagedTaskExecutor();
    	executor.setResourceRef(true);
    	executor.setJndiName("taskExecutor1");
        return executor;
    }
    

}
