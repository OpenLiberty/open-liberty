package com.ibm.ws.kernel.boot;
/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    private static final String OPENJ9_JAVA_OPTIONS = "OPENJ9_JAVA_OPTIONS";
    private static final String IBM_JAVA_OPTIONS = "IBM_JAVA_OPTIONS";
    private static final String XSHARECLASSES_STRING = "-Xshareclasses:";
    private static final String XSHARECLASSES_OPTION = XSHARECLASSES_STRING + "name=test,nonfatal,cacheDirPerm=999";
    private static final String XDUMP_OPTION = "-Xdump:what";

    /**
     * Ensures that OPENJ9_JAVA_OPTIONS environment variable is set with values added in the server script and
     * that IBM_JAVA_OPTIONS is set to the same value.
     */
    @Test
    public void testServerStartOpenJ9JavaOptionsSet() throws Exception {
        Log.entering(c, testName.getMethodName());

        try {
            Properties envVars = new Properties();
            envVars.put("CDPATH", ".");
            envVars.put(IBM_JAVA_OPTIONS, XDUMP_OPTION);
            envVars.put(OPENJ9_JAVA_OPTIONS, XSHARECLASSES_OPTION);

            File dumpFile = runServerAndDump(envVars);

            String openj9 = getEnvironmentVariable(dumpFile, OPENJ9_JAVA_OPTIONS);
            assertNotNull("The variable " + OPENJ9_JAVA_OPTIONS + " should be found", openj9);

            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should contain Xsharedlasses ", openj9.contains(XSHARECLASSES_STRING));
            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should only containe one Xsharedclasses definition",
                       openj9.indexOf(XSHARECLASSES_STRING) == openj9.lastIndexOf(XSHARECLASSES_STRING));
            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should contain the provided value", openj9.contains(XSHARECLASSES_OPTION));

            String ibmOptions = getEnvironmentVariable(dumpFile, IBM_JAVA_OPTIONS);
            assertNotNull("The variable " + IBM_JAVA_OPTIONS + " should be found", ibmOptions);
            assertTrue("The variable " + IBM_JAVA_OPTIONS + " should contain Xshareclasses", ibmOptions.contains(XSHARECLASSES_STRING));
            assertFalse("The variable " + IBM_JAVA_OPTIONS + " should not contain Xdump:what ", ibmOptions.contains(XDUMP_OPTION));

        } finally {
            server.stopServer();
        }
    }

    /**
     * In this test we set only IBM_JAVA_OPTIONS. The value should be modified by the server script, and OPENJ9_JAVA_OPTIONS should have the same value.
     *
     * @throws Exception
     */
    @Test
    public void testServerStartIBMJavaOptionsSet() throws Exception {
        Log.entering(c, testName.getMethodName());
        try {

            Properties envVars = new Properties();
            envVars.put("CDPATH", ".");
            envVars.put(IBM_JAVA_OPTIONS, XDUMP_OPTION);

            File dumpFile = runServerAndDump(envVars);

            String openj9 = getEnvironmentVariable(dumpFile, OPENJ9_JAVA_OPTIONS);
            assertNotNull("The variable " + OPENJ9_JAVA_OPTIONS + " should be found", openj9);

            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should contain Xshareclasses ", openj9.contains(XSHARECLASSES_STRING));
            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should only contain one Xshareclasses definition",
                       openj9.indexOf(XSHARECLASSES_STRING) == openj9.lastIndexOf(XSHARECLASSES_STRING));
            assertTrue("The variable " + OPENJ9_JAVA_OPTIONS + " should contain Xdump:what", openj9.contains(XDUMP_OPTION));

            String ibmOptions = getEnvironmentVariable(dumpFile, IBM_JAVA_OPTIONS);
            assertNotNull("The variable " + IBM_JAVA_OPTIONS + " should be found", ibmOptions);
            assertTrue("The variable " + IBM_JAVA_OPTIONS + " should contain Xshareclasses", ibmOptions.contains(XSHARECLASSES_STRING));
            assertTrue("The variable " + IBM_JAVA_OPTIONS + " should contain Xdump:what ", ibmOptions.contains(XDUMP_OPTION));

        } finally {
            server.stopServer();
        }
    }

    private String getEnvironmentVariable(File dumpFile, String variable) throws Exception {
        ZipFile zipFile = new ZipFile(dumpFile);
        try {
            for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                ZipEntry entry = en.nextElement();
                if (entry.getName().endsWith("EnvironmentVariables.txt")) {
                    InputStream entryInputStream = zipFile.getInputStream(entry);
                    BufferedReader entryReader = new BufferedReader(new InputStreamReader(entryInputStream));

                    try {
                        String line = null;
                        while ((line = entryReader.readLine()) != null) {
                            if (line.startsWith(variable + "=")) {
                                Log.info(c, testName.getMethodName(), line);
                                return line;
                            }
                        }

                        return null;
                    } finally {

                        entryReader.close();
                        entryInputStream.close();
                    }
                }
            }
        } finally {
            try {
                zipFile.close();
            } catch (IOException ex) {
            }
        }

        return null;
    }

    /**
     * @param envVars
     * @return
     */
    private File runServerAndDump(Properties envVars) throws Exception {
        String[] parms = new String[] { "start", SERVER_NAME };
        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);
        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        server.serverDump();

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

        return dumpFile;
    }
}
