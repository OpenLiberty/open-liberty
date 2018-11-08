/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.java11_fat;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import java11.cnfe.web.CNFETestServlet;

@RunWith(FATRunner.class)
@AllowedFFDC("java.lang.ClassNotFoundException")
public class Java11CNFETest extends FATServletClient {

    private static final String APP_NAME = "cnfeApp";

    @Server("server_Java11CNFETest")
    @TestServlet(servlet = CNFETestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "java11.cnfe.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKL0084W");
    }

    @Test
    public void testClassForName() throws Exception {
        runTest();
        findStringInLog("CWWKL0084W: .* javax\\.jws\\.WebService.*jaxws-2\\.2");
    }

    @Test
    public void testClassForNameTCCL() throws Exception {
        runTest();
        // Don't look for a specific JAX-B class, since a different test may have triggered the message first
        findStringInLog("CWWKL0084W: .* javax\\.xml\\.bind.*jaxb-2\\.2");
    }

    private void runTest() throws Exception {
        runTest(server, APP_NAME + "/" + CNFETestServlet.class.getSimpleName(), testName.getMethodName());
    }

    private void findStringInLog(String regex) throws Exception {
        List<String> matches = server.findStringsInLogs(regex);
        assertTrue("Did not find expected string in logs: " + regex, matches.size() > 0);
    }

}
