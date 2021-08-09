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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
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
 * <p>Test that Jaeger for MicroProfile OpenTracing works.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testTraceCreated()}</li>
 * <li>{@link #testImproperConfig()}</li>
 * </ul>
 */
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 8)
public class JaegerConfigTest {
	
    private static final Class<?> CLASS = JaegerConfigTest.class;

    @Server("jaegerProperConfig")
    private static LibertyServer server1;

    @Server("jaegerServerImproper")
    private static LibertyServer server2;
    
    @Server("jaegerServerLibInWar")
    private static LibertyServer server3;
    
    private static LibertyServer currentServer;
    
    /**
     * Deploy the application and start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server1 = LibertyServerFactory.getLibertyServer("jaegerServer1");
        server2 = LibertyServerFactory.getLibertyServer("jaegerServer2");
        server3 = LibertyServerFactory.getLibertyServer("jaegerServer3");
        WebArchive serviceWar = ShrinkWrap.create(WebArchive.class, "mpOpenTracing.war");
        serviceWar.addPackages(true, "io.openliberty.microprofile.opentracing.internal.testing.mpOpenTracing");
        serviceWar.addAsWebInfResource(
                                       new File("test-applications/mpOpenTracing/resources/beans.xml"));
        WebArchive serviceWarWithLib = ShrinkWrap.create(WebArchive.class, "mpOpenTracing.war");
        serviceWarWithLib.addPackages(true, "io.openliberty.microprofile.opentracing.internal.testing.mpOpenTracing");
        serviceWarWithLib.addAsWebInfResource(
                                       new File("test-applications/mpOpenTracing/resources/beans.xml")); 
        File libsDir = new File("lib");
        File[] libs = libsDir.listFiles();
        for (File file : libs) {
            server1.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
            // We are not copying the library to server2 for improper config test
            // Adding library jars to war file for testLibraryInWar
            serviceWarWithLib.addAsLibrary(file);
        }
        ShrinkHelper.exportAppToServer(server1, serviceWar);
        ShrinkHelper.exportAppToServer(server2, serviceWar);
        ShrinkHelper.exportAppToServer(server3, serviceWarWithLib);
    }
    
    /**
     * Create traces with a proper Jaeger configuration.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testTraceCreated() throws Exception {
    	server1.startServer();
    	currentServer = server1;
        String methodName = "testTraceCreated";
        List<String> actualResponseLines = executeWebService(server1, "helloWorld");

        FATLogging.info(CLASS, methodName, "Actual Response", actualResponseLines);

        String logMsg = server1.waitForStringInLog("INFO io.jaegertracing");
        FATLogging.info(CLASS, methodName, "Actual Response", logMsg);
        Assert.assertNotNull(logMsg);
    }
    
    /**
     * Ensure exception is thrown when Jaeger libraries are not defined in server.xml.
     * 
     * @throws Exception Errors executing the service.
     */
    
    @Test
    public void testImproperConfig() throws Exception {
    	server2.startServer();
    	currentServer = server2;
        String methodName = "testImproperConfig";
        try {
        	executeWebService(server2, "helloWorld");
        } catch (IOException e) {
        	// Error should be thrown when hitting endpoint
        }
        String logMsg = server2.waitForStringInLog("CWMOT0010W");
        FATLogging.info(CLASS, methodName, "Actual Response", logMsg);
        Assert.assertNotNull(logMsg);
    }
    
    /**
     * Test Jaeger configuration with library in war file
     * 
     */
    @Test
    public void testLibraryInWar() throws Exception {
        server3.startServer();
        currentServer = server3;
        String methodName = "testLibraryInWar";
        List<String> actualResponseLines = executeWebService(server3, "helloWorld");;
        
        FATLogging.info(CLASS, methodName, "Actual Response", actualResponseLines);
          
        String logMsg = server3.waitForStringInLog("INFO io.jaegertracing");
        FATLogging.info(CLASS, methodName, "Actual Response", logMsg);
        Assert.assertNotNull(logMsg);
    }

    protected List<String> executeWebService(LibertyServer server, String method) throws Exception {
        String requestUrl = "http://" +
                            server.getHostname() + ":" +
                            server.getHttpDefaultPort() +
                            "/mpOpenTracing/rest/ws/" + method;

        return FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
    }
    
    @After
    public void tearDown() throws Exception {
    	if (currentServer != null && currentServer.isStarted()) {
        	currentServer.stopServer("CWMOT0009W", "CWMOT0010W");
        }
    }
    
    @AfterClass
    public static void shutdown() throws Exception {
    	LibertyServer[] serversToShutDown = {server1, server2, server3};
    	for (LibertyServer server : serversToShutDown) {
        	if (server != null && server.isStarted()) {
        		server.stopServer("CWMOT0009W", "CWMOT0010W");
            }
    	}
    }
}
