/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat.jakarta;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class MPContextProp1_3_EE10_Test extends FATServletClient {

    private static final String APP_NAME = "MPContextProp1_3_EE10_App";

    @Server("com.ibm.ws.concurrent.mp.fat.1.3.ee10")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.v13.ee10.web");

        // This JAR file contains both Jakarta EE and MicroProfile context providers mixed together
        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders2.jar")
                        .addPackage("org.test.ee.context.priority")
                        .addAsServiceProvider(Class.forName("jakarta.enterprise.concurrent.spi.ThreadContextProvider"),
                                              Class.forName("org.test.ee.context.priority.PriorityContextProvider"))
                        .addPackage("org.test.mp.context.priority")
                        .addAsServiceProvider(Class.forName("org.eclipse.microprofile.context.spi.ThreadContextProvider"),
                                              Class.forName("org.test.mp.context.priority.PriorityContextProvider"));
        ShrinkHelper.exportToServer(server, "lib", customContextProviders);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Verify that Asynchronous is not allowed at the class level.
     */
    @Test
    public void testAsynchronousNotAllowedOnClass() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Invoke an asynchronous method that relies on a Jakarta EE ManagedExecutorService
     * while MicroProfile Context Propagation is enabled.
     */
    @Test
    public void testAsyncMethodUsesJakartaEEManagedExecutorService() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Invoke an asynchronous method that uses a resource reference to the
     * default managed executor as a MicroProfile ManagedExecutor.
     */
    @Test
    public void testAsyncMethodUsesResourceRefToManagedExecutor() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Use ExecutorService.close to shut down a MicroProfile ManagedExecutor and await completion of running tasks, if on Java 19 or above.
     * Otherwise, use shutdown and awaitCompletion.
     */
    @Test
    public void testClose() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * ManagedExecutor.copy was added in 1.2 and should still be working.
     * With this method, you should be able to copy from an unmanaged completion stage
     * that lacks context propagation to create a copied stage that uses the
     * managed executor to propagate thread context.
     */
    @Test
    public void testCopy() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Verify that custom thread context types are not propagated by the default ManagedExecutorService.
     */
    @Test
    public void testCustomContextIsNotPropagatedByDefault() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Verify that custom thread context types are propagated if configured on the ContextServiceDefinition.
     */
    @Test
    public void testCustomEEContextIsPropagatedWhenConfigured() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Verify that custom thread context types are propagated if configured on the builder.
     */
    @Test
    public void testCustomMPContextIsPropagatedWhenConfigured() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * java:comp/DefaultManagedExecutorService can be used interchangeably as a MicroProfile ManagedExecutor
     * or as a Jakarta EE ManagedExecutorService.
     */
    @Test
    public void testDefaultManagedExecutorService() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Jakarta EE Concurrency should not interfere with MicroProfile FaultTolerance Asynchronous
     * when only the MicroProfile annotation is present.
     */
    @Test
    public void testFaultToleranceAsyncMethod() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * A method that is annotated with Jakarta EE Concurrency's Asynchronous must fail
     * if the class is annotated with MicroProfile Fault Tolerance Asynchronous.
     */
    @Test
    public void testFaultToleranceClassLevelAsynchronousCollidesWithMethod() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * A method that is annotated with Jakarta EE Concurrency's Asynchronous must fail
     * if the method is also annotated with MicroProfile Fault Tolerance Asynchronous.
     */
    @Test
    public void testFaultToleranceCollidesOnSameAsyncMethod() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }

    /**
     * Use a MicroProfile ManagedExecutor and a Jakarta EE ManagedExecutorService
     * to create completion stages and combine the two to create dependent stages,
     * ensuring that each dependent stage runs with the configured context propagation
     * of the managed executor of the stage that creates the dependent stage.
     */
    @Test
    public void testIntermixMicroProfileAndJakartaEECompletionStages() throws Exception {
        runTest(server, APP_NAME + "/MPContextProp1_3_EE10_TestServlet", testName);
    }
}
