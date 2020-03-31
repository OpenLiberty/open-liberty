/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.autonomicalpolling1serv;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This test bucket attempts to simulate fail-over of tasks.
 * For this oversimplified scenario, we don't actually have multiple servers, just multiple
 * persistent executor instances on a single server.
 * We can simulate an instance going down by removing it from the configuration.
 */
@RunWith(FATRunner.class)
public class AutonomicalPolling1ServerTest extends FATServletClient {
	private static final String APP_NAME = "autonomicalpolling1servApp";
	private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);

	private static ServerConfiguration originalConfig;

	@Server("com.ibm.ws.concurrent.persistent.fat.autonomicalpolling1serv")
	public static LibertyServer server;

	private static final String TASK_ID_MESSAGE = "Task id is ";

	@BeforeClass
	public static void setUp() throws Exception {
		originalConfig = server.getServerConfiguration().clone();
		ShrinkHelper.defaultDropinApp(server, APP_NAME, "autonomicalpolling1serv.web");

		server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/autonomicalpolling1db");

		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		try {
			server.stopServer("J2CA0027E", "DSRA0304E", "DSRA0302E");
		} finally {
			server.updateServerConfiguration(originalConfig);
		}
	}

	/**
	 * testAdd10Remove1PersistentExecs - Add 10 persistent executors, then remove 1, and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	@Mode(TestMode.FULL)
	public void testAdd10Remove1PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable 1 of the 10 persistent executors.
		ConfigElementList<PersistentExecutor> executors = config.getPersistentExecutors();
		executors.get(9).setEnableTaskExecution("false");
		
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(10000);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=9");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}

	/**
	 * testAdd10Remove9PersistentExecs - Add 10 persistent executors, then remove 9, and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	@Mode(TestMode.FULL)
	public void testAdd10Remove9PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable all but 1 persistent executor.
		ConfigElementList<PersistentExecutor> executors = config.getPersistentExecutors();
		for (int i = 1; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("false");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(10000);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=1");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testAdd10RemoveAllImmediatelyAdd10PersistentExecs - Add 10 persistent executors, remove them all,
	 * immediately add 10 persistent executors back, and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	@Mode(TestMode.FULL)
	public void testAdd10RemoveAllImmediatelyAdd10PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable all.
		ConfigElementList<PersistentExecutor> executors = config.getPersistentExecutors();
		for (int i = 0; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("false");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for 3 seconds so we have missed more than 1 poll interval.
		Thread.sleep(3000);
		
		// Enable all.
		for (int i = 0; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("true");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=10");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testAdd10RemoveAllWaitAdd10PersistentExecs - Add 10 persistent executors, remove them all, wait long enough to miss more than one poll interval,
	 * add 10 persistent executors back, and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	@Mode(TestMode.FULL)
	public void testAdd10RemoveAllWaitAdd10PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable all.
		ConfigElementList<PersistentExecutor> executors = config.getPersistentExecutors();
		for (int i = 0; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("false");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for 3 seconds so we have missed more than 1 poll interval.
		Thread.sleep(3000);
		
		// Enable all.
		for (int i = 0; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("true");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=10");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testAdd10Remove5PersistentExecs - Add 10 persistent executors, then disable 5 of them,
	 * and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	@Mode(TestMode.FULL)
	public void testAdd10Remove5PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable 5 persistent executors.
		ConfigElementList<PersistentExecutor> executors = config.getPersistentExecutors();
		for (int i = 5; i < executors.size(); i++) {
			executors.get(i).setEnableTaskExecution("false");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle. 10 seconds isn't always enough for this scenario, so we may need to increase in the future.
		// Currently our retries cover the cases when we haven't waited long enough.
		Thread.sleep(10000);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=5");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testAdd3Add4PersistentExecs - Add 3 persistent executors, remove 2, and then add 4 more. Verify the polling intervals for all the
	 * persistent executors are correct.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	public void testAdd3Remove2Add4PersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[7];

		// Add 3 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < 3; i++) {
			persistentExecs[i] = new PersistentExecutor();
			if (i == 0) {
				persistentExecs[0].setId("persistentExec" + 0);
				persistentExecs[0].setJndiName("persistent/exec" + 0);
			} else {
				persistentExecs[i].setId("taskExecutionWillBeDisabled" + i);
				persistentExecs[i].setJndiName("persistent/taskExecutionWillBeDisabled" + i);
			}
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}

		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec0&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Disable all but one.
		for (int i = 1; i < 3; i++) {
			persistentExecs[i].setEnableTaskExecution("false");
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Allow polling to settling to one persistent executor.
		Thread.sleep(3000);
		
		// Add 4 persistent executors.
		for (int i = 1; i < 5; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=5");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testAddPersistentExecs - Add persistent executors and verify the polling interval is set correctly.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	public void testAddPersistentExecs() throws Exception {
		PersistentExecutor persistentExecs[] = new PersistentExecutor[10];

		// Create 10 persistent executors.
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		// Wait for polling to settle.
		Thread.sleep(6000);
		
		// Schedule a repeating task to run.
		StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
				"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1900&test=testPersistentExecPolling[1]");

		int start = result.indexOf(TASK_ID_MESSAGE);
		if (start < 0)
			fail("Task id of scheduled task not found in servlet output: " + result);
		String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

		System.out.println("Scheduled task " + taskId);
		
		// Verify the polling delay of the persistent executors.
		try {
			result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPersistentExecPolling&numPersistentExecs=10");
			assertTrue("testPersistentExecPolling failed with delay results of (values should be sequential starting with 0): " + result.toString(), 
					result.indexOf("PASSED") > -1);
		} finally {
			// always cancel the task
			runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
					"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPersistentExecPolling[1]");

			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}
	}
	
	/**
	 * testPollIntervalStable - Have 3 persistent executors available for running tasks. Verify the persistent executors are all taking regular turns.
	 */
	// When more than one Persistent Executor tries to request a unique Partition ID at the same time the following Derby error occurs:   
	// "ERROR 40XL1: A lock could not be obtained within the time requested"
	// This later results in a XA_RBTIMEOUT (106) error on the transaction (javax.transaction.xa.XAException).
	// We recover/retry just fine, but this is a reason why Derby is not used for production.
	@AllowedFFDC("javax.transaction.xa.XAException")
	@Test
	public void testPollIntervalStable() throws Exception {
		int attempts = 0;

		// Create 3 persistent executors.
		PersistentExecutor persistentExecs[] = new PersistentExecutor[3];
		ServerConfiguration config = originalConfig.clone();
		for (int i = 0; i < persistentExecs.length; i++) {
			persistentExecs[i] = new PersistentExecutor();
			persistentExecs[i].setId("persistentExec" + i);
			persistentExecs[i].setJndiName("persistent/exec" + i);
			persistentExecs[i].setPollInterval("1s");
			persistentExecs[i].setMissedTaskThreshold("3s");
			persistentExecs[i].setExtraAttribute("ignore.minimum.for.test.use.only", "true");
			persistentExecs[i].setExtraAttribute("pollingCoordination.for.test.use.only", "true");
			config.getPersistentExecutors().add(persistentExecs[i]);
		}
		server.setMarkToEndOfLog();
		server.updateServerConfiguration(config);
		server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		
		try {
			while (attempts < 5) {
				attempts++;
				System.out.println("testPollIntervalStable attempt #" + attempts);

				StringBuilder result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
						"testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=2000&test=testPollIntervalStable[1]");

				int start = result.indexOf(TASK_ID_MESSAGE);
				if (start < 0)
					fail("Task id of scheduled task not found in servlet output: " + result);
				String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

				System.out.println("Scheduled task " + taskId);
				try {   
					result = runTestWithResponse(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet", "testPollIntervalStable");
				} finally {
					// always cancel the task
					runTest(server, APP_NAME + "/AutonomicalPolling1ServerTestServlet",
							"testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testPollIntervalStable[1]");
				}

				if(result.indexOf("PASSED") > -1) {
					return;
				}
			}
		} finally {
			// restore original configuration
			server.setMarkToEndOfLog();
			server.updateServerConfiguration(originalConfig);
			server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
		}

		fail("testPollIntervalStable failed after multiple attemps. This likely means the the autonomical poll interval algorithm is not working.");

	}
}