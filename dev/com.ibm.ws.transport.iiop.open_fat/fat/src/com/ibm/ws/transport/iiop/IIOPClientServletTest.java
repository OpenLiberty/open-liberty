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

import static com.ibm.ws.transport.iiop.FATSuite.TEST_CORBA_REMOTE_WAR;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.remote.war.IIOPClientServlet;

@RunWith(FATRunner.class)
public class IIOPClientServletTest extends FATServletClient {
    @Server("bandyball")
    @TestServlet(servlet = IIOPClientServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer iiopClient;

    @Server("basketball")
    public static LibertyServer iiopServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(iiopServer, FATSuite.TEST_CORBA_EAR);
        iiopServer.startServer();
        ShrinkHelper.exportDropinAppToServer(iiopClient, TEST_CORBA_REMOTE_WAR);
        iiopClient.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        iiopClient.stopServer();
        iiopServer.stopServer();
    }
}
