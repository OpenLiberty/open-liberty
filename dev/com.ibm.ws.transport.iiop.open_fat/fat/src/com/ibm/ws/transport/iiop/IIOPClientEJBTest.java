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
import test.corba.web.war.IIOPDelegatingServlet;

@RunWith(FATRunner.class)
public class IIOPClientEJBTest extends FATServletClient {
    @Server("buckyball")
    @TestServlet(servlet = IIOPDelegatingServlet.class, contextRoot = "test.corba.web")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        for (Archive<?> app: FATSuite.SERVER_APPS) ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
