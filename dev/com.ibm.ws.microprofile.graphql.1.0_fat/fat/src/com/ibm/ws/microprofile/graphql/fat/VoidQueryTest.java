/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.fat;

import java.util.List;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpGraphQL10.voidQuery.VoidQueryTestServlet;

@RunWith(FATRunner.class)
public class VoidQueryTest {

    private static final String SERVER = "mpGraphQL10.voidQuery";
    private static final String APP_NAME = "voidQueryApp";

    @Server(SERVER)
    @TestServlet(servlet = VoidQueryTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "mpGraphQL10.voidQuery");
        server.startServer();
    }

    @Test
    public void checkForStartFailureMessage() throws Exception {
        String expectedErrorMsg = server.waitForStringInLog("CWMGQ0001E");
        assertThat(expectedErrorMsg, containsString(APP_NAME));
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWWC0001W", "SRVE0190E", "CWMGQ0001E");
    }
}
