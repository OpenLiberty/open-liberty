/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

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
import mpRestClient11.produceConsume.ProduceConsumeTestServlet;

/**
 * This test should only be run with mpRestClient-1.1 and above.
 * Note that mpRestClient-1.1 can work with Java EE 7.0 (MP 1.4) and EE 8 (MP 2.0).
 */
@RunWith(FATRunner.class)
public class ProduceConsumeTest extends FATServletClient {

    private static final String appName = "produceConsumeApp";
    final static String SERVER_NAME = "mpRestClient11.produceConsume";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(), "1.2", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(), "1.3", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(), "1.4", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT(FeatureReplacementAction.EE8_FEATURES(), "2.0", SERVER_NAME));

    @Server(SERVER_NAME)
    @TestServlet(servlet = ProduceConsumeTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient11.produceConsume");
        server.startServer();
        server.waitForStringInLog("CWWKO0219I.*ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
    }
}
