/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class LogLevelPropertyTest {

    private static LibertyServer server;
    private static boolean isMac;
    private static boolean isZos;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.output.fat");

        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

        Map<String, String> options = server.getJvmOptionsAsMap();
        String maxPermSize = "-XX:MaxPermSize";
        if (options.containsKey(maxPermSize)) {
            options.remove(maxPermSize);
        }

        // Temporary debug for 130858.
        isZos = osName.contains("z/os");
        if (isZos) {
            String option = "-Xtrace:output=shared_classes.trc,maximal={j9shr}";
            Log.info(LogLevelPropertyTest.class, "before", "Adding " + option + " for z/OS");
            options.put(option, null);

        }

        server.setJvmOptions(options);

        server.startServer();

        isMac = osName.indexOf("mac os") >= 0;
    }

    @Test
    public void testLogLevelPropertyDisabled() throws Exception {
        try {
            if (isMac) {
                // There might (with Java7 on some versions of the OS) be garbage in console.log that is
                // printed by the JVM (we have no control over it). It looks something like this:
                // objc[25086]: Class JavaLaunchHelper is implemented in both /.../jre/bin/java and /.../jre/lib/libinstrument.dylib. One of the two will be used. Which one is undefined.
                // only test for the empty console log if that message isn't present
                if (server.waitForStringInLog("objc.*", 0, server.getConsoleLogFile()) == null) {
                    assertEquals("Console log file was not empty.", 0, server.getConsoleLogFile().length());
                }
            } else if (JavaInfo.forServer(server).majorVersion() == 8) {
                // On Oracle JDK and OpenJDK 8, the JVM will complain about the MaxPermSize option
                if (server.waitForStringInLog(".*MaxPermSize", 0, server.getConsoleLogFile()) == null) {
                    assertEquals("Console log file was not empty.", 0, server.getConsoleLogFile().length());
                }
            } else if (JavaInfo.forServer(server).majorVersion() == 9) {
                if (server.waitForStringInLog("java\\.version is now: 1\\.9", 0, server.getConsoleLogFile()) == null) {
                    assertEquals("Console log file was not empty.", 0, server.getConsoleLogFile().length());
                }
            } else {
                assertEquals("Console log file was not empty.", 0, server.getConsoleLogFile().length());
            }
        } finally {
            // Temporary debug for 130858.
            if (isZos) {
                final Class<?> c = LogLevelPropertyTest.class;
                final String m = "testLogLevelPropertyDisabled";

                Machine machine = server.getMachine();
                ProgramOutput output = machine.execute("ls",
                                                       new String[] {
                                                                      "-l",
                                                                      new File(server.getUserDir(), "servers/.classCache").getAbsolutePath(),
                                                                      new File(server.getUserDir(), "servers/.classCache/javasharedresources").getAbsolutePath() });
                Log.info(c, m, "ls -l diagnostic return code: " + output.getReturnCode());
                Log.info(c, m, "ls -l diagnostic stdout: " + output.getStdout());
                Log.info(c, m, "ls -l diagnostic stderr: " + output.getStderr());

                output = machine.execute("ipcs");
                Log.info(c, m, "icps diagnostic return code: " + output.getReturnCode());
                Log.info(c, m, "icps diagnostic stdout: " + output.getStdout());
                Log.info(c, m, "icps diagnostic stderr: " + output.getStderr());

                output = machine.execute(new File(server.getMachineJavaJDK(), "../bin/jar").getAbsolutePath(),
                                         new String[] {
                                                        "cfM",
                                                        // Weird extension to avoid filtering.
                                                        new File(server.getServerRoot(), "scControlFiles.zip.diag").getAbsolutePath(),
                                                        new File(server.getUserDir(), "servers/.classCache").getAbsolutePath()
                                         });
                Log.info(c, m, "jar cf diagnostic return code: " + output.getReturnCode());
                Log.info(c, m, "jar cf diagnostic stdout: " + output.getStdout());
                Log.info(c, m, "jar cf diagnostic stderr: " + output.getStderr());
            }
        }
    }

    @AfterClass
    public static void after() throws Exception {
        server.stopServer();
    }
}