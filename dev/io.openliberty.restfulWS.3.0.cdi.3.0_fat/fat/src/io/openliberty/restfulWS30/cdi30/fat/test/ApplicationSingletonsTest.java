/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

// These tests test when CDI will and won't inject into a singleton class
@RunWith(FATRunner.class)
public class ApplicationSingletonsTest {

    @Server("io.openliberty.restfulWS.3.0.cdi.3.0.fat.applicationsingleton")
    public static LibertyServer server;

    static String appname = "applicationsingleton";

    static WebArchive pojoSingletonApp;
    static WebArchive cdiSingletonApp;
    static WebArchive resourceContextSingletonApp;

    @BeforeClass
    public static void setUp() throws Exception {
        // Build different apps for each scenario
        pojoSingletonApp = ShrinkHelper.buildDefaultApp(appname,
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton",
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.pojosingleton");
        cdiSingletonApp = ShrinkHelper.buildDefaultApp(appname,
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton",
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.cdisingleton");
        resourceContextSingletonApp = ShrinkHelper.buildDefaultApp(appname,
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton",
                                   "io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.resourcecontextcdisingleton");
    }

    @Before
    public void preTest() throws Exception {
        // make sure the server is stopped and applications are removed before starting each test
        if (server.isStarted()) {
            server.removeAndStopDropinsApplications(appname);
        }
    }

    @After
    public void afterTest() throws Exception {
        System.out.println("removing " + appname + "=" +
        server.removeDropinsApplications(appname + ".war"));
        server.stopServer("SRVE0271E", "SRVE0276E", "SRVE0315E", "SRVE0777E");
    }
    
    private int initializeApplication() throws IOException {
        URL url = new URL("http://localhost:" + server.getHttpDefaultPort()
        + "/" + appname + "/hello");

        int status;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            status = con.getResponseCode();

            return status;
        } finally {
            con.disconnect();
        }
    }

    @Test
    @AllowedFFDC("org.jboss.resteasy.spi.UnhandledException")
    public void testPojoSingletonInjection() throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, pojoSingletonApp, DeployOptions.OVERWRITE);
        server.startServer("testPojoSingletonInjection.log");
        int status = initializeApplication();
        System.out.println(status);
        Assert.assertTrue(status == 500);
        server.findStringsInLogs("Caused by: java.lang.NullPointerException");
    }

    @Test
    public void testCDISingletonInjection() throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, cdiSingletonApp, DeployOptions.OVERWRITE);
        server.startServer("testCDISingletonInjection.log");
        int status = initializeApplication();
        System.out.println(status);
        Assert.assertTrue(status == 200);
        server.findStringsInLogs("Hello from SimpleBean");
    }
    
    @Test
    @AllowedFFDC( {"org.jboss.resteasy.spi.LoggableFailure", "jakarta.servlet.ServletException" })
    public void testResourceContextCDISingletonInjection() throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, resourceContextSingletonApp, DeployOptions.OVERWRITE);
        server.startServer("testResourceContextCDISingletonInjection.log");
        int status = initializeApplication();
        System.out.println(status);
        Assert.assertTrue(status == 500);
        server.findStringsInLogs("RESTEASY003880: Unable to find contextual data of type: jakarta.ws.rs.container.ResourceContext");
    }
}
