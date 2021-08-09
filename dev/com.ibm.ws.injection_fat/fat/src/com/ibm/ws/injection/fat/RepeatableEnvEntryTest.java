/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case ensures that repeated @Resource can be declared and inject an
 * env-entry for all the boxed types (Boolean, Integer, etc.) into the fields and
 * methods of servlets, listeners, and filters. To perform the test, a servlet is
 * invoked in the web module with a listener or filter declared in the web.xml.
 * The expected result is that the servlet, listener, or filter is created and injected
 * with the values specified in the ibm-web-bnd.xml.
 *
 * @author bmdecker
 *
 */
@RunWith(FATRunner.class)
public class RepeatableEnvEntryTest extends FATServletClient {

    private static final String SERVLET_BASIC_PRIM = "RepeatableEnvEntryMixWeb/BasicRepeatableEnvMixPrimServlet";
    private static final String SERVLET_BASIC_OBJ = "RepeatableEnvEntryMixWeb/BasicRepeatableEnvMixObjServlet";
    private static final String SERVLET_ADV_PRIM = "RepeatableEnvEntryMixWeb/AdvRepeatableEnvMixPrimServlet";
    private static final String SERVLET_ADV_OBJ = "RepeatableEnvEntryMixWeb/AdvRepeatableEnvMixObjServlet";

    @Server("com.ibm.ws.injection.fat.RepeatableEnvEntryServer")
//    @TestServlets({ @TestServlet(servlet = BasicRepeatableEnvMixPrimServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
//                    @TestServlet(servlet = BasicRepeatableEnvMixObjServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
//                    @TestServlet(servlet = AdvRepeatableEnvMixPrimServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
//                    @TestServlet(servlet = AdvRepeatableEnvMixObjServlet.class, contextRoot = "RepeatableEnvEntryMixWeb")
//    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.RepeatableEnvEntryServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.injection.fat.RepeatableEnvEntryServer").fullFATOnly());

    @BeforeClass
    public static void setUp() throws Exception {
        // Because of a bug / gap in function in the JDK 11 compiler, we cannot compile these applications with a source=8
        // and also override the bootclasspath to use javax.annotation 1.3. For this reason, we are going to check in the
        // app as binaries compiled on Java 8 so we can continue to have coverage on JDK 8+
        // If these apps ever need to be changed:
        //   1) add the app path back to the 'src' list in bnd.bnd
        //   2) add the app path back to the .classpath
        //   3) un-comment the Shrinkwrap code to build the app in the respective test class

//        // Use ShrinkHelper to build the ears
//        WebArchive RepeatableEnvEntryMixWeb = ShrinkHelper.buildDefaultApp("RepeatableEnvEntryMixWeb.war", "com.ibm.ws.injection.repeatable.envmix.web.");
//        EnterpriseArchive RepeatableEnvEntryMixTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableEnvEntryMixTest.ear");
//        RepeatableEnvEntryMixTest.addAsModule(RepeatableEnvEntryMixWeb);
//
//        ShrinkHelper.exportDropinAppToServer(server, RepeatableEnvEntryMixTest);

        // Since not using ShrinkWrap, manually transform the application if required
        if (JakartaEE9Action.isActive()) {
            transformJakartaEE9App(server, "dropins", "RepeatableEnvEntryMixTest.ear");
        }

        server.addInstalledAppForValidation("RepeatableEnvEntryMixTest");

        server.startServer();
    }

    private static void transformJakartaEE9App(LibertyServer server, String path, String filename) throws Exception {
        String localLocation = "publish/servers/" + server.getServerName() + "/" + path;

        Path localAppPath = Paths.get(localLocation + "/" + filename);
        JakartaEE9Action.transformApp(localAppPath);

        server.copyFileToLibertyServerRoot(localLocation, path, filename);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private final void runTest(String path) throws Exception {
        FATServletClient.runTest(server, path, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableEnvPrimFldMixInjection() throws Exception {
        runTest(SERVLET_BASIC_PRIM);
    }

    @Test
    public void testRepeatableEnvPrimJNDIClassLevelResourceMixLookup() throws Exception {
        runTest(SERVLET_BASIC_PRIM);
    }

    @Test
    public void testRepeatableEnvPrimMthdMixInjection() throws Exception {
        runTest(SERVLET_BASIC_PRIM);
    }

    @Test
    public void testRepeatableEnvObjFldMixInjection() throws Exception {
        runTest(SERVLET_BASIC_OBJ);
    }

    @Test
    public void testRepeatableEnvObjJNDIClassLevelResourceMixLookup() throws Exception {
        runTest(SERVLET_BASIC_OBJ);
    }

    @Test
    public void testRepeatableEnvObjMthdMixInjection() throws Exception {
        runTest(SERVLET_BASIC_OBJ);
    }

    @Test
    public void testRepeatableEnvMixPrimHttpSessionAttributeListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimHttpSessionListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimRequestListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimServletContextAttributeListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimServletContextListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimServletFilter() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixPrimServletRequestAttributeListener() throws Exception {
        runTest(SERVLET_ADV_PRIM);
    }

    @Test
    public void testRepeatableEnvMixObjHttpSessionAttributeListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjHttpSessionListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjRequestListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjServletContextAttributeListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjServletContextListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjServletFilter() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

    @Test
    public void testRepeatableEnvMixObjServletRequestAttributeListener() throws Exception {
        runTest(SERVLET_ADV_OBJ);
    }

}