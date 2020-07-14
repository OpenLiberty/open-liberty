/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class LogstashCollectorIndependentTest extends LogstashCollectorTest {

    private static Class<?> c = LogstashCollectorIndependentTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashCollectorServer");

    private static String JVMSecurity = System.getProperty("Djava.security.properties");

    protected static boolean runTest;

    @MinimumJavaLevel(javaLevel = 8)
    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());

        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

        String extendedPath = "usr/servers/LogstashServer/jvm.options";
        if (server.getServerRoot().contains(server.getInstallRoot())) {
            extendedPath = server.getServerRoot().replaceAll(server.getInstallRoot(), "").substring(1);
        }
        server.copyFileToLibertyInstallRoot(extendedPath, "jvm.options");
        server.copyFileToLibertyInstallRoot(extendedPath.replace("jvm.options", "java.security"), "java.security");
        serverStart();
    }

    /* Test that logstash-collector-1.0 feature can run independently, without any other features present */
    @MinimumJavaLevel(javaLevel = 8)
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

    @MinimumJavaLevel(javaLevel = 8)
    @After
    public void tearDown() {
    }

    @MinimumJavaLevel(javaLevel = 8)
    @AfterClass
    public static void completeTest() throws Exception {
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
                resetServerSecurity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetServerSecurity() {
        //Reset JVM security to its original value
        System.setProperty("Djava.security.properties", JVMSecurity);
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
