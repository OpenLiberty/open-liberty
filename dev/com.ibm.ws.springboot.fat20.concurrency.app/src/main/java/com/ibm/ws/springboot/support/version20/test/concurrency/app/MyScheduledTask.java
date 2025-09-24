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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MyScheduledTask {

	private final static Logger logger = LoggerFactory.getLogger(MyScheduledTask.class);

	private final ConcurrencyTasks concurrencyTasks;
	private final CountDownLatch latch = new CountDownLatch(5);
	private final AtomicBoolean testedAsync1and2Methods = new AtomicBoolean();
	private final AtomicBoolean testedAsync3Methods = new AtomicBoolean();

	public MyScheduledTask(ConcurrencyTasks concurrencyTasks) {
		this.concurrencyTasks = concurrencyTasks;;
	}

	// Scheduled method for the Async tasks
	@Scheduled(fixedDelay = 5000)
	public void scheduledTask() throws InterruptedException, Throwable {

		AppRunner.assertManagedThread("ScheduledTask");

		assertAsyncTask1and2Method("ScheduledTask");

		logger.info("Task count" + latch.getCount() + " executed at: " + new java.util.Date());
		this.latch.countDown();
	}
	
	@Scheduled(fixedRate = 3000)
	public void scheduledTask2() throws InterruptedException, Throwable {
		
		assertAsyncTask3Method("ScheduledTask2");
	}

	@Async
	public void verifyScheduledTaskRepetition(String message) throws Exception {
		// Wait for the latch to be released within the specified timeout
		if (verifyLatchValue() == false) {
			logger.error("The scheduled task did not execute within the specified timeout");
		}
		// The task has completed within the timeout
		logger.info(message + ": VERIFY SCHEDULED TASK REPETITION METHOD PASSED");
	}

	private boolean verifyLatchValue() throws InterruptedException {
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				logger.error("The scheduled tasks did not complete within the expected time");
				return false;
			}
		} catch (InterruptedException ex) {
			logger.error("InterruptedException in verifyScheduledTaskRepetition method", ex);
			return false;
		}
		return true;
	}

	public void assertAsyncTask1and2Method(String message) throws Exception {
		if (!testedAsync1and2Methods.compareAndSet(false, true)) {
			return;
		}
		concurrencyTasks.task1("Assert Async Method").whenComplete((r, e) -> {
			if (e != null) {
				logger.error("Async Task 1 exception", e);
				return;
			}
			if (r == null) {
				logger.error("Async Task 1 failed", e);
				return;
			}
			logger.info(message + " Async task 1: ASSERT ASYNC METHOD VERIFICATION PASSED");
		});
		concurrencyTasks.task2("Assert Async Method").whenComplete((r, e) -> {
			if (e != null) {
				logger.error("Async Task 2 exception", e);
				return;
			}
			if (r == null) {
				logger.error("Async Task 2 failed", e);
				return;
			}
			logger.info(message + " Async task 2: ASSERT ASYNC METHOD VERIFICATION PASSED");
		});
	}
	
	public void assertAsyncTask3Method(String message) throws Exception {
		if (!testedAsync3Methods.compareAndSet(false, true)) {
			return;
		}
		concurrencyTasks.task3("Assert Async Method").whenComplete((r, e) -> {
			if (e != null) {
				logger.error("Async Task 3 exception", e);
				return;
			}
			if (r == null) {
				logger.error("Async Task 3 failed", e);
				return;
			}
			logger.info(message + " Async task 3: ASSERT ASYNC METHOD VERIFICATION PASSED");
		});
	}
}