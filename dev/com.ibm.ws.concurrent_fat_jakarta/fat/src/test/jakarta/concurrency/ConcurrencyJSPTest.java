/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import componenttest.topology.utils.HttpUtils;
import junit.framework.Assert;

/**
 * Basic tests to cover EE Concurrency inside a JSP.
 * Demonstrates security context propagation, by using basic security on the web app.
 * Calls to the webapp are made with the username 'concurrency' and password 'password'
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrencyJSPTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.jakarta.jsp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        HttpUtils.setDefaultAuth("concurrency", "password");

        JavaArchive ConcurrencyWebFragment = ShrinkHelper.buildJavaArchive("ConcurrencyWebFragment");
        ShrinkHelper.addDirectory(ConcurrencyWebFragment, "test-applications/ConcurrencyWebFragment/resources");

        WebArchive ConcurrencyJSPTestApp = ShrinkHelper.buildDefaultApp("ConcurrencyJSPTestApp")
                        .addAsLibrary(ConcurrencyWebFragment);
        ShrinkHelper.addDirectory(ConcurrencyJSPTestApp, "test-applications/ConcurrencyJSPTestApp/resources");

        ShrinkHelper.exportAppToServer(server, ConcurrencyJSPTestApp);

        server.startServer();

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    /**
     * Configure a managed-scheduled-executor within a web fragment and verify that its
     * max-async (1) and context propagation settings are honored.
     */
    @Test
    public void maxAsyncTest() throws Exception {
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/MaxAsync.jsp").trim());
    }

    /**
     * Uses the default ContextService, which propagates security, to confirm that a ContextService can propagate security context inside a JSP
     */
    @Test
    public void securityPropogatedTest() throws Exception {
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/SecurityPropagated.jsp").trim());
    }

    /**
     * Uses java:app/concurrent/securityClearedContextSvc, to confirm that a ContextService can clear security context inside a JSP
     */
    @Test
    public void securityClearedTest() throws Exception {
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/SecurityCleared.jsp").trim());
    }

    /**
     * Uses java:app/concurrent/securityUnchangedContextSvc, to confirm that a ContextService can leave security context unchanged inside a JSP.
     * Tests both on the thread to confirm Security context is available, and on a separate thread to confirm it is not propagated.
     */
    @Test
    public void securityUnchangedTest() throws Exception {
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/SecurityUnchanged.jsp").trim());
    }

    /**
     * Configure a managed-thread-factory within a web fragment and verify that its
     * priority (7) and context propagation settings are honored.
     */
    @Test
    public void threadPriorityTest() throws Exception {
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/ThreadPriority.jsp").trim());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS4000E"); //Ignore CWWKS4000E just in case.
    }
}
