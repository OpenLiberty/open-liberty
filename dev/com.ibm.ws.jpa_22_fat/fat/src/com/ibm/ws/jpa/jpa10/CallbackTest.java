/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.jpa10;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import jpa10callback.web.CallbackOrderOfInvocationTestServlet;
import jpa10callback.web.CallbackRuntimeExceptionTestServlet;
import jpa10callback.web.CallbackTestServlet;
import jpa10callback.web.DefaultListenerCallbackRuntimeExceptionTestServlet;
import jpa10callback.web.DefaultListenerCallbackTestServlet;

@RunWith(FATRunner.class)
public class CallbackTest extends JPAFATServletClient {
    public static final String APP_NAME = "callback";
    public static final String SERVLET = "TestCallback";
    public static final String SERVLET2 = "DefaultTestCallback";
    public static final String SERVLET3 = "TestCallbackRuntimeException";
    public static final String SERVLET4 = "DefaultCallbackRuntimeTestCallback";
    public static final String SERVLET5 = "TestCallbackOrderOfInvocation";
    private static final String resPath = "test-applications/jpa10/" + APP_NAME + "/resources";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_CALLBACK_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_CALLBACK_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = CallbackTestServlet.class, path = APP_NAME + "/" + SERVLET),
                    @TestServlet(servlet = DefaultListenerCallbackTestServlet.class, path = APP_NAME + "/" + SERVLET2),
                    @TestServlet(servlet = CallbackRuntimeExceptionTestServlet.class, path = APP_NAME + "/" + SERVLET3),
                    @TestServlet(servlet = DefaultListenerCallbackRuntimeExceptionTestServlet.class, path = APP_NAME + "/" + SERVLET4),
                    @TestServlet(servlet = CallbackOrderOfInvocationTestServlet.class, path = APP_NAME + "/" + SERVLET5)
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(CallbackTest.class);
        timestart = System.currentTimeMillis();

        server1.startServer();

        setupDatabaseApplication(server1, resPath + "/ddl/");

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
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        webApp.addPackages(true, "jpa10callback");
        ShrinkHelper.addDirectory(webApp, resPath + "/callback.war");

        final JavaArchive jpaJar = ShrinkWrap.create(JavaArchive.class, "JPACallbackLib.jar");
        ShrinkHelper.addDirectory(jpaJar, resPath + "/lib/JPACallbackLib.jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        app.addAsModule(webApp);
        app.addAsLibrary(jpaJar);
        app.setApplicationXML(new File(resPath + "/META-INF/application.xml"));

        ShrinkHelper.exportToServer(server1, "apps", app);
        server1.addInstalledAppForValidation(APP_NAME);

        Application appRecord = new Application();
        appRecord.setLocation(APP_NAME + ".ear");
        appRecord.setName(APP_NAME);

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(APP_NAME);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.dumpServer("callback");

            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(CallbackTest.class, timestart);
        }
    }
}
