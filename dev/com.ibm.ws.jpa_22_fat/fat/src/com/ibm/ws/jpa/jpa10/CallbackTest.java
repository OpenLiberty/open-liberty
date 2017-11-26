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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jpa10callback.web.CallbackOrderOfInvocationTestServlet;
import jpa10callback.web.CallbackRuntimeExceptionTestServlet;
import jpa10callback.web.CallbackTestServlet;
import jpa10callback.web.DefaultListenerCallbackRuntimeExceptionTestServlet;
import jpa10callback.web.DefaultListenerCallbackTestServlet;

@RunWith(FATRunner.class)
public class CallbackTest extends FATServletClient {
    public static final String APP_NAME = "callback";
    public static final String SERVLET = "TestCallback";
    public static final String SERVLET2 = "DefaultTestCallback";
    public static final String SERVLET3 = "TestCallbackRuntimeException";
    public static final String SERVLET4 = "DefaultCallbackRuntimeTestCallback";
    public static final String SERVLET5 = "TestCallbackOrderOfInvocation";

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
        final String resPath = "test-applications/jpa10/" + APP_NAME + "/resources/";

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

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                           "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
        );
    }
}
