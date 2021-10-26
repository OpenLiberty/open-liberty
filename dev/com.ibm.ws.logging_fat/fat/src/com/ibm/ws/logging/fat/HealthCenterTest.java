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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfoFATUtils;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class HealthCenterTest {
    private static LibertyServer server;

    @ClassRule
    public static HealthCenterRule skipRule = new HealthCenterRule();

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.healthcenter");
        ShrinkHelper.defaultDropinApp(server, "logger-servlet", "com.ibm.ws.logging.fat.logger.servlet");

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

    private static class HealthCenterRule implements TestRule {

        @Override
        public Statement apply(Statement stmt, Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (shouldRun(desc)) {
                        stmt.evaluate();
                    }
                }
            };
        }

        public static boolean shouldRun(Description desc) {
            Class<?> c = desc == null ? HealthCenterRule.class : desc.getTestClass();
            String m = (desc == null || desc.getMethodName() == null) ? "shouldRun" : desc.getMethodName();

            /*
             * Keystore is PKCS12 and was created using openjdk.
             * Our z/OS and SOE test systems use IBM JDK and will fail with
             * java.io.IOException: Invalid keystore format
             * since the keystore provider is SUN instead of IBMJCE.
             * Skip this test if JDK Vendor is IBM
             */
            if (!JavaInfoFATUtils.isSystemClassAvailable("com.ibm.ws.health.center.proxy.HCConnectorImpl")) {
                Log.info(c, m, "Skipping tests because JVM does not support Health Center");
                return false;
            }

            return true;
        }

    }
}
