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
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class EntityListenerTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive cdiInEntityListernersTestEarLib = ShrinkWrap.create(JavaArchive.class, "CDIInEntityListernersTestEarLib.jar")
                        .addPackage("cdi.entity.listeners.test.model.lib");

        WebArchive cdiInEntityListernersTestWar = ShrinkWrap.create(WebArchive.class, "CDIInEntityListernersTest.war")
                        .addPackage("cdi.entity.listeners.test.web")
                        .addPackage("cdi.entity.listeners.test.model")
                        .add(new FileAsset(new File("test-applications/CDIInEntityListernersTest.war/resources/WEB-INF/classes/META-INF/persistence.xml")), "/WEB-INF/classes/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/CDIInEntityListernersTest.war/resources/WEB-INF/classes/META-INF/jpaorm.xml")), "/WEB-INF/classes/META-INF/jpaorm.xml");

        EnterpriseArchive cdiInEntityListernersTestEar = ShrinkWrap.create(EnterpriseArchive.class, "CDIInEntityListernersTest.ear")
                        .addAsModule(cdiInEntityListernersTestWar)
                        .addAsLibrary(cdiInEntityListernersTestEarLib);

        server = LibertyServerFactory.getLibertyServer("cdiEntityListenersServer");
        ShrinkHelper.exportAppToServer(server, cdiInEntityListernersTestEar);
        server.startServer();
    }

    @Test
    public void testInjectWorksInsideEntityListeners() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        String url = createURL("/CDIInEntityListernersTest/SimpleTestServlet");
        WebResponse response = browser.request(url);
        String body = response.getResponseBody(); // we just poke the server to make sure everything runs.

        List<String> matching = server.findStringsInLogsAndTraceUsingMark("testInjectWorksInsideEntityListeners passed!");
        assertFalse("Did not find the test passed string in logs.", matching.isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private String createURL(String path) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

}
