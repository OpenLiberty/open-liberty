package com.example.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MyScheduledTask {

	private final static Logger logger = LoggerFactory.getLogger(MyScheduledTask.class);

	private final ConcurrencyTasks concurrencyTasks;
	private final CountDownLatch latch = new CountDownLatch(5);

	public MyScheduledTask(ConcurrencyTasks concurrencyTasks) {
		this.concurrencyTasks = concurrencyTasks;
	}

	// Scheduled method for the Async tasks
	@Async
	@Scheduled(fixedDelay = 5000)
	public Runnable scheduledTask() throws InterruptedException, Throwable {

		AppRunner.assertManagedThread("ScheduledTask");

		assertAsyncMethod("ScheduledTask");

		logger.info("Task executed at: " + new java.util.Date());
		this.latch.countDown();
		return null;
	}

	@Async
	public void verifyScheduledTaskRepetition() throws Exception {
		// Wait for the latch to be released within the specified timeout
		if (verifyLatchValue() == false) {
			logger.error("The scheduled task did not execute within the specified timeout");
		}
		// The task has completed within the timeout
		logger.info("VERIFY SCHEDULED TASK METHOD PASSED");
	}

	private boolean verifyLatchValue() throws InterruptedException {
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

	public void assertAsyncMethod(String message) throws Exception {

		try {
			assertNotNull("Async Task 1 failed", concurrencyTasks.task1("Assert Async Method").get());
			assertNotNull("Async Task 2 failed", concurrencyTasks.task2("Assert Async Method").get());
		} catch (Exception e) {
			logger.error(message + ": ASYNC TASK FAILED", e);
			fail("Async Task failed: " + e.getMessage());
		}
		logger.info(message + ": ASSERT ASYNC METHOD VERIFICATION PASSED");
	}
}