/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.traditional.client.web.TraditionalClientServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests remote EJB method calls between a Liberty servers and a WebSphere traditional server.
 */
@RunWith(FATRunner.class)
public class Server2TraditionalTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.remote.fat.TraditionalServerClient")
    @TestServlets({ @TestServlet(servlet = TraditionalClientServlet.class, contextRoot = "TraditionalClientWeb") })
    public static LibertyServer unsecureClientServer;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.TraditionalServerSecureClient")
    public static LibertyServer secureClientServer;

    private static boolean isSecureActive = Boolean.getBoolean("traditionalSecurity");

    @Override
    public LibertyServer getServer() {
        return isSecureActive ? secureClientServer : unsecureClientServer;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        LibertyServer clientServer = isSecureActive ? secureClientServer : unsecureClientServer;

        // Use ShrinkHelper to build the Ear

        //#################### TraditionalClientApp
        WebArchive TraditionalClientWeb = ShrinkHelper.buildDefaultApp("TraditionalClientWeb.war", "com.ibm.ws.ejbcontainer.traditional.client.web.");

        EnterpriseArchive TraditionalClientApp = ShrinkWrap.create(EnterpriseArchive.class, "TraditionalClientApp.ear");
        TraditionalClientApp.addAsModule(TraditionalClientWeb);
        TraditionalClientApp = (EnterpriseArchive) ShrinkHelper.addDirectory(TraditionalClientApp, "test-applications/TraditionalClientApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(unsecureClientServer, TraditionalClientApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(secureClientServer, TraditionalClientApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        clientServer.useSecondaryHTTPPort();
        clientServer.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (isSecureActive && secureClientServer.isStarted()) {
            secureClientServer.stopServer();
        } else if (unsecureClientServer.isStarted()) {
            unsecureClientServer.stopServer();
        }
    }
}