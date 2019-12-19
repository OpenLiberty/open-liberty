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

package com.ibm.ws.jpa.spec10;

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
import com.ibm.ws.jpa.FATSuite;
import com.ibm.ws.jpa.JPAFATServletClient;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListenerException_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListenerException_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListenerException_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListener_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListener_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackDefaultListener_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackException_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackException_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackException_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackListenerException_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackListenerException_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackListener_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackListener_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackListener_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackOrderOfInvocation_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackOrderOfInvocation_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallbackOrderOfInvocation_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallback_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallback_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.callback.tests.ejb.TestCallback_EJB_SL_Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Callback_EJB extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jpa10/callback/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_CALLBACK_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_CALLBACK_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestCallback_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallback_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallback_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallback_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallback_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallback_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackDefaultListener_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackDefaultListener_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackDefaultListener_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackDefaultListener_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackDefaultListener_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackDefaultListener_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackDefaultListenerException_EJB_SL_Servlet.class,
                                 path = "Callback10EJB" + "/" + "TestCallbackDefaultListenerException_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackDefaultListenerException_EJB_SF_Servlet.class,
                                 path = "Callback10EJB" + "/" + "TestCallbackDefaultListenerException_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackDefaultListenerException_EJB_SFEX_Servlet.class,
                                 path = "Callback10EJB" + "/" + "TestCallbackDefaultListenerException_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackException_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackException_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackException_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackException_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackException_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackException_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackListener_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackListener_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackListener_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackListener_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackListener_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackListener_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackListenerException_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackListenerException_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackListenerException_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackListenerException_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackOrderOfInvocation_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackOrderOfInvocation_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestCallbackOrderOfInvocation_EJB_SL_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackOrderOfInvocation_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestCallbackOrderOfInvocation_EJB_SF_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackOrderOfInvocation_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestCallbackOrderOfInvocation_EJB_SFEX_Servlet.class, path = "Callback10EJB" + "/" + "TestCallbackOrderOfInvocation_EJB_SFEX_Servlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Callback_EJB.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

        server1.startServer();

        setupDatabaseApplication(server1, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "callbackejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.callback.tests.ejb");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "ejb/callbackejb.war");

        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, "callback.jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.callback");
        ShrinkHelper.addDirectory(ejbApp, RESOURCE_ROOT + "ejb/callback.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final JavaArchive jpaJar = ShrinkWrap.create(JavaArchive.class, "JPACallbackLib.jar");
        ShrinkHelper.addDirectory(jpaJar, RESOURCE_ROOT + "/lib/JPACallbackLib.jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "Callback_EJB.ear");
        app.addAsModule(ejbApp);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(jpaJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "ejb", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation("Callback_EJB.ear");
        appRecord.setName("Callback_EJB");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("Callback_EJB");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("callback_ejb");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Callback_EJB.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/Callback_EJB.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }
}
