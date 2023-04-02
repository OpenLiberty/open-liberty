/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient20.sse.SseClientTestServlet;

@RunWith(FATRunner.class)
public class SseTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient20.sse";

    private static final String appName = "sseApp";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, 
                                                             MicroProfileActions.MP40, //mpRestClient-2.0
                                                             MicroProfileActions.MP50); // 3.0
                                                             // TODO: MP60 currently causes Java 2 security errors
                                                             // MicroProfileActions.MP60);// 3.0+EE10
    /*
     * We need two servers to clearly distinguish that the "client" server
     * only has the client features enabled - it includes mpRestClient-1.0
     * (which includes the jaxrsClient-2.0 feature) and mpConfig-1.1 (which
     * includes cdi-1.2), but not the jaxrs-2.0 feature that contains server
     * code. The client should be able to work on its own - by splitting out
     * the "server" server into it's own server, we can verify this.
     */
    @Server(SERVER_NAME)
    @TestServlet(servlet = SseClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient20.sse");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}