/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class LogstashCollectorIndependentTest extends LogstashCollectorTest {

    private static Class<?> c = LogstashCollectorIndependentTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashCollectorServer");

    protected static boolean runTest;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        clearContainerOutput();
        String host = logstashContainer.getHost();
        String port = String.valueOf(logstashContainer.getMappedPort(5043));
        Log.info(c, "setUp", "Logstash container: host=" + host + "  port=" + port);
        server.addEnvVar("LOGSTASH_HOST", host);
        server.addEnvVar("LOGSTASH_PORT", port);

        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());
        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());
        serverStart();
    }

    /* Test that logstash-collector-1.0 feature can run independently, without any other features present */
    @AllowedFFDC({ "java.lang.NullPointerException", "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    @Test
    public void logstashCollectorIndependentTest() throws Exception {
        RemoteFile logFile = server.getDefaultLogFile();

        /* Search for Could not resolve module errors in messages.log */
        List<String> serverErrors = server.findStringsInLogs("CWWKE0702E*|CWWKF0029E*", logFile);
        boolean errors = false;

        /* If error messages are found in the log, the test fails */
        if (!serverErrors.isEmpty()) {
            errors = true;
        }
        assertFalse("Errors starting up server", errors);

    }

    @After
    public void tearDown() {
    }

    @AfterClass
    public static void completeTest() throws Exception {
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setConfig(String conf) throws Exception {
        Log.info(c, "setConfig entry", conf);
        getServer().setMarkToEndOfLog();
        getServer().setServerConfigurationFile(conf);
        assertNotNull("Cannot find CWWKG0016I from messages.log", getServer().waitForStringInLogUsingMark("CWWKG0016I", 10000));
        String line = getServer().waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 10000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from messages.log", line);
        Log.info(c, "setConfig exit", conf);
    }

    private static void serverStart() throws Exception {
        serverSecurityOverwrite();
        Log.info(c, "serverStart", "--->  Starting Server.. ");

        server.startServer();
    }

    private static void serverSecurityOverwrite() throws Exception {
        //Overwrite JVM security setting to enable logstash Collector as a temporary fix
        System.setProperty("Djvm.options.properties", server.getServerRoot() + "/jvm.options");
    }

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }
}
