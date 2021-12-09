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
import com.ibm.ws.jpa.fvt.cdi.jpalib.ejb.web.JPACDIWithJPALibEJBServlet;
import com.ibm.ws.jpa.fvt.cdi.simple.ejb.web.JPACDISimpleEJBServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class TestCDI_EJB extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "TestCDISimpleEJB";
    private final static String CONTEXT_ROOT_JPALIB = "TestCDIWithJPALibEJB";

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
                    @TestServlet(servlet = JPACDISimpleEJBServlet.class, path = CONTEXT_ROOT + "/" + "JPACDISimpleEJBServlet"),
                    @TestServlet(servlet = JPACDIWithJPALibEJBServlet.class, path = CONTEXT_ROOT_JPALIB + "/" + "JPACDIWithJPALibEJBServlet"),

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestCDI_EJB.class);
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
        Application simpleAppRecord = setupSimpleTestApplication();
        Application jpaLibAppRecord = setupJpaLibTestApplication();

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(simpleAppRecord);
        sc.getApplications().add(jpaLibAppRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("TestCDISimpleEJB");
        appNamesSet.add("TestCDIWithJPALibEJB");
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static Application setupSimpleTestApplication() throws Exception {
        String appName = "TestCDISimpleEJB";
        String appNameEar = appName + ".ear";

        // TestCDISimple.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple.ejb.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appNameEar + "/" + appName + ".war");

        // TestCDISimpleEJB.jar
        JavaArchive cdiSimpleEjb = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        cdiSimpleEjb.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple");
        cdiSimpleEjb.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple.model");
        cdiSimpleEjb.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.simple.ejb");
        ShrinkHelper.addDirectory(cdiSimpleEjb, RESOURCE_ROOT + appFolder + "/" + appNameEar + "/" + appName + ".jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsModule(cdiSimpleEjb);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder + "/" + appNameEar, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

    private static Application setupJpaLibTestApplication() throws Exception {
        String appName = "TestCDIWithJPALibEJB";
        String appNameEar = appName + ".ear";

        // JPALib
        JavaArchive jpalib = ShrinkWrap.create(JavaArchive.class, "jpamodel.jar");
        jpalib.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.jpalib.model");
        jpalib.addPackages(false, "com.ibm.ws.jpa.fvt.cdi.jpalib");
        ShrinkHelper.addDirectory(jpalib, RESOURCE_ROOT + appFolder + "/" + appNameEar + "/lib/jpamodel.jar");

        // TestCDIWithJPALib.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.cdi.jpalib.ejb.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appNameEar + "/" + appName + ".war");

        // TestCDIWithJPALibEJB.jar
        JavaArchive cdiJPALibEjb = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        cdiJPALibEjb.addPackages(false, "com.ibm.ws.jpa.fvt.cdi.jpalib.ejb");
        ShrinkHelper.addDirectory(cdiJPALibEjb, RESOURCE_ROOT + appFolder + "/" + appNameEar + "/" + appName + ".jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsModule(cdiJPALibEjb);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(jpalib);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder + "/" + appNameEar, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

                server.deleteFileFromLibertyServerRoot("apps/" + "TestCDISimpleEJB.ear");
                server.deleteFileFromLibertyServerRoot("apps/" + "TestCDIWithJPALibEJB.ear");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestCDI_EJB.class, timestart);
        }
    }
}
