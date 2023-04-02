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
package com.ibm.ws.transaction.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.fat.util.XATestBase;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.XAServlet;

@RunWith(FATRunner.class)
public class XATest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/XAServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = XAServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static XATestBase xa = null;

    @BeforeClass
    public static void setUp() throws Exception {
        xa = new XATestBase(server, APP_NAME, SERVLET_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        xa.tearDown();
    }

    @Test
    public void testSetTransactionTimeoutReturnsTrue() throws Exception {
        xa.testSetTransactionTimeoutReturnsTrue();
    }

    @Test
    public void testSetTransactionTimeoutReturnsFalse() throws Exception {
        xa.testSetTransactionTimeoutReturnsFalse();
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testSetTransactionTimeoutThrowsException() throws Exception {
        xa.testSetTransactionTimeoutThrowsException();
    }
}