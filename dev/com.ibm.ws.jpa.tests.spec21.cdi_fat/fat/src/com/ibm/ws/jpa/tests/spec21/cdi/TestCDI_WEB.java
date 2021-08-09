/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec21.cdi;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.fvt.cdi.jpalib.web.JPACDIJPALibServlet;
import com.ibm.ws.jpa.fvt.cdi.simple.web.JPACDISimpleServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class TestCDI_WEB extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "TestCDISimple";
    private final static String CONTEXT_ROOT_JPALIB = "TestCDIWithJPALib";
    private final static String RESOURCE_ROOT = "test-applications/CDI/";
    private final static String appFolder = "apps";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA_CDI_DROP_${dbvendor}.ddl");
        createSet.add("JPA_CDI_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA21CDIServer")
    @TestServlets({
                    @TestServlet(servlet = JPACDISimpleServlet.class, path = CONTEXT_ROOT + "/" + "JPACDISimpleServlet"),
                    @TestServlet(servlet = JPACDIJPALibServlet.class, path = CONTEXT_ROOT_JPALIB + "/" + "JPACDIJPALibServlet"),

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestCDI_WEB.class);
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

        setupDatabaseApplication(server, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        Application simpleAppRecord = setupCDISimpleTestApplication();
        Application jpaLibAppRecord = setupJPALibTestApplication();

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(simpleAppRecord);
        sc.getApplications().add(jpaLibAppRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("TestCDISimple");
        appNamesSet.add("TestCDIWithJPALib");
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static Application setupCDISimpleTestApplication() throws Exception {
        final String appName = "TestCDISimple";
        final String appNameEar = appName + ".ear";

        // TestCDISimple.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple.model");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".ear/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder + "/" + appName + ".ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(appName);

        return appRecord;
    }

    private static Application setupJPALibTestApplication() throws Exception {
        final String appName = "TestCDIWithJPALib";
        final String appNameEar = appName + ".ear";
        // JPALib
        JavaArchive jpalib = ShrinkWrap.create(JavaArchive.class, "jpamodel.jar");
        jpalib.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.jpalib.model");
        jpalib.addPackages(false, "com.ibm.ws.jpa.fvt.cdi.jpalib");
        ShrinkHelper.addDirectory(jpalib, RESOURCE_ROOT + appFolder + "/" + appName + ".ear/lib/jpamodel.jar");

        // TestCDISimple.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.jpalib.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".ear/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(jpalib);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder + "/" + appName + ".ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(appName);

        return appRecord;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + "TestCDISimple.ear");
                server.deleteFileFromLibertyServerRoot("apps/" + "TestCDIWithJPALib.ear");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestCDI_WEB.class, timestart);
        }
    }
}
