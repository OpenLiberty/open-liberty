/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop;

import static com.ibm.websphere.simplicity.ShrinkHelper.exportDropinAppToServer;
import static com.ibm.ws.transport.iiop.FATSuite.TEST_CORBA_REMOTE_WAR;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.remote.war.CosNamingClientStringToObjectServlet;
import test.iiop.common.NamingUtil;

@RunWith(FATRunner.class)
public class CosNamingViaStringToObjectFatTest extends FATServletClient {
    @Server("basketball")
    public static LibertyServer iiopServer;

    @Server("bandyball")
    @TestServlet(servlet = CosNamingClientStringToObjectServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer iiopClient;

    @BeforeClass
    public static void startServers() throws Exception {
        exportDropinAppToServer(iiopClient, TEST_CORBA_REMOTE_WAR);
        iiopClient.startServer();
        iiopServer.startServer();
        iiopServer.waitForStringInLog(NamingUtil.CNC_LOG_MSG);
    }

    @AfterClass
    public static void stopServers() throws Exception {
        iiopClient.stopServer();
        iiopServer.stopServer();
    }
}
