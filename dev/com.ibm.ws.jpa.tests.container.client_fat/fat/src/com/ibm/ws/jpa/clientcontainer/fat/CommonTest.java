/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.clientcontainer.fat;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class CommonTest {
    private static final Class<?> c = CommonTest.class;

    protected static LibertyClient testClient;
    protected static LibertyServer testServer;
    protected static final String JAVA_VERSION = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty("java.version");
                        }
                    });
    protected static final String MAC_RUN = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty("fat.on.mac");
                        }
                    });
    protected static final String GLOBAL_TRACE = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            String prop = System.getProperty("global.trace.spec");
                            return prop == null ? "" : prop.trim();
                        }
                    });

    protected static final boolean GLOBAL_JAVA2SECURITY = AccessController
                    .doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            String prop = System.getProperty("global.java2.sec");
                            boolean java2security = false;
                            if (prop != null) {
                                Log.info(c, "<clinit>", "global.java2.sec=" + prop);
                                java2security = Boolean.parseBoolean(prop);
                            }
                            Log.info(c, "<clinit>", "GLOBAL_JAVA2SECURITY="
                                                    + java2security);
                            return java2security;
                        }
                    });

    protected static final String GLOBAL_JVM_ARGS = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            String prop = System.getProperty("global.jvm.args");
                            return prop == null ? "" : prop.trim();
                        }
                    });
    protected static final String TMP_DIR = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty("java.io.tmpdir");
                        }
                    });

    protected static final boolean DO_COVERAGE = AccessController
                    .doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            String fatCoverageString = System
                                            .getProperty("test.coverage");
                            boolean fatcoverage = false;
                            if (fatCoverageString != null) {
                                Log.info(c, "<clinit>", "test.coverage="
                                                        + fatCoverageString);
                                fatcoverage = Boolean.parseBoolean(fatCoverageString);
                            }
                            Log.info(c, "<clinit>", "DO_COVERAGE=" + fatcoverage);
                            return fatcoverage;
                        }
                    });

    protected static final String JAVA_AGENT_FOR_JACOCO = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            String agent = System.getProperty("javaagent.for.jacoco");
                            Log.info(c, "<clinit>", "JAVA_AGENT_FOR_JACOCO=" + agent);
                            return agent;
                        }
                    });

    protected static final String RELEASE_MICRO_VERSION = AccessController
                    .doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            String micro = System.getProperty("micro.version");
                            Log.info(c, "<clinit>", "RELEASE_MICRO_VERSION=" + micro);
                            return micro;
                        }
                    });

    @Rule
    public TestName name = new TestName();

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     *
     * @param testClientName
     * @throws Exception
     */
    public static void commonClientSetUp(String testClientName, String clientEarName, String clientModuleName) throws Exception {

        String thisMethod = "commonClientSetUp";
        Log.info(c, thisMethod, "Performing common client setup");

        if (testClientName != null) {
            testClient = LibertyClientFactory.getLibertyClient(testClientName);
        }
        List<String> startParms = new ArrayList<String>();
        startParms.add(clientEarName + ".ear -CCjar=" + clientModuleName + ".jar");
        testClient.addIgnoreErrors("CWWJP9991W");
        testClient.startClient();
        assertNotNull("Application " + clientEarName + " did not report that it started.",
                      testClient.waitForStringInCopiedLog("CWWKZ0001I.*" + clientEarName));
        // assertNotNull("FeatureManager should report installed features",
        // testClient.waitForStringInLog("CWWKF0034I"));
        // assertNotNull("Client should report it has started",
        // testClient.waitForStringInLog("CWWKF0035I"));

    }

    /**
     * Sets up and starts a server. A server.xml file is expected to be present
     * in the client's root directory.
     *
     * @param testServerName
     * @throws Exception
     */
    public static void commonServerSetUp(String testServerName) throws Exception {
        String thisMethod = "commonServerSetUp";
        Log.info(c, thisMethod, "Performing common server setup");

        if (testServerName != null) {
            testServer = LibertyServerFactory.getLibertyServer(testServerName);
        }

        testServer.startServer();

        // Wait for feature update to complete
        assertNotNull("FeatureManager did not report update was complete",
                      testServer.waitForStringInLog("CWWKF0008I"));
        // assertNotNull("Application did not start",
        // testServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("LTPA configuration did not report it was ready",
                      testServer.waitForStringInLog("CWWKS4105I"));

    }

    public static ProgramOutput runSecurityUtility(Machine machine,
                                                   String clientCmd, String clientToUse) throws Exception {

        final String method = "runSecurityUtility";
        Log.info(c, method, "cmd=" + clientCmd);

        String clientRoot = null;
        String relativeLogsRoot = "/logs/";
        Properties envVars = new Properties();
        String installRoot = CommonTest.testClient.getInstallRoot();
        clientRoot = installRoot + "/usr/clients/" + clientToUse;
        String policyFilePath = installRoot
                                + "/../../com.ibm.ws.kernel.boot/resources/security.policy";
        Bootstrap b = Bootstrap.getInstance();
        String hostName = b.getValue("hostName");
        String machineJava = b.getValue(hostName + ".JavaHome");
        String clientOutputRoot = clientRoot;
        String logsRoot = clientOutputRoot + relativeLogsRoot;

        String cmd = installRoot + "/bin/securityUtility";
        cmd = cmd + clientCmd;
        ArrayList<String> parametersList = new ArrayList<String>();

        final String[] parameters = parametersList.toArray(new String[] {});

        // Need to ensure JAVA_HOME is set correctly - can't rely on user's
        // environment to be set to the same Java as the build/runtime
        // environment
        envVars.setProperty("JAVA_HOME", machineJava);

        // Pick up global JVM args (forced by build properties)
        String JVM_ARGS = GLOBAL_JVM_ARGS;

        // Always set tmp dir.
        JVM_ARGS += " -Djava.io.tmpdir=" + TMP_DIR;

        // Add JaCoCo java agent to generate code coverage for FAT test run
        if (DO_COVERAGE) {
            JVM_ARGS += " " + JAVA_AGENT_FOR_JACOCO;
        }

        // if we are on Mac then use the value of the perm gen arg that has been
        // passed in via the system property
        if (MAC_RUN != null
            && !!!MAC_RUN.equalsIgnoreCase(Boolean.toString(false))) {
            JVM_ARGS += " " + MAC_RUN;
        }

        // if we have java 2 security enabled, add java.security.manager and
        // java.security.policy
        if (GLOBAL_JAVA2SECURITY) {
            // if (1 == 1) {

            // update the bootstrap.properties file with the java 2 security
            // property
            RemoteFile f = getClientBootstrapPropertiesFile(machine, clientRoot);
            Log.info(c, "runSecurityUtility",
                     "remoteFile: " + f.getAbsolutePath());
            if (f.exists()) {
                java.io.OutputStream w = f.openForWriting(true);
                try {
                    String policyString = "-Djava.security.policy=file:/"
                                          + policyFilePath;
                    Log.info(c, "policyString path = ", policyString);
                    // w.write("-Djava.security.policy=file:/c:/liberty/java2sec/com.ibm.ws.kernel.boot/resources/security.policy".getBytes());
                    w.write(policyString.getBytes());
                    w.write("\n".getBytes());
                    w.write("-Djava.security.manager".getBytes());
                    Log.info(
                             c,
                             "getClientBootstrapPropertiesFile",
                             "Successfully updated bootstrap.properties file with Java 2 Security properties");
                } catch (Exception e) {
                    Log.info(
                             c,
                             "getClientBootstrapPropertiesFile",
                             "caught exception updating bootstap.properties file with Java 2 Security properties, e: ",
                             e.getMessage());

                    // oh well
                }
                w.flush();
                w.close();
            }
            Log.info(c, "<clinit>", "JVM_ARGS = " + JVM_ARGS);
        }

        // Look for forced client trace..
        if (!GLOBAL_TRACE.isEmpty()) {
            RemoteFile f = getClientBootstrapPropertiesFile(machine, clientRoot);
            Properties props = new Properties();

            if (f.exists()) {
                InputStream is = null;
                try {
                    is = f.openForReading();
                    props.load(is);
                } catch (Exception e) {
                    Log.info(c, method,
                             "Error reading " + f + ": " + e.getMessage());
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            // get configured trace string from bootstrap properties..
            // (have to take include into account, ugh..
            String configuredTrace = props
                            .getProperty("com.ibm.ws.logging.trace.specification");
            if (configuredTrace == null) {
                String includeFiles = props.getProperty("bootstrap.include");
                if (includeFiles != null) {
                    String[] files = includeFiles.split("\\s*,\\s*");
                    for (String fileName : files) {
                        RemoteFile x = new RemoteFile(machine, clientRoot + "/"
                                                               + fileName);
                        if (x.exists()) {
                            props.clear();
                            InputStream is = null;
                            try {
                                is = x.openForReading();
                                props.load(is);
                                configuredTrace = props
                                                .getProperty("com.ibm.ws.logging.trace.specification");
                                if (configuredTrace != null)
                                    break;
                            } catch (Exception e) {
                                Log.info(c, method, "Error reading " + x + ": "
                                                    + e.getMessage());
                            } finally {
                                if (is != null) {
                                    is.close();
                                }
                            }
                        }
                    }
                }
            }

            if (configuredTrace != null && !configuredTrace.isEmpty()) {
                configuredTrace = GLOBAL_TRACE + ":" + configuredTrace.trim();
            } else {
                configuredTrace = GLOBAL_TRACE;
            }

            JVM_ARGS += " -Dcom.ibm.ws.logging.trace.specification="
                        + configuredTrace;
        }

        envVars.setProperty("JVM_ARGS", JVM_ARGS);

        // This takes the custom console file name used for tests into
        // consideration
        envVars.setProperty("LOG_DIR", logsRoot);

        Log.info(c, method, "Using additional env props: " + envVars.toString());

        Log.info(c, method, "Starting command: " + cmd);

        ProgramOutput output;
        output = machine.execute(cmd, parameters, envVars);

        int rc = output.getReturnCode();
        Log.info(c, method, "Response from script is: " + output.getStdout());
        Log.info(c, method, "Return code from script is: " + rc);

        Log.exiting(c, method);
        return output;
    }

    public static RemoteFile getClientBootstrapPropertiesFile(Machine machine,
                                                              String clientRoot) throws Exception {
        return new RemoteFile(machine, clientRoot + "/bootstrap.properties");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "commonTearDown", "Common tear down");

    }

}
