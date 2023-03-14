/*
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
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
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests for SessionPersistence with CDI and Faces. Since Faces-4.0 relies on CDI much more than earlier versions,
 * this test has been added to ensure the CDI classes are properly serialized. 
 * See https://github.com/OpenLiberty/open-liberty/issues/24631 and https://github.com/OpenLiberty/open-liberty/pull/24642
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat(NO_MODIFICATION)  // Skipped due to java.lang.ClassNotFoundException: org.apache.commons.collections.map.LRUMap see issue https://github.com/OpenLiberty/open-liberty/issues/24680
public class CDISessionPersistenceTests {

    private static Logger LOG = Logger.getLogger(CDISessionPersistenceTests.class.getName());

    public static final String APP_NAME = "CDISessionPersistence";
    public static final String SERVER_NAME = "facesCDISessionPersistence";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage("com.ibm.ws.jsf22.fat.cditests.beans.persistence");
        ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + ".war/resources");
        // Add simpleFlow directory to application
        app.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory("test-applications/" + APP_NAME + ".war/simpleFlow").as(GenericArchive.class), "/simpleFlow");
        CDIArchiveHelper.addBeansXML(app, DiscoveryMode.ALL);
        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started");
    }

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
            server.stopServer(true, false);
            LOG.info("Server is Stopped at " + System.nanoTime());

            server.startServer(CDISessionPersistenceTests.class.getSimpleName() + "-2.log");
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
