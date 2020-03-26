/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ServerStartJVMOptionsTest {
    private static final Class<?> c = ServerStartJVMOptionsTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    static String executionDir;
    static String serverCommand;

    @Rule
    public TestName testName = new TestName();

    private static LibertyServer server;
    static File jvmoptionsconfigdefaults;
    static File jvmoptionsserverroot;
    static File jvmoptionsconfigoverrides;
    static File etcjvmoptions;
    static File sharedjvmoptions;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        executionDir = server.getInstallRoot();
        jvmoptionsconfigdefaults = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/configDropins/defaults/jvm.options");
        jvmoptionsserverroot = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/jvm.options");
        jvmoptionsconfigoverrides = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/configDropins/overrides/jvm.options");
        etcjvmoptions = new File(executionDir + "/etc/jvm.options");
        etcjvmoptions.mkdirs();
        sharedjvmoptions = new File(executionDir + "/usr/shared/jvm.options");
        sharedjvmoptions.getParentFile().mkdirs();
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
     * This test ensures the server can be started with no jvm.options files or merged.jvm.options file
     */
    @Test
    @Mode(TestMode.FULL)
    public void testServerStartNoJVMOptions() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);
        Log.info(c, testName.getMethodName(), "server start stdout = " + po.getStdout());
        Log.info(c, testName.getMethodName(), "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        server.stopServer();
    }

    /**
     * This test ensures the server fails over to etc/jvm.options if none are found
     */
    @Test
    @Mode(TestMode.FULL)
    public void testServerStartNoJVMOptionsETCFailover() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

        Writer isw = new OutputStreamWriter(new FileOutputStream(etcjvmoptions), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("-DTest1=Test1");
        bw.close();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean foundTest1 = false;
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("JavaRuntimeInformation.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, testName.getMethodName(), "Run" + i + ": " + line);
                    if (line.contains("-DTest1=Test1")) {
                        foundTest1 = true;
                        break;
                    }
                    i++;
                }

                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        dumpFile.delete();
        assertTrue("The jvm option was not found", foundTest1);

        server.stopServer();
    }

    /**
     * This test ensures the server can be started with one jvm.options file
     */
    @Test
    @Mode(TestMode.FULL)
    public void testServerStartOneJVMOption() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

        Writer isw = new OutputStreamWriter(new FileOutputStream(jvmoptionsconfigdefaults), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("-DTest1=Test1");
        bw.close();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean foundTest1 = false;
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("JavaRuntimeInformation.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, testName.getMethodName(), "Run" + i + ": " + line);
                    if (line.contains("-DTest1=Test1")) {
                        foundTest1 = true;
                        break;
                    }
                    i++;
                }

                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        dumpFile.delete();
        assertTrue("The jvm option was not found", foundTest1);

        server.stopServer();
    }

    /**
     * This test ensures the server can be started with all jvm.options files and
     * the right options are used at the end
     */
    @Test
    public void testServerStartAllJVMOptions() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

        Writer isw = new OutputStreamWriter(new FileOutputStream(jvmoptionsconfigdefaults), "UTF-8");
        BufferedWriter bw = new BufferedWriter(isw);
        bw.write("-DTest=Bad Value\n");
        bw.write("-DTest2=Bad Value\n");
        bw.write("-DTest3=Good Value\n");
        bw.write("-Xmx512m\n");
        bw.close();
        Writer isw2 = new OutputStreamWriter(new FileOutputStream(jvmoptionsserverroot), "UTF-8");
        BufferedWriter bw2 = new BufferedWriter(isw2);
        bw2.write("-DTest=Good Value\n");
        bw2.write("-DTest4=Bad Value\n");
        bw2.write("-DTest5=Good Value\n");
        bw2.write("-Xmx1024m\n");
        bw2.close();
        Writer isw3 = new OutputStreamWriter(new FileOutputStream(jvmoptionsconfigoverrides), "UTF-8");
        BufferedWriter bw3 = new BufferedWriter(isw3);
        bw3.write("-DTest2=Good Value\n");
        bw3.write("-DTest4=Good Value\n");
        bw3.write("-DTest6=Good Value\n");
        bw3.write("-XX:MaxPermSize=512m");
        bw3.close();
        Writer isw4 = new OutputStreamWriter(new FileOutputStream(sharedjvmoptions), "UTF-8");
        BufferedWriter bw4 = new BufferedWriter(isw4);
        bw4.write("-DTest2=Bad Value\n");
        bw4.write("-DTest7=Good Value\n");
        bw4.close();
        Writer isw5 = new OutputStreamWriter(new FileOutputStream(etcjvmoptions), "UTF-8");
        BufferedWriter bw5 = new BufferedWriter(isw5);
        bw5.write("-DTest8=Bad Value\n");
        bw5.close();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean[] foundTest = new boolean[8];
        for (int i = 0; i < 8; i++) {
            foundTest[i] = false;
        }
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("JavaHeapInfo.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("MaxHeapSizeLimit")) {
                        String num[] = line.split(":");
                        int heapsize = Integer.parseInt(num[1].substring(1));
                        assertTrue("JVM Option " + i + " wasn't found", (heapsize == 1073741824));
                    }
                }
            }
            if (entryName.endsWith("JavaRuntimeInformation.txt")) {
                boolean afterSysProps = false;
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, "testJVMOptionsMergingAllOptions", "Run" + i + ": " + line);
                    if (line.contains("Java System Properties")) {
                        afterSysProps = true;
                    }
                    if (line.contains("-XX:MaxPermSize=512m")) {
                        foundTest[0] = true;
                    }
                    if (afterSysProps) {
                        if (line.contains("Test3=Good")) {
                            foundTest[1] = true;
                        } else if (line.contains("Test=Good Value")) {
                            foundTest[2] = true;
                        } else if (line.contains("Test5=Good Value")) {
                            foundTest[3] = true;
                        } else if (line.contains("Test2=Good Value")) {
                            foundTest[4] = true;
                        } else if (line.contains("Test4=Good Value")) {
                            foundTest[5] = true;
                        } else if (line.contains("Test6=Good Value")) {
                            foundTest[6] = true;
                        } else if (line.contains("Test7=Good Value")) {
                            foundTest[7] = true;
                        } else if (line.contains("Test=Bad Value")) {
                            fail("Contains Option 'Test=Bad Value' that it shouldn't");
                        } else if (line.contains("Test2=Bad Value")) {
                            fail("Contains Option 'Test2=Bad Value' that it shouldn't");
                        } else if (line.contains("Test4=Bad Value")) {
                            fail("Contains Option 'Test4=Bad Value' that it shouldn't");
                        } else if (line.contains("Test2=Bad Value")) {
                            fail("Contains Option 'Test2=Bad Value' that it shouldn't");
                        } else if (line.contains("Test8=Bad Value")) {
                            fail("Contains Option 'Test8=Bad Value' that it shouldn't");
                        }
                    }
                    i++;
                }
                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        for (int i = 0; i < 8; i++) {
            assertTrue("JVM Option " + i + " wasn't found", foundTest[i]);
        }

        server.stopServer();
    }

    @Test
    public void testStartServerCmdlineJVMOption() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[3];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "-Dfoo=bar";

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean foundTest1 = false;
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("JavaRuntimeInformation.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                boolean afterSystemProperies = false;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, testName.getMethodName(), "Run" + i + ": " + line);
                    if (line.contains("Java System Properties"))
                        afterSystemProperies = true;
                    if (afterSystemProperies && line.contains("foo=bar")) {
                        foundTest1 = true;
                        break;
                    }
                    i++;
                }

                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        dumpFile.delete();
        assertTrue("The jvm option foo=bar was not found", foundTest1);

        server.stopServer();
    }

    @Test
    public void testStartServerCmdlineJVMOptionsAndSystemProps() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[6];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "-Dfoo=bar";
        parms[3] = "--";
        parms[4] = "--wom=bat";
        parms[5] = "--help=me";

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean foundTest1 = false;
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("ConfigVariables.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                boolean afterSystemProperies = false;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, testName.getMethodName(), "Run" + i + ": " + line);
                    if (line.contains("User Config Variables"))
                        afterSystemProperies = true;
                    if (afterSystemProperies && line.contains("wom=bat")) {
                        foundTest1 = true;
                        break;
                    }
                    i++;
                }

                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        dumpFile.delete();
        assertTrue("The config variable wom=bat was not found", foundTest1);

        server.stopServer();
    }

    /**
     * This test ensures the beta system property is set. It can either be true or false depending on the
     * early access tag in the productInfo
     */
    @Test
    @Mode(TestMode.LITE)
    public void testServerStartBETAJVMOption() throws Exception {
        Log.entering(c, testName.getMethodName());

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        initialize();

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
            Log.info(c, testName.getMethodName(), "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        ZipFile zipFile = new ZipFile(dumpFile);

        boolean foundTest1 = false;
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("JavaRuntimeInformation.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    Log.info(c, testName.getMethodName(), "Run" + i + ": " + line);
                    if (line.contains("com.ibm.ws.beta.edition=false") || line.contains("com.ibm.ws.beta.edition=true")) {
                        foundTest1 = true;
                        break;
                    }
                    i++;
                }

                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();
        dumpFile.delete();
        assertTrue("JVM option com.ibm.ws.beta.edition was not found", foundTest1);

        server.stopServer();
    }

    public void initialize() {
        File dirs = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/configDropins/defaults");
        File dirs2 = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/configDropins/overrides");
        dirs.mkdirs();
        dirs2.mkdirs();
        if (jvmoptionsconfigdefaults.exists()) {
            jvmoptionsconfigdefaults.delete();
        }
        if (jvmoptionsserverroot.exists()) {
            jvmoptionsserverroot.delete();
        }
        if (jvmoptionsconfigoverrides.exists()) {
            jvmoptionsconfigoverrides.delete();
        }
        if (sharedjvmoptions.exists()) {
            sharedjvmoptions.delete();
        }
        if (etcjvmoptions.exists()) {
            etcjvmoptions.delete();
        }
        File bootstrapPropFile = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/bootstrap.properties");
        if (bootstrapPropFile.exists()) {
            bootstrapPropFile.delete();
        }
    }
}