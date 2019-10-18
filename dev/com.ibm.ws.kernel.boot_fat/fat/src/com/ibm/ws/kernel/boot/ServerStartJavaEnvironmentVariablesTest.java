package com.ibm.ws.kernel.boot;
/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ServerStartJavaEnvironmentVariablesTest {
    private static final Class<?> c = ServerStartJavaEnvironmentVariablesTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    static String executionDir;
    static String serverCommand;

    @Rule
    public TestName testName = new TestName();

    private static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        executionDir = server.getInstallRoot();
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS)
            serverCommand = "bin\\server.bat";
        else
            serverCommand = "bin/server";
    }

    @AfterClass
    public static void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Ensures that OPENJ9_JAVA_OPTIONS and IBM_JAVA_OPTIONS enviroment variables
     * are both set and are equal to eachother
     */
    @Test
    public void testServerStartJavaOptionsSet() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[] { "start", SERVER_NAME };

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);
        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        ProgramOutput dumpOut = server.serverDump();

        File[] filesAfterDump = new File(executionDir + "/usr/servers/" + SERVER_NAME).listFiles();

        File dumpFile = new File("");
        for (File f : filesAfterDump) {
            String fileName = f.getName();
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }
        assertTrue("The Dump File was not found", dumpFile.getPath().compareTo("") != 0);

        ZipFile zipFile = new ZipFile(dumpFile);
        try {
            for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                ZipEntry entry = en.nextElement();
                if (entry.getName().endsWith("EnvironmentVariables.txt")) {
                    InputStream entryInputStream = zipFile.getInputStream(entry);
                    BufferedReader entryReader = new BufferedReader(new InputStreamReader(entryInputStream));

                    String[] firstLine = new String[2], secondLine = new String[2];
                    while ((firstLine = entryReader.readLine().split("=", 2)) != null) {
                        if (firstLine[0].startsWith("IBM_JAVA_OPTIONS")) {
                            while ((secondLine = entryReader.readLine().split("=", 2)) != null) {
                                if (secondLine[0].equals("OPENJ9_JAVA_OPTIONS")) {
                                    Log.info(c, testName.getMethodName(), String.format("%-20s=%s", firstLine[0], firstLine[1]));
                                    Log.info(c, testName.getMethodName(), String.format("%-20s=%s", secondLine[0], secondLine[1]));
                                    assertTrue("IBM_JAVA_OPTIONS did not equal OPENJ9_JAVA OPTIONS", firstLine[1].equals(secondLine[1]));
                                    break;
                                }
                            }
                            assertTrue("OPENJ9_JAVA_OPTIONS was not found", secondLine[0].equals("OPENJ9_JAVA_OPTIONS"));
                            break;
                        }
                    }
                    assertTrue("IBM_JAVA_OPTIONS was not found", firstLine[0].equals("IBM_JAVA_OPTIONS"));

                    entryReader.close();
                    entryInputStream.close();
                }
            }
        } finally {
            try {
                zipFile.close();
                dumpFile.delete();
            } catch (IOException ex) {
            }
        }

        server.stopServer();
    }
}
