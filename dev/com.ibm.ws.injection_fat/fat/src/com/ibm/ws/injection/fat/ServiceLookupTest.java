/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.injection.service.lookup.web.ServiceLookupServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ServiceLookupTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.ServiceLookupServer")
    @TestServlet(servlet = ServiceLookupServlet.class, contextRoot = "ServiceLookupWeb")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.injection.fat.ServiceLookupServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.ServiceLookupServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.injection.fat.ServiceLookupServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive ServiceLookupWeb = ShrinkHelper.buildDefaultApp("ServiceLookupWeb.war", "com.ibm.ws.injection.service.lookup.web.");
        ShrinkHelper.exportDropinAppToServer(server, ServiceLookupWeb);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.com.ibm.ws.injection.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/injectionBVTinternals-1.0.mf");
        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/test.com.ibm.ws.injection.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/injectionBVTinternals-1.0.mf");
    }
}
