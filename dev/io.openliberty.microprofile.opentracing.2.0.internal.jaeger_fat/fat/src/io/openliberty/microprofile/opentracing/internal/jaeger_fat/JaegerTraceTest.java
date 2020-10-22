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
package io.openliberty.microprofile.opentracing.internal.jaeger_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    private static final Class<?> CLASS = JaegerTraceTest.class;

    private static LibertyServer systemServer;
    private static LibertyServer inventoryServer;
    private static final String TRACE_ID_KEY = "ext_traceId";
    private static final String SPAN_ID_KEY = "ext_spanId";
    
    /**
     * Deploy the application and start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        systemServer = LibertyServerFactory.getLibertyServer("jaegerServerSystem");
        inventoryServer = LibertyServerFactory.getLibertyServer("jaegerServerInventory");
        inventoryServer.useSecondaryHTTPPort();
        
        WebArchive systemWar = ShrinkWrap.create(WebArchive.class, "system.war");
        WebArchive inventoryWar = ShrinkWrap.create(WebArchive.class, "inventory.war");
        
        systemWar.addPackages(true, "io.openliberty.guides.system");
        inventoryWar.addPackages(true, "io.openliberty.guides.inventory");

        ShrinkHelper.exportAppToServer(systemServer, systemWar);
        ShrinkHelper.exportAppToServer(inventoryServer, inventoryWar);
        
        File libsDir = new File("lib");
        File[] libs = libsDir.listFiles();
        for (File file : libs) {
            systemServer.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
            inventoryServer.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
        }
    }

    /**
     * Check that spans are created across multiple servers.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testMultiSpans() throws Exception {
        systemServer.startServer();
        inventoryServer.startServer();
        
        String methodName = "testMultiSpans";
        
        // Both servers should have a tracer created when hitting the localhost endpoint on the Inventory server
        List<String> responseLines = executeWebService(inventoryServer, "inventory/systems/localhost");

        FATLogging.info(CLASS, methodName, "Response lines:", responseLines);
        
        String logMsg = systemServer.waitForStringInLog("CWMOT1001I");
        String logMsg2 = inventoryServer.waitForStringInLog("CWMOT1001I");
        FATLogging.info(CLASS, methodName, "Assert not null:", logMsg);
        FATLogging.info(CLASS, methodName, "Assert not null:", logMsg2);
        
        Assert.assertNotNull(logMsg);
        Assert.assertNotNull(logMsg2);
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testLogRecordContext() throws Exception {
        String methodName = "testLogRecordContext";

        FATLogging.info(CLASS, methodName, "Start server1");
        systemServer.addEnvVar("WLP_LOGGING_MESSAGE_FORMAT", "json");
        systemServer.addEnvVar("WLP_LOGGING_MESSAGE_LOGLEVEL", "info");
        systemServer.addEnvVar("WLP_LOGGING_MESSAGE_SOURCE", "message");
        systemServer.startServer();

        FATLogging.info(CLASS, methodName, "Start server2");
        inventoryServer.addEnvVar("WLP_LOGGING_MESSAGE_FORMAT", "json");
        inventoryServer.addEnvVar("WLP_LOGGING_MESSAGE_LOGLEVEL", "info");
        inventoryServer.addEnvVar("WLP_LOGGING_MESSAGE_SOURCE", "message");
        inventoryServer.startServer();
        
        List<String> responseLines = executeWebService(inventoryServer, "inventory/systems/localhost");
        FATLogging.info(CLASS, methodName, "Response ", responseLines);

        String systemEvent = systemServer.waitForStringInLog("io\\.openliberty\\.guides\\.system\\.SystemResource");
        assertNotNull("System service event not found", systemEvent);
        FATLogging.info(CLASS, methodName, "System event:", systemEvent);
        assertTrue(TRACE_ID_KEY + " not found", systemEvent.contains(TRACE_ID_KEY));
        assertTrue(SPAN_ID_KEY + " not found", systemEvent.contains(SPAN_ID_KEY));

        String inventoryEvent = inventoryServer.waitForStringInLog("io\\.openliberty\\.guides\\.inventory\\.InventoryResource");
        assertNotNull("Inventory service event not found", inventoryEvent);
        FATLogging.info(CLASS, methodName, "System event:", inventoryEvent);
        assertTrue(TRACE_ID_KEY + " not found", inventoryEvent.contains(TRACE_ID_KEY));
        assertTrue(SPAN_ID_KEY + " not found", inventoryEvent.contains(SPAN_ID_KEY));
    }
    
    protected List<String> executeWebService(LibertyServer server, String method) throws Exception {
        String requestUrl = "http://" +
                            server.getHostname() +
                            ":" + server.getHttpSecondaryPort() + 
                            "/" + method;

        return FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
    }
    
    @After
    public void stopServers() throws Exception {
        if (systemServer.isStarted()) {
            systemServer.stopServer();
        }
        if (inventoryServer.isStarted()) {
            inventoryServer.stopServer("CWMOT0009W", "CWMOT0010W");
        }
    }
}
