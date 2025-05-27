package com.example.demo;


import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
 public class MyScheduledTask{
	 
	private final static Logger logger = LoggerFactory.getLogger(MyScheduledTask.class);


	private static CountDownLatch latch = new CountDownLatch(5);
	 
//	Scheduled method for the Async tasks
	@Async
    @Scheduled(fixedDelay = 5000)
    public Runnable scheduledTask() throws InterruptedException, Throwable {
    	
    	AppRunner.assertManagedThread("ScheduledTask");

    	AppRunner.assertAsyncMethod("ScheduledTask");

    	logger.info("Task executed at: " + new java.util.Date());
        this.latch.countDown();
        System.out.println("Value of latch is: "+ latch);
    	ConcurrencyTasks.verifyScheduledTaskRepetition();
		return null;
    }

	public static boolean verifyLatchValue() throws InterruptedException{
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				logger.error("InterruptedException in verifyScheduledTaskRepetition method");
				return false;
			}
    	} catch (InterruptedException ex) {
		logger.error("InterruptedException in verifyScheduledTaskRepetition method", ex);
    	}
    	return true;
	}
 }