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
 * <p>Test that a JAXRS application works even if the opentracing
 * features is enabled without a Tracer.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testHelloWorld()}</li>
 * </ul>
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 1.8)
public class FATOpentracingHelloWorld {
    /**
     * For tracing.
     */
    private static final Class<?> CLASS = FATOpentracingHelloWorld.class;

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
        server = LibertyServerFactory.getLibertyServer("opentracingFATServer2");
        WebArchive serviceWar = ShrinkWrap.create(WebArchive.class, "jaxrsHelloWorld.war");
        serviceWar.addPackages(true, "com.ibm.ws.testing.opentracing.jaxrsHelloWorld");
        serviceWar.addAsWebInfResource(
                                       new File("test-applications/jaxrsHelloWorld/resources/beans.xml"));
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

        // com.ibm.ws.opentracing.OpentracingUserFeatureAccessService   E CWMOT0001E: Invocation of user supplied OpentracingTracerFactory.newInstance(...) method failed with Exception. Message = null
        // com.ibm.ws.opentracing.OpentracingContainerFilter            E CWMOT0002E: No Tracer is available for an inbound request. The inbound request will not be correlated with the upstream service.
        // com.ibm.ws.opentracing.OpentracingContainerFilter            E CWMOT0003E: The Span created for an inbound request is not available for the response to the request. The inbound request will not be correlated the upstream service.

        server.stopServer("CWMOT0001E", "CWMOT0002E", "CWMOT0003E");
    }

    /**
     * Execute the Hello World JAXRS service and ensure it returns the expected response.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testHelloWorld() throws Exception {
        String methodName = "testHelloWorld";

        String requestUrl = "http://" +
                            server.getHostname() + ":" +
                            server.getHttpDefaultPort() +
                            "/jaxrsHelloWorld/rest/ws/helloWorld";

        List<String> actualResponseLines = FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);

        FATLogging.info(CLASS, methodName, "Actual Response", actualResponseLines);

        Assert.assertEquals(1, actualResponseLines.size());
        Assert.assertEquals("Hello World", actualResponseLines.get(0));
    }
}
