package com.example.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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

@Configuration
@EnableAsync
public class ConcurrencyTasks {
	
	private final static Logger logger = LoggerFactory.getLogger(ConcurrencyTasks.class);
	
	private static MyScheduledTask scheduled_task;

	public ConcurrencyTasks(MyScheduledTask scheduled_task) {
		this.scheduled_task = scheduled_task;
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

    @Async
    public static void verifyScheduledTaskRepetition() throws Exception {
    	// Wait for the latch to be released within the specified timeout
    	if(scheduled_task.verifyLatchValue() == false) {
    		logger.error("The scheduled task did not execute within the specified timeout");
    	} 
    	// The task has completed within the timeout
		logger.info("VERIFY SCHEDULED TASK METHOD PASSED");
    }
}
    