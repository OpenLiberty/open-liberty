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
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMEXSpecificRunnerTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMEXSpecificSyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMEXSpecificUnsyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMTCMEXSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMTCMTSSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMTSSpecificEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDCMTSSpecificEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDEJBSFExSyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDEJBSFExUnsyncTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb.TxSynchronizationDDEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.web.TxSynchronizationDDCMTSSpecificWebTestServlet;
import com.ibm.ws.jpa.fvt.txsync.tests.dd.web.TxSynchronizationDDWebTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
public class TestTXDDSynchronization extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/TxSynchronization/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA21_TXSYNC_DROP_${dbvendor}.ddl");
        createSet.add("JPA21_TXSYNC_CREATE_${dbvendor}.ddl");
    }

    private final static String EJB_CONTEXTROOT = "TxSynchronizationEJBDD";
    private final static String WEB_CONTEXTROOT = "TxSynchronizationDD";

    @Server("JPAServerTxSynchronization")
    @TestServlets({
                    @TestServlet(servlet = TxSynchronizationDDWebTestServlet.class, path = WEB_CONTEXTROOT + "/" + "TxSynchronizationDDWebTestServlet"),
                    @TestServlet(servlet = TxSynchronizationDDCMTSSpecificWebTestServlet.class, path = WEB_CONTEXTROOT + "/" + "TxSynchronizationDDCMTSSpecificWebTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDEJBSLTestServlet.class, path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDEJBSLTestServlet"),
                    @TestServlet(servlet = TxSynchronizationDDCMTSSpecificEJBSLTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMTSSpecificEJBSLTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDEJBSFTestServlet.class, path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDEJBSFTestServlet"),
                    @TestServlet(servlet = TxSynchronizationDDCMTSSpecificEJBSFTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMTSSpecificEJBSFTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDEJBSFExSyncTestServlet.class, path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDEJBSFExSyncTestServlet"),
                    @TestServlet(servlet = TxSynchronizationDDEJBSFExUnsyncTestServlet.class, path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDEJBSFExUnsyncTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDCMEXSpecificSyncTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMEXSpecificSyncTestServlet"),
                    @TestServlet(servlet = TxSynchronizationDDCMEXSpecificUnsyncTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMEXSpecificUnsyncTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDCMEXSpecificRunnerTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMEXSpecificRunnerTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDCMTCMTSSpecificEJBSLTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMTCMTSSpecificEJBSLTestServlet"),

                    @TestServlet(servlet = TxSynchronizationDDCMTCMEXSpecificEJBSLTestServlet.class,
                                 path = EJB_CONTEXTROOT + "/" + "TxSynchronizationDDCMTCMEXSpecificEJBSLTestServlet"),

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestTXDDSynchronization.class);
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

        System.out.println("TestTXDDSynchronization Setting up database tables...");

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
        ShrinkHelper.addDirectory(entityJar, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear/lib/jpaentities.jar");

        // testlogic.jar
        final JavaArchive testLogicJar = ShrinkWrap.create(JavaArchive.class, "testlogic.jar");
        testLogicJar.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.testlogic");
        testLogicJar.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.testlogic.cm");

        // TxSynchronizationDD.war
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "TxSynchronizationDD.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.tests.dd.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear/TxSynchronizationDD.war");

        // TxSynchronizationDDEJB.war
        WebArchive ejbwebApp = ShrinkWrap.create(WebArchive.class, "TxSynchronizationEJBDD.war");
        ejbwebApp.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb");
        ShrinkHelper.addDirectory(ejbwebApp, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear/TxSynchronizationDDEJB.war");

        // TxSynchronizationDDEJB
        JavaArchive txsyncEjb = ShrinkWrap.create(JavaArchive.class, "TxSynchronizationDDEJB.jar");
        txsyncEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.dd.ejb");
        txsyncEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.ejblocal");
        ShrinkHelper.addDirectory(txsyncEjb, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear/TxSynchronizationDDEJB.jar");

        // TxSynchronizationBuddyDDEJB
        JavaArchive buddyEjb = ShrinkWrap.create(JavaArchive.class, "TxSynchronizationBuddyEJB.jar");
        buddyEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.buddy.dd.ejb");
        buddyEjb.addPackages(true, "com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal");

        ShrinkHelper.addDirectory(buddyEjb, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear/TxSynchronizationBuddyEJB.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "TxSynchronizationDD.ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbwebApp);
        app.addAsModule(txsyncEjb);
        app.addAsModule(buddyEjb);
        app.addAsLibrary(entityJar);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(testLogicJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "apps/TxSynchronizationDD.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation("TxSynchronizationDD.ear");
        appRecord.setName("TxSynchronizationDD");
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
        appNamesSet.add("TxSynchronizationDD");
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

                server.deleteFileFromLibertyServerRoot("apps/" + "TxSynchronizationDD.ear");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestTXDDSynchronization.class, timestart);
        }
    }
}
