/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class SimpleJSPTest extends LoggingTest {
    public static LibertyServer server;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("cdi12BasicServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application simpleJSPApp started");
    }

    @Test
    public void testSimpleJSP() throws Exception {
        HttpUtils.findStringInUrl(server, "/simpleJSPApp/", "Test Sucessful!");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
