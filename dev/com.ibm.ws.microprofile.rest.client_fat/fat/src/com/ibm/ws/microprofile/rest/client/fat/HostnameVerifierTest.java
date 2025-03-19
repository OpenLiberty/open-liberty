/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package com.ibm.ws.microprofile.rest.client.fat;


import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
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
    public static RepeatTests r = FATSuite.repeatMP30Up(SERVER_NAME);

    private static final String appName = "hostnameVerifierApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HostnameVerifierTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultDropinApp(server, appName, new DeployOptions[] {DeployOptions.SERVER_ONLY}, "mpRestClient13.hostnameVerifier");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines

    }
}