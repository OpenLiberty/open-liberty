/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.faces40;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.jee.ShrinkWrapUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/*
 *  Faces-4.0 relies on CDI much more than earlier versions, so this test has been added to ensure
 *  the CDI classes are also properly serialized. 
 *  See https://github.com/OpenLiberty/open-liberty/issues/24631
 *  
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Faces40CDISessionPersistence {

    private static Logger LOG = Logger.getLogger(Faces40CDISessionPersistence.class.getName());

    public static final String APP_NAME = "persistFacesSession";
    public static final String SERVER_NAME = "cdi40SessionPersistenceServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive simpleJSFApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        Package pkg = com.ibm.ws.cdi.jee.faces40.app.DataBean.class.getPackage();
        simpleJSFApp.addPackage(pkg);
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "WEB-INF/faces-config.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "WEB-INF/web.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "simpleFlow/simpleFlow-2.xhtml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "simpleFlow/simpleFlow-flow.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "simpleFlow/simpleFlow.xhtml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "index.xhtml");
        CDIArchiveHelper.addBeansXML(simpleJSFApp, DiscoveryMode.ALL);

        ShrinkHelper.exportDropinAppToServer(server, simpleJSFApp, DeployOptions.SERVER_ONLY);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started");
    }

    /*
     * The client enters a Faces Flow. The server is restarted, and the application should continue 
     * where it left off (resfresh should not throw any errors)
     * 
     * Tests serialization of CDI classes (Event objects). Contact Faces Team for other errors. 
     */
    @Test
    public void testFaceletCDISessionPersistenceViaRestart() throws Exception {

        URL defaultURL = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME);;

        try (WebClient webClient = new WebClient()) {

            LOG.info("Navigating to: " + defaultURL + "/index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(defaultURL + "/index.xhtml");

            LOG.info(page.asXml());

            Assert.assertTrue("Can't access index.xhtml!",
                              page.asText().contains("Index"));

            HtmlElement button = (HtmlElement) page.getElementById("form:submitButton");
            page = button.click();

            LOG.info(page.asXml());

            Assert.assertTrue("Application is not working.",
                              page.asText().contains("test"));

            HtmlInput input = (HtmlInput) page.getElementById("form:data");
            input.setValueAttribute("indexdata");

            button = (HtmlElement) page.getElementById("form:submitButton");
            page = button.click();

            LOG.info(page.asXml());

            LOG.info("Stopping server at " + System.nanoTime());
            server.stopServer(true, false); // (postStopServerArchive, boolean forceStop)
            LOG.info("Server is Stopped at " + System.nanoTime());

            server.startServer(Faces40CDISessionPersistence.class.getSimpleName() + "-2.log");
            LOG.info("Server is restarted at " + System.nanoTime());

            //Ensure App has started
            server.waitForStringInLog("CWWKT0016I.*");

            button = (HtmlElement) page.getElementById("form:refresh");
            page = button.click();

            LOG.info(page.asXml());

            Assert.assertTrue("Session Persistence Failed!",
                              page.asText().contains("indexdata"));

        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
