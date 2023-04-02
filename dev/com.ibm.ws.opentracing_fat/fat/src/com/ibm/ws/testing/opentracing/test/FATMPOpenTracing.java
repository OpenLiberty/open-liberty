/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.testing.opentracing.test;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNull;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.Assert;

/**
 * <p>Test that a JAXRS application with @Traced(false) works.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testHelloWorld()}</li>
 * </ul>
 */
@Mode(TestMode.FULL)
public class FATMPOpenTracing {
    /**
     * For tracing.
     */
    private static final Class<?> CLASS = FATMPOpenTracing.class;

    /**
     * Set to the generated server before any tests are run.
     */
    private static LibertyServer server;

    /**
     * Deploy the application and start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("opentracingFATServer3");
        WebArchive serviceWar = ShrinkWrap.create(WebArchive.class, "mpOpenTracing.war");
        serviceWar.addPackages(true, "com.ibm.ws.testing.mpOpenTracing");
        serviceWar.addAsWebInfResource(
                                       new File("test-applications/mpOpenTracing/resources/beans.xml"));
        ShrinkHelper.exportAppToServer(server, serviceWar);
        server.startServer();
    }

    /**
     * Stop the server.
     * 
     * @throws Exception Errors stopping the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Execute the Hello World JAXRS service and ensure it returns the expected response.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testHelloWorld() throws Exception {
        String methodName = "testHelloWorld";

        List<String> actualResponseLines = executeWebService("helloWorld");

        FATLogging.info(CLASS, methodName, "Actual Response", actualResponseLines);

        Assert.assertEquals(1, actualResponseLines.size());
        Assert.assertEquals("Hello World", actualResponseLines.get(0));

        actualResponseLines = executeWebService("getTracerState");
        
        String tracerState = "";
        for (String actualResponseLine : actualResponseLines) {
            tracerState += actualResponseLine;
        }

        int expectedSpans = 2;
        int spanCount = getSpanCount(tracerState);
        if (spanCount != expectedSpans) {
            Assert.assertEquals("Expected " + expectedSpans + " spans but found " + spanCount + ":", tracerState);
        }
    }

    @Test
    public void testNotFoundRemoval() throws Exception {
        String methodName = "testNotFoundRemoval";
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        try {
            executeWebService("notFoundRemoval");
        } catch (TestAppException tae) {
            FATLogging.info(CLASS, methodName, "Expected exception", tae);
        } catch (Exception ex) {
            FATLogging.info(CLASS, methodName, "Unexpected exception", ex);
            ex.printStackTrace();
        }

        String line = server.waitForStringInLog("HTTP 404 Not Found", 15000, consoleLogFile);
        assertNull("HTTP 404 Not Found exception appeared in the logs", line);
    }

    protected List<String> executeWebService(String method) throws Exception {
        String requestUrl = "http://" +
                            server.getHostname() + ":" +
                            server.getHttpDefaultPort() +
                            "/mpOpenTracing/rest/ws/" + method;

        return FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
    }
    
    protected int getSpanCount(String tracerState) {
        int result = 0;
        int i = 0;
        i = tracerState.indexOf("spanId", i);
        while (i != -1) {
            result++;
            i = tracerState.indexOf("spanId", i + 1);
        }
        return result;
    }
}
