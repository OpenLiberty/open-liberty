/*
 * Copyright (c) 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.web.war.IIOPDelegatingServlet;

@RunWith(FATRunner.class)
public class IIOPWorkContextTest extends FATServletClient {
    @Server("baseball")
    @TestServlet(servlet = IIOPDelegatingServlet.class, contextRoot = "test.corba.web")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        for (Archive<?> app: FATSuite.SERVER_APPS) ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer();
    }

    @Before
    public void markLog() throws Exception {
        server.setMarkToEndOfLog();
    }

    @After
    public void checkForIIOPWorkContextMsg() throws Exception {
        final String method = "checkForIIOPWorkContextMsg";
        System.out.println(" ## Test IIOP WorkContext msg in log ### - " + method);

        // verify that the task interceptor ran by looking for the System.out.println it puts in the server log
        List<String> strings = server.findStringsInLogs("This runnable has work context. The type is IIOP.");
        assertTrue("Did not find 'This runnable has work context. The type is IIOP.' in log file", strings.size() > 0);
        strings.forEach(System.out::println);
        System.out.println(" End - Check for iiop type ");    
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
