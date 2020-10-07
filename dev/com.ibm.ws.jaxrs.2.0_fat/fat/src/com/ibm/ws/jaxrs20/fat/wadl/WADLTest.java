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
package com.ibm.ws.jaxrs20.fat.wadl;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class WADLTest {

    @Server("com.ibm.ws.jaxrs.fat.wadl")
    public static LibertyServer server;

    /**
     * True if running on Windows and the .bat file should be used.
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    private static final String TEST_PACKAGE_DIR = new StringBuilder().append("com").append(File.separator).append("ibm").append(File.separator).append("ws").append(File.separator).append("jaxrs").append(File.separator).append("wadl").toString();
    private static final String TEST_PACKAGE = "com.ibm.ws.jaxrs.wadl";

    private RemoteFile WADL2JAVASrcDir;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "wadl", "com.ibm.ws.jaxrs.fat.wadl");
    }

    @Before
    public void start() throws Exception {
        WADL2JAVASrcDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "WADL2JAVASrc");
    }

    @After
    public void tearDown() throws Exception {
        if (null != server && server.isStarted()) {
            server.stopServer();
        }
    }

    @Mode(TestMode.LITE)
    @Test
    public void testWADL2JAVATool() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wadl");

        String TEST_WADL_LOCATION = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/wadl/1/Order?_wadl").toString();

        String wadl2javaPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java").toString();
        String wadl2javaBatPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java.bat").toString();

        RemoteFile wadl2java = server.getFileFromLibertyInstallRoot(wadl2javaPath);
        RemoteFile wadl2javaBat = server.getFileFromLibertyInstallRoot(wadl2javaBatPath);
        String wadl2javaArgs = new StringBuilder().append("-d ").append(WADL2JAVASrcDir.getAbsolutePath()).append(" -p ").append(TEST_PACKAGE).append(" ").append(TEST_WADL_LOCATION).toString();

        assertTrue("The file bin/jaxrs/wadl2java does not exist.", wadl2java.exists());
        assertTrue("The file bin/jaxrs/wadl2java.bat does not exist.", wadl2javaBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wadl2javaBat.getAbsolutePath());
        } else {
            if (WLP_CYGWIN_HOME == null) {
                commandBuilder.append("/bin/sh");
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wadl2java.getAbsolutePath());
        }
        commandBuilder.append(" ").append(wadl2javaArgs);

        StringBuilder outputBuilder = new StringBuilder();

        for (String line : execute(commandBuilder.toString())) {
            outputBuilder.append(line);
        }

        Log.info(WADLTest.class, "print Output Msg for testWADL2JAVATool", outputBuilder.toString());

        RemoteFile orderResource = server.getFileFromLibertyServerRoot("temp" + File.separator + "WADL2JAVASrc" + File.separator
                                                                       + TEST_PACKAGE_DIR
                                                                       + File.separator + "OrderResource.java");

        assertTrue("OrderResource.java does not exist.", orderResource.exists());
    }

    @Test
    public void testWADL2JAVAToolNoWADL() throws Exception {
        String wadl2javaPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java").toString();
        String wadl2javaBatPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java.bat").toString();

        RemoteFile wadl2java = server.getFileFromLibertyInstallRoot(wadl2javaPath);
        RemoteFile wadl2javaBat = server.getFileFromLibertyInstallRoot(wadl2javaBatPath);
        String wadl2javaArgs = new StringBuilder().append("-p ").append(TEST_PACKAGE).toString();

        assertTrue("The file bin/wsimport does not exist.", wadl2java.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wadl2javaBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wadl2javaBat.getAbsolutePath());
        } else {
            if (WLP_CYGWIN_HOME == null) {
                commandBuilder.append("/bin/sh");
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wadl2java.getAbsolutePath());
        }
        commandBuilder.append(" ").append(wadl2javaArgs);

        StringBuilder outputBuilder = new StringBuilder();

        for (String line : execute(commandBuilder.toString(), 1)) {
            outputBuilder.append(line);
        }
        Log.info(WADLTest.class, "print Output Msg for testWADL2JAVAToolNoWADL", outputBuilder.toString());

        assertTrue("The output should contain the error message 'Missing argument: wadl', but the actual is " + outputBuilder.toString() + "\n" + "commandBuilder="
                   + commandBuilder.toString() + "\nwadl2javaArgs=" + wadl2javaArgs,
                   outputBuilder.toString().indexOf("Missing argument: wadl") >= 0);
    }

    private List<String> execute(String commandLine) throws IOException, InterruptedException {
        return execute(commandLine, 0);
    }

    private List<String> execute(String commandLine, int expectedExitValue) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        for (String arg : commandLine.split(" ")) {
            command.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        builder.environment().put("JAVA_HOME", server.getMachineJavaJDK());

        final Process p = builder.start();
        List<String> output = new ArrayList<String>();

        Thread stderrCopier = new Thread(new OutputStreamCopier(p.getErrorStream(), output));
        stderrCopier.start();
        new OutputStreamCopier(p.getInputStream(), output).run();

        stderrCopier.join();
        p.waitFor();

        int exitValue = p.exitValue();
        if (exitValue != expectedExitValue) {
            throw new IOException(command.get(0) + " failed (exit=" + exitValue + ", expected " + expectedExitValue + "): " + output + "\ncommandLine=" + commandLine);
        }

        return output;
    }

    private class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final List<String> output;

        OutputStreamCopier(InputStream in, List<String> lines) {
            this.in = in;
            this.output = lines;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                boolean inEval = false;
                int carryover = 0;

                for (String line; (line = reader.readLine()) != null;) {
                    // Filter empty lines and sh -x trace output.
                    if (inEval) {
                        System.out.println("(trace eval) " + line);
                        if (line.trim().equals("'")) {
                            inEval = false;
                        }
                    } else if (line.equals("+ eval '")) {
                        inEval = true;
                        System.out.println("(trace eval) " + line);
                    } else if (carryover > 0) {
                        carryover--;
                        System.out.println("(trace) " + line);
                    } else if (line.startsWith("+") || line.equals("'")) {
                        int index = 0;
                        index = line.indexOf("+", index + 1);
                        while (index != -1) {
                            index = line.indexOf("+", index + 1);
                            carryover++;
                        }
                        System.out.println("(trace) " + line);
                    } else if (!line.isEmpty()) {
                        synchronized (output) {
                            output.add(line);
                        }
                        System.out.println(line);
                    }
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }
}
