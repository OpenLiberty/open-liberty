/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class RepeatableDSDTest extends FATServletClient {

    private static final String APP_ANN_WEB = "RepeatableDSDAnnWeb/BasicRepeatableDSDAnnServlet";
    private static final String APP_MIX_WEB = "RepeatableDSDMixWeb/BasicRepeatableDSDMixServlet";
    private static final String APP_XML_WEB = "RepeatableDSDXMLWeb/BasicRepeatableDSDXMLServlet";

    @Server("com.ibm.ws.injection.fat.RepeatableDSDServer")
//    @TestServlets({ @TestServlet(servlet = BasicRepeatableDSDAnnServlet.class, contextRoot = "RepeatableDSDAnnWeb"),
//                    @TestServlet(servlet = BasicRepeatableDSDMixServlet.class, contextRoot = "RepeatableDSDMixWeb"),
//                    @TestServlet(servlet = BasicRepeatableDSDXMLServlet.class, contextRoot = "RepeatableDSDXMLWeb")
//    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(new JakartaEE9Action().fullFATOnly().forServers("com.ibm.ws.injection.fat.RepeatableDSDServer"));

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
//        JavaArchive RepeatableDSDAnnEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDAnnEJB.jar", "com.ibm.ws.injection.repeatable.dsdann.ejb.");
//        WebArchive RepeatableDSDAnnWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDAnnWeb.war", "com.ibm.ws.injection.repeatable.dsdann.web.");
//        EnterpriseArchive RepeatableDSDAnnTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDAnnTest.ear");
//        RepeatableDSDAnnTest.addAsModule(RepeatableDSDAnnEJB).addAsModule(RepeatableDSDAnnWeb);
//
//        JavaArchive RepeatableDSDMixEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDMixEJB.jar", "com.ibm.ws.injection.repeatable.dsdmix.ejb.");
//        WebArchive RepeatableDSDMixWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDMixWeb.war", "com.ibm.ws.injection.repeatable.dsdmix.web.");
//        EnterpriseArchive RepeatableDSDMixTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDMixTest.ear");
//        RepeatableDSDMixTest.addAsModule(RepeatableDSDMixEJB).addAsModule(RepeatableDSDMixWeb);
//
//        JavaArchive RepeatableDSDXMLEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDXMLEJB.jar", "com.ibm.ws.injection.repeatable.dsdxml.ejb.");
//        WebArchive RepeatableDSDXMLWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDXMLWeb.war", "com.ibm.ws.injection.repeatable.dsdxml.web.");
//        EnterpriseArchive RepeatableDSDXMLTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDXMLTest.ear");
//        RepeatableDSDXMLTest.addAsModule(RepeatableDSDXMLEJB).addAsModule(RepeatableDSDXMLWeb);
//
//        ShrinkHelper.exportAppToServer(server, RepeatableDSDAnnTest);
//        ShrinkHelper.exportAppToServer(server, RepeatableDSDMixTest);
//        ShrinkHelper.exportAppToServer(server, RepeatableDSDXMLTest);

        // Since not using ShrinkWrap, manually transform the applications if required
        if (JakartaEE9Action.isActive()) {
            transformJakartaEE9App(server, "apps", "RepeatableDSDAnnTest.ear");
            transformJakartaEE9App(server, "apps", "RepeatableDSDMixTest.ear");
            transformJakartaEE9App(server, "apps", "RepeatableDSDXMLTest.ear");
        }

        server.addInstalledAppForValidation("RepeatableDSDAnnTest");
        server.addInstalledAppForValidation("RepeatableDSDMixTest");
        server.addInstalledAppForValidation("RepeatableDSDXMLTest");

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

    private final void runTest(String path, String test) throws Exception {
        FATServletClient.runTest(server, path, test);
    }

    @Test
    public void testAnnRepeatableDSDAppLevel() throws Exception {
        runTest(APP_ANN_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testAnnRepeatableDSDCompLevel() throws Exception {
        runTest(APP_ANN_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testAnnRepeatableDSDGlobalLevel() throws Exception {
        runTest(APP_ANN_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testAnnRepeatableDSDModLevel() throws Exception {
        runTest(APP_ANN_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testRepeatableDSDAnnOnly() throws Exception {
        runTest(APP_MIX_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableDSDMerge() throws Exception {
        runTest(APP_MIX_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableDSDOverride() throws Exception {
        runTest(APP_MIX_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableDSDXMLOnly() throws Exception {
        runTest(APP_MIX_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testXmlRepeatableDSDAppLevel() throws Exception {
        runTest(APP_XML_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testXmlRepeatableDSDCompLevel() throws Exception {
        runTest(APP_XML_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testXmlRepeatableDSDGlobalLevel() throws Exception {
        runTest(APP_XML_WEB, "testRepeatableDSDAppLevel");
    }

    @Test
    public void testRepeatableDSDMetaDataCompleteAnnOnly() throws Exception {
        runTest(APP_XML_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableDSDMetaDataCompleteValid() throws Exception {
        runTest(APP_XML_WEB, getTestMethodSimpleName());
    }

    @Test
    public void testXmlRepeatableDSDModLevel() throws Exception {
        runTest(APP_XML_WEB, "testRepeatableDSDAppLevel");
    }
}