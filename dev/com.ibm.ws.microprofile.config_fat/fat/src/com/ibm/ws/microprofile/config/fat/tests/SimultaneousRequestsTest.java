/*******************************************************************************
* Copyright (c) 2016, 2018 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.ordForDefaults.test.OrdinalsForDefaultsTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserException;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig11EE7;
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig12EE8;

import com.ibm.websphere.simplicity.log.Log;

@RunWith(FATRunner.class)
public class SimultaneousRequestsTest extends FATServletClient {

    public static final String APP_NAME = "simultaneousRequests";

    @Server("SimultaneousRequestsServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive simultaneousRequests_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.simultaneousRequests.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, simultaneousRequests_war);

        server.startServer();
    }

    @Test
    public void testSimultaneousRequests() throws Exception {

        Callable<WebResponse> callableWebResponse = new Callable<WebResponse>() {
            @Override
            public WebResponse call() throws WebBrowserException {
                WebResponse wr = WebBrowserFactory.getInstance().createWebBrowser().request("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/simultaneousRequests/");
                return wr;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<WebResponse> requestOne = executor.submit(callableWebResponse);
        Thread.sleep(1000); //Just to be safe, space the requests out to ensure the first thread sets a flag.
        Future<WebResponse> requestTwo = executor.submit(callableWebResponse);

        requestOne.get().verifyResponseBodyContains("No exceptions were thrown");
        requestTwo.get().verifyResponseBodyContains("No exceptions were thrown");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }


    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatConfig11EE7("CDIConfigServer"))
                    .andWith(new RepeatConfig12EE8("CDIConfigServer"));

}
