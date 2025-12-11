/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mprestclient.fat.myrestclient.servlet.MyRestClientTestServlet;

/*
 * This test is to test a MicroProfile REST Client in a user feature and make sure there
 * are no classloading issues.
 */
@RunWith(FATRunner.class)
public class RESTClientUserFeatureTest extends FATServletClient {
    
    private static final String appName = "MyRestClient";
    private static final String bundleName = "MyRestClientBundle";
    private static final String serverName = "MyRESTClientServer";

    @ClassRule
    public static RepeatTests r = FATSuite.repeatMP13Up(serverName);
    
    @Server(serverName)
    @TestServlet(servlet = MyRestClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        // We need to overwrite the app so that the javax tests don't fail when the jakarta version runs first.
        ShrinkHelper.defaultDropinApp(server, appName,  new DeployOptions[] { DeployOptions.OVERWRITE }, "io.openliberty.mprestclient.fat.myrestclient.app", "io.openliberty.mprestclient.fat.myrestclient.servlet");

        /*
         * Build and install the user feature (wlp/usr/extension/lib)
         */
        ShrinkHelper.defaultUserFeatureArchive(server, bundleName, "io.openliberty.mprestclient.fat.myrestclient.bundle",
                                                                   "io.openliberty.mprestclient.fat.myrestclient.internal");
        server.installUserFeature(getUserFeatureFile());
        Thread.sleep(5000);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
            server.uninstallUserFeature("MyRESTClient");
            server.uninstallUserBundle(bundleName);
        }
    }
    
    private static String getUserFeatureFile() throws Exception {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            return "jakarta/MyRESTClient";
        } else {
            return "javax/MyRESTClient";
        }
    }

}
