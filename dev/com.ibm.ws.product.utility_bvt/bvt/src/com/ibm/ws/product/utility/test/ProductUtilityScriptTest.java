/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

public class ProductUtilityScriptTest {
    /**
     * Install directory property set by bvt.xml.
     */
    private static final String WLP_INSTALL_DIR = System.getProperty("install.dir");

    /**
     * True if running on Windows and the .bat file should be used.
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testHelp() throws Exception {
        List<String> usageOutput = getScriptHelpOutput(null);
        Assert.assertEquals("Usage help should produce one line of output", 1, usageOutput.size());

        List<String> actionsHelp = getScriptInvalidOutput(null, "invalidAction");
        Assert.assertEquals("Invalid actions help should produce two lines of output", 2, actionsHelp.size());

        List<String> helpOutput = getScriptHelpOutput(null, "help");
        Assert.assertTrue("Full help should contain encode help", findMatchingLine(helpOutput, "\\s*version.*"));

        List<String> helpEncodeOutput = execute(null, Arrays.asList("help", "version"));
        StringBuilder builder = new StringBuilder();
        boolean foundText = false;
        String findString = "productInfo version [options]";
        for (String s : helpEncodeOutput) {
            if (!!!foundText && s.contains(findString)) {
                foundText = true;
            }
            builder.append(s);
            builder.append("\r\n");
        }
        Assert.assertTrue("Help for version should contain version action description. Expected: '" + findString + "' Found: " + builder,
                          foundText);
    }

    public void testJvmArgs() throws Exception {
        List<String> output = getScriptHelpOutput(Arrays.asList(new EnvVar("JVM_ARGS", "-Duser.language=es")));
        Assert.assertTrue("JVM_ARGS with -Duser.language should cause 'Usage:' to be printed as 'Uso:'", output.get(0).startsWith("Uso:"));
    }

    @Test
    public void testVersionVerbose() throws Exception {
        List<String> argOutput = execute(null, Arrays.asList("version", "--verbose"));
        Assert.assertTrue("The version command should at least display the content of one version properties files", findSubStringLine(argOutput, "com.ibm.websphere.productId="));
    }

    @Test
    public void testVersion() throws Exception {
        List<String> argOutput = execute(null, Arrays.asList("version"));
        Assert.assertTrue("The version command should at least display one product name", findSubStringLine(argOutput, "WebSphere Application Server"));
    }

    @Test
    public void testFeatureInfo() throws Exception {
        List<String> argOutput = execute(null, Arrays.asList("featureInfo"));
        Assert.assertTrue("One feature infomation should be print, at least.", argOutput.size() > 0);
    }

    /*
     * @Test
     * public void testValidate() throws Exception {
     * List<String> argOutput = execute(null, Arrays.asList("validate"));
     * Assert.assertTrue("The install validation should complete successfully", findMatchingLine(argOutput, "Product validation completed successfully\\."));
     * } re-enable checksums at GA
     */

    private static class EnvVar {
        String name;
        final String value;

        EnvVar(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static List<String> execute(List<EnvVar> envVars, List<String> args) throws IOException, InterruptedException {
        if (envVars == null) {
            envVars = Collections.emptyList();
        }

        List<String> command = new ArrayList<String>();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            command.add(WLP_INSTALL_DIR + "/bin/productInfo.bat");
        } else {
            if (WLP_CYGWIN_HOME == null) {
                command.add("/bin/sh");
            } else {
                command.add(WLP_CYGWIN_HOME + "/bin/sh");
            }
            command.add("-x");
            command.add(WLP_INSTALL_DIR + "/bin/productInfo");
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

    private List<String> getScriptHelpOutput(List<EnvVar> envVars, String... args) throws IOException, InterruptedException {
        List<String> output = execute(envVars, Arrays.asList(args));

        Assert.assertFalse("Help output should contain at least one line", output.isEmpty());
        Assert.assertTrue("Usage line should include server script name", output.get(0).contains("productInfo "));

        return output;
    }

    private List<String> getScriptInvalidOutput(List<EnvVar> envVars, String... args) throws IOException, InterruptedException {
        List<String> output = execute(envVars, Arrays.asList(args));

        Assert.assertFalse("Help output should contain at least one line", output.isEmpty());
        Assert.assertTrue("Bad-arg line should contain unknown task message" + output.get(0), output.get(0).contains("CWWKE0502E"));
        Assert.assertTrue("Usage line should include server script name" + output.get(1), output.get(1).contains("productInfo "));

        return output;
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

    private boolean findSubStringLine(List<String> lines, String prefix) {
        for (String line : lines) {
            if (line != null && line.indexOf(prefix) != -1) {
                System.out.println("Found line containing " + prefix + ": " + line);
                return true;
            }
        }

        System.out.println("Did not find line containing " + prefix);
        return false;
    }
}
