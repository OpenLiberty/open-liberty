/*******************************************************************************
 * Copyright (c) 2019, 2024, 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.graphql.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpGraphQL10.defaultvalue.DefaultValueTestServlet;
import io.openliberty.microprofile.graphql.fat.repeat.GraphQlRepeatActions;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

@RunWith(FATRunner.class)
public class DefaultValueTest extends FATServletClient {

    private static final String SERVER = "mpGraphQL10.defaultvalue";
    private static final String APP_NAME = "defaultvalueApp";

    @Server(SERVER)
    @TestServlet(servlet = DefaultValueTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = GraphQlRepeatActions.repeatDefault(SERVER);

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY }, "mpGraphQL10.defaultvalue");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
