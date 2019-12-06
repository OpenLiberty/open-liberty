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
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackDefaultListenerExceptionServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackDefaultListenerServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackExceptionServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackListenerExceptionServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackListenerServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackOrderOfInvocationServlet;
import com.ibm.ws.jpa.fvt.callback.tests.web.TestCallbackServlet;

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
public class Callback_Web extends JPAFATServletClient {
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
                    @TestServlet(servlet = TestCallbackServlet.class, path = "Callback10Web" + "/" + "TestCallbackServlet"),
                    @TestServlet(servlet = TestCallbackExceptionServlet.class, path = "Callback10Web" + "/" + "TestCallbackExceptionServlet"),
                    @TestServlet(servlet = TestCallbackListenerServlet.class, path = "Callback10Web" + "/" + "TestCallbackListenerServlet"),
                    @TestServlet(servlet = TestCallbackListenerExceptionServlet.class, path = "Callback10Web" + "/" + "TestCallbackListenerExceptionServlet"),
                    @TestServlet(servlet = TestCallbackDefaultListenerServlet.class, path = "Callback10Web" + "/" + "TestCallbackDefaultListenerServlet"),
                    @TestServlet(servlet = TestCallbackDefaultListenerExceptionServlet.class, path = "Callback10Web" + "/" + "TestCallbackDefaultListenerExceptionServlet"),
                    @TestServlet(servlet = TestCallbackOrderOfInvocationServlet.class, path = "Callback10Web" + "/" + "TestCallbackOrderOfInvocationServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Callback_Web.class);
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
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "callback.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.callback");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "web/callback.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final JavaArchive jpaJar = ShrinkWrap.create(JavaArchive.class, "JPACallbackLib.jar");
        ShrinkHelper.addDirectory(jpaJar, RESOURCE_ROOT + "/lib/JPACallbackLib.jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "Callback_Web.ear");
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        app.addAsLibrary(jpaJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "web", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation("Callback_Web.ear");
        appRecord.setName("Callback_Web");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("Callback_Web");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("callback_web");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Callback_Web.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/Callback_Web.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }
}
