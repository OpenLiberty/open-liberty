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
package com.ibm.ws.jsonb.fat;

import static com.ibm.ws.jsonb.fat.FATSuite.JSONB_APP;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.jsonbtest.JSONBTestServlet;
import web.jsonbtest.JohnzonTestServlet;

@RunWith(FATRunner.class)
public class JSONBInAppTest extends FATServletClient {

    @Server("com.ibm.ws.jsonb.inapp")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = JohnzonTestServlet.class, contextRoot = JSONB_APP) // TODO: once https://github.com/eclipse-ee4j/jsonp/issues/78 is resolved, switch back to Yasson
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.jsonbApp(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
