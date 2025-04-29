package com.example.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.stereotype.Service;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class ConcurrencyApplicationConfig {
	
	private final static Logger logger = LoggerFactory.getLogger(ConcurrencyApplicationConfig.class);
   
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
    
   @Async
   public CompletableFuture<String> task1(String message) throws Exception {
        System.out.println("Async Task 1: " + Thread.currentThread().getName());
        TimeUnit.SECONDS.sleep(5);
        AppRunner.assertManagedThread(message + ": Async Task 1");
        return CompletableFuture.completedFuture("Async Task 1 passed");
    }
    @Async
    public CompletableFuture<String> task2(String message) throws Exception {
        System.out.println("Async Task 2: " + Thread.currentThread().getName());
        TimeUnit.SECONDS.sleep(3);
        AppRunner.assertManagedThread(message + ": Async Task 2");
        return CompletableFuture.completedFuture("Async Task 2 passed");
    }
    
	//Call a new static method assertAsyncMethod, it will call Task 1 and 2, takes a String message as arg
	//Verifies the result of the CompletableFuture, assert on the string value returned by the tasks
    //Log the message based on success on failure
    public void assertAsyncMethod(String message) throws Exception {
    	
    	try {	
    		assertNotNull("Async Task 1 failed", task1("Assert Async Method").get());
    		assertNotNull("Async Task 2 failed", task2("Assert Async Method").get());
    	}catch (NamingException e){
    		logger.error(message + ": ASYNC TASK FAILED", e);
			fail("Async Task failed: " + e.getMessage());
    	}  	
    	logger.info(message + ": ASSERT ASYNC METHOD VERIFICATION PASSED");
    }
}
    