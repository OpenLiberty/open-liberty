/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package componenttest.topology.impl;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ClientConfiguration;
import com.ibm.websphere.simplicity.config.ClientConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.soe_reporting.SOEHttpPostUtil;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.LocalMachine;
import componenttest.custom.junit.runner.LogPolice;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyFileManager.LogSearchResult;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;
import componenttest.topology.utils.PrivHelper;

public class LibertyClient {
    protected static final Class<?> c = LibertyClient.class;
    protected static final String CLASS_NAME = c.getName();
    protected static Logger LOG = Logger.getLogger(CLASS_NAME); // why don't we always use the Logger directly?
    /** How frequently we poll the logs when waiting for something to happen */
    protected static final int WAIT_INCREMENT = 300;

    protected static final String DEBUGGING_PORT = PrivHelper.getProperty("debugging.port");
    protected static final boolean DEFAULT_PRE_CLEAN = true;
    protected static final boolean DEFAULT_CLEANSTART = Boolean.parseBoolean(PrivHelper.getProperty("default.clean.start", "true"));
    protected static final boolean DEFAULT_VALIDATE_APPS = true;
    public static boolean VALIDATE_APPS = DEFAULT_VALIDATE_APPS;

    protected static final JavaInfo javaInfo = JavaInfo.forCurrentVM();
    protected static final boolean J9_JVM_RUN = javaInfo.vendor() == Vendor.IBM;

    protected static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");
    protected static final String MAC_RUN = PrivHelper.getProperty("fat.on.mac");
    protected static final String GLOBAL_TRACE = PrivHelper.getProperty("global.trace.spec", "").trim();
    protected static final boolean GLOBAL_JAVA2SECURITY = javaInfo.MAJOR > 17 ? false : FAT_TEST_LOCALRUN //
                    ? Boolean.parseBoolean(PrivHelper.getProperty("global.java2.sec", "true")) //
                    : Boolean.parseBoolean(PrivHelper.getProperty("global.java2.sec", "false"));

    //FIPS 140-3
    protected static final boolean GLOBAL_CLIENT_FIPS_140_3 = Boolean.parseBoolean(PrivHelper.getProperty("global.client.fips_140-3", "false"));

    protected static final String GLOBAL_JVM_ARGS = PrivHelper.getProperty("global.jvm.args", "").trim();
    protected static final String TMP_DIR = PrivHelper.getProperty("java.io.tmpdir");
    protected static final boolean DO_COVERAGE = PrivHelper.getBoolean("test.coverage");
    protected static final String JAVA_AGENT_FOR_JACOCO = PrivHelper.getProperty("javaagent.for.jacoco");
    protected static final String RELEASE_MICRO_VERSION = PrivHelper.getProperty("micro.version");

    protected static final int CLIENT_START_TIMEOUT = FAT_TEST_LOCALRUN ? 15 * 1000 : 30 * 1000;
    protected static final int CLIENT_STOP_TIMEOUT = CLIENT_START_TIMEOUT;

    // Increasing this from 50 seconds to 120 seconds to account for poorly performing code;
    // this timeout should only pop in the event of an unexpected failure of apps to start.
    protected static final int LOG_SEARCH_TIMEOUT = FAT_TEST_LOCALRUN ? 12 * 1000 : 120 * 1000;

    protected List<String> installedApplications;

    protected static final String DEFAULT_CLIENT = "defaultClient";

    protected static final String DEFAULT_MSG_FILE = "messages.log";
    protected static final String DEFAULT_CONSOLE_FILE = "console.log";

    protected static final String DEFAULT_TRACE_FILE_PREFIX = "trace";

    protected static final String CLIENT_CONFIG_FILE_NAME = "client.xml";
    protected static final String JVM_OPTIONS_FILE_NAME = "client.jvm.options";

    protected static final String EBCDIC_CHARSET_NAME = "IBM1047";

    protected volatile boolean isStarted = false;
    protected volatile boolean isStopped = false;
    protected boolean isStartedConsoleLogLevelOff = false;

    protected int osgiConsolePort = 5678; // The port number of the OSGi Console

    // Use port 0 if the property can't be found, these should be picked up from a properties file
    // if not then the test may create a liberty client and get the ports from a bootstrap port.
    // If neither way obtains a port then port 0 will be used which will cause the tests to fail in
    // an obvious way (rather than using a port which looks like it might be right, but isn't, e.g. 8000)

    protected int iiopDefaultPort = Integer.parseInt(System.getProperty("IIOP.client"));

    protected String installRoot; // The root of the Liberty Install
    protected String userDir; // The WLP_USER_DIR for this client

    protected String policyFilePath; // Location of the kernel policy file

    protected String installRootParent; // The parent directory of the Liberty Install

    protected final AtomicBoolean checkForRestConnector = new AtomicBoolean(false);

    // used by the saveClientConfiguration / restoreClientConfiguration methods to save the current client
    // configuration at a specific point in time and then be able to restore it back in the future
    protected RemoteFile savedClientXml = null;

    public final static String DISABLE_FAILURE_CHECKING = "DISABLE_CHECKING";

    /**
     * @return the installRoot
     */
    public String getInstallRoot() {
        return installRoot;
    }

    /**
     * @return the release micro version
     */
    public String getMicroVersion() {
        return RELEASE_MICRO_VERSION;
    }

    public String getMicroSuffix() {
        return "." + RELEASE_MICRO_VERSION;
    }

    /**
     * Returns the value of WLP_USER_DIR for the client.
     *
     * @return
     */
    public String getUserDir() {
        return userDir;
    }

    protected String clientRoot; // The root of the client for Liberty
    protected String clientOutputRoot; // The output root of the client
    protected String logsRoot; // The root of the Logs Files

    protected long lastConfigUpdate = 0; // Time stamp (in millis) of the last configuration update

    protected String relativeLogsRoot = "/logs/"; // this will be appended to logsRoot in setUp
    protected String consoleFileName = DEFAULT_CONSOLE_FILE; // Console log file name
    protected String messageFileName = DEFAULT_MSG_FILE; // Messages log file name (optionally changed by the FAT)
    protected String messageAbsPath = null;
    protected String consoleAbsPath = null;

    protected String machineJava; // Path to Java 6 JDK on the Machine

    protected String machineJarPath; //Path to the jar command

    protected Machine machine; // Machine the client is on

    protected String clientToUse; // the client to use

    //An ID given to the client topology that will be used as a reference e.g. JPAFATTestClient
    protected String clientTopologyID;

    protected OperatingSystem machineOS;

    //These aren't final as we have to massage them if they are used for tWAS FAT suites
    public String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    protected String pathToAutoFVTOutputClientsFolder = "output/clients";
    protected String pathToAutoFVTOutputFolder = "output/";

    protected final String PATH_TO_AUTOFVT_CLIENTS = "publish/clients/";
    protected static final String PATH_TO_AUTOFVT_SHARED = "publish/shared/";
    //Only need this at the moment as we only support single Liberty Clients
    protected String pathToAutoFVTNamedClient = PATH_TO_AUTOFVT_CLIENTS;

    protected long clientStartTimeout = CLIENT_START_TIMEOUT;
    protected long clientStopTimeout = CLIENT_STOP_TIMEOUT;

    protected final AtomicInteger stopApplicationMessages = new AtomicInteger(0);
    protected final AtomicInteger startApplicationMessages = new AtomicInteger(0);

    public String getPathToAutoFVTNamedClient() {
        return pathToAutoFVTNamedClient;
    }

    protected List<String> originalFeatureSet = null;

    //Used for keeping track of offset positions of log files
    protected final HashMap<String, Long> logOffsets = new HashMap<String, Long>();

    //Used for keeping track of mark positions of log files
    protected final HashMap<String, Long> logMarks = new HashMap<String, Long>();

    protected boolean clientCleanupProblem = false;

    /** When we stopped searching for a string in the logs. */
    public long searchStopTime;

    protected String logStamp;

    //List of patterns to ignore when checking logs
    protected final List<Pattern> ignorePatterns = new ArrayList<Pattern>();

    //Default list of patterns to ignore when checking logs
    protected final List<String> fixedIgnoreErrorsList = new ArrayList<String>();

    protected boolean checkingDisabled;

    /**
     * @param clientCleanupProblem the clientCleanupProblem to set
     */
    public void setClientCleanupProblem(boolean clientCleanupProblem) {
        this.clientCleanupProblem = clientCleanupProblem;
    }

    public Machine getMachine() {
        return machine;
    }

    /**
     * Protected - This constructor is protected as users should use the
     * LibertyClientFactory's static methods to get LibertyClient instances.
     *
     * @param  clientName The name of the client that is going to used
     * @throws Exception
     */
    protected LibertyClient(String clientName, Bootstrap b) throws Exception {
        final String method = "setup";
        Log.entering(c, method);
        clientTopologyID = b.getValue("clientTopologyID");
        String hostName = b.getValue("hostName");
        String user = b.getValue(hostName + ".user");
        String password = b.getValue(hostName + ".password");
        machineJava = b.getValue(hostName + ".JavaHome");
        String keystore = b.getValue("keystore");
        checkingDisabled = false;

        if (clientName != null) {
            clientToUse = clientName;
            pathToAutoFVTNamedClient += clientToUse + "/";
        } else {
            clientToUse = b.getValue("clientName");
            if (clientToUse == null || clientToUse.trim().equals("")) {
                clientToUse = DEFAULT_CLIENT;
            }
        }

        // This is the only case where we will allow the messages.log name to  be changed
        // by the fat framework -- because we want to look at messasges.log for start/stop/blah
        // messages, we shouldn't be pointing it all over everywhere else. For those FAT tests
        // that need a messages file in an alternate location, they should set the corresponding
        // com.ibm.ws.logging.message.file.name property in bootstrap.properties
        String nonDefaultLogFile = b.getValue("NonDefaultConsoleLogFileName");
        if (nonDefaultLogFile != null && nonDefaultLogFile.startsWith("CLIENT_NAME/")) {
            relativeLogsRoot = "/logs/" + clientToUse + "/";
            messageFileName = nonDefaultLogFile.substring(12);
        } else {
            relativeLogsRoot = "/logs/";
            messageFileName = DEFAULT_MSG_FILE;
        }

        try {
            osgiConsolePort = Integer.parseInt(b.getValue("osgi.console"));
        } catch (Exception e) {
            Log.debug(c, "No osgi.console set in bootstrap.properties.  Will use default value: "
                         + osgiConsolePort);
        }

        try {
            iiopDefaultPort = Integer.parseInt(b.getValue("IIOP.client"));
        } catch (Exception e) {
            Log.debug(c, "No iiop.Default.Port set in bootstrap.properties.  Will use default value: "
                         + iiopDefaultPort);
        }

        if (machineJava == null) {
            throw new IllegalArgumentException("No " + hostName
                                               + ".JavaHome was set in " + b);
        }
        installRoot = b.getValue("libertyInstallPath");
        if (installRoot == null) {
            throw new IllegalArgumentException("No installRoot was set in " + b);
        }

        Log.info(c, method,
                 "getting Machine from credentials in Boostrapping file");
        Log.info(c, method, "Connecting to machine " + hostName + " with User "
                            + user + ".");
        ConnectionInfo machineDetails = new ConnectionInfo(hostName, user, password);
        if ((password == null || password.length() == 0) && keystore != null && keystore.length() != 0) {
            File keyfile = new File(keystore);
            machineDetails = new ConnectionInfo(hostName, keyfile, user, password);
        }
        machine = Machine.getMachine(machineDetails);
        setup();

        // Populate the fixed set error and warning messages to be ignored for those
        // buckets that choose to care about error or warning messages when the client
        // is stopped.
        populateFixedListOfMessagesToIgnore();

        Log.exiting(c, method);
    }

    //This isn't that elegant but it works
    //If this is a tWAS FAT suite the relative path to the autoFVT folder
    //is different so we need to check and set variables to the autoFVT/output folder
    //and autoFVT/lib/testFiles accordingly
    protected void massageAutoFVTAbsolutePath() throws Exception {
        final String METHOD = "massageAutoFVTAbsolutePath";
        Log.entering(c, METHOD);

        //Firstly see if the TestBuild.xml file exists
        LocalFile testBuildFile = new LocalFile("TestBuild.xml");
        //If the file doesn't exist it is a tWAS FAT Suite and so need to update some vars
        if (!!!testBuildFile.exists()) {
            Properties localProps = new Properties();
            FileInputStream in = new FileInputStream(System.getProperty("local.properties"));
            localProps.load(in);
            in.close();

            String bucketsDir = localProps.getProperty("buckets.dir");
            pathToAutoFVTTestFiles = bucketsDir + "/" + pathToAutoFVTTestFiles;
            pathToAutoFVTNamedClient = bucketsDir + "/" + pathToAutoFVTNamedClient;
            pathToAutoFVTOutputClientsFolder = bucketsDir + "/" + "output/clients";
            pathToAutoFVTOutputFolder = bucketsDir + "/output";

            Log.info(c, METHOD, "This seems to be a tWAS FAT suite so updating the path to the" +
                                "AutoFVTTestFiles to " + pathToAutoFVTTestFiles + " and the testOutputFolder to " +
                                pathToAutoFVTOutputClientsFolder + " and the path to the AutoFVTNamedClient to "
                                + pathToAutoFVTNamedClient);
        }
        Log.exiting(c, METHOD);
    }

    protected void setup() throws Exception {
        installedApplications = new ArrayList<String>();
        machine.connect();
        machine.setWorkDir(installRoot);
        if (this.clientToUse == null) {
            this.clientToUse = DEFAULT_CLIENT;
        }
        this.installRoot = LibertyServerUtils.makeJavaCompatible(installRoot);
        this.userDir = installRoot + "/usr";
        this.clientRoot = installRoot + "/usr/clients/" + clientToUse;
        this.clientOutputRoot = this.clientRoot;
        this.logsRoot = clientOutputRoot + relativeLogsRoot;
        this.messageAbsPath = logsRoot + messageFileName;
        this.policyFilePath = installRoot + "/../../com.ibm.ws.kernel.boot/resources/security.policy";

        File installRootfile = new File(this.installRoot);
        this.installRootParent = installRootfile.getParent();

        // Now it sets all OS specific stuff
        machineOS = machine.getOperatingSystem();
        this.machineJava = LibertyServerUtils.makeJavaCompatible(machineJava);
        Log.info(c, "setup", "Successfully obtained machine. Operating System is: " + machineOS.name());
        // Continues with setup, we now validate the Java used is a JDK by looking for java and jar files
        String jar = "jar";
        String java = "java";
        if (machineOS == OperatingSystem.WINDOWS) {
            jar += ".exe";
            java += ".exe";
        }
        RemoteFile testJar = machine.getFile(machineJava + "/bin/" + jar);
        RemoteFile testJava = machine.getFile(machineJava + "/bin/" + java);
        machineJarPath = testJar.getAbsolutePath();
        if (!!!testJar.exists()) {
            //if we come in here we might be pointing at a JRE instead of a JDK so we'll go up a level in hope it's there
            testJar = machine.getFile(machineJava + "/../bin/" + jar);
            machineJarPath = testJar.getAbsolutePath();
            if (!!!testJar.exists()) {
                throw new TopologyException("cannot find a " + jar + " file in " + machineJava + "/bin. Please ensure you have set the machine javaHome to point to a JDK");
            } else {
                Log.info(c, "setup", "Jar Home now set to: " + machineJarPath);
            }
        }
        if (!!!testJava.exists())
            throw new TopologyException("cannot find a " + java + " file in " + machineJava + "/bin. Please ensure you have set the machine javaHome to point to a JDK");

        massageAutoFVTAbsolutePath();

        preTestTidyup();
    }

    protected void preTestTidyup() {
        //Deletes the logs and work area folder and the apps folder
        try {
            machine.getFile(logsRoot).delete();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            machine.getFile(clientRoot + "/workarea").delete();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            RemoteFile applicationsFolder = machine.getFile(installRoot + "/usr/shared/apps");
            applicationsFolder.delete();
            applicationsFolder.mkdir();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
    }

    /**
     * Set the feature list of the client.xml to the new features specified
     * in the {@code List<String>}. Each String should be the only feature
     * name, e.g. servlet-3.0
     *
     * @param  newFeatures
     * @throws Exception
     */
    public void changeFeatures(List<String> newFeatures) throws Exception {
        RemoteFile clientXML = machine.getFile(clientRoot + "/" + CLIENT_CONFIG_FILE_NAME);
        LocalFile tempclientXML = new LocalFile(CLIENT_CONFIG_FILE_NAME);
        boolean createOriginalList;
        if (originalFeatureSet == null) {
            createOriginalList = true;
            originalFeatureSet = new ArrayList<String>();
        } else {
            createOriginalList = false;
        }
        Writer w = new OutputStreamWriter(tempclientXML.openForWriting(false));
        InputStream originalOutput = clientXML.openForReading();
        InputStreamReader in2 = new InputStreamReader(originalOutput);
        Scanner s2 = new Scanner(in2);
        while (s2.hasNextLine()) {
            String line = s2.nextLine();
            if (line.contains("<featureManager>")) {//So has reached featureSets
                while (s2.hasNextLine()) {
                    line = s2.nextLine();
                    if (line.contains("</featureManager>"))
                        break;
                    //Otherwise is a featureset and only if the original featureset list is null do we need to get
                    if (createOriginalList) {
                        line = line.replaceAll("<feature>", "");
                        line = line.replaceAll("</feature>", "");
                        line = line.trim();
                        originalFeatureSet.add(line);
                    }
                }
                w.write("   <featureManager>");
                w.write("\n");
                for (String feature : newFeatures) {
                    w.write("               <feature>" + feature.trim() + "</feature>");
                    w.write("\n");
                }
                w.write("   </featureManager>");
                w.write("\n");
            } else {
                w.write(line);
                w.write("\n");
            }
        }
        s2.close();
        originalOutput.close();
        w.flush();
        w.close();
        //Now we need to copy file overwriting existing client.xml and delete the temp
        tempclientXML.copyToDest(clientXML, false, true);
        tempclientXML.delete();
    }

    protected void printProcessHoldingPort(int port) {
        final String m = "printProcessHoldingPort";
        try {
            PortDetectionUtil detector = PortDetectionUtil.getPortDetector(machine);
            Log.info(c, m, detector.determineOwnerOfPort(port));
        } catch (Exception ex) {
            Log.error(c, m, ex, "Caught exception while trying to detect the process holding port " + port);
        }
    }

    public enum IncludeArg {
        MINIFY, ALL, USR;

        public String getIncludeString() {
            return "--include=" + this.toString().toLowerCase();
        }
    }

    protected ArrayList<String> setArgs(final IncludeArg include, final String osFilter) {
        final ArrayList<String> args = new ArrayList<String>();

        args.add(include.getIncludeString());

        if (osFilter != null) {
            args.add("--os=" + osFilter);
        }
        return args;
    }

    public ProgramOutput startClientWithArgs(boolean preClean, boolean cleanStart,
                                             boolean validateApps, boolean expectStartFailure,
                                             String clientCmd, List<String> args,
                                             boolean validateTimedExit) throws Exception {
        return (startClientWithArgs(preClean, cleanStart, false, validateApps, expectStartFailure, clientCmd, args, validateTimedExit));
    }

    public ProgramOutput startClientWithArgs(boolean preClean, boolean cleanStart,
                                             boolean autoAccept, boolean validateApps, boolean expectStartFailure,
                                             String clientCmd, List<String> args,
                                             boolean validateTimedExit) throws Exception {
        final String method = "startClientWithArgs";

        if (clientCleanupProblem) {
            throw new Exception("The client was not cleaned up on the previous test.");
        }

        //Tidy up any pre-existing logs
        if (preClean)
            preStartClientLogsTidy();

        Properties envVars = new Properties();

        final String cmd = installRoot + "/bin/client";
        ArrayList<String> parametersList = new ArrayList<String>();
        boolean executeAsync = false;
        if ("run".equals(clientCmd) && DEBUGGING_PORT != null && !!!DEBUGGING_PORT.equalsIgnoreCase(Boolean.toString(false))) {
            Log.info(c, method, "Setting up commands for debug");
            parametersList.add("debug");
            parametersList.add(clientToUse);
            envVars.setProperty("DEBUG_PORT", DEBUGGING_PORT);
            // set client time out to 15 minutes to give time to connect. Timed exit likely kicks in after that, so
            // a larger value is worthless (and, since we multiply it by two later, will wrap if you use MAX_VALUE)
            clientStartTimeout = 15 * 60 * 60 * 1000;
            executeAsync = true;
        } else {
            parametersList.add(clientCmd);
            parametersList.add(clientToUse);
        }

        if (cleanStart) {
            parametersList.add("--clean");
        }

        if (autoAccept) {
            parametersList.add("--autoAcceptSigner");
        }

        if (args != null) {
            parametersList.addAll(args);
        }

        //Setup the client logs assuming the default setting.
        messageAbsPath = logsRoot + messageFileName;
        consoleAbsPath = logsRoot + consoleFileName;

        Log.info(c, method, "Starting client, messages will go to file " + messageAbsPath);

        final String[] parameters = parametersList.toArray(new String[] {});

        //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
        envVars.setProperty("JAVA_HOME", machineJava);

        // Pick up global JVM args (forced by build properties)
        String JVM_ARGS = GLOBAL_JVM_ARGS;

        // Always set tmp dir.
        JVM_ARGS += " -Djava.io.tmpdir=" + TMP_DIR;

        //FIPS 140-3
        JavaInfo javaInfo = JavaInfo.forClient(this);

        // Add JaCoCo java agent to generate code coverage for FAT test run
        if (DO_COVERAGE) {
            JVM_ARGS += " " + JAVA_AGENT_FOR_JACOCO;
        }

        //if we are on Mac then use the value of the perm gen arg that has been
        //passed in via the system property
        if (MAC_RUN != null && !!!MAC_RUN.equalsIgnoreCase(Boolean.toString(false))) {
            JVM_ARGS += " " + MAC_RUN;
        }

        // if we have java 2 security enabled, add java.security.manager and java.security.policy
        if (GLOBAL_JAVA2SECURITY) {
            RemoteFile f = getClientBootstrapPropertiesFile();
            Log.info(c, "startClientWithArgs", "remoteFile: " + f.getAbsolutePath());

            if (clientNeedsToRunWithJava2Security()) {
                addJava2SecurityPropertiesToBootstrapFile(f);
                Log.info(c, "startClientWithArgs", "Java 2 Security enabled for client " + getClientName() + " because GLOBAL_JAVA2SECURITY=true");
            } else {
                LOG.warning("The build is configured to run FAT tests with Java 2 Security enabled, but the FAT client " + getClientName() +
                            " is exempt from Java 2 Security regression testing.");
            }
        } else if (javaInfo.majorVersion() >= 18) {
            // Check if "websphere.java.security" has been added to bootstrapping.properties
            // as some tests will add it for their own security enable tests
            boolean bootstrapHasJava2SecProps = false;
            RemoteFile f = getClientBootstrapPropertiesFile();
            java.io.BufferedReader reader = null;
            try {
                reader = new java.io.BufferedReader(new java.io.InputStreamReader(f.openForReading()));
                String line = reader.readLine();
                while (line != null) {
                    if (line != null && line.trim().equals("websphere.java.security")) {
                        bootstrapHasJava2SecProps = true;
                        break;
                    }
                    line = reader.readLine();
                }
            } catch (Exception e) {
                Log.info(c, "startClientWithArgs", "caught exception checking bootstap.properties file for Java 2 Security properties, e: ", e.getMessage());
            } finally {
                if (reader != null)
                    reader.close();
            }

            if (bootstrapHasJava2SecProps) {
                if (javaInfo.majorVersion() >= 24) {
                    // Security manager is permanently disabled starting in Java 24
                    LOG.severe("The build is configured to run FAT tests with Java 2 security enabled, but the security manager is permanently disabled in Java versions 24 and later.  The security manager cannot be set!");
                    throw new RuntimeException("The security manager is permanently disabled in Java versions 24 and later.  When running FATs, use @MaximumJavaLevel(javaLevel = 23) or disable Java 2 security to prevent this test from failing running in Java 24 or later.");
                }

                // If we are running on Java 18 through 23, then we need to explicitly enable the security manager
                Log.info(c, "startClientWithArgs", "Java 18 + Java2Sec requested, setting -Djava.security.manager=allow");
                JVM_ARGS += " -Djava.security.manager=allow";
            }
        }

        //FIPS 140-3
        // if we have FIPS 140-3 enabled, and the matched java/platform, add JVM arg
        if (isFIPS140_3EnabledAndSupported()) {
            Log.info(c, "startClientWithArgs", "The JDK version: " + javaInfo.majorVersion() + " and vendor: " + JavaInfo.Vendor.IBM);

            if (javaInfo.majorVersion() == 17){
                Log.info(c, "startClientWithArgs", "FIPS 140-3 global build properties is set for Client " + getClientName()
                                                   + " with IBM Java 17, adding required JVM arguments to run with FIPS 140-3 enabled");
                                                   
                JVM_ARGS += " -Dsemeru.fips=true";
                JVM_ARGS += " -Dsemeru.customprofile=OpenJCEPlusFIPS.FIPS140-3-withPKCS12";
                JVM_ARGS += " -Dcom.ibm.fips.mode=140-3";
                // JVM_ARGS += " -Djavax.net.debug=all";  // Uncomment as needed for additional debugging
            } else if (javaInfo.majorVersion() == 8) {
                Log.info(c, "startClientWithArgs", "FIPS 140-3 global build properties is set for Client " + getClientName()
                                               + " with IBM Java 8, adding JVM arguments -Xenablefips140-3, ...,  to run with FIPS 140-3 enabled");

                JVM_ARGS += " -Xenablefips140-3";
                JVM_ARGS += " -Dcom.ibm.jsse2.usefipsprovider=true";
                JVM_ARGS += " -Dcom.ibm.jsse2.usefipsProviderName=IBMJCEPlusFIPS";
                JVM_ARGS += " -Dcom.ibm.fips.mode=140-3";
                // JVM_ARGS += " -Djavax.net.debug=all";  // Uncomment as needed for additional debugging
            }
        }

        // Look for forced client trace..
        if (!GLOBAL_TRACE.isEmpty()) {
            RemoteFile f = getClientBootstrapPropertiesFile();
            Properties props = new Properties();

            if (f.exists()) {
                InputStream is = null;
                try {
                    is = f.openForReading();
                    props.load(is);
                } catch (Exception e) {
                    LOG.warning("Error reading " + f + ": " + e.getMessage());
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            // get configured trace string from bootstrap properties..
            // (have to take include into account,  ugh..
            String configuredTrace = props.getProperty("com.ibm.ws.logging.trace.specification");
            if (configuredTrace == null) {
                String includeFiles = props.getProperty("bootstrap.include");
                if (includeFiles != null) {
                    String[] files = includeFiles.split("\\s*,\\s*");
                    for (String fileName : files) {
                        RemoteFile x = machine.getFile(clientRoot + "/" + fileName);
                        if (x.exists()) {
                            props.clear();
                            InputStream is = null;
                            try {
                                is = x.openForReading();
                                props.load(is);
                                configuredTrace = props.getProperty("com.ibm.ws.logging.trace.specification");
                                if (configuredTrace != null)
                                    break;
                            } catch (Exception e) {
                                LOG.warning("Error reading " + x + ": " + e.getMessage());
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

            JVM_ARGS += " -Dcom.ibm.ws.logging.trace.specification=" + configuredTrace;
        }

        envVars.setProperty("JVM_ARGS", JVM_ARGS);

        // This takes the custom console file name used for tests into consideration
        envVars.setProperty("LOG_DIR", logsRoot);
        envVars.setProperty("LOG_FILE", consoleFileName);

        Log.info(c, method, "Using additional env props: " + envVars.toString());

        Log.info(c, method, "Starting Client with command: " + cmd);

        if (isFIPS140_3EnabledAndSupported()) {
            String clientSecurityDir = clientRoot + File.separator + "resources" + File.separator + "security";
            File ltpaFIPSKeys = new File(clientSecurityDir, "ltpaFIPS.keys");
            File ltpaKeys = new File(clientSecurityDir, "ltpa.keys");
        
            if (!ltpaKeys.exists() && !ltpaFIPSKeys.exists()) {
                Log.info(this.getClass(), "startClientWithArgs", 
                        "FIPS 140-3 global build properties are set for client " + getClientName() 
                        + ", but neither ltpa.keys nor ltpaFIPS.keys is found in " + clientSecurityDir);
            } else {
                Log.info(this.getClass(), "startClientWithArgs", 
                        "FIPS 140-3 global build properties are set for client " + getClientName() 
                        + ", swapping ltpaFIPS.keys into ltpa.keys");
        
                try {
                    // Delete ltpa.keys if it exists
                    if (ltpaKeys.exists()) {
                        if (!ltpaKeys.delete()) {
                            Log.info(this.getClass(), "startClientWithArgs", "Failed to delete existing ltpa.keys.");
                        } else {
                            Log.info(this.getClass(), "startClientWithArgs", "Waiting for 1 second after deleting ltpa.keys.");
                            Thread.sleep(1000);
                        }
                    }
        
                    // Rename ltpaFIPS.keys to ltpa.keys if ltpaFIPS.keys exists
                    if (ltpaFIPSKeys.exists()) {
                        if (!ltpaFIPSKeys.renameTo(ltpaKeys)) {
                            Log.info(this.getClass(), "startClientWithArgs", "Failed to rename ltpaFIPS.keys to ltpa.keys.");
                        } else {
                            Log.info(this.getClass(), "startClientWithArgs", "Waiting for 1 second after rename.");
                            Thread.sleep(1000);
                        }
                    
                        // Log the content of ltpa.keys
                        String content = FileUtils.readFile(ltpaKeys.getAbsolutePath());
                        Log.info(this.getClass(), "printLtpaKeys", "Content of ltpa.keys: " + content);
                    }
        
                } catch (Exception e) {
                    Log.info(this.getClass(), "startClientWithArgs", "Error during ltpa.keys handling: " + e.getMessage());
                }
            }
        }        

        ProgramOutput output;
        if (executeAsync) {
            if (!(machine instanceof LocalMachine)) {
                throw new Exception("");
            }

            LocalMachine localMachine = (LocalMachine) machine;
            File f = new File(logsRoot + File.separator + consoleFileName);
            if (!f.exists())
                f.getParentFile().mkdirs();
            OutputStream redirect = new FileOutputStream(f);
            String workDir = new File(this.clientOutputRoot).getAbsolutePath();
            localMachine.executeAsync(cmd, parameters, workDir, envVars, redirect);
            Log.info(c, method, "Started client process in debug mode");
            output = null;
        } else {
            output = machine.execute(cmd, parameters, machine.getWorkDir(), envVars, 300);

            int rc = output.getReturnCode();
            Log.info(c, method, "Response from script is: " + output.getStdout());
            Log.info(c, method, "Return code from script is: " + rc);

            if (rc != 0 && expectStartFailure) {
                Log.info(c, method, "EXPECTED: Client didn't start");
                Log.exiting(c, method);
                return output;
            }
        }

        // Validate the client and apps started - if they didn't, that
        // method will throw an appropriate exception

        if ("run".equals(clientCmd)) {
            validateClientStarted(output, validateApps, expectStartFailure, validateTimedExit);
            // validate will set correctly - can't assume the answer is true
            //isStarted = true;
        }

        // Create a marker file to indicate client is started
        createClientMarkerFile();

        if ("run".equals(clientCmd)) {
            validateClientStopped(output, expectStartFailure);
        }
        postStopClientArchive();
        return output;
    }

    private void addJava2SecurityPropertiesToBootstrapFile(RemoteFile f) throws Exception {
        java.io.OutputStream w = f.openForWriting(true);
        try {
            w.write("\n".getBytes());
            w.write("websphere.java.security".getBytes());
            w.write("\n".getBytes());
            w.write("websphere.java.security.norethrow=false".getBytes());
            w.write("\n".getBytes());

            Log.info(c, "addJava2SecurityPropertiesToBootstrapFile", "Successfully updated bootstrap.properties file with Java 2 Security properties");
        } catch (Exception e) {
            Log.info(c, "addJava2SecurityPropertiesToBootstrapFile", "Caught exception updating bootstap.properties file with Java 2 Security properties, e: ", e.getMessage());
        }
        w.flush();
        w.close();
    }

    /**
     * Create a marker file for the client to indicate it is started.
     *
     * @throws IOException
     */
    protected void createClientMarkerFile() throws Exception {

        File outputFolder = new File(pathToAutoFVTOutputFolder);
        if (!outputFolder.exists())
            outputFolder.mkdirs();

        String path = pathToAutoFVTOutputFolder + getClientName() + ".mrk";
        LocalFile clientRunningFile = new LocalFile(path);
        File createFile = new File(clientRunningFile.getAbsolutePath());
        createFile.createNewFile();
        OutputStream os = clientRunningFile.openForWriting(true);
        os.write(0);
        os.flush();
        os.close();
    }

    /**
     * Delete a marker file for the client (after stopped).
     *
     * @throws IOException
     */
    protected void deleteClientMarkerFile() throws Exception {

        String path = pathToAutoFVTOutputFolder + getClientName() + ".mrk";
        LocalFile clientRunningFile = new LocalFile(path);
        Log.info(c, "deleteClientMarkerFile", "Client marker file: " + clientRunningFile.getAbsolutePath());
        File deleteFile = new File(clientRunningFile.getAbsolutePath());
        if (deleteFile.exists()) {
            deleteFile.delete();
        }
    }

    public void validateAppLoaded(String appName) throws Exception {
        String exceptionText = validateAppsLoaded(Collections.singletonList(appName), LOG_SEARCH_TIMEOUT, getDefaultLogFile());
        if (exceptionText != null) {
            throw new TopologyException(exceptionText);
        }
    }

    protected void validateAppsLoaded(RemoteFile outputFile) throws Exception {
        final String method = "validateAppsLoaded";

        if (installedApplications.isEmpty()) {
            Log.info(c, method, "No applications are installed so no need to validate they are loaded");
            return;
        }

        String exceptionText = validateAppsLoaded(installedApplications, LOG_SEARCH_TIMEOUT, outputFile);
        if (exceptionText != null) {
            throw new TopologyException(exceptionText);
        }
    }

    protected String validateAppsLoaded(List<String> appList, int timeout, RemoteFile outputFile) throws Exception {
        // At time of writing, timeout argument was being ignored. Preserve that for now...
        timeout = LOG_SEARCH_TIMEOUT;
        return validateAppsLoaded(appList, timeout, 2 * timeout, outputFile);
    }

    /**
     * We are adding a "soft failure" mode, in an attempt to compensate for the fact that the Virtual Machines
     * we're now running regression tests on have "bursty" performance and may introduce substantial delays with
     * no warning, making the originally coded times fragile.
     *
     * With this change, if intendedTimeout is exceeded, we report this to the SOE client
     * but do not consider it a test failure. Only if extendedTimeout is exceeded will we return a not-found indication.
     *
     * @param  regexp          a regular expression to search for
     * @param  intendedTimeout a timeout, in milliseconds, within which string was expected to occur
     * @param  extendedTimeout a timeout, in milliseconds, within which string may acceptably occur
     * @param  outputFile      file to check
     * @return                 line that matched the regexp, or null to indicate not found within acceptable (extended) timeout
     */
    protected String validateAppsLoaded(List<String> appList, int intendedTimeout, int extendedTimeout, RemoteFile outputFile) throws Exception {
        final String method = "validateAppsLoaded";

        final long startTime = System.currentTimeMillis();
        final long finalTime = startTime + extendedTimeout;
        long slowTime = startTime + intendedTimeout;
        try {
            long offset = 0;
            final List<String> regexpList = Collections.singletonList("CWWKZ");
            Map<String, Pattern> unstartedApps = new HashMap<String, Pattern>();
            for (String appName : appList) {
                unstartedApps.put(appName, Pattern.compile(".*\\b" + appName + "\\b.*"));
            }
            Map<String, List<String>> failedApps = new HashMap<String, List<String>>();
            boolean timedOut = false;

            Log.info(c, method, "Searching for app manager messages in " + outputFile.getAbsolutePath());
            for (;;) {
                LogSearchResult allMatches = LibertyFileManager.findStringsInFileCommon(regexpList, Integer.MAX_VALUE, outputFile, offset);
                if (allMatches != null && !!!allMatches.getMatches().isEmpty()) {
                    processAppManagerMessages(allMatches, unstartedApps, failedApps);
                }
                if (unstartedApps.isEmpty()) {
                    break;
                }
                if (System.currentTimeMillis() > finalTime) {
                    timedOut = true;
                    break;
                }
                if (System.currentTimeMillis() > slowTime) {
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(CLASS_NAME, method, 1071, intendedTimeout, "Unstarted: " + unstartedApps);
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(CLASS_NAME, method, 1072, intendedTimeout, "Failed: " + failedApps);
                    slowTime = finalTime + WAIT_INCREMENT; // don't report again
                }
                offset = allMatches == null ? 0 : allMatches.getOffset();
                try {
                    Thread.sleep(WAIT_INCREMENT);
                } catch (InterruptedException e) {
                    // Ignore and carry on
                }
            }
            if (timedOut) {
                Log.warning(c, "Timed out searching for app manager messages in log file: " + outputFile.getAbsolutePath());
            }
            String plural = unstartedApps.size() > 1 ? "s" : "";
            if (failedApps.isEmpty()) {
                if (unstartedApps.isEmpty()) {
                    return null;
                }

                // If apps failed to start, try to make sure the port opened so we correctly
                // flag a port issue as the culprit.
                //validatePortStarted();

                return "Timed out waiting for application" + plural + " " + unstartedApps.keySet() + " to start.";

            }
            String exceptionText = "Failures occured while waiting for app" + plural + " to start:";
            for (Map.Entry<String, List<String>> entry : failedApps.entrySet()) {
                for (String failure : entry.getValue()) {
                    String text;
                    if (entry.getKey().equals("*")) {
                        text = "App Manager Failure: " + failure;
                    } else {
                        text = "Application " + entry.getKey() + " failure: " + failure;
                    }
                    Log.info(c, method, text);
                    exceptionText += "\n  " + text;
                }
            }
            return exceptionText;
        } catch (Exception e) {
            Log.error(c, method, e, "Exception thrown confirming apps are loaded when validating that "
                                    + outputFile.getAbsolutePath() + " contains application install messages.");
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(c, method,
                     "Started searching for app manager messages at " +
                                formatter.format(new Date(startTime)) +
                                " and finished at " +
                                formatter.format(new Date(endTime)));
        }
    }

/*
 * App Manager messages that tests wait for in the log:
 *
 * CWWKZ0001I - many
 * CWWKZ0002E - none
 * CWWKZ0003I - com.ibm.ws.app.manager_fat, com.ibm.ws.classloading_fat, com.ibm.ws.ejbcontainer.security_fat,
 * com.ibm.ws.jpa_fat, com.ibm.ws.jsf_fat_lWAS, com.ibm.ws.management.repository.client_fat,
 * com.ibm.ws.ssl_fat, com.ibm.ws.webcontainer-8.0_fat_lWAS
 * CWWKZ0004E - none
 * CWWKZ0005E - none
 * CWWKZ0006E - none
 * CWWKZ0007W - none
 * CWWKZ0008E - none
 * CWWKZ0009I - com.ibm.ws.app.manager_fat, com.ibm.ws.app.manager.eba_fat, com.ibm.ws.eba.bundle.repository_fat,
 * com.ibm.ws.jaxws_fat, com.ibm.ws.jca_fat, com.ibm.ws.jsf_fat_lWAS, com.ibm.ws.management.repository.client_fat,
 * com.ibm.ws.session-3.0_fat, com.ibm.ws.webcontainer-8.0_fat_lWAS, com.ibm.ws.webcontainer.security_fat
 * CWWKZ0010E - none
 * CWWKZ0011E - none
 * CWWKZ0012I - com.ibm.ws.app.manager.eba_fat, com.ibm.ws.webcontainer-8.0_fat_lWAS
 * CWWKZ0013E - com.ibm.ws.app.manager_fat, com.ibm.ws.security_fat
 * CWWKZ0014W - com.ibm.ws.app.manager_fat
 * CWWKZ0015E - none
 * CWWKZ0016E - none
 * CWWKZ0017E - none
 * CWWKZ0018I - none
 * CWWKZ0019I - none
 * CWWKZ0020I - none
 * CWWKZ0021E - none
 * CWWKZ0022W - none
 * CWWKZ0053E - none
 * CWWKZ0054E - none
 * CWWKZ0055E - none
 * CWWKZ0056E - none
 * CWWKZ0057E - none
 * CWWKZ0058I - com.ibm.ws.app.manager_fat, javax.jaxb-2.2_fat
 * CWWKZ0059E - com.ibm.ws.app.manager_fat
 * CWWKZ0060E - none
 * CWWKZ0060W - none
 * CWWKZ0106E - none
 * CWWKZ0107E - none
 * CWWKZ0111E - none
 * CWWKZ0112E - none
 * CWWKZ0113E - none
 * CWWKZ0114E - none
 * CWWKZ0115E - none
 * CWWKZ0116E - none
 * CWWKZ0117E - none
 * CWWKZ0118E - none
 * CWWKZ0120E - none
 * CWWKZ0121E - none
 * CWWKZ0201E - none
 * CWWKZ0202E - none
 * CWWKZ0203E - none
 * CWWKZ0204E - none
 * CWWKZ0205E - none
 * CWWKZ0206E - none
 * CWWKZ0207E - none
 * CWWKZ0301E - com.ibm.ws.app.manager.eba_fat?, com.ibm.ws.eba.fidelity_fat
 * CWWKZ0302E - com.ibm.ws.app.manager.eba_fat
 * CWWKZ0303E - none
 * CWWKZ0304E - com.ibm.ws.app.manager.eba_fat, com.ibm.ws.eba.bundle.repository_fat
 * CWWKZ0401E - none
 * CWWKZ0402E - none
 * CWWKZ0403E - none
 * CWWKZ0404E - none
 */
    protected enum Action {
        REMOVE_APP_NAME_FROM_UNSTARTED_APPS,
        ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS,
        ADD_FAILURE_FOR_ALL_FAILED_APPS,
        IGNORE
    }

    protected enum AppManagerMessage {
        // app.manager
        // APPLICATION_START_SUCCESSFUL=CWWKZ0001I: Application {0} started in {1} seconds.
        CWWKZ0001I(Action.REMOVE_APP_NAME_FROM_UNSTARTED_APPS),
        // APPLICATION_START_FAILED=CWWKZ0002E: An exception occurred while starting the application {0}. The exception message was: {1}
        CWWKZ0002E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // APPLICATION_UPDATE_SUCCESSFUL=CWWKZ0003I: The application {0} updated in {1} seconds.
        CWWKZ0003I,
        // APPLICATION_UPDATE_FAILED=CWWKZ0004E: An exception occurred while starting the application {0}. The exception message was: {1}
        CWWKZ0004E,
        // NO_APPLICATION_HANDLER=CWWKZ0005E: The application {0} cannot start because the client is not configured to handle applications of type {1}.
        CWWKZ0005E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // CANNOT_CREATE_DIRECTORY=CWWKZ0006E: The client could not create a download location at {0} for the {1} application.
        CWWKZ0006E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // DOWNLOAD_EXCEPTION_ENCOUNTERED=CWWKZ0007W: An exception occurred while downloading the file from {0}. The exception message was: {1}
        CWWKZ0007W(Action.ADD_FAILURE_FOR_ALL_FAILED_APPS),
        // LOCATION_SERVICE_NOT_FOUND=CWWKZ0008E: An internal error has occurred. The system could not get the location service that is required to resolve file locations.
        CWWKZ0008E(Action.ADD_FAILURE_FOR_ALL_FAILED_APPS),
        // APPLICATION_STOPPED=CWWKZ0009I: The application {0} has stopped successfully.
        CWWKZ0009I,
        // APPLICATION_STOP_FAILED=CWWKZ0010E: An exception occurred while stopping the application {0}. The exception message was: {1}
        CWWKZ0010E,
        // FIND_FILE_EXCEPTION_ENCOUNTERED=CWWKZ0011E: An exception occurred while searching for the file {0}. The exception message was: {1}
        CWWKZ0011E,
        // APPLICATION_NOT_STARTED=CWWKZ0012I: The application {0} was not started.
        CWWKZ0012I(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // DUPLICATE_APPLICATION_NAME=CWWKZ0013E: It is not possible to start two applications called {0}.
        CWWKZ0013E,
        // APPLICATION_NOT_FOUND=CWWKZ0014W: The application {0} could not be started as it could not be found at location {1}.
        CWWKZ0014W(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // APPLICATION_NO_LOCATION_NO_NAME=CWWKZ0015E: An application has been configured with no location or name.
        CWWKZ0015E,
        // APPLICATION_NO_LOCATION=CWWKZ0016E: The application {0} has not been configured with a location.
        CWWKZ0016E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // APPLICATION_NO_TYPE=CWWKZ0017E: It was not possible to infer the application type for application {0} from the location {1}.
        CWWKZ0017E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // STARTING_APPLICATION=CWWKZ0018I: Starting application {0}.
        CWWKZ0018I,
        // PARTIAL_START=CWWKZ0019I: Application {0} partly started in {1} seconds.
        CWWKZ0019I,
        // APPLICATION_NOT_UPDATED=CWWKZ0020I: Application {0} not updated.
        CWWKZ0020I,
        // APPLICATION_AT_LOCATION_NOT_VALID=CWWKZ0021E: Application {0} at location {1} is invalid.
        CWWKZ0021E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // APPLICATION_SLOW_STARTUP=CWWKZ0022W: Application {0} has not started in {1} seconds.
        CWWKZ0022W,
        //
        // MONITOR_APP_STOP_FAIL=CWWKZ0053E: An exception occurred while trying to stop the {0} application automatically.
        CWWKZ0053E,
        // INVALID_FILE_NAME=CWWKZ0054E: The application monitoring service could not find a suitable name for the {0} application.
        CWWKZ0054E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // UNABLE_TO_DETERMINE_APPLICATION_TYPE=CWWKZ0055E: The application monitoring service could not determine the type of the {0} application.
        CWWKZ0055E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // MONITOR_APP_START_FAIL=CWWKZ0056E: An exception occurred while trying to automatically start the {0} application.
        CWWKZ0056E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // MONITOR_INVALID_CACHE_FILE_ENTRY=CWWKZ0057E: When reading the cached list of started applications from the application monitor ({0}), line number {1} was not valid. The line was: {2}.
        CWWKZ0057E,
        // APPLICATION_MONITOR_STARTED=CWWKZ0058I: Monitoring {0} for applications.
        CWWKZ0058I,
        // INVALID_DELETE_OF_APPLICATION=CWWKZ0059E: The {0} application installed from {1} has been deleted while it is still configured.
        CWWKZ0059E,
        // MONITOR_DIR_CLEANUP_FAIL=CWWKZ0060E: The client could not clean up the old monitored directory at {0}.
        CWWKZ0060E,
        // APPLICATION_MONITORING_FAIL=CWWKZ0060W: Unable to monitor the {0} application.
        CWWKZ0060W,
        // app.manager.eba
        // eba.installer.resolver.fail=CWWKZ0301E: An exception occurred trying to resolve the application {0} into an OSGi framework.  The error text is: {1}
        CWWKZ0301E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // eba.installer.bundle.fail=CWWKZ0302E: A bundle exception was generated when trying to install the application {0} into an OSGi framework.  The error text from the OSGi framework is: {1}
        CWWKZ0302E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // eba.installer.aries.management.fail=CWWKZ0303E: A management exception was generated when trying to install the application {0} into an OSGi framework.  The error text from the OSGi framework is: {1}
        CWWKZ0303E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // eba.installer.aries.resolver.fail=CWWKZ0304E: An exception was generated when trying to resolve the contents of the application {0}.  The exception text from the OSGi framework is: {1}
        CWWKZ0304E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // app.manager.esa
        // esa.installer.resolver.fail=CWWKZ0401E: An exception occurred trying to resolve the application {0} into an OSGi framework.  The error text is: {1}
        CWWKZ0401E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // esa.installer.bundle.fail=CWWKZ0402E: A bundle exception was generated when trying to install the application {0} into an OSGi framework.  The error text from the OSGi framework is: {1}
        CWWKZ0402E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // esa.installer.aries.management.fail=CWWKZ0403E: A management exception was generated when trying to install the application {0} into an OSGi framework.  The error text from the OSGi framework is: {1}
        CWWKZ0403E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // esa.installer.aries.resolver.fail=CWWKZ0404E: An exception was generated when trying to resolve the contents of the application {0}.  The exception text from the OSGi framework is: {1}
        CWWKZ0404E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // app.manager.module
        // error.cache.adapt=CWWKZ0107E: An internal error occurred. Unable to adapt cache for web module {0}.
        CWWKZ0107E,
        // app.manager.wab
        // bundle.tracker.init.fail=CWWKZ0201E: An error occurred initializing the WAB installer.
        CWWKZ0201E,
        // wab.install.fail=CWWKZ0202E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0202E,
        // wab.install.fail.adapt=CWWKZ0203E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0203E,
        // wab.install.fail.overlay=CWWKZ0204E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0204E,
        // wab.install.fail.cache=CWWKZ0205E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0205E,
        // wab.install.fail.container=CWWKZ0206E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0206E,
        // wab.install.fail.wiring=CWWKZ0207E: Unable to install bundle {0} with context root {1} into the web container.
        CWWKZ0207E,
        // wab.install.fail.clash=CWWKZ0208E: Unable to install bundle {0} with context root {1} into the web container because that context root is already in use by {2}
        CWWKZ0208E,
        // app.manager.war
        // error.not.installed=CWWKZ0106E: Could not start web application {0}.
        CWWKZ0106E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.application.library.container=CWWKZ0111E: Application {0} encountered a error when accessing application library {1}: {2}
        CWWKZ0111E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.application.libraries=CWWKZ0112E: Application {0} encountered an error when listing application libraries: {1}
        CWWKZ0112E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.application.parse.descriptor=CWWKZ0113E: Application {0} encountered a parse error when processing application descriptor {1}: {2}
        CWWKZ0113E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.container=CWWKZ0114E: Application {0} encountered an error when accessing the contents of module {1} of type {2}: {3}
        CWWKZ0114E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.container.null=CWWKZ0115E: Application {0} obtained a null value when accessing the contents candidate module {1} of type {2}
        CWWKZ0115E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.create=CWWKZ0116E: Application {0} failed to finish creation of module {1} of type {2}: {3}
        CWWKZ0116E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.locate.failed=CWWKZ0117E: Application {0} failed to locate module {1} of type {2}
        CWWKZ0117E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.parse.descriptor=CWWKZ0118E: Application {0} encountered a parse error when processing descriptor {1} of module {2} of type {3}: {4}
        CWWKZ0118E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.class.source=CWWKZ0120E: Application {0} failed to access classes for module {1} of type {2}: {3}
        CWWKZ0120E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // error.module.annotation.targets=CWWKZ0121E: Application {0} failed to access annotations for module {1} of type {2}: {3}
        CWWKZ0121E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS);

        final Action action;

        AppManagerMessage() {
            this(Action.IGNORE);
        }

        AppManagerMessage(Action action) {
            this.action = action;
        }

        public void processMessage(String[] tokens, Map<String, Pattern> unstartedApps, Map<String, List<String>> failedApps) {
            String appName;
            switch (action) {
                case REMOVE_APP_NAME_FROM_UNSTARTED_APPS:
                    appName = findAppNameInTokens(unstartedApps, tokens);
                    if (appName != null) {
                        unstartedApps.remove(appName);
                    }
                    break;
                case ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS:
                    appName = findAppNameInTokens(unstartedApps, tokens);
                    if (appName != null) {
                        List<String> failures = failedApps.get(appName);
                        if (failures == null) {
                            failures = new ArrayList<String>();
                            failedApps.put(appName, failures);
                        }
                        failures.add(tokensToString(tokens));
                        unstartedApps.remove(appName);
                    }
                    break;
                case ADD_FAILURE_FOR_ALL_FAILED_APPS:
                    addFailureToAllFailedApps(tokens, failedApps);
                    break;
                case IGNORE:
                    break;
            }
        }
    }

    protected static String tokensToString(String[] tokens) {
        return tokens[0] + ":" + tokens[1];
    }

    protected static void addFailureToAllFailedApps(String[] tokens, Map<String, List<String>> failedApps) {
        List<String> failures = failedApps.get("*");
        if (failures == null) {
            failures = new ArrayList<String>();
            failedApps.put("*", failures);
        }
        failures.add(tokensToString(tokens));
    }

    protected void processAppManagerMessages(LogSearchResult allMatches, Map<String, Pattern> unstartedApps, Map<String, List<String>> failedApps) {
        final String method = "processAppManagerMessages";

        for (String line : allMatches.getMatches()) {
            line = line.substring(line.indexOf("CWWKZ"));
            Log.info(c, method, "line is " + line);
            String[] tokens = line.split(":", 2);
            Log.info(c, method, "tokens are (" + tokens[0] + ") (" + tokens[1] + ")");
            try {
                AppManagerMessage matchedMessage = AppManagerMessage.valueOf(tokens[0]);
                if (matchedMessage != null) {
                    matchedMessage.processMessage(tokens, unstartedApps, failedApps);
                }
            } catch (IllegalArgumentException ex) {
                ex.getCause();
                if (tokens[0].endsWith("E")) {
                    addFailureToAllFailedApps(tokens, failedApps);
                }
            }
        }
    }

    protected static String findAppNameInTokens(Map<String, Pattern> unstartedApps, String[] tokens) {
        final String method = "findAppNameInTokens";

        for (Map.Entry<String, Pattern> entry : unstartedApps.entrySet()) {
            Log.info(c, method, "looking for app " + entry.getKey() + " in " + tokens[1]);
            if (entry.getValue().matcher(tokens[1]).matches()) {
                Log.info(c, method, "matched app " + entry.getKey());
                return entry.getKey();
            }
        }
        Log.info(c, method, "no matches for apps found in " + tokens[1]);
        return null;
    }

    protected void validateClientStarted(ProgramOutput output, boolean validateApps,
                                         boolean expectStartFailure, boolean validateTimedExit) throws Exception {
        final String method = "validateClientStarted";

        final String START_MESSAGE_CODE = "CWWKF0035I";

        boolean clientStarted = false;

        if (checkForRestConnector.get()) {
            //since this is going to connect to the secure port, that needs to be ready
            //before an attempt to make the JMX connection
            Log.info(c, method, "Checking that the JMX RestConnector is available and secured");
            assertNotNull("CWWKO0219I.*ssl not received", waitForStringInLogUsingMark("CWWKO0219I.*ssl"));

            assertNotNull("IBMJMXConnectorREST app did not report as ready", waitForStringInLogUsingMark("CWWKT0016I.*IBMJMXConnectorREST"));

            assertNotNull("Security service did not report it was ready", waitForStringInLogUsingMark("CWWKS0008I"));

            //backup the key file
            try {
                copyFileToTempDir("resources/security/key.jks", "key.jks");
            } catch (Exception e) {
                copyFileToTempDir("resources/security/key.p12", "key.p12");
            }
        }

        Log.info(c, method, "Waiting up to " + (clientStartTimeout / 1000)
                            + " seconds for client confirmation:  "
                            + START_MESSAGE_CODE.toString() + " to be found in " + messageAbsPath);

        RemoteFile messagesLog = machine.getFile(messageAbsPath);

        try {
            RemoteFile f = getClientBootstrapPropertiesFile();
            Properties props = new Properties();

            if (f.exists()) {
                InputStream is = null;
                try {
                    is = f.openForReading();
                    props.load(is);
                } catch (Exception e) {
                    LOG.warning("Error reading " + f + ": " + e.getMessage());
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            String startMessage = waitForStringInLog(START_MESSAGE_CODE, clientStartTimeout, messagesLog);
            clientStarted = (startMessage != null);
            // If the client started successfully, we're started
            // (but the opposite isn't true since the client could already have been running)
            if (clientStarted) {
                isStarted = true;
                Log.info(c, method, "Client has started successfully");
            }
        } catch (Exception e) {
            Log.error(c, method, e, "Exception thrown confirming client started in " + consoleAbsPath);
            throw e;
        }

    }

    protected void validateClientStopped(ProgramOutput output, boolean expectStopFailure) throws Exception {
        final String method = "validateClientStopped";

        final String STOP_MESSAGE_CODE = "CWWKE0908I";

        boolean clientStopped = false;

        if (checkForRestConnector.get()) {
            //since this is going to connect to the secure port, that needs to be ready
            //before an attempt to make the JMX connection
            Log.info(c, method, "Checking that the JMX RestConnector is available and secured");
            assertNotNull("CWWKO0219I.*ssl not received", waitForStringInLogUsingMark("CWWKO0219I.*ssl"));

            assertNotNull("IBMJMXConnectorREST app did not report as ready", waitForStringInLogUsingMark("CWWKT0016I.*IBMJMXConnectorREST"));

            assertNotNull("Security service did not report it was ready", waitForStringInLogUsingMark("CWWKS0008I"));

            //backup the key file
            try {
                copyFileToTempDir("resources/security/key.jks", "key.jks");
            } catch (Exception e) {
                copyFileToTempDir("resources/security/key.p12", "key.p12");
            }
        }

        Log.info(c, method, "Waiting up to " + (clientStopTimeout / 1000)
                            + " seconds for client confirmation:  "
                            + STOP_MESSAGE_CODE.toString() + " to be found in " + messageAbsPath);

        RemoteFile messagesLog = machine.getFile(messageAbsPath);

        try {
            RemoteFile f = getClientBootstrapPropertiesFile();
            Properties props = new Properties();

            if (f.exists()) {
                InputStream is = null;
                try {
                    is = f.openForReading();
                    props.load(is);
                } catch (Exception e) {
                    LOG.warning("Error reading " + f + ": " + e.getMessage());
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            String stopMessage = waitForStringInLog(STOP_MESSAGE_CODE, clientStopTimeout, messagesLog);
            clientStopped = (stopMessage != null);
            // If the client started successfully, we're started
            // (but the opposite isn't true since the client could already have been running)
            if (clientStopped) {
                isStopped = true;
                Log.info(c, method, "Client has stopped successfully");
            }
        } catch (Exception e) {
            Log.error(c, method, e, "Exception thrown confirming client stopped in " + consoleAbsPath);
            throw e;
        }

    }

    /**
     * Checks client logs for any lines containing errors or warnings that
     * do not match any regular expressions provided in regIgnore.
     * regIgnore list provided by
     *
     *
     * @return A list of lines containing errors/warnings from client logs
     */
    protected void checkLogsForErrorsAndWarnings() throws Exception {
        final String method = "checkLogsForErrorsAndWarnings";

        if (!isClientExemptFromChecking()) {
            // Get all warnings and errors in logs
            List<String> errorsInLogs = null;
            try {
                errorsInLogs = this.findStringsInLogs(".*\\d{4}[EW]: .*");
            } catch (Exception e) {

            }
            //Add default Errors to the list of errors to ignore
            for (String ignoreRegEx : fixedIgnoreErrorsList) {
                ignorePatterns.add(Pattern.compile(ignoreRegEx));
            }

            // Remove any ignored warnings or patterns
            for (Pattern ignorePattern : ignorePatterns) {
                Iterator<String> iter = errorsInLogs.iterator();
                while (iter.hasNext()) {
                    if (ignorePattern.matcher(iter.next()).find()) {
                        // this is an ignored warning/error, remove it from list
                        iter.remove();
                    }
                }
            }

            Exception ex = null;
            if (errorsInLogs != null && !errorsInLogs.isEmpty()) {
                // There were unexpected errors in logs, print them
                // and set an exception to return
                StringBuffer sb = new StringBuffer("Errors/warnings were found in client logs:");
                for (String errorInLog : errorsInLogs) {
                    sb.append("\n <br>");
                    sb.append(errorInLog);
                    Log.info(c, method, "Error/warning found: " + errorInLog);
                }
                ex = new Exception(sb.toString());
            }

            if (ex == null)
                Log.info(c, method, "No unexpected errors or warnings found in client logs.");
            else
                throw ex;
        } else {
            Log.info(c, method, "skipping log validation for client " + getClientName());
        }
    }

    protected void clearMessageCounters() {
        //this is because we will be getting a new log file
        stopApplicationMessages.set(0);
        startApplicationMessages.set(0);
    }

    /**
     * This method is used to tidy away the client logs at the start.
     */
    protected void preStartClientLogsTidy() throws Exception {
        //should be .../liberty/usr/clients/<client>/logs
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getClientRoot() + "/logs");

        // Look for javacore/heapdump/snap. These are collected by stop/archive. We don't need
        // to collect them in the archive of every subsequent client run.
        List<String> files = listLibertyClientRoot(null, null);
        for (String name : files) {
            if (name.startsWith("javacore*") || name.startsWith("heapdump*") || name.startsWith("Snap*") || name.startsWith(clientToUse + ".dump")) {
                deleteFileFromLibertyInstallRoot(name);
            }
        }
    }

    /**
     * This method is used to archive client logs after the client completes.
     * Also, this will prevent the client log contents from being lost (over written) in a restart case.
     */
    public void postStopClientArchive() throws Exception {
        final String method = "postStopClientArchive";
        Log.entering(c, method);
        try {
            checkLogsForErrorsAndWarnings();
        } finally {
            ignorePatterns.clear();

            Log.info(c, method, "Moving logs to the output folder");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
            Date d = new Date(System.currentTimeMillis());
            this.logStamp = sdf.format(d);

            String logDirectoryName = pathToAutoFVTOutputClientsFolder + "/" + clientToUse + "-" + logStamp;
            LocalFile logFolder = new LocalFile(logDirectoryName);
            RemoteFile clientFolder = machine.getFile(clientRoot);

            runJextract(clientFolder);

            // Copy the log files: try to move them instead if we can
            recursivelyCopyDirectory(clientFolder, logFolder, false, true, true);

            deleteClientMarkerFile();

            Log.exiting(c, method);
        }
    }

    protected void runJextract(RemoteFile clientFolder) throws Exception {
        RemoteFile[] files = clientFolder.list(false);
        if (files != null) {
            for (RemoteFile file : files) {
                String filename = file.getAbsolutePath();
                if (filename.endsWith(".dmp")) {

                    Properties envVars = new Properties();
                    envVars.setProperty("JAVA_HOME", machineJava);
                    Log.info(c, "runJextract", "Running jextract on file: " + filename);

                    String outputFilename = filename + ".zip.DMP"; //adding .DMP to ensure it is collected even when not collecting archives
                    String cmd = machineJava + "/bin/jextract";
                    String[] parms = new String[] { filename, outputFilename };
                    ProgramOutput output = machine.execute(cmd, parms, clientFolder.getAbsolutePath(), envVars);
                    Log.info(c, "runJextract stdout", output.getStdout());
                    Log.info(c, "runJextract stderr", output.getStderr());
                    Log.info(c, "runJextract", "rc = " + output.getReturnCode());
                }
            }
        }
    }

    /**
     * @param remoteFile
     * @param logFolder
     * @param b
     * @param d
     */
    protected void recursivelyCopyDirectory(RemoteFile remoteFile, LocalFile logFolder, boolean ignoreFailures) throws Exception {
        recursivelyCopyDirectory(remoteFile, logFolder, ignoreFailures, false, false);

    }

    /**
     * @param  method
     * @throws Exception
     */
    protected void recursivelyCopyDirectory(RemoteFile remoteDirectory, LocalFile destination, boolean ignoreFailures, boolean skipArchives, boolean moveFile) throws Exception {
        destination.mkdirs();

        ArrayList<String> logs = new ArrayList<String>();
        logs = listDirectoryContents(remoteDirectory);
        for (String l : logs) {
            if (remoteDirectory.getName().equals("workarea")) {
                if (l.equals("org.eclipse.osgi") || l.startsWith(".s")) {
                    // skip the osgi framework cache, and runtime artifacts: too big / too racy
                    continue;
                }
            }
            if (remoteDirectory.getName().equals("messaging")) {
                continue;
            }

            RemoteFile toCopy = machine.getFile(remoteDirectory, l);
            LocalFile toReceive = new LocalFile(destination, l);
            String absPath = toCopy.getAbsolutePath();

            if (absPath.endsWith(".log"))
                LogPolice.measureUsedTrace(toCopy.length());

            if (toCopy.isDirectory()) {
                // Recurse
                recursivelyCopyDirectory(toCopy, toReceive, ignoreFailures, skipArchives, moveFile);
            } else {
                try {
                    if (skipArchives
                        && (absPath.endsWith(".jar")
                            || absPath.endsWith(".war")
                            || absPath.endsWith(".ear")
                            || absPath.endsWith(".rar")
                            //If we're only getting logs, skip jars, wars, ears, zips, unless they are client dump zips
                            || (absPath.endsWith(".zip") && !toCopy.getName().contains(clientToUse + ".dump")))) {
                        continue;
                    }

                    // We're only going to attempt to move log files. Because of ffdc log checking, we
                    // can't move those. But we should move other log files..
                    boolean isLog = (absPath.contains("logs") && !absPath.contains("ffdc"))
                                    || toCopy.getName().contains("javacore")
                                    || toCopy.getName().contains("heapdump")
                                    || toCopy.getName().contains("Snap")
                                    || toCopy.getName().contains(clientToUse + ".dump");

                    if (moveFile && isLog) {
                        boolean copied = false;

                        // If we're local, try to rename the file instead..
                        if (machine.isLocal() && toCopy.rename(toReceive)) {
                            copied = true; // well, we moved it, but it counts.
                        }

                        if (!copied && toReceive.copyFromSource(toCopy)) {
                            // copy was successful, clean up the source log
                            toCopy.delete();
                        }
                    } else {
                        toReceive.copyFromSource(toCopy);
                    }
                } catch (Exception e) {
                    // Ignore on request and carry on copying the rest of the files
                    if (!ignoreFailures) {
                        throw e;
                    }
                }
            }

        }
    }

    /**
     * This method will copy a file from the client root into the AutoFVT {@link #pathToAutoFVTTestFiles}/tmp folder.
     * If you are copying a directory and some of the files cannot be copied due to an error then these errors will
     * be ignored, this can happen if the client is still active and the files are locked by another process.
     *
     * If copying a file the destination will be overwritten.
     *
     * @param  pathInClientRoot The path to the file or directory in the client root, must not start with a "/"
     * @param  destination      The place within the temp folder to store this file, must not start with a "/"
     * @return                  the LocalFile of the copied RemoteFile
     * @throws Exception
     */
    public LocalFile copyFileToTempDir(String pathInClientRoot, String destination) throws Exception {
        return copyFileToTempDir(machine.getFile(clientRoot + "/" + pathInClientRoot), destination);
    }

    /**
     * This method will copy a file from the install root into the AutoFVT {@link #pathToAutoFVTTestFiles}/tmp folder.
     * If you are copying a directory and some of the files cannot be copied due to an error then these errors will
     * be ignored, this can happen if the client is still active and the files are locked by another process.
     *
     * If copying a file the destination will be overwritten.
     *
     * @param  pathInInstallRoot The path to the file or directory in the install root, must not start with a "/"
     * @param  destination       The place within the temp folder to store this file, must not start with a "/"
     * @return                   the LocalFile of the copied RemoteFile
     * @throws Exception
     */
    public LocalFile copyInstallRootFileToTempDir(String pathInInstallRoot, String destination) throws Exception {
        return copyFileToTempDir(machine.getFile(installRoot + "/" + pathInInstallRoot), destination);
    }

    protected LocalFile copyFileToTempDir(RemoteFile remoteToCopy, String destination) throws Exception {
        // Make sure the tmp dir exists
        LocalFile tmpDir = new LocalFile(pathToAutoFVTTestFiles + "/tmp");
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }

        LocalFile localCopy = new LocalFile(pathToAutoFVTTestFiles + "/tmp/" + destination);

        if (remoteToCopy.isDirectory()) {
            recursivelyCopyDirectory(remoteToCopy, localCopy, true);
        } else {
            localCopy.copyFromSource(remoteToCopy, false, true);
        }

        return localCopy;
    }

    public String getClientRoot() {
        return clientRoot;
    }

    public String getClientSharedPath() {
        return clientRoot + "/../../shared/";
    }

    /**
     * Get the collective dir under the client resources dir. For instance,
     * this is where the collective trust stores are located.
     *
     * @return the path
     */
    public String getCollectiveResourcesPath() {
        return clientRoot + "/resources/collective/";
    }

    public void setClientRoot(String clientRoot) {
        this.clientRoot = clientRoot;
    }

    public String getMachineJavaJDK() {
        return machineJava;
    }

    public String getMachineJavaJarCommandPath() {
        return machineJarPath;
    }

    /* not called */public void setMachineJava(String machineJava) {
        this.machineJava = machineJava;
    }

    public void copyFileToLibertyInstallRoot(String fileName) throws Exception {
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    public void copyFileToLibertyInstallRoot(String extendedPath, String fileName) throws Exception {
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/" + extendedPath, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    protected void copyFileToLibertyClientRootUsingTmp(String path, String relPathTolocalFile) throws Exception {
        LocalFile localFileToCopy = new LocalFile(LibertyServerUtils.makeJavaCompatible(relPathTolocalFile));
        LibertyFileManager.copyFileIntoLiberty(machine, path, localFileToCopy.getName(), relPathTolocalFile, false, clientRoot);
    }

    /**
     * Copies a file into the ${client.config.dir} of a Liberty Client.
     *
     * @param fromDir  The directory of the file to copy.
     * @param toDir    Any extra path beyond ${client.config.dir} for the destination.
     *                     For example, for a destination of ${client.config.dir}/test/ you would use toClientDir=test
     * @param fileName The name of the file to copy. The file name will be unchanged form source to dest
     */
    public void copyFileToLibertyClientRoot(String fromDir, String toDir, String fileName) throws Exception {
        if (toDir == null)
            toDir = "";
        copyFileToLibertyClientRootUsingTmp(clientRoot + "/" + toDir, (fromDir + "/" + fileName));
    }

    public void copyFileToLibertyClientRoot(String fileName) throws Exception {
        copyFileToLibertyClientRootUsingTmp(clientRoot, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    public void copyFileToLibertyClientRoot(String extendedPath, String fileName) throws Exception {
        copyFileToLibertyClientRootUsingTmp(clientRoot + "/" + extendedPath, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    public void renameLibertyClientRootFile(String oldFileName, String newFileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, clientRoot + "/" + oldFileName, clientRoot + "/" + newFileName);
    }

    public void renameLibertyInstallRootFile(String oldFileName, String newFileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, installRoot + "/" + oldFileName, installRoot + "/" + newFileName);
    }

    public RemoteFile getFileFromLibertyInstallRoot(String filePath) throws Exception {
        final String method = "getFileFromLibertyInstallRoot";
        Log.entering(c, method);
        return getFileFromLiberty(installRoot + "/" + filePath);
    }

    public RemoteFile getFileFromLibertyClientRoot(String filePath) throws Exception {
        final String method = "getFileFromLibertyClientRoot";
        Log.entering(c, method);
        return getFileFromLiberty(clientRoot + "/" + filePath);
    }

    /* not called */public RemoteFile getFileFromLibertySharedDir(String filePath) throws Exception {
        final String method = "getFileFromLibertySharedDir";
        Log.entering(c, method);
        return getFileFromLiberty(getClientSharedPath() + filePath);
    }

    protected RemoteFile getFileFromLiberty(String fullPath) throws Exception {
        Log.info(c, "getFileFromLiberty", "Getting file: " + fullPath);
        return LibertyFileManager.getLibertyFile(machine, fullPath);
    }

    public boolean fileExistsInLibertyInstallRoot(String filePath) throws Exception {
        final String method = "fileExistsInLibertyInstallRoot";
        Log.entering(c, method);
        return libertyFileExists(installRoot + "/" + filePath);
    }

    public boolean fileExistsInLibertyClientRoot(String filePath) throws Exception {
        final String method = "fileExistsInLibertyClientRoot";
        Log.entering(c, method);
        return libertyFileExists(clientRoot + "/" + filePath);
    }

    /* not called */public boolean fileExistsInLibertySharedDir(String filePath) throws Exception {
        final String method = "fileExistsInLibertySharedDir";
        Log.entering(c, method);
        return libertyFileExists(getClientSharedPath() + filePath);
    }

    protected boolean libertyFileExists(String fullPath) throws Exception {
        boolean exists = LibertyFileManager.libertyFileExists(machine, fullPath);
        Log.info(c, "libertyFileExists", "File: " + fullPath + " exists " + exists);
        return exists;
    }

    public String getClientName() {
        return clientToUse;
    }

    public void deleteFileFromLibertyInstallRoot(String filePath) throws Exception {
        LibertyFileManager.deleteLibertyFile(machine, (installRoot + "/" + filePath));
    }

    public void deleteDirectoryFromLibertyInstallRoot(String directoryPath) throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, (installRoot + "/" + directoryPath));
    }

    public void deleteDirectoryFromLibertyClientRoot(String directoryPath) throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, (clientRoot + "/" + directoryPath));
    }

    public void deleteFileFromLibertyClientRoot(String filePath) throws Exception {
        LibertyFileManager.deleteLibertyFile(machine, (clientRoot + "/" + filePath));
    }

    public RemoteFile getClientBootstrapPropertiesFile() throws Exception {
        return machine.getFile(clientRoot + "/bootstrap.properties");
    }

    /**
     * Non-recursively list the contents of the client install root directory, or if the relativePath
     * parameter is non-null, the indicated directory (relative the the install root). If filter is
     * non-null, return only those directory names or filenames that contain the filter string.
     *
     * @param  relativeDir path to a directory relative to the install root directory, should not begin with path separator, may be null.
     * @param  filter      string to filter the results by, returned file and directory names must contain this, may be null.
     * @return             a list of file and directory names indicating the contents of the specified directory.
     * @throws Exception
     */
    public List<String> listLibertyInstallRoot(String relativeDir, String filter) throws Exception {
        String path = installRoot;
        if (relativeDir != null && !relativeDir.equals("")) {
            path = path + "/" + relativeDir;
        }
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(path), filter);
    }

    /**
     * Non-recursively list the contents of the client install root directory, or if the relativePath
     * parameter is non-null, the indicated directory (relative the the install root). If filter is
     * non-null, return only those directory names or filenames that contain the filter string.
     *
     * @param  relativeDir path to a directory relative to the install root directory, should not begin with path separator, may be null.
     * @param  filter      string to filter the results by, returned file and directory names must contain this, may be null.
     * @return             a list of file and directory names indicating the contents of the specified directory.
     * @throws Exception
     */
    public ArrayList<String> listLibertyClientRoot(String relativeDir, String filter) throws Exception {
        String path = clientRoot;
        if (relativeDir != null && !relativeDir.equals("")) {
            path = path + "/" + relativeDir;
        }
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(path), filter);
    }

    /**
     * Method for returning the directory contents as a list of Strings representing first level file/dir names
     *
     * @return                   ArrayList of File/Directory names
     *                           that exist at the first level i.e. it's not recursive. If it's a directory the String in the list is prefixed with a /
     * @throws TopologyException
     */
    protected ArrayList<String> listDirectoryContents(RemoteFile clientDir) throws Exception {
        return listDirectoryContents(clientDir, null);

    }

    protected ArrayList<String> listDirectoryContents(String path, String fileName) throws Exception {

        RemoteFile clientDir = machine.getFile(path);
        return listDirectoryContents(clientDir, fileName);

    }

    protected ArrayList<String> listDirectoryContents(RemoteFile clientDir, String fileName) throws Exception {

        final String method = "clientDirectoryContents";
        Log.entering(c, method);
        if (!clientDir.isDirectory() || !clientDir.exists())
            throw new TopologyException("The specified directoryPath \'"
                                        + clientDir.getAbsolutePath() + "\' was not a directory");

        RemoteFile[] firstLevelFiles = clientDir.list(false);
        ArrayList<String> firstLevelFileNames = new ArrayList<String>();

        for (RemoteFile f : firstLevelFiles) {

            if (fileName == null) {
                firstLevelFileNames.add(f.getName());
            } else if (f.getName().contains(fileName)) {
                firstLevelFileNames.add(f.getName());

            }
        }

        return firstLevelFileNames;
    }

    public RemoteFile getMostRecentTraceFile() throws Exception {
        List<String> files = listDirectoryContents(logsRoot, DEFAULT_TRACE_FILE_PREFIX);

        if (files == null || files.isEmpty()) {
            return null;
        }

        RemoteFile rf = null;
        long maxLastModified = 0;
        for (int i = 0; i < files.size(); i++) {
            final RemoteFile f = getTraceFile(files.get(i));
            if (f.lastModified() > maxLastModified) {
                maxLastModified = f.lastModified();
                rf = f;
            }
        }

        return rf;
    }

    public ArrayList<String> listFFDCFiles(String client) throws Exception {
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(installRoot + "/usr/clients/" + client + "/logs/ffdc"), "ffdc");
    }

    public ArrayList<String> listFFDCSummaryFiles(String client) throws Exception {
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(installRoot + "/usr/clients/" + client + "/logs/ffdc"), "exception_summary");
    }

    /* not called */public int getOsgiConsolePort() {
        return osgiConsolePort;
    }

    /**
     * @return the iiopDefaultPort
     */
    /* not called */public int getIiopDefaultPort() {
        return iiopDefaultPort;
    }

    /**
     * @param iiopDefaultPort
     *                            the iiopDefaultPort to set
     */
    /* not called */public void setIiopDefaultPort(int iiopDefaultPort) {
        this.iiopDefaultPort = iiopDefaultPort;
    }

    public int getPort(PortType port) throws Exception {
        Log.entering(c, "getPort", port);
        int ret = 0;
        switch (port) {
            case OSGi:
                ret = getOsgiConsolePort();
                break;
            case IIOP:
                ret = getIiopDefaultPort();
            case JMX_REST:
                // ret = getPort("com.ibm.ws.admin.core.endpoint.JMXREST_defaultEP",
                // 8880);
                break;
            default:
                Exception e = new IllegalArgumentException("The specified PortType is not supported for Liberty: " + port);
                Log.error(c, "getPort", e);
                throw e;
        }
        Log.exiting(c, "getPort", ret);
        return ret;
    }

    public void setClientStartTimeout(long timeout) {
        clientStartTimeout = timeout;
    }

    /* not called */public long getClientStartTimeout() {
        return clientStartTimeout;
    }

    public String getBootstrapKey() {
        return clientTopologyID;
    }

    /**
     * Method used to autoinstall apps in
     * publish/clients/<clientName>/dropins folder found in the FAT project
     * Under the covers this will not use the install functionality found in the
     * ApplicationManager but use the Application Csar which is part of Liberty.
     *
     * @param  appName   The name of the application
     * @throws Exception
     */
    protected void autoInstallApp(String appName) throws Exception {
        Log.info(c, "InstallApp", "Adding app " + appName + " to startup verification list");
        this.addInstalledAppForValidation(appName);
    }

    /**
     * Shortcut for new FATTests to install apps assuming the app is
     * located in the publish/clients/<clientName>/apps folder of the FAT project
     *
     * @param  appName   The name of the application
     * @throws Exception
     */
    public void installApp(String appName) throws Exception {
        Log.info(c, "InstallApp", "Installing from: " + pathToAutoFVTNamedClient + "apps/" + appName);
        finalInstallApp(pathToAutoFVTNamedClient + "apps/" + appName);
    }

    /**
     * Shortcut for new FATTests to install apps located anywhere on the file system
     *
     * @param  path      The absolute path to the application
     * @param  appName   The name of the application
     * @throws Exception
     */
    public void installApp(String path, String appName) throws Exception {
        Log.info(c, "InstallApp", "Installing from: " + path + "/" + appName);
        finalInstallApp(path + "/" + appName);
    }

    /**
     * Given a sample name that corresponds to a sample in a jar named <sample-name>.jar in
     * the FAT files directory, runs the self extractor with the --downloadDependencies and
     * --acceptLicense flag in order to create a working copy of the sample client for test.
     *
     * @param  sample
     * @return
     * @throws Exception
     */
    public ProgramOutput installSampleWithExternalDependencies(String sample) throws Exception {
        Log.info(c, "installSampleWithExternalDependencies", "Installing sample '" + sample);

        List<String> pathsToDelete = new ArrayList<String>();

        String sampleJarFileName = sample + ".jar";
        String sampleJarFilePath = installRoot + "/" + sampleJarFileName;
        String javaFilePath = machineJava + "/bin/java";

        LibertyFileManager.copyFileIntoLiberty(machine, installRoot, "lib/LibertyFATTestFiles/" + sampleJarFileName);
        pathsToDelete.add(sampleJarFilePath);

        // Always have the accept license and download dependencies headers as we do not run the
        // command in an interactive way. This is ok from a testing point of view as the interactive
        // code is re-used from the self extracting JAR file where it is already being well tested
        String[] args = new String[] { "-jar", sampleJarFilePath, "--downloadDependencies", "--acceptLicense", installRoot };

        Log.info(c, "installSampleWithExternalDependencies", "Using args " + Arrays.toString(args));
        ProgramOutput po = machine.execute(javaFilePath, args);

        if (po.getReturnCode() == 0) {
            Log.info(c, "installSampleWithExternalDependencies", "Successfully installed sample: " + sample);
        } else {
            Log.warning(c, "Sample install process failed with return code " + po.getReturnCode());
            Log.warning(c, "Sample install process failed with error " + po.getStderr());
            Log.warning(c, "Sample install process failed with output " + po.getStdout());
            throw new Exception("Could not install sample client - return code " + po.getReturnCode());
        }

        //Move the test client xml into sample.xml
        RemoteFile sampleClientFile = LibertyFileManager.createRemoteFile(machine, getClientRoot() + "/sample.xml");
        LibertyFileManager.moveLibertyFile(getClientConfigurationFile(), sampleClientFile);
        //And upload the FAT client XML that will include sample.xml
        LibertyFileManager.copyFileIntoLiberty(machine, getClientRoot(), "client.xml", "productSampleClient.xml");

        //Move the test client bootstrap.properties into sample.properties if it exists
        RemoteFile clientBootStrapProps = machine.getFile(getClientRoot() + "/bootstrap.properties");
        if (clientBootStrapProps.exists()) {
            //This is optional
            RemoteFile samplePropertiesFile = LibertyFileManager.createRemoteFile(machine, getClientRoot() + "/sample.properties");
            LibertyFileManager.moveLibertyFile(clientBootStrapProps, samplePropertiesFile);
            //And upload the FAT client properties that will include the sample properties
            LibertyFileManager.copyFileIntoLiberty(machine, getClientRoot(), "bootstrap.properties", "productSample.properties");
        } else {
            //Just take the testports, no include of existing config needed
            LibertyFileManager.copyFileIntoLiberty(machine, getClientRoot(), "bootstrap.properties", "productSample_noBootstrap.properties");
        }

        for (String pathName : pathsToDelete) {
            LibertyFileManager.deleteLibertyFile(machine, pathName);
        }

        return po;
    }

    /**
     * Method used by exposed installApp methods that calls into the ApplicationManager
     * to actually install the required application
     *
     * @param  appPath   Absolute path to application (includes app name)
     * @throws Exception
     */
    protected void finalInstallApp(String appPath) throws Exception {
        String onlyAppName = appPath; //for getting only the name if appName given is actually a path i.e. autoinstall/acme.zip
        if (appPath.contains("/")) {
            String[] s = appPath.split("/");
            onlyAppName = s[s.length - 1]; //get the last one as this will be the filename only
        } else if (appPath.contains("\\\\")) {
            String[] s = appPath.split("\\\\");
            onlyAppName = s[s.length - 1]; //get the last one as this will be the filename only
        }

        if (onlyAppName.endsWith(".xml")) {
            onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 4);
        }
        if (onlyAppName.endsWith(".ear") || onlyAppName.endsWith(".eba") || onlyAppName.endsWith(".war") ||
            onlyAppName.endsWith(".jar") || onlyAppName.endsWith(".rar") || onlyAppName.endsWith(".zip")) {
            onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 4);
        }
        Log.finer(c, "InstallApp", "Application name is: " + onlyAppName);

    }

    public String getHostname() {
        return machine.getHostname();
    }

    protected String getJvmOptionsFilePath() {
        return this.getClientRoot() + "/" + JVM_OPTIONS_FILE_NAME;
    }

    protected RemoteFile getJvmOptionsFile() throws Exception {
        return LibertyFileManager.createRemoteFile(this.machine, this.getJvmOptionsFilePath());
    }

    /**
     * Reads the current jvm.options file into memory and returns the result.
     * Lines with a '=' in the middle are treated as key-value mappings,
     * and lines without a '=' character are treated as a key with an empty value.
     *
     * @return           key/value pairs from the jvm.options file
     * @throws Exception if the file can't be read
     */
    public Map<String, String> getJvmOptionsAsMap() throws Exception {
        Map<String, String> result = new LinkedHashMap<String, String>();
        List<String> options = this.getJvmOptions();
        for (String option : options) {
            int equals = option.indexOf('=');
            // if '=' is not the first or last character
            if (equals > 0 && equals < (option.length() - 1)) {
                String key = option.substring(0, equals);
                String value = option.substring(equals + 1);
                result.put(key, value);
            } else {
                result.put(option, "");
            }
        }
        return result;
    }

    /**
     * Reads the current jvm.options file into memory and returns the result.
     *
     * @return           key/value pairs from the jvm.options file
     * @throws Exception if the file can't be read
     */
    protected List<String> getJvmOptions() throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        RemoteFile file = this.getJvmOptionsFile();
        if (file == null || !file.exists()) {
            return result;
        }
        LOG.info("Reading " + JVM_OPTIONS_FILE_NAME + " file: " + file);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(file.openForReading(), "UTF-8"));
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                result.add(line);
            }
        } catch (Exception e) {
            throw new IOException("Failed to read JVM options file: " + file, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    LOG.info("Failed to close InputStream for " + file + ".  Exception: " + e.getMessage());
                }
            }
        }
        LOG.info(JVM_OPTIONS_FILE_NAME + ": " + result); // tell the user which properties you're reading
        return result;
    }

    /**
     * <p>Writes a mapping of options to the jvm.options file.
     * Note that all existing mappings will be overwritten.
     * If this is not the desired behavior, call {@link #getJvmOptions()} to retrieve the existing mappings first,
     * and then pass the updated map into this method.</p>
     * <ul>
     * <li>Null or empty keys will be ignored.</li>
     * <li>Non-empty keys mapped to a null or empty value will be added to jvm.options without a value (no '=' suffix).</li>
     * <li>Non-empty keys mapped to a non-empty value will be added to jvm.options in the format: <code>key=value</code>.</li>
     * </ul>
     *
     * @param  options   key/value pairs to set in the jvm.options file
     * @throws Exception if the jvm.options file can't be written to. Note that this exception may indicate that the file is no longer formatted correctly.
     * @see              #getJvmOptions()
     */
    public void setJvmOptions(Map<String, String> options) throws Exception {
        ArrayList<String> optionList = new ArrayList<String>();
        if (options != null) {
            for (Map.Entry<String, String> entry : options.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                key = key.trim();
                if (key.isEmpty()) {
                    continue;
                }
                StringBuilder option = new StringBuilder(key);
                String value = entry.getValue();
                if (value != null) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        option.append("=");
                        option.append(value);
                    }
                }
                optionList.add(option.toString());
            }
        }

        this.setJvmOptions(optionList);
    }

    public void setJvmOptions(List<String> options) throws Exception {
        // Step 1: Write options to local temporary file
        File tmpFile = File.createTempFile("client.jvm", "options");
        LOG.info("Writing temporary " + JVM_OPTIONS_FILE_NAME + " file: " + tmpFile);
        LOG.info(JVM_OPTIONS_FILE_NAME + ": " + options); // tell the user which properties you're setting
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
            out.write("#Updated by " + this.getClass().getName() + " on " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
            out.newLine();
            if (options != null) {
                for (String option : options) {
                    if (option == null) {
                        continue;
                    }
                    String line = option.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    out.write(line);
                    out.newLine();
                }
            }
        } catch (Exception e) {
            tmpFile.delete();
            throw new IOException("Failed to write JVM options to local temporary file " + tmpFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    LOG.info("Failed to close OutputStream for " + tmpFile + ".  Exception: " + e.getMessage());
                }
            }
        }

        // Step 3: Copy temporary file to remote machine, and delete temporary file
        RemoteFile remoteFile = null;
        try {
            remoteFile = this.getJvmOptionsFile(); // won't return null
            LibertyFileManager.copyFileIntoLiberty(machine, remoteFile.getParent(), remoteFile.getName(), tmpFile.getAbsolutePath(), false);
        } catch (Exception e) {
            throw new IOException("Failed to write JVM options to " + remoteFile, e);
        } finally {
            tmpFile.delete();
        }
    }

    public void deleteDropinDefaultConfiguration(String fileName) throws Exception {
        deleteDropinConfiguration(fileName, true);
    }

    public void deleteDropinOverrideConfiguration(String fileName) throws Exception {
        deleteDropinConfiguration(fileName, false);
    }

    private void deleteDropinConfiguration(String fileName, boolean isDefault) throws Exception {
        String location = getClientRoot() + "/configDropins/defaults/" + fileName;
        if (!isDefault)
            location = getClientRoot() + "/configDropins/overrides/" + fileName;

        LibertyFileManager.deleteLibertyFile(machine, location);
    }

    public void deleteAllDropinConfigurations() throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getClientRoot() + "/configDropins");
    }

    public void addDropinDefaultConfiguration(String fileName) throws Exception {
        addDropinConfiguration(fileName, true);
    }

    public void addDropinOverrideConfiguration(String fileName) throws Exception {
        addDropinConfiguration(fileName, false);
    }

    private void addDropinConfiguration(String fileName, boolean isDefault) throws Exception {
        String location = getClientRoot() + "/configDropins";
        if (isDefault)
            location += "/defaults";
        else
            location += "/overrides";

        waitIfNeeded();
        LibertyFileManager.moveFileIntoLiberty(machine, location, new File(fileName).getName(), pathToAutoFVTTestFiles + "/" + fileName);

        lastConfigUpdate = System.currentTimeMillis();
    }

    private void waitIfNeeded() throws Exception {
        String osName = System.getProperty("os.name");
        boolean isUnix = !(osName.startsWith("win") || osName.startsWith("Win"));
        boolean lastConfigLessThanOneSecAgo = (System.currentTimeMillis() - lastConfigUpdate) < 1000;

        Log.finer(c, "replaceClientConfiguration", "isUnix=" + isUnix + " lastConfigLessThanOneSecAgo=" + lastConfigLessThanOneSecAgo);
        if (lastConfigLessThanOneSecAgo && isUnix) {
            // Due to a java limitation on Unix, we need to wait at least
            // 1 second between config updates so the client can see it.
            // See https://www-01.ibm.com/support/docview.wss?uid=swg21446506
            // Note that the above page says that it affects versions up to 1.6, but if you look at the sun bug it is not fixed until java 8.
            Log.finer(c, "replaceClientConfiguration", "Sleeping for 1 second to work around Unix / JDK limitation fixed in Java 8");
            Thread.sleep(1000);
        }
    }

    /**
     * Replaces the client configuration. This encapsulates the necessary logic
     * to deal with system / JDK idiosyncrasies.
     *
     * @param  fileName
     * @throws Exception
     */
    protected void replaceClientConfiguration(String fileName) throws Exception {
        waitIfNeeded();

        LibertyFileManager.moveFileIntoLiberty(machine, getClientRoot(), "client.xml", fileName);
        lastConfigUpdate = System.currentTimeMillis();
    }

    /**
     * This will put the named file into the root directory of the client and name it client.xml. As the file name is changed if you want to copy files for use in an include
     * statement or if the location of the config file is being changed using the was.configroot.uri property or --config-root command line then you should use the
     * {@link #copyFileToLibertyInstallRoot(String)} method.
     *
     * @param  fileName  The name of the file from the FVT test suite
     * @throws Exception
     */
    public void setClientConfigurationFile(String fileName) throws Exception {
        replaceClientConfiguration(pathToAutoFVTTestFiles + "/" + fileName);
        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
    }

    /**
     * This will save the current client configuration, so that it can be restored later on via the
     * restoreClientConfiguration method.
     *
     * @throws Exception
     */
    public void saveClientConfiguration() throws Exception {
        try {
            savedClientXml = machine.getFile(clientRoot + "/savedClientXml" + System.currentTimeMillis() + ".xml");
            getClientConfigurationFile().copyToDest(savedClientXml);
        } catch (Exception e) {
            savedClientXml = null;
            throw e;
        }
    }

    /**
     * This will restore the client configuration that was saved by a prior call to the
     * saveClientConfiguration method.
     *
     * @throws Exception
     */
    public void restoreClientConfiguration() throws Exception {
        if (savedClientXml == null) {
            throw new RuntimeException("The client configuration cannot be restored because it was never saved via the saveClientConfiguration method.");
        }
        getClientConfigurationFile().copyFromSource(savedClientXml);
    }

    public String getClientConfigurationPath() {
        return this.getClientRoot() + "/" + CLIENT_CONFIG_FILE_NAME;
    }

    public RemoteFile getClientConfigurationFile() throws Exception {
        return LibertyFileManager.getLibertyFile(machine, getClientConfigurationPath());
    }

    /**
     * This will load the {@link ClientConfiguration} from the default config file returned from {@link #getClientConfigurationFile()}.
     *
     * @return           The loaded {@link ClientConfiguration}
     * @throws Exception
     */
    public ClientConfiguration getClientConfiguration() throws Exception {
        RemoteFile file = getClientConfigurationFile();
        return getClientConfiguration(file);
    }

    /**
     * This gets the {@link ClientConfiguration} for the supplied XML file.
     *
     * @param  file      The file to load the client configuration from
     * @return           The loaded {@link ClientConfiguration}
     * @throws Exception
     */
    public ClientConfiguration getClientConfiguration(RemoteFile file) throws Exception {
        return ClientConfigurationFactory.getInstance().unmarshal(file.openForReading());
    }

    public void updateClientConfiguration(File clientConfig) throws Exception {
        replaceClientConfiguration(clientConfig.getAbsolutePath());

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Client configuration updated from: " + clientConfig);
            logClientConfiguration(Level.INFO, false);
        }
    }

    /**
     * This updates the supplied file with the supplied config.
     *
     * @param  clientConfig The config to store to the file
     * @param  file         The file to store the config to
     * @throws Exception
     */
    public void updateClientConfiguration(ClientConfiguration clientConfig, RemoteFile file) throws Exception {
        // write contents to a temporary file
        RemoteFile newClientFile = LibertyFileManager.createRemoteFile(machine, getClientConfigurationPath() + ".tmp");
        OutputStream os = newClientFile.openForWriting(false);
        ClientConfigurationFactory.getInstance().marshal(clientConfig, os);

        if (newClientFile.length() == file.length()) {
            clientConfig.setDescription(clientConfig.getDescription() + " (this is some random text to make the file size bigger)");
            os = newClientFile.openForWriting(false);
            ClientConfigurationFactory.getInstance().marshal(clientConfig, os);
        }

        // replace the file
        // This logic does not need to be time protected (as we do in method
        // replaceClientConfiguration) because of the "extra random text" logic
        // above. Even if the timestamp would not be changed, the size out be.
        LibertyFileManager.moveLibertyFile(newClientFile, file);

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Client configuration updated:");
            logClientConfiguration(Level.INFO, false);
        }
    }

    /**
     * This stores the supplied content to the default client XML file returned from {@link #getClientConfigurationFile()}.
     *
     * @param  clientConfig The configuration to store
     * @throws Exception
     */
    public void updateClientConfiguration(ClientConfiguration clientConfig) throws Exception {
        updateClientConfiguration(clientConfig, getClientConfigurationFile());
    }

    /**
     * Logs the contents of a File. If <code>singleLine=true</code>, then the
     * whole File will be logged as one single message. Otherwise, each
     * individual line in the file will be logged separately. Some logging
     * formatters look prettier with single line log messages; others look
     * better with multiple-line log messages.
     *
     * @param file
     *                       the file whose contents you want to log.
     * @param singleLine
     *                       true to log the whole file in one message, false to log each
     *                       individual line
     */
    protected void logClientConfiguration(Level level, boolean singleLine) {
        String method = "logClientConfiguration";
        BufferedReader reader = null;
        try {
            StringWriter stringWriter = null;
            PrintWriter printWriter = null;
            reader = new BufferedReader(new InputStreamReader(this.getClientConfigurationFile().openForReading()));
            if (singleLine) {
                stringWriter = new StringWriter();
                printWriter = new PrintWriter(stringWriter);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (singleLine) {
                    printWriter.println(line);
                } else {
                    LOG.logp(level, CLASS_NAME, method, line);
                }
            }
            if (singleLine) {
                LOG.logp(level, CLASS_NAME, method, stringWriter.toString());
            }
        } catch (Exception e) {
            LOG.logp(level, CLASS_NAME, method, "Failed to read " + this.getClientConfigurationPath() + ".  Exception: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.logp(level, CLASS_NAME, method, "Failed to close reader for " + this.getClientConfigurationPath() + ".  Exception: " + e.getMessage());
                }
            }
        }
    }

    public RemoteFile getConsoleLogFile() throws Exception {
        // Find the currently configured/in-use console log file.
        final RemoteFile remoteFile;
        if (machineOS == OperatingSystem.ZOS) {
            remoteFile = machine.getFile(consoleAbsPath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = machine.getFile(consoleAbsPath);
        }
        return remoteFile;
    }

    public RemoteFile getDefaultLogFile() throws Exception {
        //Set path to client log assuming the default setting.
        // ALWAYS RETURN messages.log -- tests assume they can look for INFO+ messages.
        RemoteFile file = LibertyFileManager.getLibertyFile(machine, messageAbsPath);
        if (file == null) {
            throw new IllegalStateException("Unable to find default log file, path=" + messageAbsPath);
        }
        return file;
    }

    protected String getDefaultLogPath() {
        try {
            RemoteFile file = getDefaultLogFile();
            return file.getAbsolutePath();
        } catch (Exception ex) {
            return "DefaultLogFile";
        }
    }

    protected RemoteFile getTraceFile(String fileName) throws Exception {
        String fileAbsPath = logsRoot + fileName;
        return LibertyFileManager.getLibertyFile(machine, fileAbsPath);
    }

    public RemoteFile getFFDCLogFile(String ffdcName) throws Exception {
        return LibertyFileManager.getLibertyFile(machine, (logsRoot + "ffdc/" + ffdcName));
    }

    public RemoteFile getFFDCSummaryFile(String ffdcSummary) throws Exception {
        return LibertyFileManager.getLibertyFile(machine, (logsRoot + "ffdc/" + ffdcSummary));
    }

    /**
     * Search for a file matching the given regex in the logs location.
     * This is just a utility, primarily for pulling out a trace file without
     * worrying about the timestamp, it isn't very clever and just returns
     * the first match it finds.
     *
     * @param  regex
     * @return       a matching RemoteFile, or null if no match was found
     */
    public RemoteFile getMatchingLogFile(String regex) throws Exception {
        Log.info(c, "getMatchingLogFile", "Looking for file matching regex: " + regex + " in " + logsRoot + " on " + machine);
        RemoteFile logsDir = LibertyFileManager.getLibertyFile(machine, logsRoot);
        RemoteFile[] logFiles = logsDir.list(false);
        for (RemoteFile r : logFiles) {
            if (r.isFile()) {
                if (Pattern.matches(regex, r.getName())) {
                    Log.info(c, "getMatchingLogFile", "Matched log file: " + r.getName());
                    return r;
                }
            }
        }
        Log.info(c, "getMatchingLogFile", "No matching log file found.");
        return null;
    }

    /**
     * This method will search the given file on this client for the specified expression.
     * The path given is relative to the install root directory.
     *
     * @param  regexp    pattern to search for.
     * @param  filePath  the pathname relative to the install root directory.
     * @return           A list of the lines in the file that contains the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    /* not called */public List<String> findStringsInFileInLibertyInstallRoot(String regexp, String filePath) throws Exception {
        RemoteFile remoteFile = LibertyFileManager.getLibertyFile(machine, (installRoot + "/" + filePath));
        List<String> strings = LibertyFileManager.findStringsInFile(regexp, remoteFile);
        return strings;
    }

    /**
     * This method will search the given file on this client for the specified expression.
     * The path given is relative to the client root directory.
     *
     * @param  regexp    pattern to search for.
     * @param  filePath  the pathname relative to the client root directory.
     * @return           A list of the lines in the file that contains the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInFileInLibertyClientRoot(String regexp, String filePath) throws Exception {

        /*
         * On z/os, the console log will be produced in EBCDIC. We know other logs are ASCII, and
         * so we can comfortably special case the console.
         */
        final RemoteFile remoteFile;
        String absolutePath = clientRoot + "/" + filePath;
        if (machineOS == OperatingSystem.ZOS && absolutePath.equalsIgnoreCase(consoleAbsPath)) {
            remoteFile = machine.getFile(absolutePath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = LibertyFileManager.getLibertyFile(machine, absolutePath);
        }
        List<String> strings = LibertyFileManager.findStringsInFile(regexp, remoteFile);
        return strings;
    }

    /**
     * This method will search the output and trace files for this client
     * for the specified expression. The default trace prefix is assumed.
     *
     * @param  regexp    pattern to search for
     * @return           A list of the lines in the trace files which contain the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogs(String regexp) throws Exception {
        return findStringsInLogs(regexp, getDefaultLogFile());
    }

    public List<String> findStringsInCopiedLogs(String regexp) throws Exception {
        String logFile = pathToAutoFVTOutputClientsFolder + "/" + clientToUse + "-" + logStamp + "/logs/messages.log";
        RemoteFile remoteLogFile = machine.getFile(logFile);
        return findStringsInLogs(regexp, remoteLogFile);
    }

    /**
     * This method will search the output and trace files for this client
     * for the specified expression. The default trace prefix is assumed.
     *
     * @param  regexp    pattern to search for
     * @return           A list of the lines in the trace files which contain the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogs(String regexp, RemoteFile logFile) throws Exception {
        List<String> matches = new ArrayList<String>();

        List<String> lines = LibertyFileManager.findStringsInFile(regexp, logFile);
        if (!lines.isEmpty()) {
            matches.addAll(lines);
        }
        return matches;
    }

    /**
     * This method will search the output and trace files for this client
     * for the specified expression. The default trace prefix is assumed.
     *
     * @param  regexp    pattern to search for
     * @return           A list of the lines in the trace files which contain the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogsAndTrace(String regexp) throws Exception {
        return findStringsInLogsAndTrace(regexp, DEFAULT_TRACE_FILE_PREFIX);
    }

    /**
     * This method will search the output and trace files for this client
     * for the specified expression.
     *
     * @param  regexp              pattern to search for
     * @param  traceFileNamePrefix trace file prefix if the trace file name is not default
     * @return                     A list of the lines in the trace files which contain the matching
     *                             pattern. No match results in an empty list.
     * @throws Exception
     */
    /* not called */public List<String> findStringsInLogsAndTrace(String regexp, String traceFileNamePrefix) throws Exception {
        List<String> matches = new ArrayList<String>();

        List<String> lines = LibertyFileManager.findStringsInFile(regexp, getDefaultLogFile());
        if (!lines.isEmpty()) {
            matches.addAll(lines);
        }

        List<String> traceLogsBaseNames = listDirectoryContents(logsRoot, traceFileNamePrefix);
        for (String logBaseName : traceLogsBaseNames) {
            RemoteFile logFile = getTraceFile(logBaseName);
            lines = LibertyFileManager.findStringsInFile(regexp, logFile);
            if (!lines.isEmpty()) {
                matches.addAll(lines);
            }
        }
        return matches;
    }

    /**
     * This method will search the trace files for this client
     * for the specified expression.
     *
     * @param  regexp              pattern to search for
     * @param  traceFileNamePrefix trace file prefix if the trace file name is not default
     * @return                     A list of the lines in the trace files which contain the matching
     *                             pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInTrace(String regexp) throws Exception {
        List<String> matches = new ArrayList<String>();

        List<String> traceLogsBaseNames = listDirectoryContents(logsRoot, DEFAULT_TRACE_FILE_PREFIX);

        System.out.println("traceLogsBaseNames: " + traceLogsBaseNames == null ? "null" : traceLogsBaseNames.size());
        for (String logBaseName : traceLogsBaseNames) {
            RemoteFile logFile = getTraceFile(logBaseName);
            System.out.println("Looking in " + logBaseName);
            List<String> lines = LibertyFileManager.findStringsInFile(regexp, logFile);
            if (!lines.isEmpty()) {
                matches.addAll(lines);
            }
        }
        return matches;
    }

    /**
     * This method will search for the provided expression in the log and trace files
     * on an incremental basis using the default trace prefix. It starts with reading
     * the file at the offset where the last mark was set (or the beginning of the file
     * if no mark has been set) and reads until the end of the file.
     *
     * @param  regexp    pattern to search for
     * @return           A list of the lines in the trace files which contain the matching
     *                   pattern. No matches result in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogsAndTraceUsingMark(String regexp) throws Exception {
        return findStringsInLogsAndTraceUsingMark(regexp, DEFAULT_TRACE_FILE_PREFIX);
    }

    /**
     * This method will search for the provided expression in the log and trace files
     * on an incremental basis using a custom trace prefix. It starts with reading the
     * file at the offset where the last mark was set (or the beginning of the file
     * if no mark has been set) and reads until the end of the file.
     *
     * @param  regexp              pattern to search for
     * @param  traceFileNamePrefix trace file prefix if the trace file name is not default
     * @return                     A list of the lines in the trace files which contain the matching
     *                             pattern. No matches result in an empty list.
     * @throws Exception
     */
    protected List<String> findStringsInLogsAndTraceUsingMark(String regexp, String traceFileNamePrefix) throws Exception {

        List<String> matches = new ArrayList<String>();
        LogSearchResult newOffsetAndMatches;

        Long offset = getMarkOffset(getDefaultLogPath());
        newOffsetAndMatches = LibertyFileManager.findStringsInFile(regexp, getDefaultLogFile(), offset);
        matches.addAll(newOffsetAndMatches.getMatches()); // get the list of matches found

        List<String> traceLogBaseNames = listDirectoryContents(logsRoot, traceFileNamePrefix);
        for (String name : traceLogBaseNames) {

            offset = getMarkOffset(logsRoot + name);
            newOffsetAndMatches = LibertyFileManager.findStringsInFile(regexp, getTraceFile(name), offset);

            matches.addAll(newOffsetAndMatches.getMatches()); // get the list of matches found
        }

        return matches;
    }

    /**
     * This method will search for the provided expressions in the log and trace files
     * on an incremental basis using the default trace prefix. It starts with reading the file
     * at the offset where the last mark was set (or the beginning of the file
     * if no mark has been set) and reads until the end of the file.
     *
     * @param  regexpList a list of expressions to search for
     * @return            a <code>List&#60String&#62</code> containing the matches
     * @throws Exception
     */
    public List<String> findStringsInLogsAndTraceUsingMarkMultiRegexp(List<String> regexpList) throws Exception {
        return findStringsInLogsAndTraceUsingMarkMultiRegexp(regexpList, DEFAULT_TRACE_FILE_PREFIX);
    }

    /**
     * This method will search for the provided expressions in the log and trace files
     * on an incremental basis using a custom trace prefix. It starts with reading the file
     * at the offset where the last mark was set (or the beginning of the file
     * if no mark has been set) and reads until the end of the file.
     *
     * @param  regexpList          a list of expressions to search for
     * @param  traceFileNamePrefix trace file prefix if the trace file name is not default
     * @return                     a <code>List&#60String&#62</code> contains the matches
     * @throws Exception
     */
    protected List<String> findStringsInLogsAndTraceUsingMarkMultiRegexp(List<String> regexpList, String traceFileNamePrefix) throws Exception {

        List<String> matches = new ArrayList<String>();
        LogSearchResult newOffsetAndMatches;

        Long offset = getMarkOffset(getDefaultLogPath());
        newOffsetAndMatches = LibertyFileManager.findStringsInFile(regexpList, getDefaultLogFile(), offset);
        matches.addAll(newOffsetAndMatches.getMatches()); // get the list of matches found

        List<String> traceLogBaseNames = listDirectoryContents(logsRoot, traceFileNamePrefix);
        for (String name : traceLogBaseNames) {

            offset = getMarkOffset(logsRoot + name);
            newOffsetAndMatches = LibertyFileManager.findStringsInFile(regexpList, getTraceFile(name), offset);

            matches.addAll(newOffsetAndMatches.getMatches()); // get the list of matches found
        }

        return matches;

    }

    /**
     * Reset offset values for logs back to the start of the file.
     * <p>
     * Note: This method doesn't set the offset values to the beginning of the file per se,
     * rather this method sets the list of logs and their offset values to null. When one
     * of the findStringsInLogsAndTrace...(...) methods are called, it will recreate the
     * list of logs and set each offset value to 0L - the start of the file.
     */
    public void resetLogOffsets() {
        logOffsets.clear();
        logMarks.clear();
        Log.info(LibertyClient.class, "resetLogOffsets", "cleared log and mark offsets");
    }

    /**
     * Set the mark offset to the end of the log file.
     *
     * @param log files to mark. If none are specified, the default log file is marked.
     */
    public void setMarkToEndOfLog(RemoteFile... logFiles) throws Exception {
        if (logFiles == null || logFiles.length == 0)
            logFiles = new RemoteFile[] { getDefaultLogFile() };

        for (RemoteFile logFile : logFiles) {
            String path = logFile.getAbsolutePath();

            long offset;
            BufferedInputStream input = new BufferedInputStream(logFile.openForReading());
            try {
                int available = input.available();
                offset = input.skip(available);
                while (input.read() != -1) {
                    offset++;
                }
            } finally {
                input.close();
            }

            Long oldMarkOffset = logMarks.put(path, offset);
            Log.info(LibertyClient.class, "setMarkToEndOfLog", path + ", old mark offset=" + oldMarkOffset + ", new mark offset=" + offset);
        }
    }

    /**
     * Get the mark offset for the specified log file.
     */
    protected Long getMarkOffset(String logFile) {
        if (!logMarks.containsKey(logFile)) {
            logMarks.put(logFile, 0L);
        }
        return logMarks.get(logFile);
    }

    /**
     * Get the offset into a log or trace file of the last message inspected.
     *
     * If the file name does not exist in the offsets, then create an entry for it and
     * set the offset for that file to '0'.
     *
     * @param  String value of the file name
     * @return        Long containing the offset into the file of the last message inspected
     */
    protected Long getLogOffset(String logFile) {
        if (!logOffsets.containsKey(logFile)) {
            logOffsets.put(logFile, 0L);
        }
        return logOffsets.get(logFile);
    }

    /**
     * Update the log offset for the specified log file to the offset provided.
     */
    public void updateLogOffset(String logFile, Long newLogOffset) {
        logOffsets.put(logFile, newLogOffset);
    }

    /**
     * Returns a subset of the supplied application names that appear to be installed based on the presence of messages in messages.log.
     *
     * @param  possiblyInstalledAppNames list of application names to check if installed. The names should be sufficiently unique (not substrings of other names).
     * @return                           a subset of the supplied application names that appear to be installed based on the presence of messages in messages.log.
     * @throws Exception                 if an error occurs.
     */
    public Set<String> getInstalledAppNames(String... possiblyInstalledAppNames) throws Exception {
        // Messages for installed/updated:
        // CWWKZ0001I: Application [name] started in 0.456 seconds.
        // CWWKZ0003I: The application [name] updated in 0.258 seconds.
        // J2CA7001I: Resource adapter [name] installed in 0.613 seconds.
        // J2CA7003I: The resource adapter [name] updated in 0.120 seconds.

        // Messages for uninstalled:
        // CWWKZ0009I: The application [name] has stopped successfully.
        // J2CA7009I: The resource adapter [name] has uninstalled successfully.

        Set<String> subset = new TreeSet<String>();

        if (possiblyInstalledAppNames.length > 0) {
            Map<String, Integer> counters = new HashMap<String, Integer>();
            for (String name : possiblyInstalledAppNames)
                counters.put(name, 0);

            for (String line : findStringsInFileInLibertyClientRoot(".*((CWWKZ0)|(J2CA7))00[139]I: .*", "logs/messages.log"))
                for (String name : possiblyInstalledAppNames)
                    if (line.contains(name))
                        counters.put(name, counters.get(name) + (line.contains("009I: ") ? -1 : 1));

            for (Map.Entry<String, Integer> entry : counters.entrySet())
                if (entry.getValue() > 0)
                    subset.add(entry.getKey());
        }

        return subset;
    }

    /**
     * Wait for completion of a configuration update and feature updates associated with it.
     * If feature updates are started (CWWKF0007I) before the CWWKG0017I message (config updates completed),
     * then this method waits for corresponding feature update completed messages (CWWKF0008I).
     * If a list of application names is supplied, this method waits for all of the apps to be started.
     * The offset is incremented every time this method is called.
     *
     * @param  appNames optional list of names of applications that should be started before returning from this method.
     * @param  regexps  optional list of regular expressions that indicate additional messages to wait for. The list should NOT include
     *                      the CWWKG0017I, CWWKG0018I, CWWKF0007I or CWWKF0007I messages, as those are implicitly handled by this method.
     *
     * @return          list of lines containing relevant messages.
     */
    public List<String> waitForConfigUpdateInLogUsingMark(Set<String> appNames,
                                                          String... regexps) throws Exception {
        return waitForConfigUpdateInLogUsingMark(appNames, false, regexps);
    }

    /**
     * Wait for completion of a configuration update and feature updates associated with it.
     * If feature updates are started (CWWKF0007I) before the CWWKG0017I message (config updates completed),
     * then this method waits for corresponding feature update completed messages (CWWKF0008I). If optionally
     * specified that a feature update must happen, regardless of when CWWKG0017I appears, CWWKF0008I will
     * be waited for. If a list of application names is supplied, this method waits for all of the apps to be started.
     * The offset is incremented every time this method is called.
     *
     * @param  appNames                      optional list of names of applications that should be started before returning from this method.
     * @param  waitForFeatureUpdateCompleted if true, this method will require a feature updated completed message
     *                                           before returning (if false, it will only wait for this message if a feature update is started
     *                                           before the config update is completed)
     * @param  regexps                       optional list of regular expressions that indicate additional messages to wait for. The list should NOT include
     *                                           the CWWKG0017I, CWWKG0018I, CWWKF0007I or CWWKF0007I messages, as those are implicitly handled by this method.
     *
     * @return                               list of lines containing relevant messages.
     */
    public List<String> waitForConfigUpdateInLogUsingMark(Set<String> appNames,
                                                          boolean waitForFeatureUpdateCompleted,
                                                          String... regexps) throws Exception {
        final String methodName = "waitForConfigUpdateInLogUsingMark";
        final long timeout = LOG_SEARCH_TIMEOUT;

        final List<String> matchingLines = new LinkedList<String>();
        final List<String> watchFor = new LinkedList<String>();
        if (regexps != null)
            for (String regexp : regexps)
                watchFor.add(regexp);
        watchFor.add("CWWKG001[7-8]I"); // Always wait for the config update completed or no changes detected message
        watchFor.add("CWWKF0007I:"); // Feature update started - as many times as we see this, we need to wait for a corresponding Feature update completed

        // If the calling test indicates that a feature update message is required, explicitly
        // add it. Otherwise if a feature update starts (CWWKF0007I) before the config update
        // finishes it will be added below.
        if (waitForFeatureUpdateCompleted) {
            watchFor.add("CWWKF0008I:");
        }

        if (appNames == null)
            appNames = Collections.emptySet();
        String[] appNamesArray = appNames.toArray(new String[appNames.size()]);

        RemoteFile logFile = getDefaultLogFile();

        Set<String> startedAppNames = Collections.emptySet();

        int count = 0;
        final long startTime = System.currentTimeMillis();
        try {
            long offset;
            for (offset = getMarkOffset(logFile.getAbsolutePath()); System.currentTimeMillis() - startTime < timeout
                                                                    && (!startedAppNames.containsAll(appNames)
                                                                        || watchFor.size() > 1); startedAppNames = getInstalledAppNames(appNamesArray)) {
                // Periodically print diagnostic info if waiting a long time
                long waited = System.currentTimeMillis() - startTime;
                if (++count % 10 == 0)
                    Log.info(LibertyClient.class, methodName, "waited " + waited + "ms" +
                                                              ", startedAppNames=" + startedAppNames +
                                                              ", appNames=" + appNames +
                                                              ", contains? " + startedAppNames.containsAll(appNames) +
                                                              ", watchFor=" + watchFor);

                LogSearchResult newOffsetAndMatches = LibertyFileManager.findStringsInFileCommon(watchFor, 1, logFile, offset);
                offset = newOffsetAndMatches.getOffset();
                List<String> matches = newOffsetAndMatches.getMatches();
                if (matches.isEmpty()) {
                    if (waited < count * WAIT_INCREMENT)
                        try {
                            Thread.sleep(WAIT_INCREMENT);
                        } catch (InterruptedException e) {
                            // Ignore and carry on
                        }
                } else {
                    String line = matches.get(0);
                    matchingLines.add(line);

                    // Indicates a feature updated was started.
                    if (line.contains("CWWKF0007I:")) {
                        // If we haven't already added the message id for the feature update to complete,
                        // do so now.
                        if (!waitForFeatureUpdateCompleted) {
                            watchFor.add("CWWKF0008I:"); // Feature update completed in X seconds.
                        }
                    } else {
                        // Remove the corresponding regexp from the watchFor list
                        for (Iterator<String> it = watchFor.iterator(); it.hasNext();) {
                            String regexp = it.next();
                            if (Pattern.compile(regexp).matcher(line).find()) {
                                it.remove();
                                break;
                            }
                        }
                    }
                }
            }
            updateLogOffset(logFile.getAbsolutePath(), offset);
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(LibertyClient.class, methodName, "Started waiting for CWWKG001[7-8]I and messages matching regexps "
                                                      + Arrays.asList(regexps) + " at " + formatter.format(new Date(startTime))
                                                      + " and finished at " + formatter.format(new Date(endTime))
                                                      + ". Found: " + matchingLines);
        }

        // Check if we timed out
        watchFor.remove("CWWKF0007I:");
        List<String> notStartedAppNames = new LinkedList<String>(appNames);
        notStartedAppNames.removeAll(startedAppNames);
        if (watchFor.size() > 0 || notStartedAppNames.size() > 0) {
            String message = "Timed out waiting for " + notStartedAppNames
                             + " and/or searching for " + watchFor
                             + " in log file: " + logFile.getAbsolutePath()
                             + ". Extra info: value of (watchFor.size() > 0): " + (watchFor.size() > 0)
                             + ", value of (notStartedAppNames.size() > 0): " + (notStartedAppNames.size() > 0);
            Log.warning(c, message);
            throw new RuntimeException(message);
        }

        return matchingLines;
    }

    /**
     * This method will time out after a sensible period of
     * time has elapsed.
     *
     * @param  regexp a regular expression to search for
     * @return        the matching line in the log, or null if no matches
     *                appear before the timeout expires
     */
    public String waitForStringInLog(String regexp) {
        String methodName = "waitForStringInLog()";
        if (regexp.startsWith("CWWKZ0001I")) {
            int index = 10;
            if (regexp.length() > index && regexp.charAt(index) == ':') {
                index++;
            }
            if (regexp.length() > index + 2 && regexp.charAt(index) == '.' && regexp.charAt(index + 1) == '*') {
                index += 2;
                String appName = regexp.substring(index).trim();
                if (appName.length() > 0) {
                    try {
                        validateAppLoaded(appName);
                        System.out.println(methodName + ": Application " + appName + " started");
                        return "Application " + appName + " started";
                    } catch (Exception ex) {
                        System.out.println(methodName + ": Application " + appName + " did not start");
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return waitForStringInLogUsingMark(regexp);
    }

    /**
     * This method will time out after a sensible period of
     * time has elapsed.
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @return                 the number of matches in the log, or 0 if no matches
     *                         appear before the timeout expires
     */
    public int waitForMultipleStringsInLog(int numberOfMatches, String regexp) {
        return waitForMultipleStringsInLog(numberOfMatches, regexp, LOG_SEARCH_TIMEOUT);
    }

    /**
     * Wait for the specified regex in the default logs from the last offset.
     * The offset is incremented every time this method is called.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     *
     * @param  regexp a regular expression to search for
     * @return        the matching line in the log, or null if no matches
     *                appear before the timeout expires
     */
    public String waitForStringInLogUsingLastOffset(String regexp) {
        return waitForStringInLogUsingLastOffset(regexp, LOG_SEARCH_TIMEOUT);
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     *
     * @param  regexp a regular expression to search for
     * @return        the matching line in the log, or null if no matches
     *                appear before the timeout expires
     */
    public String waitForStringInLogUsingMark(String regexp) {
        return waitForStringInLogUsingMark(regexp, LOG_SEARCH_TIMEOUT);
    }

    /**
     * Unless there's a strong functional requirement that
     * your string appear super-quickly, or you know your string
     * might take a ridiculously long time (like five minutes),
     * consider using the method which takes a default timeout, {@link }
     *
     * @param  regexp
     * @param  timeout a timeout, in milliseconds
     * @return
     */
    public String waitForStringInLog(String regexp, long timeout) {
        return waitForStringInLogUsingMark(regexp, timeout);
    }

    /**
     * Unless there's a strong functional requirement that
     * your string appear super-quickly, or you know your string
     * might take a ridiculously long time (like five minutes),
     * consider using the method which takes a default timeout.
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @param  timeout         a timeout, in milliseconds
     * @return
     */
    public int waitForMultipleStringsInLog(int numberOfMatches, String regexp, long timeout) {
        try {
            return waitForMultipleStringsInLog(numberOfMatches, regexp, timeout, getDefaultLogFile());
        } catch (Exception e) {
            Log.warning(c, "Could not get default log file: " + e);
            return 0;
        }
    }

    /**
     * Wait for the specified regex in the default logs from the last offset.
     * The offset is incremented every time this method is called.
     * <p>
     * Unless there's a strong functional requirement that
     * your string appear super-quickly, or you know your string
     * might take a ridiculously long time (like five minutes),
     * consider using the method which takes a default timeout, {@link }
     *
     * @param  regexp
     * @param  timeout a timeout, in milliseconds
     * @return
     */
    public String waitForStringInLogUsingLastOffset(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingLastOffset(regexp, timeout, getDefaultLogFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in default log file due to exception " + e);
            return null;
        }
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * Unless there's a strong functional requirement that
     * your string appear super-quickly, or you know your string
     * might take a ridiculously long time (like five minutes),
     * consider using the method which takes a default timeout, {@link }
     *
     * @param  regexp
     * @param  timeout a timeout, in milliseconds
     * @return
     */
    public String waitForStringInLogUsingMark(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingMark(regexp, timeout, getDefaultLogFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in default log file due to exception " + e);
            return null;
        }
    }

    /**
     * @param  regexp
     * @param  ClientConfigurationFile
     * @return
     */
    public String waitForStringInCopiedLog(String regexp) {
        return waitForStringInCopiedLog(regexp, LOG_SEARCH_TIMEOUT);

    }

    public String waitForStringInCopiedLog(String regexp, long timeout) {
        String logFile = pathToAutoFVTOutputClientsFolder + "/" + clientToUse + "-" + logStamp + "/logs/messages.log";
        RemoteFile remoteLogFile = machine.getFile(logFile);
        return waitForStringInLogUsingMark(regexp, timeout, remoteLogFile);
    }

    /**
     * @param  regexp
     * @param  ClientConfigurationFile
     * @return
     */
    public String waitForStringInLog(String regexp, RemoteFile outputFile) {
        return waitForStringInLogUsingMark(regexp, outputFile);
    }

    /**
     * Check for a number of strings in a potentially remote file
     *
     * @param  regexp     a regular expression to search for
     * @param  timeout    a timeout, in milliseconds
     * @param  outputFile file to check
     * @return            line that matched the regexp
     */
    public String waitForStringInLog(String regexp, long timeout, RemoteFile outputFile) {
        return waitForStringInLogUsingMark(regexp, timeout, outputFile);
    }

    /**
     * Check for a number of strings in a potentially remote file
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @param  timeout         a timeout, in milliseconds
     * @param  outputFile      file to check
     * @return                 number of matches found
     */
    public int waitForMultipleStringsInLog(int numberOfMatches, String regexp, long timeout, RemoteFile outputFile) {
        long startTime = System.currentTimeMillis();
        int waited = 0;
        int count = 0;
        //Ensure we always search for at least 1 occurrence
        if (numberOfMatches <= 0) {
            numberOfMatches = 1;
        }
        try {
            while (count < numberOfMatches && waited <= timeout) {

                count = LibertyFileManager.findMultipleStringsInFile(numberOfMatches, regexp, outputFile);
                try {
                    Thread.sleep(WAIT_INCREMENT);
                } catch (InterruptedException e) {
                    // Ignore and carry on
                }
                waited += WAIT_INCREMENT;
            }
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due to exception " + e.toString());
            e.printStackTrace();
            return 0;
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(LibertyClient.class, "waitForMultipleStringsInLog",
                     "Started waiting for " + numberOfMatches + " messages matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                         + " and finished at " + formatter.format(new Date(endTime)));
        }
        return count;
    }

    /**
     * Wait for the specified regex in the specified RemoteFile from the last
     * mark.
     *
     * @param  regexp
     * @param  outputFile
     * @return
     */
    public String waitForStringInLogUsingMark(String regexp, RemoteFile outputFile) {
        return waitForStringInLogUsingMark(regexp, LOG_SEARCH_TIMEOUT, outputFile);
    }

    /**
     * Wait for the specified regexp in the default logs from the last offset.
     * The offset is incremented every time this method is called.
     *
     * @param  regexp     a regular expression to search for
     * @param  timeout    a timeout, in milliseconds
     * @param  outputFile file to check
     * @return            line that matched the regexp
     */
    protected String waitForStringInLogUsingLastOffset(String regexp, long intendedTimeout, RemoteFile outputFile) {
        return waitForStringInLogUsingLastOffset(regexp, intendedTimeout, 2 * intendedTimeout, outputFile);
    }

    /**
     * Wait for the specified regexp in the default logs from the last offset.
     * The offset is incremented every time this method is called.
     *
     * @param  regexp          a regular expression to search for
     * @param  intendedTimeout a timeout, in milliseconds, within which we expect the wait to complete. Missing this is a soft fail.
     * @param  extendedTimeout a timeout, in milliseconds, within which we insist the wait complete. Missing this is an error.
     * @param  outputFile      file to check
     * @return                 line that matched the regexp
     */
    protected String waitForStringInLogUsingLastOffset(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile) {
        final String METHOD_NAME = "waitForStringInLogUsingLastOffset";
        long startTime = System.currentTimeMillis();
        int waited = 0;

        Long offset = getLogOffset(outputFile.getAbsolutePath());

        try {
            LogSearchResult newOffsetAndMatches;
            while (waited <= extendedTimeout) {
                if (waited > intendedTimeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(CLASS_NAME, METHOD_NAME, 3906, intendedTimeout, regexp);
                    intendedTimeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                offset = newOffsetAndMatches.getOffset();
                List<String> matches = newOffsetAndMatches.getMatches();
                if (matches.isEmpty()) {
                    try {
                        Thread.sleep(WAIT_INCREMENT);
                    } catch (InterruptedException e) {
                        // Ignore and carry on
                    }
                    waited += WAIT_INCREMENT;
                } else {
                    updateLogOffset(outputFile.getAbsolutePath(), offset);
                    return matches.get(0);
                }
            }
            Log.warning(c, "Timed out searching for " + regexp + " in log file: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due do exception " + e.toString());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(LibertyClient.class, "waitForStringInLogUsingLastOffset",
                     "Started waiting for message matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                               + " and finished at " + formatter.format(new Date(endTime)));
        }
        // If we didn't find the string, we still want to mark the offest so
        // we don't have to re-search the whole file on the next call.
        updateLogOffset(outputFile.getAbsolutePath(), offset);
        return null;
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark.
     * The offset is also incremented every time this method is called.
     *
     * @param  regexp     a regular expression to search for
     * @param  timeout    a timeout, in milliseconds
     * @param  outputFile file to check
     * @return            line that matched the regexp
     */
    protected String waitForStringInLogUsingMark(String regexp, long intendedTimeout, RemoteFile outputFile) {
        return waitForStringInLogUsingMark(regexp, intendedTimeout, 2 * intendedTimeout, outputFile);
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark.
     * The offset is also incremented every time this method is called.
     *
     * @param  regexp          a regular expression to search for
     * @param  intendedTimeout a timeout, in milliseconds, within which the wait should complete. Exceeding this is a soft fail.
     * @param  extendedTimeout a timeout, in milliseconds, within which the wait must complete. Exceeding this is a hard fail.
     * @param  outputFile      file to check
     * @return                 line that matched the regexp
     */
    protected String waitForStringInLogUsingMark(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile) {
        final String METHOD_NAME = "waitForStringInLogUsingMark";
        long startTime = System.currentTimeMillis();
        int waited = 0;

        Long offset = getMarkOffset(outputFile.getAbsolutePath());

        try {
            LogSearchResult newOffsetAndMatches;
            while (waited <= extendedTimeout) {
                if (waited > intendedTimeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(CLASS_NAME, METHOD_NAME, 3977, intendedTimeout, regexp);
                    intendedTimeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                offset = newOffsetAndMatches.getOffset();
                List<String> matches = newOffsetAndMatches.getMatches();
                if (matches.isEmpty()) {
                    try {
                        Thread.sleep(WAIT_INCREMENT);
                    } catch (InterruptedException e) {
                        // Ignore and carry on
                    }
                    waited += WAIT_INCREMENT;
                } else {
                    updateLogOffset(outputFile.getAbsolutePath(), offset);
                    return matches.get(0);
                }
            }
            Log.warning(c, "Timed out searching for " + regexp + " in log file: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due do exception " + e.toString());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(LibertyClient.class, "waitForStringInLogUsingMark",
                     "Started waiting for message matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                         + " and finished at " + formatter.format(new Date(endTime)));
        }
        return null;
    }

    /**
     * Check for multiple instances of the regex in log using mark
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @return                 number of matches found
     */
    public int waitForMultipleStringsInLogUsingMark(int numberOfMatches, String regexp) {
        try {
            return waitForMultipleStringsInLogUsingMark(numberOfMatches, regexp, LOG_SEARCH_TIMEOUT, getDefaultLogFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in default log file due to exception " + e);
            return 0;
        }
    }

    /**
     * Check for multiple instances of the regex in log using mark
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @param  timeout         a timeout, in milliseconds
     * @param  outputFile      file to check
     * @return                 number of matches found
     */
    public int waitForMultipleStringsInLogUsingMark(int numberOfMatches, String regexp, long timeout, RemoteFile outputFile) {
        long startTime = System.currentTimeMillis();
        int waited = 0;
        int count = 0;

        long extendedTimeout = 2 * timeout;
        Long offset = getMarkOffset(outputFile.getAbsolutePath());

        //Ensure we always search for at least 1 occurrence
        if (numberOfMatches <= 0) {
            numberOfMatches = 1;
        }

        try {
            LogSearchResult newOffsetAndMatches;
            while (count < numberOfMatches && waited <= extendedTimeout) {
                if (waited > timeout) { // first time only
                    SOEHttpPostUtil.reportSoftLogTimeoutToSOE(CLASS_NAME, "waitForMultipleStringsInLogUsingMark", 4319, timeout, regexp);
                    timeout = extendedTimeout + WAIT_INCREMENT; // don't report again
                }
                newOffsetAndMatches = LibertyFileManager.findStringInFile(regexp, outputFile, offset);
                offset = newOffsetAndMatches.getOffset();
                try {
                    Thread.sleep(WAIT_INCREMENT);
                } catch (InterruptedException e) {
                    // Ignore and carry on
                }
                waited += WAIT_INCREMENT;
                updateLogOffset(outputFile.getAbsolutePath(), offset);
                count += newOffsetAndMatches.getMatches().size();
            }
        } catch (Exception e) {
            // I think we can assume if we can't read the file it doesn't contain our string
            Log.warning(c, "Could not read log file: " + outputFile + " due to exception " + e.toString());
            e.printStackTrace();
            return 0;
        } finally {
            long endTime = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(LibertyClient.class, "waitForMultipleStringsInLog",
                     "Started waiting for " + numberOfMatches + " messages matching regexp [ " + regexp + "] at " + formatter.format(new Date(startTime))
                                                                         + " and finished at " + formatter.format(new Date(endTime)) + " finding " + count + " matches.");
        }

        return count;
    }

    /**
     * Wait for a regex in the most recent trace file
     *
     * @param  regexp
     * @return
     */
    public String waitForStringInTrace(String regexp) {
        return waitForStringInTrace(regexp, 0);
    }

    /**
     * Wait for a regex in the most recent trace file
     *
     * @param  regexp
     * @return
     */
    public String waitForStringInTrace(String regexp, long timeout) {
        RemoteFile f = null;

        try {
            f = getMostRecentTraceFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.info(c, "waitForStringInTrace", "Waiting for " + regexp + " to be found in " + (f == null ? "null" : f.getAbsolutePath()));

        if (f != null) {
            if (timeout > 0) {
                return waitForStringInLog(regexp, timeout, f);
            } else {
                return waitForStringInLog(regexp, f);
            }
        } else {
            return null;
        }
    }

    protected void searchForMessages(String message_code, String message_type, AtomicInteger counter) {
        final String method = "searchForMessages";
        // Get a remote file whether it exists yet or not (thus don't use the LibertyFileManager API)
        if (messageAbsPath == null) {
            Log.info(c, method, "Messages file path  is null - no check for message in logs");
        } else {
            RemoteFile outputFile = machine.getFile(messageAbsPath);
            int oldNumber = counter.getAndIncrement();
            int newNumber = oldNumber + 1;
            int numberFound = waitForMultipleStringsInLog(newNumber, message_code, clientStartTimeout, outputFile);
            //waitForStringInLog(REMOVE_APP_MESSAGE_CODE, clientStartTimeout, outputFile);
            if (numberFound == newNumber) {
                Log.info(c, method, message_type + " message appears in log " + numberFound + " time(s)");
            } else if (numberFound > counter.get()) {
                //need to update stopApplicationMessages
                Log.info(c, method, "Resetting the number of " + message_type + " messages that appear in the log");
                counter.set(numberFound);
            } else {
                Log.info(c, method, "Incorrect number of " + message_type + " messages in the log.  An error may have occurred.");
            }
        }
    }

    public void addInstalledAppForValidation(String app) {
        final String method = "addInstalledAppForValidation";
        final String START_APP_MESSAGE_CODE = "CWWKZ0001I";
        Log.info(c, method, "Adding installed app: " + app + " for validation");
        installedApplications.add(app);

        if (isStarted) {
            searchForMessages(START_APP_MESSAGE_CODE, "installApp", startApplicationMessages);
        }
    }

    public void removeInstalledAppForValidation(String app) {
        final String method = "removeInstalledAppForValidation";
        final String REMOVE_APP_MESSAGE_CODE = "CWWKZ0009I";
        Log.info(c, method, "Removing installed app: " + app + " for validation");
        installedApplications.remove(app);

        if (isStarted) {
            searchForMessages(REMOVE_APP_MESSAGE_CODE, "uninstallApp", stopApplicationMessages);
        }
    }

    /**
     * Returns true if the client has been successfully started, and either
     * hasn't been stopped or hit exceptions during client stop.
     *
     * @return
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Sets the client's state to "started". The client has been started by some
     * other means, and we are simply telling this class about it so that the client
     * can be stopped later.
     */
    public void setStarted() {
        isStarted = true;
    }

    /**
     * Sets the client's state flag to the state specified by the isStarted parameter.
     * The isStarted flag may not always be correct, if the client happens to be started
     * external from this class. This method allows for management of the state externally.
     *
     * @param isStarted
     */
    public void setStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }

    /**
     * This method will check the client state and reset the state based on the results of the
     * status operation.
     *
     * @throws Exception
     */
    public void resetStarted() throws Exception {
        ProgramOutput clientStatusOutput = executeClientScript("status", null);
        int rc = clientStatusOutput.getReturnCode();
        if (rc == 0) {
            // Client is still running when rc == 0
            isStarted = true;

            //Setup the client logs assuming the default setting.
            messageAbsPath = logsRoot + messageFileName;
            consoleAbsPath = logsRoot + consoleFileName;

        } else {
            // Client is stopped when rc == 1.  Any other value means client
            // is in a bad way, but we still will treat it as not started.
            isStarted = false;
        }
    }

    public ProgramOutput start(String... args) throws Exception {
        return startClientWithArgs(true, true, true, false, "run", Arrays.asList(args), true);
    }

    /**
     * Start the client.
     *
     * @throws Exception
     */
    public ProgramOutput startClient() throws Exception {
        return startClientAndValidate(true, true, true);
    }

    /**
     * Start the client.
     *
     * @param  cleanStart   if true, the client will be started with a clean start
     * @param  validateApps if true, block until all of the registered apps have started
     * @throws Exception
     */
    public ProgramOutput startClient(boolean cleanStart, boolean validateApps) throws Exception {
        return startClientAndValidate(true, cleanStart, validateApps);
    }

    /**
     * "Restart" an applications in dropins by renaming it and putting it back.
     * Very clever, eh?
     * <p>
     * This will use {@link #waitForStringInLogUsingMark(String)} so ensure
     * the offset is set to the correct point before invoking.
     *
     * @param  fileName  the file name of the application, e.g. snoop.war
     * @return           {@code true} if the application was restarted successfully, {@code false} otherwise.
     * @throws Exception
     */
    public boolean restartDropinsApplication(String fileName) throws Exception {
        final String method = "restartDropinsApplication";

        String appName = fileName.substring(0, fileName.lastIndexOf("."));
        String oldFilePath = clientRoot + "/dropins/" + fileName;
        String newFilePath = clientRoot + "/" + fileName;

        String stopMsg, startMsg = null;

        // Move the file out of dropins...
        if (LibertyFileManager.renameLibertyFile(machine, oldFilePath, newFilePath)) {
            Log.info(c, method, fileName + " successfully moved out of dropins, waiting for message...");
            stopMsg = waitForStringInLogUsingMark("CWWKZ0009I:.*" + appName);
        } else {
            Log.info(c, method, "Unable to move " + fileName + " out of dropins, failing.");
            return false;
        }

        // Pause for 4 seconds
        Thread.sleep(4000);

        // Move it back into dropins...
        if (LibertyFileManager.renameLibertyFile(machine, newFilePath, oldFilePath)) {
            Log.info(c, method, fileName + " successfully moved back into dropins, waiting for message...");
            startMsg = waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName);
        } else {
            Log.info(c, method, "Unable to move " + fileName + " back into dropins, failing.");
            return false;
        }

        return stopMsg != null && startMsg != null;
    }

    public void setLogsRoot(String root) {
        this.logsRoot = root;
    }

    public String getLogsRoot() {
        return logsRoot;
    }

    /**
     * Start the client and validate that the client was started
     *
     * @param  cleanStart     if true, the client will be started with a clean start
     * @param  validateApps   if true, block until all of the registered apps have started
     * @param  preCleanClient if true, the client directory will be reset before the client is started (reverted to vanilla backup).
     * @throws Exception
     * @return                the output of the start command
     */
    public ProgramOutput startClientAndValidate(boolean preClean, boolean cleanStart, boolean validateApps) throws Exception {
        return startClientAndValidate(preClean, cleanStart, validateApps, false);
    }

    /**
     * Start the client and validate that the client was started
     *
     * @param  cleanStart     if true, the client will be started with a clean start
     * @param  validateApps   if true, block until all of the registered apps have started
     * @param  preCleanClient if true, the client directory will be reset before the client is started (reverted to vanilla backup).
     * @throws Exception
     * @return                the output of the start command
     */
    public ProgramOutput startClientAndValidate(boolean preClean, boolean cleanStart, boolean validateApps, boolean expectStartFailure) throws Exception {
        return startClientAndValidate(preClean, cleanStart, validateApps, expectStartFailure, true);
    }

    /**
     * Start the client and validate that the client was started
     *
     * @param  preClean           if true, the client directory will be reset before
     *                                the client is started (reverted to vanilla backup).
     * @param  cleanStart         if true, the client will be started with a clean start
     * @param  validateApps       if true, block until all of the registered apps have started
     * @param  expectStartFailure if true, a the client is not expected to start
     *                                due to a failure
     * @param  validateTimedExit  if true, the client will make sure that timedexit-1.0 is enabled
     * @throws Exception
     */
    public ProgramOutput startClientAndValidate(boolean preClean, boolean cleanStart,
                                                boolean validateApps, boolean expectStartFailure,
                                                boolean validateTimedExit) throws Exception {
        return startClientWithArgs(preClean, cleanStart, validateApps, expectStartFailure, "run", null, validateTimedExit);
    }

    /**
     * Issues a client script command against this client
     *
     * @param  command      command name
     * @param  optionalArgs any optional args needed by the command
     * @throws Exception    if the operation fails
     * @return              the output of the command
     */
    public ProgramOutput executeClientScript(String command, String[] optionalArgs) throws Exception {
        final String method = "executeClientScript";
        Log.info(c, method, "Running client script with command=" + command, optionalArgs);

        String cmd = installRoot + "/bin/client";

        // organize parms properly - the command name comes first, followed by the client name, followed
        // by an optional arguments
        String[] parms;
        if (optionalArgs == null) {
            parms = new String[2];
        } else {
            parms = new String[2 + optionalArgs.length];
            System.arraycopy(optionalArgs, 0, parms, 2, optionalArgs.length);
        }
        parms[0] = command;
        parms[1] = clientToUse;

        //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
        Properties envVars = new Properties();
        envVars.setProperty("JAVA_HOME", machineJava);
        Log.info(c, method, "Using additional env props: " + envVars.toString());

        ProgramOutput output = machine.execute(cmd, parms, envVars);
        String stdout = output.getStdout();
        Log.info(c, method, "Client script output: " + stdout);
        Log.info(c, method, "Return code from script is: " + output.getReturnCode());
        return output;
    }

    /**
     * Set the list of errors to be ignored
     *
     * @param  String .... list of expected errors
     * @return        void
     */
    public void addIgnoreErrors(String... regex) {
        if (regex != null && regex.length != 0) {
            List<String> expectedErrors = new ArrayList<String>(regex.length);
            for (String errmsg : regex) {
                expectedErrors.add(errmsg);
            }

            // Compile set of regex's
            if (expectedErrors != null && expectedErrors.size() != 0)
                for (String ignoreRegEx : expectedErrors) {
                    ignorePatterns.add(Pattern.compile(ignoreRegEx));
                    if (ignoreRegEx.equals(LibertyClient.DISABLE_FAILURE_CHECKING))
                        checkingDisabled = true;
                }
        } else {
            ignorePatterns.clear();
        }
    }

    protected boolean isClientExemptFromChecking() {
        return checkingDisabled;
    }

    protected Properties getBootstrapProperties() {
        Properties props = new Properties();
        try {
            String serverEnv = FileUtils.readFile(getClientRoot() + "/bootstrap.properties");
            props.load(new StringReader(serverEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }
        return props;
    }

    protected boolean clientNeedsToRunWithJava2Security() {
        // Allow clients to opt-out of j2sec by setting
        // websphere.java.security.exempt=true
        // in their ${client.config.dir}/bootstrap.properties
        boolean j2secEnabled = !("true".equalsIgnoreCase(getBootstrapProperties().getProperty("websphere.java.security.exempt")));
        Log.info(c, "clientNeedsToRunWithJava2Security", "Will client " + getClientName() + " run with Java 2 Security enabled?  " + j2secEnabled);
        return j2secEnabled;
    }

    /*
     * Provide a method for setting default messages to ignore for all clients
     * This mimics the implementation of the same function in LibertyServer.
     */
    private void populateFixedListOfMessagesToIgnore() {

        // Added to stop iFix/testFix builds failing when listing warning message of testFix installed,
        // of course there is a test fix installed ...it is a test fix build
        fixedIgnoreErrorsList.add("CWWKF0014W:");

    }

    //FIPS 140-3
    public boolean isFIPS140_3EnabledAndSupported() {
        String methodName = "isFIPS140_3EnabledAndSupported";
        boolean isIBMJVM8 = (javaInfo.majorVersion() == 8) && (javaInfo.VENDOR == Vendor.IBM);
        boolean isIBMJVM17 = (javaInfo.majorVersion() == 17) && (javaInfo.VENDOR == Vendor.IBM);
        if (GLOBAL_CLIENT_FIPS_140_3) {
            Log.info(c, methodName, "Liberty client is running JDK version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.VENDOR);
            if (isIBMJVM8) {
                Log.info(c, methodName, "global build properties FIPS_140_3 is set for client " + getClientName() +
                                        " and IBM java 8 is available to run with FIPS 140-3 enabled.");
            } else if (isIBMJVM17) {
                Log.info(c, methodName, "global build properties FIPS_140_3 is set for client " + getClientName() +
                                        " and IBM java 17 is available to run with FIPS 140-3 enabled.");
            } else {
                Log.info(c, methodName, "The global build properties FIPS_140_3 is set for client " + getClientName() +
                                        ",  but no IBM java 8 or java 17 on liberty client to run with FIPS 140-3 enabled.");
            }
        }
        return GLOBAL_CLIENT_FIPS_140_3 && (isIBMJVM8 || isIBMJVM17);
    }

    //FIPS 140-3
    public Properties getClientEnv() {
        Properties props = new Properties();

        props.put("JAVA_HOME", getMachineJavaJDK());

        // First load ${wlp.install.dir}/etc/client.env
        try {
            String clientEnv = FileUtils.readFile(getInstallRoot() + "/etc/client.env");
            props.load(new StringReader(clientEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }

        // Then load ${server.config.dir}/client.env
        try {
            String clientEnv = FileUtils.readFile(getClientRoot() + "/client.env");
            props.load(new StringReader(clientEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }

        return props;
    }

}
