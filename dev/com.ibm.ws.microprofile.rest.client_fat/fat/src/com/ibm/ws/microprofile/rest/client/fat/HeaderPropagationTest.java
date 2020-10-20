/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient10.headerPropagation.HeaderPropagationTestServlet;

@RunWith(FATRunner.class)
public class HeaderPropagationTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient10.headerPropagation";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(FATSuite.MP_REST_CLIENT("1.1", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(), "1.2", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(),"1.3", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(),"1.4", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(),"2.0", SERVER_NAME));

    private static final String appName = "headerPropagationApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HeaderPropagationTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, appName, "mpRestClient10.headerPropagation");
        server.startServer();
        assertNotNull("LTPA configuration should report it is ready", server.waitForStringInLog("CWWKS4105I"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}