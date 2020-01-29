/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing.jaeger_fat;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>Test that Jaeger for MicroProfile OpenTracing is able to send sequential spans between servers.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testMultiTrace()}</li>
 * </ul>
 */
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 8)

public class JaegerTraceTest {
    private static final Class<?> CLASS = JaegerConfigTest.class;

    @Server("jaegerServerSystem")
    private static LibertyServer server1;
    
    @Server("jaegerServerInventory")
    private static LibertyServer server2;
    
    private static LibertyServer currentServer;
    
    /**
     * Deploy the application and start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server1 = LibertyServerFactory.getLibertyServer("jaegerServerSystem");
        server2 = LibertyServerFactory.getLibertyServer("jaegerServerInventory");
        
        WebArchive systemWar = ShrinkWrap.create(WebArchive.class, "system.war");
        WebArchive inventoryWar = ShrinkWrap.create(WebArchive.class, "inventory.war");
        
        systemWar.addPackages(true, "io.openliberty.guides.system");
        inventoryWar.addPackages(true, "io.openliberty.guides.inventory");

        ShrinkHelper.exportAppToServer(server1, systemWar);
        ShrinkHelper.exportAppToServer(server2, inventoryWar);
        
        File libsDir = new File("lib");
        File[] libs = libsDir.listFiles();
        for (File file : libs) {
            server1.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
            server2.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (currentServer != null && currentServer.isStarted()) {
            currentServer.stopServer("CWMOT0009W", "CWMOT0010W");
        }
    }
    
    /**
     * Check that spans are created across multiple servers.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testMultiSpans() throws Exception {
        server1.startServer();
        server2.startServer();
        
        currentServer = server2;
        String methodName = "testMultiSpans";
        
        // Both servers should have a tracer created when hitting the localhost endpoint on the Inventory server
        List<String> actualResponseLines = executeWebService(server2, "inventory/systems/localhost");

        FATLogging.info(CLASS, methodName, "Actual Response", actualResponseLines);
        
        String logMsg = server1.waitForStringInLog("INFO io.jaegertracing");
        String logMsg2 = server2.waitForStringInLog("INFO io.jaegertracing");
        FATLogging.info(CLASS, methodName, "Actual Response", logMsg);
        FATLogging.info(CLASS, methodName, "Actual Response", logMsg2);
        server1.stopServer();
        
        Assert.assertNotNull(logMsg);
        Assert.assertNotNull(logMsg2);
    }

    protected List<String> executeWebService(LibertyServer server, String method) throws Exception {
        String requestUrl = "http://" +
                            server.getHostname() +
                            ":" + server.getHttpSecondaryPort() + 
                            "/" + method;

        return FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
    }
}
