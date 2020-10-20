/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.common.apiservices.Bootstrap;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class CommonTest {

    //@ClassRule
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
    protected static final boolean JAVA_VERSION_6 = JAVA_VERSION
                    .startsWith("1.6.");
    protected static final boolean HOTSPOT_JVM_RUN = AccessController
                    .doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            String hotspotString = System.getProperty("fat.on.hotspot");
                            boolean hotspot;
                            if (hotspotString != null) {
                                Log.info(c, "<clinit>", "fat.on.hotspot="
                                                        + hotspotString);
                                hotspot = Boolean.parseBoolean(hotspotString);
                            } else {
                                String vm = System.getProperty("java.vm.name");
                                Log.info(c, "<clinit>", "java.vm.name=" + vm);
                                hotspot = vm.contains("HotSpot");
                            }

                            Log.info(c, "<clinit>", "HOTSPOT_JVM_RUN=" + hotspot);
                            return hotspot;
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

    protected static final int CLIENT_START_TIMEOUT = 30 * 1000;
    protected static final int CLIENT_STOP_TIMEOUT = 30 * 1000;

    @Rule
    public TestName name = new TestName();
    public String _testName = "";

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @param ignoreErrors
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUp(String testClientName, String... ignoreErrors) throws Exception {

        String thisMethod = "commonClientSetUp";
        Log.info(c, thisMethod, "Performing common client setup");

        if (testClientName != null) {
            testClient = LibertyClientFactory.getLibertyClient(testClientName);
            transformApps(testClient);
        } else {
            throw new Exception("Test Client name is missing");
        }
        addServerPortsToClientBootStrapProp();
        List<String> startParms = new ArrayList<String>();
        startParms.add("--");
        startParms.add("add");
        startParms.add("2");
        startParms.add("3");
        //testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);

        testClient.addIgnoreErrors(ignoreErrors);
        return testClient.startClient();
        // assertNotNull("FeatureManager should report installed features",
        // testClient.waitForStringInLog("CWWKF0034I"));
        //assertNotNull("Client should report it has started", testClient.waitForStringInLog("CWWKF0035I"));

    }

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithCalcArgs(String testClientName) throws Exception {

        return commonClientSetUpWithCalcArgs(testClientName, null);
    }

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @param clientxml
     * @param pause the maximum wait time in seconds prior to invoke init() method of the ORB object.
     *            This can be used in order to make sure that a SSL certificate is being genereated.
     *            At the worst case, it took more than 20 seconds to generate. So putting 30 seconds.
     *            When this value is set, the calc application will check whether key.jks file exists
     *            in the default location for every two seconds, and if it's not there, wait up to specified wait time.
     *            When it reaches the maximum wait time, the program resumes.
     * @param ignoreErrors
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithCalcArgs(String testClientName, String clientXml, String... ignoreErrors) throws Exception {
        return commonClientSetUpWithCalcArgs(testClientName, clientXml, 0, ignoreErrors);
    }

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @param clientxml
     * @param pause the maximum wait time in seconds prior to invoke init() method of the ORB object.
     *            This can be used in order to make sure that a SSL certificate is being genereated.
     *            At the worst case, it took more than 20 seconds to genereate. So putting 30 seconds.
     *            When this value is set, the calc application will check whether key.jks file exists
     *            in the default location for every two seconds, and if it's not there, wait up to specified wait time.
     *            When it reaches the maximum wait time, the program resumes.
     * @param ignoreErrors
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithCalcArgs(String testClientName, String clientXml, int waitTime, String... ignoreErrors) throws Exception {

        String thisMethod = "commonClientSetUp";
        Log.info(c, thisMethod, "Performing common client setup");

        if (testClientName != null) {
            testClient = LibertyClientFactory.getLibertyClient(testClientName);
            transformApps(testClient);
        } else {
            throw new Exception("Test Client name is missing");
        }

        if (clientXml == null) {
            clientXml = "client_orig.xml";
        }
        String fullClientXmlPath = buildFullClientConfigPath(testClient, clientXml);
        Log.info(c, thisMethod, "Using client configuration file: " + fullClientXmlPath);
        copyNewClientConfig(fullClientXmlPath);

        addServerPortsToClientBootStrapProp();
        List<String> startParms = new ArrayList<String>();
        startParms.add("--");
        startParms.add("add");
        startParms.add("2");
        startParms.add("3");
        if (waitTime > 0) {
            startParms.add(Integer.toString(waitTime));
            String clientRoot = testClient.getClientRoot();
            String keyLocation = clientRoot + "/resources/security/key.jks";
            Log.info(c, thisMethod, "wait Time is " + waitTime + " seconds. keyLocation is" + keyLocation);
            startParms.add(keyLocation);
        }

        testClient.addIgnoreErrors(ignoreErrors);
        return testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
        //testClient.startClient();
        // assertNotNull("FeatureManager should report installed features",
        // testClient.waitForStringInLog("CWWKF0034I"));
        // assertNotNull("Client should report it has started",
        // testClient.waitForStringInLog("CWWKF0035I"));

    }

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @throws Exception
     */
    public static void commonClientSetUpforConfFile(String testClientName, String clientConf) throws Exception {

        String thisMethod = "commonClientSetUpforConfFile";
        Log.info(c, thisMethod, "Performing common client setup for wsjaas_client.conf");

        if (testClientName == null) {
            throw new Exception("Test Client name is missing");
        }

        testClient = LibertyClientFactory.getLibertyClient(testClientName);
        transformApps(testClient);
        if (clientConf == null) {
            clientConf = "wsjaas_client.conf.orig";
        }
        String fullClientConfPath = buildFullClientJaasConfigPath(testClient, clientConf);
        Log.info(c, thisMethod, "Using client jaas configuration file: " + fullClientConfPath);
        copyNewClientJaasConfig(fullClientConfPath);

    }

    /**
     * Sets up and runs a client. A client.xml file is expected to be present in
     * the client's root directory.
     * 
     * @param testClientName
     * @param ignoreErrors
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithParms(String testClientName, String... ignoreErrors) throws Exception {

        String thisMethod = "commonClientSetUpWithParms";
        Log.info(c, thisMethod, "Performing common client setup");

        if (testClientName != null) {
            testClient = LibertyClientFactory.getLibertyClient(testClientName);
            transformApps(testClient);
        } else {
            throw new Exception("Test Client name is missing");
        }
        addServerPortsToClientBootStrapProp();
        List<String> startParms = new ArrayList<String>();
        startParms.add("--");
        // the file location of the current client keystore
        //String clientRoot = testClient.getInstallRoot();
        String clientRoot = testClient.getClientRoot();
        String keyLocation = clientRoot + "/resources/security/key.jks";
        Log.info(c, thisMethod, "keyLocation is" + keyLocation);
        startParms.add(keyLocation);
        // the file location of the new client keystore
        String newKeyLocation = clientRoot + "/resources/security/newkey.jks";
        Log.info(c, thisMethod, "newKeyLocation is" + newKeyLocation);
        startParms.add(newKeyLocation);

        testClient.addIgnoreErrors(ignoreErrors);
        return testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
        //testClient.startClient();
        // assertNotNull("FeatureManager should report installed features",
        // testClient.waitForStringInLog("CWWKF0034I"));
        // assertNotNull("Client should report it has started",
        // testClient.waitForStringInLog("CWWKF0035I"));

    }

    /**
     * Sets up and runs a client, passing in the specified parameters as arguments to the client. A client.xml file is
     * expected to be present in the client's root directory.
     * 
     * @param testClientName
     * @param parameters
     * @return
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithParms(String testClientName, List<String> parameters) throws Exception {
        return commonClientSetUpWithParms(testClientName, null, parameters);
    }

    /**
     * Sets up and runs a client, passing in the specified parameters as arguments to the client. If {@code clientXml} is
     * not {@code null}, the client is configured to use the specified client configuration file from within the configs/
     * directory of the client. Otherwise, a client.xml file is expected to be present in the client's root directory.
     * 
     * @param testClientName
     * @param clientXml
     * @param parameters
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpWithParms(String testClientName, String clientXml, List<String> parameters, String... ignoreErrors) throws Exception {
        String thisMethod = "commonClientSetUpWithParms";
        Log.info(c, thisMethod, "Performing common client setup with parameters");

        if (testClientName == null) {
            throw new Exception("Test Client name is missing");
        }

        testClient = LibertyClientFactory.getLibertyClient(testClientName);
        transformApps(testClient);

        if (clientXml != null) {
            String fullClientXmlPath = buildFullClientConfigPath(testClient, clientXml);
            Log.info(c, thisMethod, "Using client configuration file: " + fullClientXmlPath);
            copyNewClientConfig(fullClientXmlPath);
        }
        addServerPortsToClientBootStrapProp();

        if (parameters == null) {
            parameters = new ArrayList<String>();
        }
        parameters.add(0, "--");

        testClient.addIgnoreErrors(ignoreErrors);
        return testClient.startClientWithArgs(true, true, true, false, "run", parameters, false);
    }

    /**
     * Sets up and runs a client, passing in the specified parameters as arguments to the client. If {@code clientXml} is
     * not {@code null}, the client is configured to use the specified client configuration file from within the configs/
     * directory of the client. Otherwise, a client.xml file is expected to be present in the client's root directory.
     * 
     * @param testClientName
     * @param clientXml
     * @param parameters
     * @throws Exception
     */
    public static ProgramOutput commonClientSetUpAutoAcceptWithParms(String testClientName, String clientXml, List<String> parameters) throws Exception {
        String thisMethod = "commonClientSetUpWithParms";
        Log.info(c, thisMethod, "Performing common client setup with parameters");

        if (testClientName == null) {
            throw new Exception("Test Client name is missing");
        }

        testClient = LibertyClientFactory.getLibertyClient(testClientName);
        transformApps(testClient);
        if (clientXml != null) {
            String fullClientXmlPath = buildFullClientConfigPath(testClient, clientXml);
            Log.info(c, thisMethod, "Using client configuration file: " + fullClientXmlPath);
            copyNewClientConfig(fullClientXmlPath);
        }
        addServerPortsToClientBootStrapProp();

        if (parameters == null) {
            parameters = new ArrayList<String>();
        }
        parameters.add(0, "--");
        return testClient.startClientWithArgs(true, true, true, false, true, "run", parameters, false);
    }

    /**
     * Sets up and starts a server. A server.xml file is expected to be present
     * in the client's root directory.
     * 
     * @param testServerName
     * @throws Exception
     */
    public static void commonServerSetUp(String testServerName, boolean isSecure) throws Exception {
        String thisMethod = "commonServerSetUp";
        Log.info(c, thisMethod, "Performing common server setup");

        if (testServerName != null) {
            testServer = LibertyServerFactory.getLibertyServer(testServerName);
        }

        transformApps(testServer);

        testServer.startServer();

        // Wait for feature update to complete
        assertNotNull("FeatureManager did not report update was complete", testServer.waitForStringInLog("CWWKF0008I"));
        // assertNotNull("Application did not start",
        // testServer.waitForStringInLog("CWWKZ0001I"));
        if (isSecure) {
            assertNotNull("LTPA configuration did not report it was ready", testServer.waitForStringInLog("CWWKS4105I"));
        }

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

        // Avoid ClassLoader deadlocks on HotSpot Java 6.
        if (HOTSPOT_JVM_RUN && JAVA_VERSION_6) {
            JVM_ARGS += " -XX:+UnlockDiagnosticVMOptions"
                        + " -XX:+UnsyncloadClass"
                        + " -Dosgi.classloader.lock=classname";
        }

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

    /**
     * Builds and returns the absolute path to the specified file within the configs/ directory under the given
     * client's root directory.
     *
     * @param theClient
     * @param fileName
     * @return
     */
    public static String buildFullClientConfigPath(LibertyClient theClient, String fileName) {
        String clientFileName = System.getProperty("user.dir") + File.separator + theClient.getPathToAutoFVTNamedClient() + "configs" + File.separator + fileName;
        Log.info(c, "buildFullClientConfigPath", "clientFileName: " + clientFileName);
        return clientFileName;
    }

    /**
     * Copies the specified file into the client's root directory and renames it to client.xml. This will overwrite
     * any existing client.xml file within the client's root directory without backing up the existing configuration.
     *
     * @param copyFromFile
     * @throws Exception
     */
    public static void copyNewClientConfig(String copyFromFile) throws Exception {
        String thisMethod = "copyNewClientConfig";

        String clientFileLoc = (new File(testClient.getClientConfigurationPath().replace('\\', '/'))).getParent();

        // Update the client config by replacing the client.xml
        if (copyFromFile != null && !copyFromFile.isEmpty()) {
            try {
                Log.info(c, thisMethod, "Copying: " + copyFromFile + " to " + clientFileLoc);
                LibertyFileManager.copyFileIntoLiberty(testClient.getMachine(), clientFileLoc, "client.xml", copyFromFile);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                throw ex;
            }
        }
    }

    /**
     * Builds and returns the absolute path to the specified file within the resource/security/jaas/ directory under the given
     * client's root directory.
     *
     * @param theClient
     * @param fileName
     * @return
     */
    public static String buildFullClientJaasConfigPath(LibertyClient theClient, String fileName) {
        String clientFileName = System.getProperty("user.dir") + File.separator + theClient.getPathToAutoFVTNamedClient() + "resources" + File.separator + "security"
                                + File.separator + "jaas" + File.separator + fileName;
        Log.info(c, "buildFullClientJaasConfigPath", "clientFileName: " + clientFileName);
        return clientFileName;
    }

    /**
     * Copies the specified file into the client's root directory and renames it to client.xml. This will overwrite
     * any existing client.xml file within the client's root directory without backing up the existing configuration.
     *
     * @param copyFromFile
     * @throws Exception
     */
    public static void copyNewClientJaasConfig(String copyFromFile) throws Exception {
        String thisMethod = "copyNewClientJaasConfig";

        String clientFileLoc = (new File(testClient.getClientRoot().replace('\\', '/'))) + "/resources/security/jaas";;

        // Update the client config by replacing the client.xml
        if (copyFromFile != null && !copyFromFile.isEmpty()) {
            try {
                Log.info(c, thisMethod, "Copying: " + copyFromFile + " to " + clientFileLoc);
                LibertyFileManager.copyFileIntoLiberty(testClient.getMachine(), clientFileLoc, "wsjaas_client.conf", copyFromFile);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                throw ex;
            }
        }
    }

//	@AfterClass
//	public static void tearDown() throws Exception {
//		Log.info(c, "commonTearDown", "Common tear down");
//
//	}
//
    /**
     * setup before running a test
     * 
     * @throws Exception
     */
    @Before
    public void setTestName() throws Exception {
        _testName = name.getMethodName();
        System.out.println("----- Start:  " + _testName + "   ----------------------------------------------------");
        printMethodName(_testName, "Starting TEST ");

        logTestCaseInServerSideLog("STARTING", testServer);

    }

    /**
     * Clean up after running a test
     * 
     * @throws Exception
     */
    @After
    public void endTest() throws Exception {

        try {

            logTestCaseInServerSideLog("ENDING", testServer);

            printMethodName(_testName, "Ending TEST ");
            System.out.println("----- End:  " + _testName + "   ----------------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    private void logTestCaseInServerSideLog(String action, LibertyServer server) throws Exception {

        //_testName = name.getMethodName();

        try {
            if (testServer != null) {
                WebConversation wc = new WebConversation();
                String markerUrl = "http://localhost:" + testServer.getHttpDefaultPort() + "/" + Constants.APP_TESTMARKER + "/" + Constants.APP_TESTMARKER_PATH;
                Log.info(c, "logTestCaseInServerSideLog", markerUrl + "  " + action + " " + _testName);
                WebRequest request = new GetMethodWebRequest(markerUrl);
                request.setParameter("action", action);
                request.setParameter("testCaseName", _testName);
                wc.getResponse(request);
            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            e.printStackTrace();
        }
    }

    public void printMethodName(String strMethod, String task) {
        System.err.flush();
        System.out.flush();
        Log.info(c, strMethod, "***************************** " + task
                               + " " + strMethod + " *****************************");
    }

    /**
     * Builds the fully qualified runtime server path.
     */
    public static String getClientFileLoc() throws Exception {
        try {
            return (new File(testClient.getClientConfigurationPath().replace('\\', '/'))).getParent();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    public static void addServerPortsToClientBootStrapProp() throws Exception {

        String thisMethod = "addServerPortsToClientBootStrapProp";

        // add properties to bootstrap.properties
        String bootProps = getClientFileLoc() + "/bootstrap.properties";
        Log.info(c, thisMethod, "client property File: " + bootProps);
        String sep = System.getProperty("line.separator");
        // append to bootstrap.properties
        FileWriter writer = new FileWriter(bootProps, true);
        writer.append(sep);
        writer.append("ServerPort=" + testServer.getHttpDefaultPort());
        writer.append(sep);
        writer.append("ServerSecurePort=" + testServer.getHttpDefaultSecurePort());
        writer.append(sep);
        writer.append("ServerIIOPPort=" + testServer.getIiopDefaultPort());
        writer.append(sep);
        writer.close();

    }

    /**
     * Asserts that no System.err messages are contained in {@code output}. This is done by checking to see if there are
     * any lines in {@code output} that begin with {@code [err]}.
     * 
     * @param output
     */
    public static void assertNoErrMessages(String output) {
        Pattern errorPattern = Pattern.compile(".+?^\\s*(\\[err\\].+?$).+", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = errorPattern.matcher(output);
        if (matcher.matches()) {
            fail("An unexpected error appears to have occurred while running the client (see output for full message): " + matcher.group(1));
        }
    }

    /**
     * JakartaEE9 transform applications for a specific client.
     *
     * @param client
     *            The client to transform the applications on.
     */
    public static void transformApps(LibertyClient client) {
        if (JakartaEE9Action.isActive()) {
            String[] apps = null;

            switch (client.getClientName()) {

                case "java2Client":
                    apps = new String[] { "apps/Java2Client.ear", "apps/Java2ClientNoPermissionsXML.ear" };
                    break;
                case "javacolonClientInjection":
                    apps = new String[] { "apps/JavaColonInjectionApp.ear", "dropins/JavaColonInjectionApp.ear" };
                    break;
                case "myFileMonitorClient":
                    apps = new String[] { "apps/FileMonitor.ear" };
                    break;
                case "mySSLAutoAcceptClient":
                case "mySSLPromptClient":
                    apps = new String[] { "apps/SSLClient.ear" };
                    break;
                case "mySSLCmdClient":
                    apps = new String[] { "apps/earDD.ear" };
                    break;
                case "mySSLTestClient":
                    apps = new String[] { "apps/BasicCalculatorClient.ear", "apps/earDD.ear" };
                    break;
                case "myTestClient":
                    apps = new String[] { "apps/BasicCalculatorClient.ear", "apps/TechnologySamples.ear" };
                    break;
                case "noDefaultKeyClient":
                    apps = new String[] { "apps/BasicCalculatorClient.ear" };
                    break;
                case "ProgrammaticJaasLoginConfigFileTestClient":
                    apps = new String[] { "CustomLoginModule.jar", "resources/security/jaas/CustomLoginModule.jar",
                                          "apps/ProgrammaticJaasLogin.ear" };
                    break;
                case "ProgrammaticLoginTestClient":
                    apps = new String[] { "CustomLoginModule.jar", /* "resources/security/jaas/CustomLoginModule.jar", */
                                          "apps/ProgrammaticLogin.ear", "apps/ProgrammaticLoginWithCallbackHandler.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerBadPwd.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerEmptyPwd.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerEmptyUser.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerNullPwd.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerNullUser.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerUnauthzUser.ear",
                                          "apps/ProgrammaticLoginWithCallbackHandlerUserNotDefined.ear",
                                          "apps/ProgrammaticLoginWithNonexistentCallbackHandler.ear",
                                          "apps/ProgrammaticLoginWithoutCallbackHandler.ear" };
                    break;
                default:
                    apps = new String[] {};
                    break;
            }

            for (String app : apps) {
                Path someArchive = Paths.get(client.getClientRoot() + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }

    /**
     * JakartaEE9 transform applications for a specific server.
     *
     * @param server The server to transform the applications on.
     */
    public static void transformApps(LibertyServer server) {
        if (JakartaEE9Action.isActive()) {
            String[] apps = null;

            switch (server.getServerName()) {

                case "BasicAuthTest":
                case "NonSecureServerTest":
                case "SSLCipherTest":
                case "SSLnonIBMCipherTest":
                    apps = new String[] { "apps/BasicCalculator.ear", "dropins/testmarker.war" };
                    break;
                case "javacolonServerInjection":
                    apps = new String[] { "dropins/JavaColonInjectionApp.ear", "dropins/testmarker.war" };
                    break;
                case "SecureServerTest":
                    apps = new String[] { "apps/basicauth.war", "dropins/testmarker.war" };
                    break;
                default:
                    apps = new String[] {};
                    break;
            }

            for (String app : apps) {
                Path someArchive = Paths.get(server.getServerRoot() + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}
