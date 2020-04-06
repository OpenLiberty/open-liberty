/*******************************************************************************
 * Copyright (c) 2017 IBM Corpo<ration and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
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
@MinimumJavaLevel(javaLevel = 8)
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
