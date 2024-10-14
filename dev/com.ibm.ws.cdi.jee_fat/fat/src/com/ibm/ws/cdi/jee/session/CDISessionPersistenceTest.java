/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.session;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.jee.session.app.CDISessionPersistenceServlet;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CDISessionPersistenceTest extends FATServletClient {

    private static Logger LOG = Logger.getLogger(CDISessionPersistenceTest.class.getName());

    public static final String APP_NAME = "cdiSessionPersistence";
    public static final String SERVER_NAME = "cdiSessionPersistenceServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE11, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE8, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(CDISessionPersistenceServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(app, DiscoveryMode.ANNOTATED);

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testSessionPersistance() throws Exception {

        // Note: We're using WebClient because it will store and send the session cookie, but we're still calling a FATServlet and using FATServletClient to check the result.
        try (WebClient webClient = new WebClient()) {
            callTest(webClient, "testPrePersist");

            LOG.info("Stopping server at " + System.nanoTime());
            server.stopServer(true, false); // (postStopServerArchive, boolean forceStop)
            LOG.info("Server is Stopped at " + System.nanoTime());

            server.startServer(CDISessionPersistenceTest.class.getSimpleName() + "-2.log");
            LOG.info("Server is restarted at " + System.nanoTime());

            callTest(webClient, "testPostRestore");
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    private String getUrl(String testMethod) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + getPathAndQuery(APP_NAME + "/persistence", testMethod);
    }

    private void callTest(WebClient webClient, String testName) throws Exception {
        TextPage p = webClient.getPage(getUrl("testPrePersist"));
        assertTestResponse(p.getContent());
    }

}
