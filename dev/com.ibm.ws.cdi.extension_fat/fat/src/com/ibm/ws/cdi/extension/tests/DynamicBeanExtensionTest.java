/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.extension.apps.dynamicBeans.DynamicBeansServlet;
import com.ibm.ws.cdi.extension.apps.dynamicBeans.jar.MyCDIExtension;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Scope tests for Dynamically Added Beans
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class DynamicBeanExtensionTest extends FATServletClient {

    public static final String APP_NAME = "dynamicallyAddedBeans";
    public static final String SERVER_NAME = "cdi12DynamicallyAddedBeansServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive dynamicallyAddedBeansJar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar");
        dynamicallyAddedBeansJar.addPackage(MyCDIExtension.class.getPackage());
        CDIArchiveHelper.addCDIExtensionService(dynamicallyAddedBeansJar, MyCDIExtension.class);

        WebArchive dynamicallyAddedBeans = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        dynamicallyAddedBeans.addClass(DynamicBeansServlet.class);

        EnterpriseArchive dynamicallyAddedBeansEar = ShrinkWrap.create(EnterpriseArchive.class,
                                                                       APP_NAME + ".ear");
        dynamicallyAddedBeansEar.setApplicationXML(DynamicBeansServlet.class.getPackage(), "application.xml");
        dynamicallyAddedBeansEar.addAsModule(dynamicallyAddedBeans);
        dynamicallyAddedBeansEar.addAsLibrary(dynamicallyAddedBeansJar);
        ShrinkHelper.exportDropinAppToServer(server, dynamicallyAddedBeansEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    /**
     * Test that bean classes which are loaded by the Root ClassLoader can be injected correctly
     *
     * @throws Exception
     */
    @Test
    public void testDynamicallyAddedBeans() throws Exception {
        WebBrowser browser = WebBrowserFactory.getInstance().createWebBrowser();
        verifyResponse(browser, server, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
        verifyResponse(browser, server, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 3, 4", "DynamicBean2 count: 1, 2" });
        browser = WebBrowserFactory.getInstance().createWebBrowser();
        verifyResponse(browser, server, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
    }

    private WebResponse verifyResponse(WebBrowser webBrowser, LibertyServer server, String resource, String... expectedResponses) throws Exception {
        String url = this.createURL(server, resource);
        WebResponse response = webBrowser.request(url);
        for (String expectedResponse : expectedResponses) {
            response.verifyResponseBodyContains(expectedResponse);
        }
        return response;
    }

    private static String createURL(LibertyServer server, String path) {
        if (!path.startsWith("/"))
            path = "/" + path;
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

}
