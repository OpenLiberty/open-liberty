/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConcurrencyPolicy;
import com.ibm.websphere.simplicity.config.ContextService;
import com.ibm.websphere.simplicity.config.ManagedExecutorService;
import com.ibm.websphere.simplicity.config.ManagedScheduledExecutorService;
import com.ibm.websphere.simplicity.config.ManagedThreadFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.context.ClassloaderContext;
import com.ibm.websphere.simplicity.config.context.JEEMetadataContext;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for EE Concurrency Utilities, including tests that make updates to the server
 * configuration while the server is running.
 * A setUpPerTest method runs before each test to restore to the original configuration,
 * so that tests do not interfere with each other.
 */
@RunWith(FATRunner.class)
public class EEConcurrencyConfigTest extends FATServletClient {

    private static final String APP_NAME = "concurrent";

    @Server("concurrent.config.fat")
    public static LibertyServer server;

    // Tests can use this to indicate they don't make any config updates and so don't need to have the original config restored
    private static boolean restoreSavedConfig = true;

    private static ServerConfiguration savedConfig;

    private static void runTest(String test, String execSvc) throws Exception {
        FATServletClient.runTest(server, APP_NAME, test + "&managedExecutorService=" + execSvc);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME,
                                      "fat.concurrent.ejb",
                                      "fat.concurrent.web");
        savedConfig = server.getServerConfiguration().clone();
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/concurrentinternals-1.0.mf");
    }

    /**
     * Before running each test, restore to the original configuration.
     */
    @Before
    public void setUpPerTest() throws Exception {
        Exception failure = null;
        String consoleLogFileName = getClass().getSimpleName() + '.' + testName.getMethodName() + ".log";
        if (!server.isStarted()) {
            server.updateServerConfiguration(savedConfig);
            server.startServer(consoleLogFileName); // clean start
            Log.info(getClass(), "setUpPerTest", "server started, log file is " + consoleLogFileName);
        } else if (restoreSavedConfig) {
            try {
                server.stopServer();
            } catch (Exception x) {
                failure = x;
            }
            server.updateServerConfiguration(savedConfig);
            server.startServer(consoleLogFileName, false, false); // warm start
            Log.info(getClass(), "setUpPerTest", "server restarted, log file is " + consoleLogFileName);
        }
        restoreSavedConfig = true; // assume all tests make config updates unless they tell us otherwise
        if (failure != null)
            throw failure;
    }

    /**
     * After completing all tests, stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
            server.updateServerConfiguration(savedConfig);
        }
    }

    @Test
    public void testClassloaderContext() throws Exception {
        runTest("testClassloaderContext", "java:comp/DefaultManagedExecutorService");
        restoreSavedConfig = false;
    }

    @Test
    public void testJEEMetadataContext() throws Exception {
        runTest("testJEEMetadataContext", "java:comp/DefaultManagedExecutorService");
        restoreSavedConfig = false;
    }

    @Test
    @Mode(FULL)
    public void testCreateNewManagedExecutorService() throws Exception {
        // Add <contextService id="contextSvc1"/>
        ServerConfiguration config = server.getServerConfiguration();
        ContextService contextSvc1 = new ContextService();
        contextSvc1.setId("contextSvc1");
        config.getContextServices().add(contextSvc1);
        // Add <managedExecutorService jndiName="concurrent/execSvc1" contextServiceRef="contextSvc1"/>
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setJndiName("concurrent/execSvc1");
        execSvc1.setContextServiceRef(contextSvc1.getId());
        config.getManagedExecutorServices().add(execSvc1);
        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContextFromEJB", null);

        // transaction context can be either because it is controlled by the app
        runTest("testNoTransactionContext", "concurrent/execSvc1");
        runTest("testTransactionContext", "concurrent/execSvc1");

        // Add <classloaderContext/> to contextSvc1
        contextSvc1.getClassloaderContexts().add(new ClassloaderContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testClassloaderContext", "concurrent/execSvc1");

        // Add <jeeMetadataContext/> to contextSvc1
        contextSvc1.getJEEMetadataContexts().add(new JEEMetadataContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);

        // Switch to <managedExecutorService jndiName="concurrent/execSvc1" contextServiceRef="contextSvc2"/>
        ContextService contextSvc2 = new ContextService();
        contextSvc2.setId("contextSvc2");
        contextSvc2.getJEEMetadataContexts().add(new JEEMetadataContext());
        config.getContextServices().add(contextSvc2);
        execSvc1.setContextServiceRef(contextSvc2.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);

        // Switch to <managedScheduledExecutorService jndiName="concurrent/execSvc1" contextServiceRef="contextSvc2"/>
        config.getManagedExecutorServices().remove(execSvc1);
        ManagedScheduledExecutorService scheduledExecSvc1 = new ManagedScheduledExecutorService();
        scheduledExecSvc1.setJndiName(execSvc1.getJndiName());
        scheduledExecSvc1.setContextServiceRef(execSvc1.getContextServiceRef());
        config.getManagedScheduledExecutorServices().add(scheduledExecSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);
    }

    @Test
    @Mode(FULL)
    public void testCreateNewManagedScheduledExecutorService() throws Exception {
        // Add <managedScheduledExecutorService jndiName="concurrent/execSvc1"/> with nested contextService (initially empty)
        ServerConfiguration config = server.getServerConfiguration();
        ManagedScheduledExecutorService scheduledExecSvc1 = new ManagedScheduledExecutorService();
        scheduledExecSvc1.setJndiName("concurrent/execSvc1");
        ContextService nestedContextSvc = new ContextService();
        scheduledExecSvc1.getContextServices().add(nestedContextSvc);
        config.getManagedScheduledExecutorServices().add(scheduledExecSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContextFromEJB", null);

        // transaction context can be either because it is controlled by the app
        runTest("testNoTransactionContext", "concurrent/execSvc1");
        runTest("testTransactionContext", "concurrent/execSvc1");

        // Add <classloaderContext/> to nested contextService
        nestedContextSvc.getClassloaderContexts().add(new ClassloaderContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testClassloaderContext", "concurrent/execSvc1");

        // Add <jeeMetadataContext/> to contextSvc1
        nestedContextSvc.getJEEMetadataContexts().add(new JEEMetadataContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);

        // Switch to <managedScheduledExecutorService jndiName="concurrent/execSvc1" contextServiceRef="contextSvc2"/>
        scheduledExecSvc1.getContextServices().clear();
        ContextService contextSvc2 = new ContextService();
        contextSvc2.setId("contextSvc2");
        contextSvc2.getJEEMetadataContexts().add(new JEEMetadataContext());
        config.getContextServices().add(contextSvc2);
        scheduledExecSvc1.setContextServiceRef(contextSvc2.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);

        // Switch to <managedExecutorService jndiName="concurrent/execSvc1" contextServiceRef="contextSvc2"/>
        config.getManagedScheduledExecutorServices().remove(scheduledExecSvc1);
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setJndiName(scheduledExecSvc1.getJndiName());
        execSvc1.setContextServiceRef(scheduledExecSvc1.getContextServiceRef());
        config.getManagedExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);
    }

    @Test
    @Mode(FULL)
    public void testCreateNewManagedThreadFactory() throws Exception {
        // Add <managedThreadFactory jndiName="concurrent/threadFactory1"/>
        ServerConfiguration config = server.getServerConfiguration();
        ManagedThreadFactory threadFactory1 = new ManagedThreadFactory();
        threadFactory1.setJndiName("concurrent/threadFactory1");
        config.getManagedThreadFactories().add(threadFactory1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testThreadIsNotDaemon", "concurrent/threadFactory1");
        runTest("testThreadPriority5", "concurrent/threadFactory1");

        // Add createDaemonThreads=true defaultPriority=3
        threadFactory1.setCreateDaemonThreads("true");
        threadFactory1.setDefaultPriority("8");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testThreadIsDaemon", "concurrent/threadFactory1");
        runTest("testThreadPriority8", "concurrent/threadFactory1");

        // Remove createDaemonThreads, Change defaultPriority=3, Add maxPriority=4
        threadFactory1.setCreateDaemonThreads(null);
        threadFactory1.setDefaultPriority("3");
        threadFactory1.setMaxPriority("4");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testThreadIsNotDaemon", "concurrent/threadFactory1");
        runTest("testThreadPriority3", "concurrent/threadFactory1");
        runTest("testThreadGroupMaxPriority4", "concurrent/threadFactory1");

        // Add createDaemonThreads=false, Remove defaultPriority, Change maxPriority=6
        threadFactory1.setCreateDaemonThreads("false");
        threadFactory1.setDefaultPriority(null);
        threadFactory1.setMaxPriority("6");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testThreadIsNotDaemon", "concurrent/threadFactory1");
        runTest("testThreadPriority5", "concurrent/threadFactory1");
        runTest("testThreadGroupMaxPriority6", "concurrent/threadFactory1");

        // Add defaultPriority=9, change maxPriority=3 (attempt to set defaultPriority greater than maxPriority)
        threadFactory1.setDefaultPriority("9");
        threadFactory1.setMaxPriority("3");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testThreadPriority3", "concurrent/threadFactory1");
    }

    @Test
    @Mode(FULL)
    public void testCreateManagedExecutorServiceWithNestedContextService() throws Exception {
        // Add <managedExecutorService jndiName="concurrent/execSvc1"> with nested contextService with nested classloaderContext
        ServerConfiguration config = server.getServerConfiguration();
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setJndiName("concurrent/execSvc1");
        ContextService contextSvc = new ContextService();
        contextSvc.getClassloaderContexts().add(new ClassloaderContext());
        execSvc1.getContextServices().add(contextSvc);
        config.getManagedExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testClassloaderContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContext", "concurrent/execSvc1");
        runTest("testNoJEEMetadataContextFromEJB", null);

        // Update the nested contextService
        contextSvc.getJEEMetadataContexts().add(new JEEMetadataContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testJEEMetadataContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContextFromEJB", null);

        // Switch to a top level contextService
        ContextService contextSvc1 = new ContextService();
        contextSvc1.setId("contextSvc1");
        contextSvc1.getJEEMetadataContexts().add(new JEEMetadataContext());
        config.getContextServices().add(contextSvc1);
        execSvc1.getContextServices().clear();
        execSvc1.setContextServiceRef(contextSvc1.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testNoClassloaderContext", "concurrent/execSvc1");
        runTest("testJEEMetadataContext", "concurrent/execSvc1");
    }

    /**
     * Should be possible to make modifications to which concurrency policy a managed executor points at via its longRunningPolicyRef
     * without interfering with tasks that are in-progress or queued. For this test, we set max concurrency to 1
     * and submit two long running tasks, where the first depends on the second, such that the first is blocked while running and
     * the second is stuck in the queue - a temporary deadlock of the executor. Then, we switch which concurrency
     * policy the managed executor's longRunningPolicyRef points to and verify that we can submit and run tasks per the new policy.
     * Then, we increase max concurrency of the old policy 2 and expect both of the previous tasks to complete successfully.
     */
    @Test
    public void testLongRunningPolicyRef() throws Exception {
        // Add: <concurrencyPolicy id="longRunningPolicy" max="1"/>
        ServerConfiguration config = server.getServerConfiguration();
        ConcurrencyPolicy longRunningPolicy = new ConcurrencyPolicy();
        longRunningPolicy.setId("longRunningPolicy");
        longRunningPolicy.setMax("1");
        config.getConcurrencyPolicies().add(longRunningPolicy);
        // Add: <managedExecutorService jndiName="concurrent/execSvc1" longRunningPolicyRef="longRunningPolicy"/>
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setId("execSvc1");
        execSvc1.setJndiName("concurrent/execSvc1");
        execSvc1.setLongRunningPolicyRef(longRunningPolicy.getId());
        config.getManagedExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This leaves 1 future running and another stuck in the queue
        runTest("testTask1BlockedByTask2LongRunning", "concurrent/execSvc1");

        // Remove the longRunningPolicyRef to switch to a different concurrency policy (defaults to the normal concurrency policy).
        // Should be able to submit more tasks on that policy.
        // Update: <managedExecutorService jndiName="concurrent/execSvc1"/>
        execSvc1.setLongRunningPolicyRef(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testLongRunningTaskSuccessful", "concurrent/execSvc1");

        // Increase max concurrency of the concurrencyPolicy that the managed executor no longer points to
        longRunningPolicy.setMax("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This verifies that the previously blocked tasks complete successfully
        runTest("testTask1BlockedByTask2LongRunningCompleted", "concurrent/execSvc1");
    }

    /**
     * Should be possible to make modifications to a concurrencyPolicy that is nested under a managed executor
     * without interfering with tasks that are in-progress or queued. For this test, we set max concurrency to 1
     * and submit two tasks, where the first depends on the second, such that the first is blocked while running and
     * the second is stuck in the queue - a temporary deadlock of the executor. Then, we increase max concurrency to 2
     * and expect both tasks to complete successfully.
     */
    @Test
    public void testNestedConcurrencyPolicy() throws Exception {
        // Add:
        // <managedExecutorService jndiName="concurrent/execSvc1">
        //   <concurrencyPolicy max="1"/>
        // </managedExecutorService>
        ServerConfiguration config = server.getServerConfiguration();
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setJndiName("concurrent/execSvc1");
        ConcurrencyPolicy policy = new ConcurrencyPolicy();
        policy.setMax("1");
        execSvc1.getConcurrencyPolicies().add(policy);
        config.getManagedExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This leaves 1 future running and another stuck in the queue
        runTest("testTask1BlockedByTask2", "concurrent/execSvc1");

        // Increase max concurrency of the nested concurrencyPolicy
        policy.setMax("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This verifies that the previously blocked tasks complete successfully
        runTest("testTask1BlockedByTask2Completed", "concurrent/execSvc1");
    }

    /**
     * Should be possible to make modifications to a longRunningPolicy that is nested under a managed executor
     * without interfering with tasks that are in-progress or queued. For this test, we set max concurrency to 1
     * and submit two tasks, where the first depends on the second, such that the first is blocked while running and
     * the second is stuck in the queue - a temporary deadlock of the executor. At this point, we can still submit
     * normal tasks (not long running) per the normal concurrencyPolicy. Then, we increase the long running policy's
     * max concurrency to 2 and expect both long running tasks to complete successfully.
     */
    @Test
    public void testNestedLongRunningPolicy() throws Exception {
        // Add:
        // <managedExecutorService jndiName="concurrent/execSvc1">
        //   <longRunningPolicy max="1"/>
        // </managedExecutorService>
        ServerConfiguration config = server.getServerConfiguration();
        ManagedScheduledExecutorService execSvc1 = new ManagedScheduledExecutorService();
        execSvc1.setJndiName("concurrent/execSvc1");
        ConcurrencyPolicy policy = new ConcurrencyPolicy();
        policy.setMax("1");
        execSvc1.getLongRunningPolicies().add(policy);
        config.getManagedScheduledExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This leaves 1 future running and another stuck in the queue
        runTest("testTask1BlockedByTask2LongRunning", "concurrent/execSvc1");

        // Can still submit normal tasks, per a different concurrency policy
        runTest("testTaskSuccessful", "concurrent/execSvc1");

        // Increase max concurrency of the nested concurrencyPolicy
        policy.setMax("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This verifies that the previously blocked tasks complete successfully
        runTest("testTask1BlockedByTask2LongRunningCompleted", "concurrent/execSvc1");
    }

    /**
     * Should be possible to make modifications to which concurrency policy a managed executor points at
     * without interfering with tasks that are in-progress or queued. For this test, we set max concurrency to 1
     * and submit two tasks, where the first depends on the second, such that the first is blocked while running and
     * the second is stuck in the queue - a temporary deadlock of the executor. Then, we switch which concurrency
     * policy the managed executor points to and verify that we can submit and run tasks per the new policy.
     * Then, we increase max concurrency of the old policy 2 and expect both of the previous tasks to complete successfully.
     */
    @Test
    public void testPolicyRef() throws Exception {
        // Add: <concurrencyPolicy id="normalPolicy" max="1"/>
        ServerConfiguration config = server.getServerConfiguration();
        ConcurrencyPolicy normalPolicy = new ConcurrencyPolicy();
        normalPolicy.setId("normalPolicy");
        normalPolicy.setMax("1");
        config.getConcurrencyPolicies().add(normalPolicy);
        // Add: <concurrencyPolicy id="policy1" max="1"/>
        ConcurrencyPolicy policy1 = new ConcurrencyPolicy();
        policy1.setId("policy1");
        policy1.setMax("1");
        config.getConcurrencyPolicies().add(policy1);
        // Add: <managedExecutorService jndiName="concurrent/execSvc1" concurrencyPolicyRef="normalPolicy"/>
        ManagedExecutorService execSvc1 = new ManagedExecutorService();
        execSvc1.setId("execSvc1");
        execSvc1.setJndiName("concurrent/execSvc1");
        execSvc1.setConcurrencyPolicyRef(normalPolicy.getId());
        config.getManagedExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This leaves 1 future running and another stuck in the queue
        runTest("testTask1BlockedByTask2", "concurrent/execSvc1");

        // Switch to a different concurrency policy. Should be able to submit more tasks on that policy.
        // Update: <managedExecutorService jndiName="concurrent/execSvc1" concurrencyPolicyRef="policy1"/>
        execSvc1.setConcurrencyPolicyRef(policy1.getId());

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest("testTaskSuccessful", "concurrent/execSvc1");

        // Increase max concurrency of the concurrencyPolicy that the managed executor no longer points to
        normalPolicy.setMax("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This verifies that the previously blocked tasks complete successfully
        runTest("testTask1BlockedByTask2Completed", "concurrent/execSvc1");
    }

    /**
     * Verify that both long running as well as normal tasks submitted by a managed executor are canceled
     * when the managed executor deactivates. We test this by submitting 2 normal tasks and 2 long running
     * tasks to a managed executor with concurrency policies that only allow 1 to run at time, and where the
     * first task blocks when it runs, leaving the other tasks stuck in the queue. Then we remove the configuration
     * of the managed executor to make it deactivate and verify that all of the task Futures are canceled.
     */
    @Test
    public void testTasksCanceledOnDeactivate() throws Exception {
        // Add:
        // <managedExecutorService jndiName="concurrent/execSvc1" concurrencyPolicy="normalPolicy" longRunningPolicyRef="longrunPolicy"/>
        // <concurrencyPolicy id="normalPolicy" max="1" maxQueueSize="5"/>
        // <concurrencyPolicy id="longrunPolicy" max="1" maxQueueSize="1" maxWaitForEnqueue="5m"/>
        ServerConfiguration config = server.getServerConfiguration();
        ConcurrencyPolicy longrunPolicy = new ConcurrencyPolicy();
        longrunPolicy.setId("longrunPolicy");
        longrunPolicy.setMax("1");
        longrunPolicy.setMaxQueueSize("1");
        longrunPolicy.setMaxWaitForEnqueue("5m");
        ConcurrencyPolicy normalPolicy = new ConcurrencyPolicy();
        normalPolicy.setId("normalPolicy");
        normalPolicy.setMax("1");
        normalPolicy.setMaxQueueSize("5");
        config.getConcurrencyPolicies().add(longrunPolicy);
        config.getConcurrencyPolicies().add(normalPolicy);
        ManagedScheduledExecutorService execSvc1 = new ManagedScheduledExecutorService();
        execSvc1.setJndiName("concurrent/execSvc1");
        execSvc1.setConcurrencyPolicyRef(normalPolicy.getId());
        execSvc1.setLongRunningPolicyRef(longrunPolicy.getId());
        config.getManagedScheduledExecutorServices().add(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // This leaves 1 future running and another stuck in the queue for normal tasks
        runTest("testTask1BlockedByTask2", "concurrent/execSvc1");

        // This leaves 1 future running and another stuck in the queue for long running tasks
        runTest("testTask1BlockedByTask2LongRunning", "concurrent/execSvc1");

        // Delete the managedScheduledExecutorService configuration
        config.getManagedScheduledExecutorServices().remove(execSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // Tasks should be canceled
        runTest("testTask1BlockedByTask2Canceled", "concurrent/execSvc1");
        runTest("testTask1BlockedByTask2LongRunningCanceled", "concurrent/execSvc1");
    }

    @Test
    public void testTransactionContext() throws Exception {
        runTest("testNoTransactionContext", "java:comp/DefaultManagedExecutorService");
        runTest("testTransactionContext", "java:comp/DefaultManagedExecutorService");
        restoreSavedConfig = false;
    }
}