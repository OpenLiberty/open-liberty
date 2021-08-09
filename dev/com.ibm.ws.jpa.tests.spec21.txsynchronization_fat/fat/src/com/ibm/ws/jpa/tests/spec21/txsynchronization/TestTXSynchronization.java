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

package com.ibm.ws.jpa.tests.spec21.txsynchronization;

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
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMEXSpecificRunnerTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMEXSpecificSyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMEXSpecificUnsyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMTCMEXSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMTCMTSSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMTSSpecificEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationCMTSSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationEJBSFExSyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationEJBSFExUnsyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.ejb.TxSynchronizationEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.web.TxSynchronizationCMTSSpecificWebTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.web.TxSynchronizationWebTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
public class TestTXSynchronization extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/TxSynchronization/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA21_TXSYNC_DROP_${dbvendor}.ddl");
        createSet.add("JPA21_TXSYNC_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerTxSynchronization")
    @TestServlets({
                    @TestServlet(servlet = TxSynchronizationWebTestServlet.class, path = "TxSynchronization" + "/" + "TxSynchronizationWebTestServlet"),
                    @TestServlet(servlet = TxSynchronizationCMTSSpecificWebTestServlet.class, path = "TxSynchronization" + "/" + "TxSynchronizationCMTSSpecificWebTestServlet"),

                    @TestServlet(servlet = TxSynchronizationEJBSLTestServlet.class, path = "TxSynchronizationEJB" + "/" + "TxSynchronizationEJBSLTestServlet"),
                    @TestServlet(servlet = TxSynchronizationCMTSSpecificEJBSLTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMTSSpecificEJBSLTestServlet"),

                    @TestServlet(servlet = TxSynchronizationEJBSFTestServlet.class, path = "TxSynchronizationEJB" + "/" + "TxSynchronizationEJBSFTestServlet"),
                    @TestServlet(servlet = TxSynchronizationCMTSSpecificEJBSFTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMTSSpecificEJBSFTestServlet"),

                    @TestServlet(servlet = TxSynchronizationEJBSFExSyncTestServlet.class, path = "TxSynchronizationEJB" + "/" + "TxSynchronizationEJBSFExSyncTestServlet"),
                    @TestServlet(servlet = TxSynchronizationEJBSFExUnsyncTestServlet.class, path = "TxSynchronizationEJB" + "/" + "TxSynchronizationEJBSFExUnsyncTestServlet"),

                    @TestServlet(servlet = TxSynchronizationCMEXSpecificSyncTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMEXSpecificSyncTestServlet"),
                    @TestServlet(servlet = TxSynchronizationCMEXSpecificUnsyncTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMEXSpecificUnsyncTestServlet"),

                    @TestServlet(servlet = TxSynchronizationCMEXSpecificRunnerTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMEXSpecificRunnerTestServlet"),

                    @TestServlet(servlet = TxSynchronizationCMTCMTSSpecificEJBSLTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMTCMTSSpecificEJBSLTestServlet"),

                    @TestServlet(servlet = TxSynchronizationCMTCMEXSpecificEJBSLTestServlet.class,
                                 path = "TxSynchronizationEJB" + "/" + "TxSynchronizationCMTCMEXSpecificEJBSLTestServlet"),

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestTXSynchronization.class);
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

        System.out.println("TestTXSynchronization Setting up database tables...");

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
        // jpaentities.jar
        final JavaArchive entityJar = ShrinkWrap.create(JavaArchive.class, "jpaentities.jar");
        entityJar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        ShrinkHelper.addDirectory(entityJar, RESOURCE_ROOT + "apps/TxSynchronization.ear/lib/jpaentities.jar");

        // testlogic.jar
        final JavaArchive testLogicJar = ShrinkWrap.create(JavaArchive.class, "testlogic.jar");
        testLogicJar.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.testlogic");
        testLogicJar.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.testlogic.cm");

        // TxSynchronization.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "TxSynchronization.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.tests.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "apps/TxSynchronization.ear/TxSynchronization.war");

        // TxSynchronizationEJB.war
        WebArchive ejbwebApp = ShrinkWrap.create(WebArchive.class, "TxSynchronizationEJB.war");
        ejbwebApp.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.tests.ejb");
        ShrinkHelper.addDirectory(ejbwebApp, RESOURCE_ROOT + "apps/TxSynchronization.ear/TxSynchronizationEJB.war");

        // TxSynchronizationEJB
        JavaArchive txsyncEjb = ShrinkWrap.create(JavaArchive.class, "TxSynchronizationEJB.jar");
        txsyncEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.ejb");
        txsyncEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.ejblocal");
        ShrinkHelper.addDirectory(txsyncEjb, RESOURCE_ROOT + "apps/TxSynchronization.ear/TxSynchronizationEJB.jar");

        // TxSynchronizationBuddyEJB
        JavaArchive buddyEjb = ShrinkWrap.create(JavaArchive.class, "TxSynchronizationBuddyEJB.jar");
        buddyEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.buddy.ejb");
        buddyEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal");

        ShrinkHelper.addDirectory(buddyEjb, RESOURCE_ROOT + "apps/TxSynchronization.ear/TxSynchronizationBuddyEJB.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "TxSynchronization.ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbwebApp);
        app.addAsModule(txsyncEjb);
        app.addAsModule(buddyEjb);
        app.addAsLibrary(entityJar);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(testLogicJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "apps/TxSynchronization.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation("TxSynchronization.ear");
        appRecord.setName("TxSynchronization");
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
        appNamesSet.add("TxSynchronization");
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // Clean up database
            try {
                final Set<String> ddlSet = new HashSet<String>();
                for (String ddlName : dropSet) {
                    ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
                }
                executeDDL(server, ddlSet, true);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation", // RuntimeException test, expected
                              "CWWJP0046E: An UNSYNCHRONIZED JPA persistence context cannot be propagated into a SYNCHRONIZED EntityManager",
                              "CNTR0020E",
                              "CWWJP0045E",
                              "WLTC0017E",
                              "CNTR0019E",
                              "CWWKG0032W");
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + "TxSynchronization.ear");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestTXSynchronization.class, timestart);
        }
    }
}
