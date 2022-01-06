/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient13.hostnameVerifier.HostnameVerifierTestServlet;

/**
 * This test should only be run with mpRestClient-1.3 and above.
 */
@RunWith(FATRunner.class)
public class HostnameVerifierTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient13.ssl";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, 
                                                             MicroProfileActions.MP30, //mpRestClient-1.3
                                                             MicroProfileActions.MP33, // 1.4
                                                             MicroProfileActions.MP40, // 2.0
                                                             MicroProfileActions.MP50); // 3.0

    private static final String appName = "hostnameVerifierApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HostnameVerifierTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient13.hostnameVerifier");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines

    }
}