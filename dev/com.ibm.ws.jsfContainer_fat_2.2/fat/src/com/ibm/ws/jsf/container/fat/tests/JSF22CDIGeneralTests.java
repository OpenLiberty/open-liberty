/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf.container.fat.FATSuite;
import com.ibm.ws.jsf.container.fat.utils.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General JSF 2.2 test cases the also require CDI.
 */
@RunWith(FATRunner.class)
public class JSF22CDIGeneralTests extends FATServletClient {

    protected static final Class<?> c = JSF22CDIGeneralTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf.container.2.2_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.removeAllInstalledAppsForValidation();
        server.startServer(JSF22CDIGeneralTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test the CDI-JSF integration.
     *
     * In this test we want make sure that a custom ViewHandler
     * and a custom Application can be used in an app.
     *
     * Also, make sure that the IBMViewHandler is used
     * when CDI is enabled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testViewHandler_Mojarra() throws Exception {
        final String appName = "ViewHandlerTest";
        WebArchive mojarraApp = ShrinkHelper.buildDefaultApp(appName, "jsf.container.viewhandlertest");
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + appName + "/resources");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
        testViewHandler(appName);
    }

    /**
     * Test the CDI-JSF integration.
     *
     * In this test we want make sure that a custom ViewHandler
     * and a custom Application can be used in an app.
     *
     * Also, make sure that the IBMViewHandler is used
     * when CDI is enabled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testViewHandler_MyFaces() throws Exception {
        final String appName = "ViewHandlerTest";
        WebArchive myfacesApp = ShrinkHelper.buildDefaultApp(appName + "_MyFaces", "jsf.container.viewhandlertest");
        ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + appName + "/resources");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
        testViewHandler(appName + "_MyFaces");
    }

    public void testViewHandler(String contextRoot) throws Exception {
        // Wait for the application to be started.
        server.waitForStringInLog("CWWKZ0001I: Application " + contextRoot + " started");

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(server, contextRoot, "index.xhtml");

        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        String responseText = page.asText();

        // Log the page for debugging if necessary in the future.
        Log.info(c, name.getMethodName(), responseText);
        Log.info(c, name.getMethodName(), page.asXml());

        assertTrue("Page does not contain expected response.", responseText.contains("CDI Integration Test"));

        assertTrue("The Custom ApplicationFactory was not invoked.",
                   !server.findStringsInTrace("CustomApplicationFactory was invoked!").isEmpty());

        assertTrue("The Custom Application was not invoked.",
                   !server.findStringsInTrace("CustomApplication was invoked!").isEmpty());

        assertTrue("The Custom ViewHandler was not invoked.",
                   !server.findStringsInTrace("CustomViewHandler was invoked!").isEmpty());

        assertTrue("The IBMViewHandler was not used.",
                   !server.findStringsInTrace("setViewHandler Setting IBM View Handler").isEmpty());
    }
}
