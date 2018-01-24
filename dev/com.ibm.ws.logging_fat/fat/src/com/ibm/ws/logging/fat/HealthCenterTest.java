/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class HealthCenterTest {
    private static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.healthcenter");
        ShrinkHelper.defaultDropinApp(server, "logger-servlet", "com.ibm.ws.logging.fat.logger.servlet");

        Assume.assumeTrue(JavaInfo.forServer(server).vendor().equals(JavaInfo.Vendor.IBM));

        if (!server.isStarted())
            server.startServer();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testHealthCenterInfo() throws Exception {
        Assert.assertFalse("Expected healthcenter INFO message",
                           server.findStringsInLogs("^INFO:.*com\\.ibm\\.java\\.diagnostics\\.healthcenter\\.agent\\.iiop\\.port",
                                                    server.getConsoleLogFile()).isEmpty());
    }

    @Test
    public void testConsoleLogLevelOff() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/logger-servlet", "Hello world!");
        List<String> messages = server.findStringsInLogs("Hello world!", server.getConsoleLogFile());
        Assert.assertTrue("Did not expect to find servlet Logger message: " + messages, messages.isEmpty());
    }
}
