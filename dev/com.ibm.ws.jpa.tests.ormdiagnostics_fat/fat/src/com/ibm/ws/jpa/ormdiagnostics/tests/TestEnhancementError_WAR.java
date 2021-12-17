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
package com.ibm.ws.jpa.ormdiagnostics.tests;

import java.util.HashSet;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.ormdiagnostics.FATSuite;
import com.ibm.ws.ormdiag.enhancementerror.war.EnhancementErrorServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import junit.framework.Assert;

@RunWith(FATRunner.class)
public class TestEnhancementError_WAR extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "enhancementErrorWAR";
    private final static String RESOURCE_ROOT = "test-applications/enhancementerror/";
    private final static String appName = "enhancementErrorWAR";

    private static long timestart = 0;

    @Server("JPAORMServer")
    @TestServlets({
                    @TestServlet(servlet = EnhancementErrorServlet.class, path = CONTEXT_ROOT + "/" + "EnhancementErrorServlet")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestExample_EAR.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        server.startServer();

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {

        //Create a WAR bundle with JPA bundled in it
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + "_WAR.war");
        webApp.addPackages(true, "com.ibm.ws.ormdiag.enhancementerror.jpa");
        webApp.addPackages(true, "com.ibm.ws.ormdiag.enhancementerror.war");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "resources/war");

        ShrinkHelper.exportToServer(server, "apps", webApp);

        Application appRecord = new Application();
        appRecord.setLocation(appName + "_WAR.war");
        appRecord.setName(appName);
//        ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
//        ClassloaderElement loader = new ClassloaderElement();
//        loader.setApiTypeVisibility("+third-party");
////        loader.getCommonLibraryRefs().add("HibernateLib");
//        cel.add(loader);

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "CWWKC0044W",
                              "CWWJP9992E",
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + appName + "_WAR.war");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestExample_WAR.class, timestart);
        }
    }

    @Test
    public void testInvalidFormatClassError() throws Exception {
        Assert.assertTrue(server.defaultTraceFileExists());

        FATServletClient.runTest(server, appName + "/EnhancementErrorServlet", "testInvalidFormatClassError");

        String hexText = "Before Class Transform: Bytecode for class com.ibm.ws.ormdiag.enhancementerror.jpa.BadClass";

        List<String> traceEntry = server.findStringsInTrace(hexText);
        Assert.assertTrue(traceEntry.size() > 0);
    }

}