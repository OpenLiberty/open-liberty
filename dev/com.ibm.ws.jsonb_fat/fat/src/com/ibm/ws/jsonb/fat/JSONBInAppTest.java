/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsonb.fat;

import static com.ibm.ws.jsonb.fat.FATSuite.JSONB_APP;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.jsonbtest.JSONBTestServlet;
import web.jsonbtest.YassonTestServlet;

@RunWith(FATRunner.class)
public class JSONBInAppTest extends FATServletClient {

    @Server("com.ibm.ws.jsonb.inapp")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = YassonTestServlet.class, contextRoot = JSONB_APP) //This test suite always uses Yasson
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(JSONBInAppTest.class, "setUp", "=====> Start JSONBInAppTest");

        FATSuite.configureImpls(server);
        FATSuite.jsonbApp(server);

        server.startServer();

        if (JakartaEE10Action.isActive()) { //TODO possibly back port this info message to EE9 and EE8
            assertTrue(!server.findStringsInLogsAndTrace("CWWKJ0351I").isEmpty());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();

        Log.info(JSONBInAppTest.class, "tearDown", "<===== Stop JSONBInAppTest");
    }
}
