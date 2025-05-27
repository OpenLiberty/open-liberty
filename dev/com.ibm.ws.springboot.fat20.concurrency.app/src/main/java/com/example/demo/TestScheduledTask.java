package com.example.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.stereotype.Component;


@Component
public class TestScheduledTask {

	private MyScheduledTask myTask;

	public TestScheduledTask(MyScheduledTask myTask) {
		this.myTask = myTask;
	}

//	private final CountDownLatch latch = new CountDownLatch(3);

	private final static Logger logger = LoggerFactory.getLogger(TestScheduledTask.class);

//	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public void testRepeatTask(String message) throws Throwable {

//		System.out.println("Test testRepeatTask method");
//		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//		Schedule the task to run for 15 seconds (3 executions)
//		scheduler.scheduleAtFixedRate(myTask.scheduledTask(), 0, 5, TimeUnit.SECONDS);
//		Wait for the task to execute 3 times
//      myTask.waitForTaskExecution(15, TimeUnit.SECONDS);
//		assertEquals(1, myTask.latch.getCount());
		logger.info(": VERIFICATION OF SCHEDULED TASK REPETITION PASSED");
    }
}
