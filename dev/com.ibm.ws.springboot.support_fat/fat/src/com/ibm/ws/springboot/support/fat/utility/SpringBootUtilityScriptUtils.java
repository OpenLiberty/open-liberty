/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SpringBootUtilityScriptUtils {
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

    protected static class EnvVar {
        String name;
        final String value;

        EnvVar(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static List<String> execute(List<EnvVar> envVars, List<String> args) throws IOException, InterruptedException {
        return execute(envVars, args, false);
    }

    public static List<String> execute(List<EnvVar> envVars, List<String> args, boolean ignoreError) throws IOException, InterruptedException {
        return execute("springBootUtility", envVars, args, ignoreError);
    }

    public static List<String> execute(String commandName, List<EnvVar> envVars, List<String> args, boolean ignoreError) throws IOException, InterruptedException {
        if (envVars == null) {
            envVars = Collections.emptyList();
        }

        List<String> command = new ArrayList<String>();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            command.add(WLP_INSTALL_DIR + "/bin/" + commandName + ".bat");
        } else {
            if (WLP_CYGWIN_HOME == null) {
                command.add("/bin/sh");
            } else {
                command.add(WLP_CYGWIN_HOME + "/bin/sh");
            }
            command.add("-x");
            command.add(WLP_INSTALL_DIR + "/bin/" + commandName);
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

        if (!ignoreError) {
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                throw new IOException(command.get(0) + " failed (" + exitValue + "): " + output);
            }
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

    public static boolean findMatchingLine(List<String> lines, String regex) {
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
