/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.beans.Transient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VerboseLogTest {
    private static final Class<?> c = VerboseLogTest.class;

    @Rule
    public TestName testName = new TestName();

    private static LibertyServer server;
    private final static String SERVER_NAME = "com.ibm.ws.kernel.boot.verbose.fat";
    static String executionDir;
    static File verboseLog;
    static File verboseLogServerRoot;
    static String serverCommand;
    static File jvmoptionsserverroot;
    static File serverEnvServerRoot;
    static boolean isOpenJ9 = false;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        executionDir = server.getInstallRoot();
        verboseLog = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/logs/verbosegc.001.log");
        verboseLogServerRoot = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/verbosegc.log");
        jvmoptionsserverroot = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/jvm.options");
        serverEnvServerRoot = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/server.env");
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS){
            serverCommand = "bin\\server.bat";
        }
        else{
            serverCommand = "bin/server";
        }

        Properties properties = System.getProperties();
        if (String.valueOf(properties.get("java.vm.name")).toLowerCase().contains("openj9")  ||
            String.valueOf(properties.get("java.vm.vendor")).toLowerCase().contains("openj9") ||
            String.valueOf(properties.get("java.vm.version")).toLowerCase().contains("openj9") ||
            String.valueOf(properties.get("java.vm.info")).toLowerCase().contains("openj9"))
        {
            isOpenJ9 = true;
        }
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testNonOpenJ9DefaultVerboseLogging() throws Exception {
        assumeTrue(!!!isOpenJ9);

        // Test with no jvm.options, verbose log should appear by default
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should not have been created because the JVM is not OpenJ9", !verboseLog.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9DefaultVerboseLogging() throws Exception {
        assumeTrue(isOpenJ9);

        // Test with no jvm.options, verbose log should appear by default
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should have been created", verboseLog.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9JvmTurnOffVerbose() throws Exception {
        assumeTrue(isOpenJ9);

        // Test with jvm.options to turn off verbose log, no verbose log should appear
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Writer isw = new OutputStreamWriter(new FileOutputStream(jvmoptionsserverroot), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("-Dverbosegc=false\n");
        bw.close();

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should not be created", !verboseLog.exists());
        assertTrue("verbosegc log should not be created in server root", !verboseLogServerRoot.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9ServerEnvTurnOffVerbose() throws Exception {
        assumeTrue(isOpenJ9);

        // Test with server.env to turn off verbose log, no verbose log should appear
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Writer isw = new OutputStreamWriter(new FileOutputStream(serverEnvServerRoot), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("VERBOSEGC=false\n");
        bw.close();

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should not be created", !verboseLog.exists());
        assertTrue("verbosegc log should not be created in server root", !verboseLogServerRoot.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9ServerEnvKeepVerbose() throws Exception {
        assumeTrue(isOpenJ9);
        
        // Test with server.env to keep verbose log, verbose log should appear
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Writer isw = new OutputStreamWriter(new FileOutputStream(serverEnvServerRoot), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("VERBOSEGC=true\n");
        bw.close();

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should be created", verboseLog.exists());
        assertTrue("verbosegc log should not be created in server root", !verboseLogServerRoot.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9JvmChangeVerbose() throws Exception {
        assumeTrue(isOpenJ9);
        
        // Test with jvm.options, change location or file name, the jvm.options log should be the one shown
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Writer isw = new OutputStreamWriter(new FileOutputStream(jvmoptionsserverroot), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("-Xverbosegclog:verbosegc.log\n");
        bw.close();

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should have been created in server root", verboseLogServerRoot.exists());
        assertTrue("verbosegc log should not be created in log dir", !verboseLog.exists());

        server.stopServer();
    }

    @Test
    public void testOpenJ9StartChangeVerbose() throws Exception {
        assumeTrue(isOpenJ9);
        
        // Test with command variable, change location or file name, the jvm.options log should be the one shown
        // ex. ./bin/server start --verbose:gc
        Log.entering(c, testName.getMethodName());

        deleteLeftoverFiles();

        String[] parms = new String[4];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "--";
        parms[3] = "-Xverbosegclog:verbosegc.log";

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());
        assertTrue("verbosegc log should have been created in server root", verboseLogServerRoot.exists());
        assertTrue("verbosegc log should not be created in log dir", !verboseLog.exists());

        server.stopServer();
    }

    private void deleteLeftoverFiles() {
        if(jvmoptionsserverroot.exists()){
            jvmoptionsserverroot.delete();
        }
        if(serverEnvServerRoot.exists()){
            serverEnvServerRoot.delete();
        }
        if(verboseLog.exists()){
            verboseLog.delete();
        }
        if(verboseLogServerRoot.exists()){
            verboseLogServerRoot.delete();
        }
    }
}
