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
