/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

public class SecurityUtilityScriptTest {
    /**
     * Install directory property set by bvt.xml.
     */
    private static final String WLP_INSTALL_DIR = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("install.dir");
        }
    });

    private static final String JAVA_HOME = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("java.home");
        }
    });

    /**
     * The name of the server from build-bvt.xml.
     */
    private static final String SERVER_NAME = "com.ibm.ws.security.utility.bvt";

    /**
     * The name of the client from build-bvt.xml.
     */
    private static final String CLIENT_NAME = "com.ibm.ws.security.utility.bvt";

    /**
     * True if running on Windows and the .bat file should be used.
     */

    private static final boolean isWindows = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
        }
    });

    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getenv("WLP_CYGWIN_HOME");
        }
    });

    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @After
    public void cleanup() throws Exception {
        File keysFile = new File("ltpa.keys");
        assertTrue("Delete " + keysFile.toString(), keysFile.delete() || !keysFile.exists());
    }

    @Test
    public void testHelp() throws Exception {
        String findString = Pattern.quote("Usage: securityUtility {encode|createSSLCertificate|createLTPAKeys|help} [options]");
        List<String> output = execute(null, new ArrayList<String>());

        assertTrue("Usage Help should contain list of actions. Expected: '" + findString + "' Found: " + output, findMatchingLine(output, findString));

        output = execute(null, Arrays.asList("invalidaction"));
        assertTrue("Invalid actions help should produce two lines of output which contains invalid task name and usage. Output was: " + output, (findMatchingLine(output, "Unknown task: invalidaction") && findMatchingLine(output, findString)));

        output = execute(null, Arrays.asList("help"));
        assertTrue("Full help should contain encode help. Output was: " + output, findMatchingLine(output, "\\s*encode.*"));
        assertTrue("Usage Help should contain list of actions. Expected: '" + findString + "' Found: " + output, findMatchingLine(output, findString));

        output = execute(null, Arrays.asList("help", "encode"));
        assertFalse("Help for encode should not contain 'Actions'. Output was: " + output, findMatchingLine(output, "Actions:"));
    }

    @Test
    public void testEncode() throws Exception {
        final String textToEncode = "textToEncode";
        final String encodedText = "\\{xor\\}KzonKwswGjE8MDs6";

        List<String> argOutput = execute(null, Arrays.asList("encode", textToEncode));
        assertTrue("encode arg result. Output was: " + argOutput, findMatchingLine(argOutput, encodedText));

        // for some reason, the following two testcases might return more than one line due to some interferance with another output,
        // therefore, instead of checking the number of lines, check whether the one of the output lines starts with {aes}.
        // Now try AES encoding
        argOutput = execute(null, Arrays.asList("encode", "--encoding=aes", textToEncode));
        System.out.println("testEncode argOutput : " + argOutput);
        assertTrue("encode arg should be tagged as aes encoded by starting {aes}, it was: " + argOutput, findMatchingLine(argOutput, "^\\{aes\\}.*"));

        // Now try AES encoding with a non-standard key
        argOutput = execute(null, Arrays.asList("encode", "--encoding=aes", "--key=this is a key that alice wont know.", textToEncode));
        System.out.println("testEncode with key argOutput : " + argOutput);
        assertTrue("encode arg should be tagged as aes encoded by starting {aes}, it was: " + argOutput, findMatchingLine(argOutput, "^\\{aes\\}.*"));
    }

    @Test
    public void testCreateSSLCertificate() throws Exception {
        File certFile = new File(WLP_INSTALL_DIR + "/usr/servers/" + SERVER_NAME + "/resources/security/key.jks");
        assertTrue("Delete " + certFile.toString(), certFile.delete() || !certFile.exists());

        List<String> output = execute(null, Arrays.asList("createSSLCertificate", "--server=" + SERVER_NAME, "--password=password"));
        assertTrue("createSSLCertificate should produce <keyStore> sample. Output was: " + output, findMatchingLine(output, ".*<keyStore.*"));
        assertTrue("Generated SSL keystore " + certFile.getAbsolutePath() + " does not exist.", certFile.exists());
    }

    @Test
    public void testCreateSSLCertificateOnClient() throws Exception {
        //create the test client if it does not exist
        File clientPath = new File(WLP_INSTALL_DIR + "/usr/clients/" + CLIENT_NAME);
        if (!clientPath.exists()) {
            List<String> outputOfCreate = createClient(null, Arrays.asList("create", CLIENT_NAME));
            System.out.println("The client folder did not previously exist. Create output: " + outputOfCreate);
            assertTrue("Client folder does not exist", clientPath.exists());
        }

        File certFile = new File(WLP_INSTALL_DIR + "/usr/clients/" + CLIENT_NAME + "/resources/security/key.jks");
        assertTrue("Delete " + certFile.toString(), certFile.delete() || !certFile.exists());

        List<String> output = execute(null, Arrays.asList("createSSLCertificate", "--client=" + CLIENT_NAME, "--password=password"));
        assertTrue("createSSLCertificate should produce <keyStore> sample. Output was: " + output, findMatchingLine(output, ".*<keyStore.*"));
        assertTrue("Generated SSL keystore " + certFile.getAbsolutePath() + " does not exist.", certFile.exists());
    }

    @Test
    public void testCreateLTPAKeysDefault() throws Exception {
        File keysFile = new File("ltpa.keys");
        assertTrue("Delete " + keysFile.toString(), keysFile.delete() || !keysFile.exists());

        List<String> output = execute(null, Arrays.asList("createLTPAKeys", "--password=password"));
        assertTrue("createLTPAKeys should produce <ltpa> sample. Output was: " + output, findMatchingLine(output, ".*<ltpa.*"));
        assertTrue("Generated LTPA keys file " + keysFile.getAbsolutePath() + " does not exist.", keysFile.exists());
    }

    @Test
    public void testCreateLTPAKeysForServer() throws Exception {
        File keysFile = new File(WLP_INSTALL_DIR + "/usr/servers/" + SERVER_NAME + "/resources/security/ltpa.keys");
        assertTrue("Delete " + keysFile.toString(), keysFile.delete() || !keysFile.exists());

        List<String> output = execute(null, Arrays.asList("createLTPAKeys", "--server=" + SERVER_NAME, "--password=password"));
        assertTrue("createLTPAKeys should produce <ltpa> sample. Output was: " + output, findMatchingLine(output, ".*<ltpa.*"));
        assertTrue("Generated LTPA keys file " + keysFile.getAbsolutePath() + " does not exist.", keysFile.exists());
    }

    @Test
    public void testCreateLTPAKeysNamedFile() throws Exception {
        File keysFile = new File(WLP_INSTALL_DIR + "ltpa2.keys");
        assertTrue("Delete " + keysFile.toString(), keysFile.delete() || !keysFile.exists());

        List<String> output = execute(null, Arrays.asList("createLTPAKeys", "--file=" + keysFile.getAbsolutePath(), "--password=password"));
        assertTrue("createLTPAKeys should produce <ltpa> sample. Output was: " + output, findMatchingLine(output, ".*<ltpa.*"));
        assertTrue("Generated LTPA keys file " + keysFile.getAbsolutePath() + " does not exist.", keysFile.exists());
    }

    private static class EnvVar {
        String name;
        String value;
    }

    private static List<String> execute(List<EnvVar> envVars, List<String> args) throws IOException, InterruptedException {
        if (envVars == null) {
            envVars = Collections.emptyList();
            EnvVar jHome = new EnvVar();
            jHome.name = "JAVA_HOME";
            jHome.value = JAVA_HOME;
            envVars = new ArrayList<EnvVar>();
            envVars.add(jHome);

        }

        List<String> command = new ArrayList<String>();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            command.add(WLP_INSTALL_DIR + "/bin/securityUtility.bat");
        } else {
            if (WLP_CYGWIN_HOME == null) {
                command.add("/bin/sh");
            } else {
                command.add(WLP_CYGWIN_HOME + "/bin/sh");
            }
            command.add("-x");
            command.add(WLP_INSTALL_DIR + "/bin/securityUtility");
        }
        command.addAll(args);

        System.out.println("Executing " + command);
        for (EnvVar envVar : envVars) {
            System.out.println("  " + envVar.name + '=' + envVar.value);
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        for (EnvVar envVar : envVars) {
            builder.environment().put(envVar.name, envVar.value);
        }

        final Process p = builder.start();
        List<String> output = new ArrayList<String>();

        Thread stderrCopier = new Thread(new OutputStreamCopier(p.getErrorStream(), output));
        stderrCopier.start();
        new OutputStreamCopier(p.getInputStream(), output).run();

        stderrCopier.join();
        p.waitFor();

        int exitValue = p.exitValue();
        if (exitValue != 0) {
            throw new IOException(command.get(0) + " failed (" + exitValue + "): " + output);
        }

        return output;
    }

    private static List<String> createClient(List<EnvVar> envVars, List<String> args) throws IOException, InterruptedException {
        if (envVars == null) {
            envVars = Collections.emptyList();
            EnvVar jHome = new EnvVar();
            jHome.name = "JAVA_HOME";
            jHome.value = JAVA_HOME;
            envVars = new ArrayList<EnvVar>();
            envVars.add(jHome);

        }

        List<String> command = new ArrayList<String>();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            command.add(WLP_INSTALL_DIR + "/bin/client.bat");
        } else {
            if (WLP_CYGWIN_HOME == null) {
                command.add("/bin/sh");
            } else {
                command.add(WLP_CYGWIN_HOME + "/bin/sh");
            }
            command.add("-x");
            command.add(WLP_INSTALL_DIR + "/bin/client");
        }
        command.addAll(args);

        System.out.println("Executing " + command);
        for (EnvVar envVar : envVars) {
            System.out.println("  " + envVar.name + '=' + envVar.value);
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        for (EnvVar envVar : envVars) {
            builder.environment().put(envVar.name, envVar.value);
        }

        final Process p = builder.start();
        List<String> output = new ArrayList<String>();

        Thread stderrCopier = new Thread(new OutputStreamCopier(p.getErrorStream(), output));
        stderrCopier.start();
        new OutputStreamCopier(p.getInputStream(), output).run();

        stderrCopier.join();
        p.waitFor();

        int exitValue = p.exitValue();
        if (exitValue != 0) {
            throw new IOException(command.get(0) + " failed (" + exitValue + "): " + output);
        }

        return output;
    }

    private static class OutputStreamCopier implements Runnable {
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

    private boolean findMatchingLine(List<String> lines, String regex) {
        Pattern pattern = Pattern.compile(regex);
        for (String line : lines) {
            if (pattern.matcher(line).matches()) {
                System.out.println("Found line matching regex " + regex + ": " + line);
                return true;
            }
        }

        System.out.println("Did not find line matching " + regex);
        return false;
    }
}
