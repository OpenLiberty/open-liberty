/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.lifecycle.apps.transientReferenceBasicWar.TransiantDependentScopedBean;
import com.ibm.ws.cdi.lifecycle.apps.transientReferenceBasicWar.TransientReferenceTestServlet;
import com.ibm.ws.cdi.lifecycle.apps.transientReferenceWithPersistenceWar.PassivationBean;
import com.ibm.ws.cdi.lifecycle.apps.transientReferenceWithPersistenceWar.PassivationBeanTestServlet;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class PassivationBeanTests extends FATServletClient {

    @Server("cdi12PassivationServer")
    @TestServlet(contextRoot = "transientReferenceBasic", servlet = TransientReferenceTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive transientReferenceBasicWar = ShrinkWrap.create(WebArchive.class, "transientReferenceBasic.war")
                                                          .addPackage(TransiantDependentScopedBean.class.getPackage())
                                                          .addAsWebInfResource(TransiantDependentScopedBean.class.getResource("beans.xml"), "beans.xml");

        WebArchive transientReferenceWithPersistenceWar = ShrinkWrap.create(WebArchive.class, "transientReferenceWithPersistence.war")
                                                                    .addPackage(PassivationBean.class.getPackage())
                                                                    .addAsWebInfResource(PassivationBean.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, transientReferenceBasicWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, transientReferenceWithPersistenceWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * @see PassivationBeanTestServlet
     * @throws Exception
     */
    @Test
    public void testTransientReferenceWithPersistence() throws Exception {

        // Use a WebBrowser so that we maintain the same session
        WebBrowser wb = WebBrowserFactory.getInstance().createWebBrowser();

        // Do the setup and check pre-reqs
        runTest(wb, server, "transientReferenceWithPersistence/", "testInitialize");

        // Restart the app (session should be persisted)
        server.getApplicationMBean("transientReferenceWithPersistence").restart();

        // Test the restored session
        runTest(wb, server, "transientReferenceWithPersistence/", "testReuse");
    }

    public static void runTest(WebBrowser wb, LibertyServer server, String path, String testName) throws Exception {
        String url = HttpUtils.createURL(server, FATServletClient.getPathAndQuery(path, testName)).toString();
        String response = wb.request(url).getResponseBody();
        FATServletClient.assertTestResponse(response);
    }

}
