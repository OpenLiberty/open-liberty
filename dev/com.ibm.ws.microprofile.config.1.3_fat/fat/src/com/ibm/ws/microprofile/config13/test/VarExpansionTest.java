/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config13.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config13.varExpansion.web.VarExpansionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.fat.repeat.ConfigRepeatActions;

/**
 * FAT test class for variable expansion in MicroProfile Config.
 */
@RunWith(FATRunner.class)
public class VarExpansionTest extends FATServletClient {

    public static final String SERVER_NAME = "VarExpansionServer";
    public static final String APP_NAME = "varExpansionApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = VarExpansionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ConfigRepeatActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, APP_NAME, options, "com.ibm.ws.microprofile.config13.varExpansion.*");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
