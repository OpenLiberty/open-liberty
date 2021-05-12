/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Assert;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.application.ApplicationType;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.soe_reporting.SOEHttpPostUtil;
import com.ibm.ws.fat.util.ACEScanner;
import com.ibm.ws.fat.util.jmx.JmxException;
import com.ibm.ws.fat.util.jmx.JmxServiceUrlFactory;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;
import com.ibm.ws.logging.utils.FileLogHolder;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.LocalMachine;
import componenttest.custom.junit.runner.LogPolice;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.depchain.FeatureDependencyProcessor;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyFileManager.LogSearchResult;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;
import componenttest.topology.utils.PrivHelper;
import componenttest.topology.utils.ServerFileUtils;

public class LibertyServer implements LogMonitorClient {

    protected static final Class<?> c = LibertyServer.class;
    protected static final String CLASS_NAME = c.getName();
    protected static Logger LOG = Logger.getLogger(CLASS_NAME); // why don't we always use the Logger directly?

    /** How frequently we poll the logs when waiting for something to happen */
    protected static final int WAIT_INCREMENT = 300;

    boolean runAsAWindowService = false;

    protected class ServerDebugInfo {

        protected static final String SERVER_DEBUG_PREFIX = "debug.server.";
        protected static final String DEBUGGING_PORT_PROP = "debugging.port";

        boolean startInDebugMode = false;
        protected String debugPort = null;

        protected ServerDebugInfo() {
            calculateDebugInfo();
        }

        /**
         * We only start in debug mode if:
         * debugging is allowed
         * AND
         * the environment sets a system property with a valid port.
         */

        private void calculateDebugInfo() {

            if (debuggingAllowed) {
                debugPort = getDebugPortForServer();
                if (debugPort == null) {
                    debugPort = getGenericDebuggingPort();
                }
                if (debugPort != null) {
                    startInDebugMode = true;
                    Log.info(c, "calculateDebugInfo", "Debug enabled for server = " + serverToUse + ", at port = " + debugPort);
                    return;
                }
                Log.info(c, "calculateDebugInfo", "debugging allowed for server = " + serverToUse + ", but didn't find valid port, so debug not enabled");
            } else {
                Log.info(c, "calculateDebugInfo", "debugging not allowed for server = " + serverToUse);
            }
        }

        String getDebugPortForServer() {
            return getValidPortPropertyVal(SERVER_DEBUG_PREFIX + serverToUse);
        }

        String getGenericDebuggingPort() {
            return getValidPortPropertyVal(DEBUGGING_PORT_PROP);
        }

        /**
         * For system property with key = "key", return the value of this system property
         * if and only if the value can be parsed into a non-negative integer.
         *
         * If not, returns <null>.
         *
         * @param  key
         * @return     value of sys property or <null>
         */
        String getValidPortPropertyVal(final String key) {
            int portVal;
            String sysPropVal = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(key);
                }
            });

            if (sysPropVal != null && sysPropVal != "false") {
                try {
                    portVal = Integer.parseInt(sysPropVal);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (portVal < 0) {
                    return null;
                }
            } else {
                return null;
            }
            Log.info(c, "getValidPortPropertyVal", "Return valid port val, key = " + key + ", val = " + sysPropVal);
            return sysPropVal;
        }
    }

    protected static final String MAC_RUN = PrivHelper.getProperty("fat.on.mac");
    protected static final String DEBUGGING_PORT = PrivHelper.getProperty("debugging.port");
    protected static final boolean DEFAULT_PRE_CLEAN = true;
    protected static final boolean DEFAULT_CLEANSTART = Boolean.parseBoolean(PrivHelper.getProperty("default.clean.start", "true"));
    protected static final boolean DEFAULT_VALIDATE_APPS = true;
    protected static final String RELEASE_MICRO_VERSION = PrivHelper.getProperty("micro.version");
    protected static final String TMP_DIR = PrivHelper.getProperty("java.io.tmpdir");
    public static boolean validateApps = DEFAULT_VALIDATE_APPS;

    protected static final JavaInfo javaInfo = JavaInfo.forCurrentVM();

    protected static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");
    protected static final boolean GLOBAL_JAVA2SECURITY = Boolean.parseBoolean(PrivHelper.getProperty("global.java2.sec", "false"));
    protected static final boolean GLOBAL_DEBUG_JAVA2SECURITY = FAT_TEST_LOCALRUN //
                    ? Boolean.parseBoolean(PrivHelper.getProperty("global.debug.java2.sec", "true")) //
                    : Boolean.parseBoolean(PrivHelper.getProperty("global.debug.java2.sec", "false"));

    protected static final String GLOBAL_TRACE = PrivHelper.getProperty("global.trace.spec", "").trim();
    protected static final String GLOBAL_JVM_ARGS = PrivHelper.getProperty("global.jvm.args", "").trim();

    protected static final boolean DO_COVERAGE = PrivHelper.getBoolean("test.coverage");
    protected static final String JAVA_AGENT_FOR_JACOCO = PrivHelper.getProperty("javaagent.for.jacoco");

    protected static final int SERVER_START_TIMEOUT = (FAT_TEST_LOCALRUN ? 15 : 30) * 1000;
    protected static final int SERVER_STOP_TIMEOUT = SERVER_START_TIMEOUT;

    // How long to wait for an app to start before failing out
    protected int APP_START_TIMEOUT = (FAT_TEST_LOCALRUN ? 12 : 120) * 1000;

    // Increasing this from 50 seconds to 120 seconds to account for poorly performing code;
    // this timeout should only pop in the event of an unexpected failure of apps to start.
    protected static final int LOG_SEARCH_TIMEOUT = (FAT_TEST_LOCALRUN ? 12 : 120) * 1000;

    // Allow configuration updates to wait for messages in the log longer than other log
    // searches. Configuration updates may take some time on slow test systems.
    protected static int LOG_SEARCH_TIMEOUT_CONFIG_UPDATE = (FAT_TEST_LOCALRUN ? 12 : 180) * 1000;

    protected Set<String> installedApplications;

    protected static final String DEFAULT_SERVER = "defaultServer";

    protected static final String DEFAULT_MSG_FILE = "messages.log";
    protected static final String DEFAULT_CONSOLE_FILE = "console.log";

    protected static final String DEFAULT_TRACE_FILE_PREFIX = "trace";

    protected static final String SERVER_CONFIG_FILE_NAME = "server.xml";
    protected static final String JVM_OPTIONS_FILE_NAME = "jvm.options";

    protected static final String EBCDIC_CHARSET_NAME = "IBM1047";

    protected volatile boolean isStarted = false;
    protected boolean isStartedConsoleLogLevelOff = false;

    protected int osgiConsolePort = 5678; // The port number of the OSGi Console

    protected static final String OSGI_DIR_NAME = "org.eclipse.osgi";

    // Use port 0 if the property can't be found, these should be picked up from a properties file
    // if not then the test may create a liberty server and get the ports from a bootstrap port.
    // If neither way obtains a port then port 0 will be used which will cause the tests to fail in
    // an obvious way (rather than using a port which looks like it might be right, but isn't, e.g. 8000)
    protected int httpDefaultPort = Integer.parseInt(System.getProperty("HTTP_default", "0"));

    protected int httpDefaultSecurePort = Integer.parseInt(System.getProperty("HTTP_default.secure", "0"));

    protected int httpSecondaryPort = Integer.parseInt(System.getProperty("HTTP_secondary", "0"));
    protected int httpSecondarySecurePort = Integer.parseInt(System.getProperty("HTTP_secondary.secure", "0"));

    protected String bvtPortPropertyName = null;
    protected String bvtSecurePortPropertyName = null;

    protected int iiopDefaultPort = Integer.parseInt(System.getProperty("IIOP", "0"));

    protected String hostName;
    protected String installRoot; // The root of the Liberty Install
    protected String userDir; // The WLP_USER_DIR for this server
    protected boolean customUserDir = false;

    protected String installRootParent; // The parent directory of the Liberty Install

    protected final AtomicBoolean checkForRestConnector = new AtomicBoolean(false);

    // used by the saveServerConfiguration / restoreServerConfiguration methods to save the current server
    // configuration at a specific point in time and then be able to restore it back in the future
    protected RemoteFile savedServerXml = null;

    //list of servers exempt from log error and failure checking
    protected HashMap<String, String> serversExemptFromChecking = null;

    protected Set<String> serversExemptFromJava2SecurityTesting = null;

    public static final String DISABLE_FAILURE_CHECKING = "DISABLE_CHECKING";

    private boolean isTidy = false;

    private boolean needsPostTestRecover = true;

    private boolean logOnUpdate = true;

    protected boolean debuggingAllowed = true;

    /**
     * This returns whether or not debugging is "programatically" allowed
     * for this server. It must still be combined with a port supplied by
     * the environment for debug to actually be enabled.
     *
     * @return {@code true} if debugging is potentially allowed for this server.
     */
    public boolean isDebuggingAllowed() {
        return debuggingAllowed;
    }

    /**
     * This sets whether or not debugging is "programatically" allowed
     * for this server. It must still be combined with a port supplied by
     * the environment for debug to actually be enabled.
     *
     * @param debuggingAllowed whether debugging is potentially allowed for this server.
     */
    public void setDebuggingAllowed(boolean debuggingAllowed) {
        this.debuggingAllowed = debuggingAllowed;
    }

    public boolean isLogOnUpdate() {
        return logOnUpdate;
    }

    public void setLogOnUpdate(boolean logOnUpdate) {
        this.logOnUpdate = logOnUpdate;
    }

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
     * Returns the value of WLP_USER_DIR for the server.
     *
     * @return
     */
    public String getUserDir() {
        return userDir;
    }

    public boolean isCustomUserDir() {
        return customUserDir;
    }

    protected String serverRoot; // The root of the server for Liberty
    protected String serverOutputRoot; // The output root of the server
    protected String logsRoot; // The root of the Logs Files

    protected long lastConfigUpdate = 0; // Time stamp (in millis) of the last configuration update

    protected Map<String, String> additionalSystemProperties = null;

    private final Map<String, String> envVars = new HashMap<>();

    protected String relativeLogsRoot = "/logs/"; // this will be appended to logsRoot in setUp
    protected String consoleFileName = DEFAULT_CONSOLE_FILE; // Console log file name
    protected String messageFileName = DEFAULT_MSG_FILE; // Messages log file name (optionally changed by the FAT)
    protected String traceFileName = DEFAULT_TRACE_FILE_PREFIX + ".log"; // Trace log file name
    protected String messageAbsPath = null;
    protected String consoleAbsPath = null;
    protected String traceAbsPath = null;

    protected final List<String> extraArgs = new ArrayList<String>();

    protected String machineJava; // Path to Java 6 JDK on the Machine

    protected String machineJarPath; //Path to the jar command

    protected Machine machine; // Machine the server is on

    protected String serverToUse; // the server to use

    //An ID given to the server topology that will be used as a reference e.g. JPAFATTestServer
    protected String serverTopologyID;

    protected OperatingSystem machineOS;

    //These aren't final as we have to massage them if they are used for tWAS FAT suites
    public String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    protected String pathToAutoFVTOutputServersFolder = "output/servers";
    protected String pathToAutoFVTOutputFolder = "output/";

    protected final String PATH_TO_AUTOFVT_SERVERS = "publish/servers/";
    protected static final String PATH_TO_AUTOFVT_SHARED = "publish/shared/";
    //Only need this at the moment as we only support single Liberty Servers
    protected String pathToAutoFVTNamedServer = PATH_TO_AUTOFVT_SERVERS;

    protected long serverStartTimeout = SERVER_START_TIMEOUT;

    protected final AtomicInteger stopApplicationMessages = new AtomicInteger(0);
    protected final AtomicInteger startApplicationMessages = new AtomicInteger(0);

    public String getPathToAutoFVTNamedServer() {
        return pathToAutoFVTNamedServer;
    }

    protected List<String> originalFeatureSet = null;

    //Used for keeping track of offset positions of log files
    protected HashMap<String, Long> logOffsets = new HashMap<String, Long>();

    protected HashMap<String, Long> originOffsets = new HashMap<String, Long>();

    protected boolean serverCleanupProblem = false;

    protected boolean ffdcChecking = true;

    /** When we stopped searching for a string in the logs. */
    public long searchStopTime;

    private final List<String> ignoredErrors = new ArrayList<>();

    /**
     * Holds a fixed set error and warning messages to be ignored for those
     * buckets that choose to care about error or warning messages when the server
     * is stopped. See populateFixedListOfMessagesToIgnore() for more details.
     */
    private final List<String> fixedIgnoreErrorsList = new ArrayList<String>();

    /**
     * Shared LogMonitor class is used to encapsulate some basic log search/wait logic
     */
    private final LogMonitor logMonitor;

    private boolean newLogsOnStart = FileLogHolder.NEW_LOGS_ON_START_DEFAULT;

    /**
     * @param serverCleanupProblem the serverCleanupProblem to set
     */
    void setServerCleanupProblem(boolean serverCleanupProblem) {
        this.serverCleanupProblem = serverCleanupProblem;
    }

    public Machine getMachine() {
        return machine;
    }

    /**
     * Protected - This constructor is default as users should use the
     * LibertyServerFactory's static methods to get LibertyServer instances.
     *
     * @param  serverName The name of the server that is going to used
     * @param  b          The bootstrap properties for this server
     * @throws Exception
     */
    LibertyServer(String serverName, Bootstrap b) throws Exception {
        this(serverName, b, false, false);
    }

    LibertyServer(String serverName, Bootstrap b, boolean deleteServerDirIfExist, boolean usePreviouslyConfigured) throws Exception {
        this(serverName, b, deleteServerDirIfExist, usePreviouslyConfigured, LibertyServerFactory.WinServiceOption.OFF);
    }

    /**
     * Protected - This constructor is default as users should use the
     * LibertyServerFactory's static methods to get LibertyServer instances.
     *
     * @param  serverName              The name of the server that is going to used
     * @param  b                       The bootstrap properties for this server
     * @param  deleteServerDirIfExist  If true and if the specified server name already exists on the file system, it will be deleted
     * @param  usePreviouslyConfigured If true do not tidy existing server
     * @param  winServiceOption
     * @throws Exception
     */
    LibertyServer(String serverName, Bootstrap b, boolean deleteServerDirIfExist, boolean usePreviouslyConfigured,
                  LibertyServerFactory.WinServiceOption winServiceOption) throws Exception {
        final String method = "setup";
        Log.entering(c, method);
        serverTopologyID = b.getValue("ServerTopologyID");
        hostName = b.getValue("hostName");
        machineJava = b.getValue(hostName + ".JavaHome");

        if (serverName != null) {
            serverToUse = serverName;
            pathToAutoFVTNamedServer += serverToUse + "/";
        } else {
            serverToUse = b.getValue("serverName");
            if (serverToUse == null || serverToUse.trim().equals("")) {
                serverToUse = DEFAULT_SERVER;
            }
        }

        if (winServiceOption == LibertyServerFactory.WinServiceOption.ON) {
            runAsAWindowService = true;
        } else {
            runAsAWindowService = false;
        }

        String newLogsOnStartProperty = b.getValue(FileLogHolder.NEW_LOGS_ON_START_PROPERTY);
        if (newLogsOnStartProperty != null) {
            newLogsOnStart = Boolean.parseBoolean(newLogsOnStartProperty);
        }

        // This is the only case where we will allow the messages.log name to  be changed
        // by the fat framework -- because we want to look at messasges.log for start/stop/blah
        // messags, we shouldn't be pointing it all over everywhere else. For those FAT tests
        // that need a messages file in an alternate location, they should set the corresponding
        // com.ibm.ws.logging.message.file.name property in bootstrap.properties
        String nonDefaultLogFile = b.getValue("NonDefaultConsoleLogFileName");
        if (nonDefaultLogFile != null && nonDefaultLogFile.startsWith("SERVER_NAME/")) {
            relativeLogsRoot = "/logs/" + serverToUse + "/";
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
            httpDefaultPort = Integer.parseInt(b.getValue("http.Default.Port"));
        } catch (Exception e) {
            Log.debug(c, "No http.Default.Port set in bootstrap.properties.  Will use default value: "
                         + httpDefaultPort);
        }

        try {
            httpDefaultSecurePort = Integer.parseInt(b.getValue("http.Default.Secure.Port"));
        } catch (Exception e) {
            Log.debug(c, "No http.Default.Secure.Port set in bootstrap.properties.  Will use default value: "
                         + httpDefaultSecurePort);
        }

        try {
            iiopDefaultPort = Integer.parseInt(b.getValue("IIOP"));
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

        // Allow user directory name to be provided in bootstrap properties.
        // It is optional and if it is not set, setup() will set it.
        userDir = b.getValue("libertyUserDir");

        // Populate the fixed set error and warning messages to be ignored for those
        // buckets that choose to care about error or warning messages when the server
        // is stopped.
        populateFixedListOfMessagesToIgnore();

        // In the multinode test environment, one of the hosts may be the local machine
        // and therefore should be mapped to LocalMachine.  Simplicity only does this
        // mapping if the host name is "localhost".  For now a check is added here
        // to compare the host name to the local host name.  This is guarded by a
        // property "checkIfLocalHost" just in case someone doesn't want this behavior.
        String checkIfLocalHost = b.getValue("checkIfLocalHost");
        if (checkIfLocalHost != null && hostName.equals(InetAddress.getLocalHost().getHostName())) {
            machine = Machine.getLocalMachine();
            // Do not update hostName because Machine will say localhost!
        } else {
            machine = LibertyServerUtils.createMachine(b);
            // Update hostName to maintain old behavior of LibertyServer.getHostname()
            // which asked the machine.
            hostName = machine.getHostname();
        }

        //Initialize LogMonitor
        logMonitor = new LogMonitor(this);

        setup(deleteServerDirIfExist, usePreviouslyConfigured);
        Log.exiting(c, method);
    }

    //This isn't that elegant but it works
    //If this is a tWAS FAT suite the relative path to the autoFVT folder
    //is different so we need to check and set variables to the autoFVT/output folder
    //and autoFVT/lib/testFiles accordingly
    protected void massageAutoFVTAbsolutePath() throws Exception {
        final String METHOD = "massageAutoFVTAbsolutePath";
        Log.entering(c, METHOD);

        LocalFile testBuildFile = new LocalFile("TestBuild.xml");
        String localPropsLoc = System.getProperty("local.properties");
        if (!!!testBuildFile.exists() && localPropsLoc != null) {
            Properties localProps = new Properties();
            FileInputStream in = new FileInputStream(localPropsLoc);
            localProps.load(in);
            in.close();

            String bucketsDir = localProps.getProperty("buckets.dir");
            pathToAutoFVTTestFiles = bucketsDir + "/" + pathToAutoFVTTestFiles;
            pathToAutoFVTNamedServer = bucketsDir + "/" + pathToAutoFVTNamedServer;
            pathToAutoFVTOutputServersFolder = bucketsDir + "/" + "output/servers";
            pathToAutoFVTOutputFolder = bucketsDir + "/output";

            Log.info(c, METHOD, "This seems to be a tWAS FAT suite so updating the path to the" +
                                "AutoFVTTestFiles to " + pathToAutoFVTTestFiles + " and the testOutputFolder to " +
                                pathToAutoFVTOutputServersFolder + " and the path to the AutoFVTNamedServer to "
                                + pathToAutoFVTNamedServer);
        }
        Log.exiting(c, METHOD);
    }

    protected void setup(boolean deleteServerDirIfExist, boolean usePreviouslyConfigured) throws Exception {
        installedApplications = new HashSet<String>();
        machine.connect();
        machine.setWorkDir(installRoot);
        if (this.serverToUse == null) {
            this.serverToUse = DEFAULT_SERVER;
        }

        machineOS = machine.getOperatingSystem();
        this.installRoot = LibertyServerUtils.makeJavaCompatible(installRoot, machine);
        // Set default usr directory if not already set.
        if (this.userDir == null)
            this.userDir = installRoot + "/usr";
        else
            customUserDir = true;
        this.serverRoot = this.userDir + "/servers/" + serverToUse;
        this.serverOutputRoot = this.serverRoot;
        this.logsRoot = serverOutputRoot + relativeLogsRoot;
        this.messageAbsPath = logsRoot + messageFileName;
        this.traceAbsPath = logsRoot + traceFileName;

        // delete existing server directory if requested:
        if (deleteServerDirIfExist) {
            RemoteFile serverDir = new RemoteFile(machine, this.serverRoot);
            if (serverDir.exists() && !serverDir.delete()) {
                Exception ex = new TopologyException("Unable to delete pre-existing server directory: " + this.serverRoot);
                Log.error(c, "setup - User requested that we delete pre-existing server directory, but this operation failed - " + this.serverRoot, ex);
                throw ex;
            }
        }

        File installRootfile = new File(this.installRoot);
        this.installRootParent = installRootfile.getParent();

        // Now it sets all OS specific stuff
        this.machineJava = LibertyServerUtils.makeJavaCompatible(machineJava, machine);

        Log.info(c, "setup", "Successfully obtained machine. Operating System is: " + machineOS.name());
        // Continues with setup, we now validate the Java used is a JDK by looking for java and jar files
        String jar = "jar";
        String java = "java";
        if (machineOS == OperatingSystem.WINDOWS) {
            jar += ".exe";
            java += ".exe";
        }
        RemoteFile testJar = new RemoteFile(machine, machineJava + "/bin/" + jar);
        RemoteFile testJava = new RemoteFile(machine, machineJava + "/bin/" + java);
        machineJarPath = testJar.getAbsolutePath();
        if (!!!testJar.exists()) {
            //if we come in here we might be pointing at a JRE instead of a JDK so we'll go up a level in hope it's there
            testJar = new RemoteFile(machine, machineJava + "/../bin/" + jar);
            machineJarPath = testJar.getAbsolutePath();
            if (!!!testJar.exists()) {
                throw new TopologyException("cannot find a " + jar + " file in " + machineJava + "/bin. Please ensure you have set the machine javaHome to point to a JDK");
            } else {
                Log.info(c, "setup", "Jar Home now set to: " + machineJarPath);
            }
        }
        if (!!!testJava.exists())
            throw new TopologyException("cannot find a " + java + " file in " + machineJava + "/bin. Please ensure you have set the machine javaHome to point to a JDK");

        Log.finer(c, "setup", "machineJava: " + machineJava + " machineJarPath: " + machineJarPath);

        massageAutoFVTAbsolutePath();

        if (!usePreviouslyConfigured)
            preTestTidyup();

        if (!newLogsOnStart) {
            initializeAnyExistingMarks();
        }
    }

    protected void preTestTidyup() {
        //Deletes the logs and work area folder and the apps folder
        try {
            machine.getFile(logsRoot).delete();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            machine.getFile(serverRoot + "/workarea").delete();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            RemoteFile applicationsFolder = new RemoteFile(machine, userDir + "/shared/apps");
            applicationsFolder.delete();
            applicationsFolder.mkdir();
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
    }

    /**
     * Set the feature list of the server.xml to the new features specified
     * in the {@code List<String>}. Each String should be the only feature
     * name, e.g. servlet-3.0
     *
     * @param  newFeatures
     * @throws Exception
     */
    public void changeFeatures(List<String> newFeatures) throws Exception {
        RemoteFile serverXML = new RemoteFile(machine, serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        LocalFile tempServerXML = new LocalFile(SERVER_CONFIG_FILE_NAME);
        boolean createOriginalList;
        if (originalFeatureSet == null) {
            createOriginalList = true;
            originalFeatureSet = new ArrayList<String>();
        } else {
            createOriginalList = false;
        }
        Writer w = new OutputStreamWriter(tempServerXML.openForWriting(false));
        InputStream originalOutput = serverXML.openForReading();
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
        //Now we need to copy file overwriting existing server.xml and delete the temp
        tempServerXML.copyToDest(serverXML, false, true);
        tempServerXML.delete();
    }

    /**
     * Copies the server.xml to the server.
     *
     * @param  newFeatures
     * @throws Exception
     */
    public void refreshServerXMLFromPublish() throws Exception {
        RemoteFile serverXML = new RemoteFile(machine, serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        LocalFile publishServerXML = new LocalFile(PATH_TO_AUTOFVT_SERVERS + "/" + getServerName() + "/" + SERVER_CONFIG_FILE_NAME);

        publishServerXML.copyToDest(serverXML, false, true);
    }

    public static void setValidateApps(boolean validateApp) {
        validateApps = validateApp;
    }

    public boolean getValidateApps() {
        return validateApps;
    }

    /**
     * Sets the server configuration to be the specified file and starts the server.
     */
    public ProgramOutput startServerUsingConfiguration(String configFile) throws Exception {
        return startServerUsingConfiguration(configFile, new ArrayList<String>());
    }

    /**
     * Sets the server configuration to be the specified file and starts the server.
     */
    public ProgramOutput startServerUsingExpandedConfiguration(String configFile) throws Exception {
        return startServerUsingExpandedConfiguration(configFile, new ArrayList<String>());
    }

    public ProgramOutput startServerUsingExpandedConfiguration(String configFile, List<String> waitForMessages) throws Exception {

        ServerFileUtils serverFileUtils = new ServerFileUtils();
        String mergedFile = serverFileUtils.expandAndBackupCfgFile(this, configFile);
        ProgramOutput startupOutput = startServerUsingConfiguration(mergedFile, waitForMessages);
        saveServerConfiguration();
        return startupOutput;
    }

    /**
     * Sets the server configuration to be the specified file, starts the server, and waits for each of the specified messages.
     */
    public ProgramOutput startServerUsingConfiguration(String configFile, List<String> waitForMessages) throws Exception {
        setServerConfigurationFromFilePath(configFile);
        ProgramOutput startupOutput = startServer();
        waitForStringsInLogUsingMark(waitForMessages);
        return startupOutput;
    }

    /**
     * Reconfigures the running server. Expands any imports in the specified server config and copies that expanded
     * configuration to server.xml of the server root.
     *
     * @param  testName        - The name of the test case requesting the reconfig - a copy of the expanded configuration
     *                             file will be saved for debug purposes
     * @param  newConfig       - The configuration to swich to
     * @param  waitForMessages - Any messages to wait (used to determine if the update is complete)
     * @throws Exception
     */
    public void reconfigureServerUsingExpandedConfiguration(String testName, String newConfig, String... waitForMessages) throws Exception {

        reconfigureServerUsingExpandedConfiguration(testName, "configs", newConfig, true, waitForMessages);
    }

    /**
     * Reconfigures the running server. Expands any imports in the specified server config and copies that expanded
     * configuration to server.xml of the server root.
     *
     * @param  testName        - The name of the test case requesting the reconfig - a copy of the expanded configuration
     *                             file will be saved for debug purposes
     * @param  configDir       - The directory under the server root where the configuration will be found ("configs" is the default)
     * @param  newConfig       - The configuration to swich to
     * @param  waitForMessages - Any messages to wait (used to determine if the update is complete)
     * @throws Exception
     */
    public void reconfigureServerUsingExpandedConfiguration(String testName, String configDir, String newConfig, boolean resetMark, String... waitForMessages) throws Exception {

        ServerFileUtils serverFileUtils = new ServerFileUtils();
        String newServerCfg = serverFileUtils.expandAndBackupCfgFile(this, configDir + "/" + newConfig, testName);
        Log.info(c, "reconfigureServerUsingExpandedConfiguration", "Reconfiguring server to use new config: " + newConfig);
        if (resetMark) {
            setMarkToEndOfLog();
        }
        replaceServerConfiguration(newServerCfg);

        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
        waitForConfigUpdateInLogUsingMark(listAllInstalledAppsForValidation(), waitForMessages);
    }

    /**
     * Start the server and validate that the server was started:
     * prepares/cleans the server directory, then performs a clean start
     *
     * @throws Exception
     * @return           the output of the start command
     */
    public ProgramOutput startServer() throws Exception {
        return startServerAndValidate(DEFAULT_PRE_CLEAN, DEFAULT_CLEANSTART, validateApps);
    }

    /**
     * Start the server and validate that the server was started:
     * prepares/cleans the server directory, then performs a clean start
     *
     * @param  consoleLogFileName name that should be used for console log. It can be helpful
     *                                to have a console log file name that is related to (or describes) the test
     *                                case the server is used for.
     * @throws Exception
     * @return                    the output of the start command
     */
    public ProgramOutput startServer(String consoleLogFileName) throws Exception {
        this.consoleFileName = consoleLogFileName;
        return startServerAndValidate(DEFAULT_PRE_CLEAN, DEFAULT_CLEANSTART, validateApps);
    }

    /**
     * Start the server and validate that the server was started:
     * prepares/cleans the server directory, then starts the server
     *
     * @param  cleanStart if true, the server will be started with a clean start
     * @throws Exception
     * @return            the output of the start command
     */
    public ProgramOutput startServer(boolean cleanStart) throws Exception {
        return startServerAndValidate(DEFAULT_PRE_CLEAN, cleanStart, validateApps);
    }

    /**
     * Start the server and validate that the server was started:
     * prepares/cleans the server directory, then starts the server
     *
     * @param  consoleFileName name that should be used for console log. It can be helpful
     *                             to have a console log file name that is related to (or describes) the test
     *                             case the server is used for.
     * @param  cleanStart      if true, the server will be started with a clean start
     * @throws Exception
     * @return                 the output of the start command
     */
    public ProgramOutput startServer(String consoleFileNameLog, boolean cleanStart) throws Exception {
        this.consoleFileName = consoleFileNameLog;
        return startServerAndValidate(DEFAULT_PRE_CLEAN, cleanStart, validateApps);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  consoleFileName name that should be used for console log. It can be helpful
     *                             to have a console log file name that is related to (or describes) the test
     *                             case the server is used for.
     * @param  cleanStart      if true, the server will be started with a clean start
     * @param  preCleanServer  if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                 the output of the start command
     */
    public ProgramOutput startServer(String consoleFileNameLog, boolean cleanStart, boolean preCleanServer) throws Exception {
        this.consoleFileName = consoleFileNameLog;
        return startServerAndValidate(preCleanServer, cleanStart, validateApps);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  consoleFileNameLog name that should be used for console log. It can be helpful
     *                                to have a console log file name that is related to (or describes) the test
     *                                case the server is used for.
     * @param  cleanStart         if true, the server will be started with a clean start
     * @param  preCleanServer     if true, the server directory will be reset before
     *                                the server is started (reverted to vanilla backup).
     * @param  validateTimedExit  if true, the server will make sure that timedexit-1.0 is enabled
     * @throws Exception
     */
    public void startServer(String consoleFileNameLog,
                            boolean cleanStart,
                            boolean preCleanServer,
                            boolean validateTimedExit) throws Exception {
        this.consoleFileName = consoleFileNameLog;
        startServerAndValidate(preCleanServer, cleanStart, validateApps, false, validateTimedExit);
    }

    /**
     * Start the server, but expect server start to fail
     *
     * @param  consoleFileName name that should be used for console log. It can be helpful
     *                             to have a console log file name that is related to (or describes) the test
     *                             case the server is used for.
     * @param  cleanStart      if true, the server will be started with a clean start
     * @param  preCleanServer  if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                 the output of the start command
     */
    public ProgramOutput startServerExpectFailure(String consoleFileNameLog, boolean preClean, boolean cleanStart) throws Exception {
        this.consoleFileName = consoleFileNameLog;
        return startServerAndValidate(preClean, cleanStart, false, true, true);
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

    protected void checkPortsOpen(boolean retry) {
        ServerSocket socket = null;
        try {
            // Create unbounded socket
            socket = new ServerSocket();
            // This allows the socket to close and others to bind to it even if its in TIME_WAIT state
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(getHttpDefaultPort()));
        } catch (Exception ex) {
            Log.error(c, "checkPortsOpen", ex, "http default port is currently bound");
            printProcessHoldingPort(getHttpDefaultPort());
            if (retry) {
                Log.info(c, "checkPortsOpen", "Waiting 5 seconds and trying again");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    // Not a lot to do
                }
                // Do this out of the try block, even if we are interrupted we want to try once more
                checkPortsOpen(false);
            }
        } finally {
            if (null != socket) {
                try {
                    // With setReuseAddress set to true we should free up our socket and allow
                    // someone else to bind to it even if we are in TIME_WAIT state.
                    socket.close();
                } catch (IOException ioe) {
                    // not a lot to do
                }
            }
        }
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  cleanStart     if true, the server will be started with a clean start
     * @param  validateApps   if true, block until all of the registered apps have started
     * @param  preCleanServer if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                the output of the start command
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart, boolean validateApps) throws Exception {
        return startServerAndValidate(preClean, cleanStart, validateApps, false);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  cleanStart     if true, the server will be started with a clean start
     * @param  validateApps   if true, block until all of the registered apps have started
     * @param  preCleanServer if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                the output of the start command
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart, boolean validateApps, boolean expectStartFailure) throws Exception {
        return startServerAndValidate(preClean, cleanStart, validateApps, expectStartFailure, true);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  preClean           if true, the server directory will be reset before
     *                                the server is started (reverted to vanilla backup).
     * @param  cleanStart         if true, the server will be started with a clean start
     * @param  validateApps       if true, block until all of the registered apps have started
     * @param  expectStartFailure if true, a the server is not expected to start
     *                                due to a failure
     * @param  validateTimedExit  if true, the server will make sure that timedexit-1.0 is enabled
     * @throws Exception
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart,
                                                boolean validateApps, boolean expectStartFailure,
                                                boolean validateTimedExit) throws Exception {
        return startServerWithArgs(preClean, cleanStart, validateApps, expectStartFailure, "start", null, validateTimedExit);
    }

    public enum IncludeArg {
        MINIFY, ALL, USR, RUNNABLE, MINIFYRUNNABLE;

        public String getIncludeString() {
            if (this.equals(MINIFYRUNNABLE)) {
                return "--include=" + "minify,runnable";
            } else {
                return "--include=" + this.toString().toLowerCase();
            }
        }
    }

    public void packageServer(final IncludeArg include, final String otherPackageArgs, final String osFilter) throws Exception {
        final ArrayList<String> args = setArgsExtended(include, otherPackageArgs, osFilter);
        startServerWithArgs(true, false, false, false, "package", args, true);
    }

    public void packageServer(final IncludeArg include, final String osFilter) throws Exception {
        final ArrayList<String> args = setArgs(include, osFilter);
        startServerWithArgs(true, false, false, false, "package", args, true);
    }

    public void packageServerWithCleanStart(final IncludeArg include, final String osFilter) throws Exception {
        final ArrayList<String> args = setArgs(include, osFilter);
        startServerWithArgs(true, true, false, false, "package", args, true);
    }

    protected ArrayList<String> setArgs(final IncludeArg include, final String osFilter) {
        ArrayList<String> args = new ArrayList<String>();

        args.add(include.getIncludeString());

        if (osFilter != null) {
            args.add("--os=" + osFilter);
        }
        return args;
    }

    protected ArrayList<String> setArgsExtended(final IncludeArg include, final String otherPackageArgs, final String osFilter) {
        ArrayList<String> args = setArgs(include, osFilter);
        args.add(otherPackageArgs);

        return args;
    }

    public ProgramOutput startServerWithArgs(boolean preClean, boolean cleanStart,
                                             boolean validateApps, boolean expectStartFailure,
                                             String serverCmd, List<String> args,
                                             boolean validateTimedExit) throws Exception {
        final String method = "startServerWithArgs";
        Log.info(c, method, ">>> STARTING SERVER: " + this.getServerName());
        Log.info(c, method,
                 "Starting " + this.getServerName() + "; preClean=" + preClean + ", clean=" + cleanStart + ", validateApps=" + validateApps + ", expectStartFailure="
                            + expectStartFailure
                            + ", cmd=" + serverCmd + ", args=" + args);

        if (serverCleanupProblem) {
            throw new Exception("The server was not cleaned up on the previous test.");
        }

        //if we're (re-)starting then we must be untidy!
        this.isTidy = false;

        if (preClean) {
            // Tidy up any pre-existing logs
            Log.info(c, method, "Tidying logs");
            preStartServerLogsTidy();
            if (!newLogsOnStart) {
                clearLogMarks();
            }
        } else {
            if (!newLogsOnStart) {
                // We were asked not to pre-clean the logs, so given the
                // new behavior of not rolling messages.log & traces.log
                // in issue 4364, check if those exist, and if so, set
                // our marks to the end of those files.
                initializeAnyExistingMarks();
            }
        }

        final Properties envVars = new Properties();

        envVars.putAll(this.envVars);
        if (!envVars.isEmpty())
            Log.info(c, method, "Adding env vars: " + envVars);
        this.envVars.clear();

        if (this.additionalSystemProperties != null && this.additionalSystemProperties.size() > 0) {
            envVars.putAll(this.additionalSystemProperties);
        }
        checkPortsOpen(true);

        final String cmd = installRoot + "/bin/server";
        ArrayList<String> parametersList = new ArrayList<String>();
        boolean executeAsync = false;
        ServerDebugInfo debugInfo = new ServerDebugInfo();
        if ("start".equals(serverCmd) && debugInfo.startInDebugMode) {
            Log.info(c, method, "Setting up commands for debug for server = " + serverToUse + ".  Using port = " + debugInfo.debugPort);
            parametersList.add("debug");
            parametersList.add(serverToUse);
            envVars.setProperty("DEBUG_PORT", debugInfo.debugPort); // Not sure what this does.  It's not read by the FAT framework, for example. Was it meant to be usable for trace/debug?
            envVars.setProperty("WLP_DEBUG_ADDRESS", debugInfo.debugPort);
            // set server time out to 15 minutes to give time to connect. Timed exit likely kicks in after that, so
            // a larger value is worthless (and, since we multiply it by two later, will wrap if you use MAX_VALUE)
            serverStartTimeout = 15 * 60 * 60 * 1000;
            executeAsync = true;
        } else {
            parametersList.add(serverCmd);
            parametersList.add(serverToUse);
        }

        if (cleanStart) {
            parametersList.add("--clean");
        }

        if (args != null) {
            parametersList.addAll(args);
        }

        parametersList.addAll(extraArgs);

        //Setup the server logs assuming the default setting.
        messageAbsPath = logsRoot + messageFileName;
        consoleAbsPath = logsRoot + consoleFileName;
        traceAbsPath = logsRoot + traceFileName;

        Log.finer(c, method, "Starting server, messages will go to file " + messageAbsPath);

        final String[] parameters = parametersList.toArray(new String[] {});

        //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
        envVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            envVars.setProperty("WLP_USER_DIR", userDir);

        // Pick up global JVM args (forced by build properties)
        String JVM_ARGS = GLOBAL_JVM_ARGS;

        // Always set tmp dir.
        JVM_ARGS += " -Djava.io.tmpdir=" + TMP_DIR;

        // 207555: A number of FAT buckets establish a lot of secure connections and drain the entropy pool of /dev/random on Linux when
        // running Oracle/Sun JVMs - this results in buckets timing out as they wait for the entropy pool to be repopulated. Additionally,
        // from Java 9 onwards, IBM JDKs will also exhibit the same behaviour as it will start to use /dev/random by default.
        // The fix is thus to ensure we use the pseudorandom entropy pool (/dev/urandom) (which is also valid for Windows/zOS).
        JVM_ARGS += " -Djava.security.egd=file:///dev/urandom";

        JavaInfo info = JavaInfo.forServer(this);
        // Debug for a highly intermittent problem on IBM JVMs.
        // Unfortunately, this problem does not seem to happen when we enable this dump trace. We also can't proceed without getting
        // a system dump, so our only option is to enable this and hope the timing eventually works out.
        if (info.VENDOR == Vendor.IBM) {
            JVM_ARGS += " -Xdump:system+java+snap:events=throw+systhrow,filter=\"java/lang/ClassCastException#ServiceFactoryUse.<init>*\"";
            JVM_ARGS += " -Xdump:system+java+snap:events=throw+systhrow,filter=\"java/lang/ClassCastException#org/eclipse/osgi/internal/serviceregistry/ServiceFactoryUse.<init>*\"";
        }

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
        if (isJava2SecurityEnabled()) {
            RemoteFile f = getServerBootstrapPropertiesFile();
            addJava2SecurityPropertiesToBootstrapFile(f, GLOBAL_DEBUG_JAVA2SECURITY);
            String reason = GLOBAL_JAVA2SECURITY ? "GLOBAL_JAVA2SECURITY" : "GLOBAL_DEBUG_JAVA2SECURITY";
            Log.info(c, "startServerWithArgs", "Java 2 Security enabled for server " + getServerName() + " because " + reason + "=true");
        }

        Properties bootstrapProperties = getBootstrapProperties();
        String newLogsOnStartProperty = bootstrapProperties.getProperty(FileLogHolder.NEW_LOGS_ON_START_PROPERTY);
        if (newLogsOnStartProperty != null) {
            newLogsOnStart = Boolean.parseBoolean(newLogsOnStartProperty);
        }

        // Look for forced server trace..
        if (!GLOBAL_TRACE.isEmpty()) {
            RemoteFile f = getServerBootstrapPropertiesFile();
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
                        RemoteFile x = new RemoteFile(machine, serverRoot + "/" + fileName);
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

        Log.finer(c, method, "Starting Server with command: " + cmd);

        // Create a marker file to indicate that we're trying to start a server
        createServerMarkerFile();

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
            String workDir = new File(this.serverOutputRoot).getAbsolutePath();
            localMachine.executeAsync(cmd, parameters, workDir, envVars, redirect);
            Log.info(c, method, "Started server process in debug mode");
            output = null;
        } else {
            if (machine instanceof LocalMachine) {
                // Running the server start asynchronously because it appears that the start
                // process is hanging from time to time. We can probably remove this when we fix
                // the issue causing the process to hang.
                final BlockingQueue<ProgramOutput> outputQueue = new LinkedBlockingQueue<ProgramOutput>();

                Runnable execServerCmd = null;

                if (this.runAsAWindowService == false) {

                    execServerCmd = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                outputQueue.put(machine.execute(cmd, parameters, envVars));
                            } catch (Exception e) {
                                Log.info(c, method, "Exception while attempting to start a server: " + e.getMessage());
                            }
                        }

                    };
                } else {
                    final ArrayList<String> registerServiceParmList = makeParmList(parametersList, 0);
                    final ArrayList<String> startServiceParmList = makeParmList(parametersList, 1);

                    execServerCmd = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.info(c, method, "runAsAWindowService RegisterService parms: " + registerServiceParmList.toString());
                                final String[] registerServiceparameters = registerServiceParmList.toArray(new String[] {});
                                outputQueue.put(machine.execute(cmd, registerServiceparameters, envVars));

                                Log.info(c, method, "runAsAWindowService StartService    parms: " + startServiceParmList.toString());
                                final String[] startServiceparameters = startServiceParmList.toArray(new String[] {});
                                outputQueue.put(machine.execute(cmd, startServiceparameters, envVars));
                            } catch (Exception e) {
                                Log.info(c, method, "Exception while attempting to start a server: " + e.getMessage());
                            }
                        }
                    };
                }

                Thread t = new Thread(execServerCmd);

                t.start();
                // Way more than we really need to wait -- in normal circumstances this will return immediately as
                // we're just kicking off the server script.
                final int SCRIPT_TIMEOUT_IN_MINUTES = 5;
                output = outputQueue.poll(SCRIPT_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
                if (runAsAWindowService == true) {
                    // wait for "register" to complete first, and now wait for "start" to complete
                    output = outputQueue.poll(SCRIPT_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
                }

                if (output == null) {
                    // We didn't get a return value from the start script. This is pretty rare, but it's possible for the JVM to miss the output
                    // from the script and wait forever for a response. When this happens, we test to see if the server was actually started (it
                    // almost always should be.) If not, we try to start the server again. The chances of both calls failing at the JVM level are
                    // extraordinarily small.
                    Log.warning(c, "The process that runs the server script did not return. The server may or may not have actually started.");

                    // Call resetStarted() to try to determine whether the server is actually running or not.
                    int rc = resetStarted();
                    if (rc == 0) {
                        // The server is running, so proceed as if nothing went wrong.
                        output = new ProgramOutput(cmd, rc, "No output buffer available", "No error buffer available");
                    } else {
                        Log.info(c, method, "The server does not appear to be running. (rc=" + rc + "). Retrying server start now");
                        // If at first you don't succeed...
                        Thread tryAgain = new Thread(cmd);
                        tryAgain.start();
                        output = outputQueue.poll(SCRIPT_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
                        if (runAsAWindowService == true) {
                            // wait for "register" to complete first, and now wait for "start" to complete
                            output = outputQueue.poll(SCRIPT_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
                        }
                        if (output == null) {
                            Log.warning(c, "The second attempt to start the server also timed out. The server may or may not have actually started");
                            return new ProgramOutput(cmd, -1, "No response from script", "No response from script");
                        }

                    }

                }
            } else {
                // If the machine is remote we can execute the command directly.
                // RXA has its own timeouts in case the command "hangs".
                output = machine.execute(cmd, parameters, envVars);
            }
            int rc = output.getReturnCode();
            if (rc != 0) {
                if (expectStartFailure) {
                    Log.info(c, method, "EXPECTED: Server didn't start");
                    deleteServerMarkerFile();
                    Log.exiting(c, method);
                    return output;
                } else {
                    Log.info(c, method, "Response from script is: " + output.getStdout());
                    Log.info(c, method, "Return code from script is: " + rc);
                }
            }
        }

        // Validate the server and apps started - if they didn't, that
        // method will throw an appropriate exception

        if ("start".equals(serverCmd)) {
            validateServerStarted(output, validateApps, expectStartFailure, validateTimedExit);
            isStarted = true;
        }

        Log.exiting(c, method);
        return output;
    }

    /**
     * Clear any log marks and then set log marks to messages.log
     * or trace.log if those exist. See issue 4364
     *
     * @throws Exception
     */
    private void initializeAnyExistingMarks() throws Exception {
        final String method = "initializeAnyExistingMarks";

        // First we clear any marks - it's possible this
        // server was re-used, but stopped, so logs were taken
        // away. So if we simply clear our cache of log marks,
        // and then for any of the files that do exist, set the
        // log marks, then we should be at a good initial state
        clearLogMarks();

        if (defaultLogFileExists()) {
            Log.info(c, method, "Saving messages.log mark");
            setMarkToEndOfLog();
        }
        if (defaultTraceFileExists()) {
            Log.info(c, method, "Saving trace.log mark");
            setTraceMarkToEndOfDefaultTrace();
        }
        Log.info(c, method, "Saving marks");
        logMonitor.setOriginLogMarks();
    }

    private ArrayList<String> makeParmList(ArrayList<String> oldParms, int type) {
        // type 0 - Register, 1 - Start,  2 - Stop,  3 - Remove

        // parms list passed in is expected to describe the command parameters for the server.bat invocation.

        ArrayList<String> newParms = new ArrayList<String>(oldParms);

        // the first parameter should be either "start" or "stop"
        String p1 = newParms.get(0);

        if ((type == 0) || (type == 1)) {
            // The desire is for this method to build a registerWinService or a startWinService command
            if (p1.compareToIgnoreCase("start") != 0) {
                // first parameter was not "start", so we can not update this command for Windows service register or start
                // so return the parameters as is.
                return newParms;
            }

            if (type == 0) {
                // replace "start" parameter with "registerWinService", and get rid of other command line parameters.
                newParms.set(0, "registerWinService");
                int size = newParms.size();
                for (int i = size; i > 2; i--) {
                    newParms.remove(i - 1);
                }
            }

            if (type == 1) {
                // replace "start" parameter with "startWinService", and keep the other command line parameters.
                newParms.set(0, "startWinService");
            }

            return newParms;
        }

        if ((type == 2) || (type == 3)) {
            // The desire is for this method to build a stopWinService or a unregisterWinService command
            if (p1.compareToIgnoreCase("stop") != 0) {
                // first parameter was not "stop", so we can not update this command for Windows service stop or unregister
                // so return the parameters as is.
                return newParms;
            }

            if (type == 2) {
                // replace "stop" parameter with "stopWinService", and keep the other command line parameters.
                newParms.set(0, "stopWinService");
            }

            if (type == 3) {
                // replace "stop" parameter with "unregisterWinService", and get rid of other command line parameters.
                newParms.set(0, "unregisterWinService");
                int size = newParms.size();
                for (int i = size; i > 2; i--) {
                    newParms.remove(i - 1);
                }
            }

            return newParms;
        }
        return newParms;
    }

    private void addJava2SecurityPropertiesToBootstrapFile(RemoteFile f, boolean debug) throws Exception {
        java.io.OutputStream w = f.openForWriting(true);
        try {
            w.write("\n".getBytes());
            w.write("websphere.java.security".getBytes());
            w.write("\n".getBytes());
            w.write(("websphere.java.security.norethrow=" + debug).getBytes());
            w.write("\n".getBytes());
            if (debug) {
                w.write("websphere.java.security.unique=true".getBytes());
                w.write("\n".getBytes());
            }
            Log.info(c, "addJava2SecurityPropertiesToBootstrapFile", "Successfully updated bootstrap.properties file with Java 2 Security properties");
        } catch (Exception e) {
            Log.info(c, "addJava2SecurityPropertiesToBootstrapFile", "Caught exception updating bootstap.properties file with Java 2 Security properties, e: ", e.getMessage());
        }
        w.flush();
        w.close();
    }

    /**
     * Create a marker file for the server to indicate it is started.
     *
     * @throws IOException
     */
    protected void createServerMarkerFile() throws Exception {

        File outputFolder = new File(pathToAutoFVTOutputFolder);
        if (!outputFolder.exists())
            outputFolder.mkdirs();

        String path = pathToAutoFVTOutputFolder + getServerName() + ".mrk";
        LocalFile serverRunningFile = new LocalFile(path);
        File createFile = new File(serverRunningFile.getAbsolutePath());
        createFile.createNewFile();
        OutputStream os = serverRunningFile.openForWriting(true);
        os.write(0);
        os.flush();
        os.close();
    }

    /**
     * Delete a marker file for the server (after stopped).
     *
     * @throws IOException
     */
    protected void deleteServerMarkerFile() throws Exception {

        String path = pathToAutoFVTOutputFolder + getServerName() + ".mrk";
        LocalFile serverRunningFile = new LocalFile(path);
        File deleteFile = new File(serverRunningFile.getAbsolutePath());
        if (deleteFile.exists()) {
            deleteFile.delete();
        }
    }

    public void setAppStartTimeout(int timeout) {
        APP_START_TIMEOUT = timeout;
    }

    public int getAppStartTimeout() {
        return APP_START_TIMEOUT;
    }

    public void setConfigUpdateTimeout(int timeout) {
        LOG_SEARCH_TIMEOUT_CONFIG_UPDATE = timeout;
    }

    public int getConfigUpdateTimeout() {
        return LOG_SEARCH_TIMEOUT_CONFIG_UPDATE;
    }

    public void validateAppLoaded(String appName) throws Exception {
        String exceptionText = validateAppsLoaded(Collections.singleton(appName), APP_START_TIMEOUT, getDefaultLogFile());
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

        String exceptionText = validateAppsLoaded(installedApplications, APP_START_TIMEOUT, outputFile);
        if (exceptionText != null) {
            throw new TopologyException(exceptionText);
        }
    }

    protected String validateAppsLoaded(Set<String> appList, int timeout, RemoteFile outputFile) throws Exception {
        // At time of writing, timeout argument was being ignored. Preserve that for now...
        timeout = APP_START_TIMEOUT;
        return validateAppsLoaded(appList, timeout, 2 * timeout, outputFile);
    }

    /**
     * We are adding a "soft failure" mode, in an attempt to compensate for the fact that the Virtual Machines
     * we're now running regression tests on have "bursty" performance and may introduce substantial delays with
     * no warning, making the originally coded times fragile.
     *
     * With this change, if intendedTimeout is exceeded, we report this to the SOE server
     * but do not consider it a test failure. Only if extendedTimeout is exceeded will we return a not-found indication.
     *
     * @param  regexp          a regular expression to search for
     * @param  intendedTimeout a timeout, in milliseconds, within which string was expected to occur
     * @param  extendedTimeout a timeout, in milliseconds, within which string may acceptably occur
     * @param  outputFile      file to check
     * @return                 line that matched the regexp, or null to indicate not found within acceptable (extended) timeout
     */
    protected String validateAppsLoaded(Set<String> appList, int intendedTimeout, int extendedTimeout, RemoteFile outputFile) throws Exception {
        final String method = "validateAppsLoaded";

        final long startTime = System.currentTimeMillis();
        final long finalTime = startTime + extendedTimeout;
        long slowTime = startTime + intendedTimeout;
        try {
            long offset = 0;
            final List<String> regexpList = Collections.singletonList("CWWKZ");
            Map<String, Pattern> unstartedApps = new HashMap<String, Pattern>();
            for (String appName : appList) {
                // When reading the log file with some encodings the log entry has a new line character in,
                // for example when reading the log files on Chinese CD open stream this won't match unless
                // we force .* to include the new line character which we do by switching the option on by
                // adding the (?s) part to the start of the pattern.
                unstartedApps.put(appName, Pattern.compile("(?s).*\\b" + appName + "\\b.*"));
            }
            Map<String, List<String>> failedApps = new HashMap<String, List<String>>();
            boolean timedOut = false;

            Log.finer(c, method, "Searching for app manager messages in " + outputFile.getAbsolutePath());
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
                // Trigger a serverDump: this will contain the output of server introspectors, which can
                // help pinpoint service resolution issues or missing dependencies.
                serverDump();

                // If apps failed to start, try to make sure the port opened so we correctly
                // flag a port issue as the culprit.
                validatePortStarted();

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
        // NO_APPLICATION_HANDLER=CWWKZ0005E: The application {0} cannot start because the server is not configured to handle applications of type {1}.
        CWWKZ0005E(Action.ADD_FAILURE_FOR_APP_NAME_TO_FAILED_APPS),
        // CANNOT_CREATE_DIRECTORY=CWWKZ0006E: The server could not create a download location at {0} for the {1} application.
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
        // MONITOR_DIR_CLEANUP_FAIL=CWWKZ0060E: The server could not clean up the old monitored directory at {0}.
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
            if (line.startsWith("{")) {
                // JSON format
                JsonReader reader = Json.createReader(new StringReader(line));
                JsonObject jsonObj = reader.readObject();
                reader.close();
                try {
                    line = jsonObj.getString("message");
                } catch (NullPointerException npe) {
                    Log.error(c, method, npe, "JSON does not contain \"message\" key. line=" + line);
                    continue;
                }
            } else {
                // Regular format
                line = line.substring(line.indexOf("CWWKZ"));
            }
            Log.finer(c, method, "line is " + line);
            String[] tokens = line.split(":", 2);
            Log.finer(c, method, "tokens are (" + tokens[0] + ") (" + tokens[1] + ")");
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
            Log.finer(c, method, "looking for app " + entry.getKey() + " in " + tokens[1]);
            if (entry.getValue().matcher(tokens[1]).matches()) {
                Log.finer(c, method, "matched app " + entry.getKey());
                return entry.getKey();
            }
        }
        Log.info(c, method, "no matches for apps found in " + tokens[1]);
        return null;
    }

    protected void validateServerStarted(ProgramOutput output, boolean validateApps,
                                         boolean expectStartFailure, boolean validateTimedExit) throws Exception {
        final String method = "validateServerStarted";

        final String START_MESSAGE_CODE = "CWWKF0011I";

        boolean serverStarted = false;

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

        Log.info(c, method, "Waiting up to " + (serverStartTimeout / 1000)
                            + " seconds for server confirmation:  "
                            + START_MESSAGE_CODE.toString() + " to be found in " + consoleAbsPath);

        RemoteFile messagesLog = new RemoteFile(machine, messageAbsPath);
        RemoteFile consoleLog = getConsoleLogFile();

        try {
            RemoteFile f = getServerBootstrapPropertiesFile();
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

            // If log.level is set to off, look in messages.log for server.start verification
            isStartedConsoleLogLevelOff = "off".equalsIgnoreCase(props.getProperty("com.ibm.ws.logging.console.log.level"));
            String startMessage = waitForStringInLog(START_MESSAGE_CODE, serverStartTimeout,
                                                     isStartedConsoleLogLevelOff ? messagesLog : consoleLog);
            serverStarted = (startMessage != null);
            // If the server started successfully, we're started
            // (but the opposite isn't true since the server could already have been running)
            if (serverStarted) {
                isStarted = true;
            }
        } catch (Exception e) {
            Log.error(c, method, e, "Exception thrown confirming server started in " + consoleAbsPath);
            postStopServerArchive();
            throw e;
        }

        if (!!!serverStarted) {
            if (expectStartFailure) {
                Log.info(c, method, "EXPECTED: Server didn't start");
                return;
            } else {
                Log.info(c, method, "Server hasn't started successfully");
                // Didn't find kernel start up message so exception
                String exMessage = "Start Server error: The server did not show kernel had started.";
                String errMessage = "Server failed to start.";
                if (output != null) {
                    exMessage += " The server did not show kernel had started. The standard error was: "
                                 + output.getStderr() + ".\n Standard out was: "
                                 + output.getStdout() + ".\n Return code was: " + output.getReturnCode()
                                 + ".\n Message was not found in " + consoleLog.getAbsolutePath();
                    errMessage += "   Command used to start server was: " + output.getCommand();
                }
                TopologyException serverStartException = new TopologyException(exMessage);
                Log.error(c, method, serverStartException, errMessage);
                // since a startup error was not expected, trigger a dump to help with debugging
                serverDump();
                postStopServerArchive();
                throw serverStartException;
            }
        } else if (expectStartFailure) {
            Log.info(c, method, "ERROR: The server started successfully, but a start failure was expected.");
            // Didn't find kernel start up message so exception
            TopologyException serverStartException = new TopologyException("The server started successully, but a failure was expected");
            Log.error(c, method, serverStartException, "ERROR: The server started successfully, but a start failure was expected.");
            throw serverStartException;
        }

        // App validation needs the info messages in messages.log
        if (!messagesLog.exists()) {
            // NOTE: The HPEL FAT bucket has a strange mechanism to create messages.log for test purposes, which may get messed up
            Log.info(c, method, "WARNING: messages.log does not exist-- trying app verification step with console.log");
            messagesLog = consoleLog;
        }

        if (validateTimedExit) {
            validateTimedExitEnabled(messagesLog);
        }
        if (validateApps) {
            validateAppsLoaded(messagesLog);
        }
        FeatureDependencyProcessor.validateTestedFeatures(this, messagesLog);
    }

    protected void validateTimedExitEnabled(RemoteFile messagesLog) throws Exception {
        final String method = "validateTimedExitEnabled";
        // 20 second timeout
        final long TIMEOUT = 20 * 1000;
        final String TIMED_EXIT_ENABLED = "TE9900A";

        List<String> message = findStringsInLogs(TIMED_EXIT_ENABLED, messagesLog);

        if (message == null || message.isEmpty()) {
            // It's fairly unusual, but it's technically possible that timed exit is enabled and the message hasn't been issued yet.
            // We use this backup rather than replacing the above findStringsInLogs because it's possible for the mark to be set to a location
            // after the timed exit message
            String takeTwo = waitForStringInLog(TIMED_EXIT_ENABLED, TIMEOUT, messagesLog);
            if (takeTwo != null) {
                // Everything is OK now, log a message indicating that we got here
                Log.info(c, method, "Found the timed exit string (late arrival)");
                return;
            }
            String errorMessage = "The necessary feature timedexit-1.0 was not enabled. " +
                                  "Please include fatTestPorts.xml or fatTestCommon.xml in the server.xml for server " + serverToUse + ".";
            Log.info(c, method, "ERROR: " + errorMessage);

            TopologyException serverStartException = new TopologyException(errorMessage);
            Log.error(c, method, serverStartException, "ERROR: " + errorMessage);

            throw serverStartException;
        }
    }

    protected void validatePortStarted() throws Exception {
        final String method = "validatePortStarted";

        // App validation needs the info messages in messages.log
        RemoteFile messagesLog = new RemoteFile(machine, messageAbsPath);
        if (!messagesLog.exists()) {
            String message = waitForStringInLog("CWWKO0219I", serverStartTimeout, messagesLog);
            if (message == null || message.isEmpty()) {
                String errorMessage = "A listening http or https port did not start. Please look at messages.log to figure out why...";
                Log.info(c, method, "ERROR: " + errorMessage);

                TopologyException serverStartException = new TopologyException(errorMessage);
                Log.error(c, method, serverStartException, "ERROR: " + errorMessage);
                throw serverStartException;
            }
        }
    }

    public ProgramOutput stopServer(String... expectedFailuresRegExps) throws Exception {
        return this.stopServer(true, expectedFailuresRegExps);
    }

    public static void stopMultipleServers(Collection<LibertyServer> servers) throws Exception {
        Exception firstException = null;
        boolean exceptionThrown = false;
        if (servers != null) {
            for (LibertyServer server : servers) {
                try {
                    server.stopServer();
                } catch (Exception e) {
                    // catch the first exception and re-throw after attempting to stop all servers
                    if (!exceptionThrown) {
                        firstException = e;
                        exceptionThrown = true;
                    }
                }
            }
        }
        if (exceptionThrown) {
            throw new RuntimeException("Exceptions occured while stopping " + servers.size() + " servers, re-throwing the first exception", firstException);
        }
    }

    public ProgramOutput stopServer(boolean postStopServerArchive, String... expectedFailuresRegExps) throws Exception {
        return this.stopServer(postStopServerArchive, false, expectedFailuresRegExps);
    }

    public ScheduledFuture<?> dumpServerOnSchedule(final String destination,
                                                   final int times,
                                                   long initialDelay,
                                                   long delay,
                                                   TimeUnit unit) throws Exception {
        final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ignoredErrors.add("CWWKE0059W"); // write error if server is stopping while dump is processing
        return ses.scheduleWithFixedDelay(new Runnable() {
            private final AtomicInteger remainingInvocations = new AtomicInteger(times);

            @Override
            public void run() {
                try {
                    dumpServer(destination);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ses.shutdown();
                }
                if (remainingInvocations.getAndDecrement() == 0) {
                    ses.shutdown();
                }
            }
        }, initialDelay, delay, unit);
    }

    public LocalFile dumpServer(final String destination) throws Exception {
        LocalFile lf = null;
        final String method = "dumpServer";
        try {
            Log.info(c, method, "<<< DUMPING SERVER: " + this.getServerName());

            if (!isStarted) {
                Log.info(c, method, "Server " + serverToUse + " is not running (stop called previously).");
                return null;
            }

            String cmd = installRoot + "/bin/server";
            String[] parameters = new String[] { "dump", serverToUse };

            //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
            Properties envVars = new Properties();
            envVars.setProperty("JAVA_HOME", machineJava);
            if (customUserDir)
                envVars.setProperty("WLP_USER_DIR", userDir);
            Log.finer(c, method, "Using additional env props: " + envVars.toString());

            final ProgramOutput output = machine.execute(cmd, parameters, envVars);

            String stdout = output.getStdout();
            Log.info(c, method, "Dump Server Response: " + stdout);
            if (output.getReturnCode() != 0)
                Log.info(c, method, "Return code from script is: " + output.getReturnCode());

            final String regex = "Server .+ dump complete in (.+)\\.";
            final Matcher m = Pattern.compile(regex).matcher(stdout);
            if (m.find()) {
                final String dumpPath = m.group(1);
                Log.info(c, method, "Dump file on server: " + dumpPath);
                if (dumpPath != null) {
                    final RemoteFile dumpFile = new RemoteFile(machine, dumpPath);
                    Log.info(c, method, "Copying RemoteFile " + dumpFile + " to " + pathToAutoFVTTestFiles + "/tmp/" + destination);
                    lf = copyFileToTempDir(dumpFile, destination);
                }
            } else {
                Log.info(c, method, "Matcher failed.");
            }

        } finally {
            Log.info(c, method, "<<< SERVER DUMP COMPLETE: " + this.getServerName() + " , localFile = " + lf);
        }

        return lf;
    }

    /**
     * Stops the server and checks for any warnings or errors that appeared in logs.
     * If warnings/errors are found, an exception will be thrown after the server stops.
     *
     * @param  postStopServerArchive true to collect server log files after the server is stopped; false to skip this step (sometimes, FATs back up log files on their own, so this
     *                                   would be redundant)
     * @param  forceStop             Force the server to stop, skipping the quiesce (default/usual value should be false)
     * @param  regIgnore             A list of reg expressions corresponding to warnings or errors that should be ignored.
     *                                   If regIgnore is null, logs will not be checked for warnings/errors
     * @return                       the output of the stop command
     * @throws Exception             if the stop operation fails or there are warnings/errors found in server
     *                                   logs that were not in the list of ignored warnings/errors.
     */
    public ProgramOutput stopServer(boolean postStopServerArchive, boolean forceStop, String... expectedFailuresRegExps) throws Exception {

        ProgramOutput output = null;
        boolean commandPortEnabled = true;
        try {
            final String method = "stopServer";
            Log.info(c, method, "<<< STOPPING SERVER: " + this.getServerName());

            if (!isStarted) {
                Log.info(c, method, "Server " + serverToUse + " is not running (stop called previously).");
                postStopServerArchive = false;
                return output;
            }

            String cmd = installRoot + "/bin/server";
            String[] parameters;
            if (forceStop) {
                parameters = new String[] { "stop", serverToUse, "--force" };
            } else {
                parameters = new String[] { "stop", serverToUse };
            }

            //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
            Properties envVars = new Properties();
            envVars.setProperty("JAVA_HOME", machineJava);
            if (customUserDir)
                envVars.setProperty("WLP_USER_DIR", userDir);
            Log.finer(c, method, "Using additional env props: " + envVars.toString());

            if (runAsAWindowService == false) {
                output = machine.execute(cmd, parameters, envVars);
            } else {
                ArrayList<String> parametersList = new ArrayList<String>();
                for (int i = 0; i < parameters.length; i++) {
                    parametersList.add(parameters[i]);
                }
                ArrayList<String> stopServiceParmList = makeParmList(parametersList, 2);
                ArrayList<String> removeServiceParmList = makeParmList(parametersList, 3);
                String[] stopServiceParameters = stopServiceParmList.toArray(new String[] {});
                String[] removeServiceParameters = removeServiceParmList.toArray(new String[] {});

                output = machine.execute(cmd, stopServiceParameters, envVars);
                output = machine.execute(cmd, removeServiceParameters, envVars);

            }

            String stdout = output.getStdout();
            Log.info(c, method, "Stop Server Response: " + stdout);
            if (output.getReturnCode() != 0)
                Log.info(c, method, "Return code from script is: " + output.getReturnCode());

            isStarted = false;
            if (stdout.contains("is not running")) {
                return output;
            }

            if (stdout.contains("command port is disabled")) {
                // Mark that the command port is not enabled
                commandPortEnabled = false;
                throw new RuntimeException("Cannot stop server because command port is disabled.");
            }
            RemoteFile log = isStartedConsoleLogLevelOff ? new RemoteFile(machine, messageAbsPath) : getConsoleLogFile();
            // Actually waits for the stop message
            waitForStringInLog("CWWKE0036I:", SERVER_STOP_TIMEOUT, log);

            int serverStopRC = output.getReturnCode();
            if (serverStopRC != 0) {
                throw new RuntimeException("Server stop failed with RC " + serverStopRC +
                                           ".\nStdout:\n" + output.getStdout() +
                                           "\nStderr:\n" + output.getStderr());
            }

            // Now verify that the server is truly stopped by checking server status from the command line.
            // This checks to see if the server lock file (<server>/workarea/.sLock) is unlocked.
            ProgramOutput serverStatusOutput = executeServerScript("status", null);
            switch (serverStatusOutput.getReturnCode()) {
                case 0:
                    Log.warning(c, method + " Server is still running - or server lock file is still locked.");
                    break;
                case 1:
                    Log.info(c, method, "Server " + getServerName() + " stopped successfully");
                    break;
                case 2:
                    Log.warning(c, method + " Unknown server - directory deleted? " + serverToUse);
                    break;
                case 5:
                    Log.warning(c, method + " Unable to detect server status - workarea directory deleted? " + serverToUse);
                    break;
                default:
                    Log.warning(c, method + " Unexpected failure occurred while checking server status");

            }

            this.isTidy = true;

            checkLogsForErrorsAndWarnings(expectedFailuresRegExps);
        } finally {
            // Issue 4363: If !newLogsOnStart, no longer reset the log offsets because if the
            // server starts again, logs will roll into the existing logs. We also don't clear
            // the message counters because the use of those counters uses searchForMessages
            // which doesn't take into account marks.
            if (newLogsOnStart && commandPortEnabled) {
                // If the command port is enabled we should reset the log offsets
                // as we will get new logs on the next start. However, if the
                // command port isn't disabled, we won't have shut down the
                // server, so we don't need to reset the log marks
                resetLogOffsets();
                clearMessageCounters();
            }

            if (isJava2SecurityEnabled()) {
                try {
                    new ACEScanner(this).run();
                } catch (Throwable t) {
                    LOG.logp(Level.WARNING, c.getName(), "stopServer", "Caught exception trying to scan for AccessControlExceptions", t);
                }
            }
            if (postStopServerArchive)
                postStopServerArchive();
            // Delete marker for stopped server
            // deleteServerMarkerFile();
        }

        return output;
    }

    /**
     * Checks server logs for any lines containing errors or warnings that
     * do not match any regular expressions provided in regIgnore.
     *
     * @param  regIgnore A list of regex strings for errors/warnings that
     *                       may be safely ignored.
     * @return           A list of lines containing errors/warnings from server logs
     */
    protected void checkLogsForErrorsAndWarnings(String... regIgnore) throws Exception {
        final String method = "checkLogsForErrorsAndWarnings";

        // Get all warnings and errors in logs - default to an empty list
        List<String> errorsInLogs = new ArrayList<String>();
        try {
            errorsInLogs = this.findStringsInLogs("^.*[EW] .*\\d{4}[EW]:.*$");
            if (!errorsInLogs.isEmpty()) {
                // There were unexpected errors in logs, print them
                // and set an exception to return
                StringBuffer sb = new StringBuffer("Errors/warnings were found in server ");
                sb.append(getServerName());
                sb.append(" logs:");
                for (String errorInLog : errorsInLogs) {
                    sb.append("\n <br>");
                    sb.append(errorInLog);
                    Log.info(c, method, "Error/warning found in log ORIGINALLY: " + errorInLog);
                }
            }
        } catch (Exception e) {
            Log.warning(getClass(), "While checking for log errors and warnings, findStringsInLogs caused an exception: " + e.getMessage());
        }

        // Compile set of regex's using input list and universal ignore list
        List<Pattern> ignorePatterns = new ArrayList<Pattern>();
        if (regIgnore != null && regIgnore.length != 0) {
            for (String ignoreRegEx : regIgnore) {
                ignorePatterns.add(Pattern.compile(ignoreRegEx));
            }
        }
        // Add the regexes added via the instance method
        for (String regex : ignoredErrors) {
            ignorePatterns.add(Pattern.compile(regex));
        }
        ignoredErrors.clear();

        // Add the global fixed list of regexes entries.
        if (fixedIgnoreErrorsList != null) {
            for (String regex : fixedIgnoreErrorsList) {
                ignorePatterns.add(Pattern.compile(regex));
            }
        }

        // Remove any ignored warnings or patterns
        for (Pattern ignorePattern : ignorePatterns) {
            Iterator<String> iter = errorsInLogs.iterator();
            while (iter.hasNext()) {
                if (ignorePattern.matcher(iter.next()).find()) {
                    // this is an ignored warning/error, remove it from list
                    iter.remove();
                    Log.finer(c, method, "Error being removed is " + ignorePattern);
                }
            }
        }

        Exception ex = null;

        if (!errorsInLogs.isEmpty()) {
            // Check which errors were j2sec related
            Pattern j2secPattern = Pattern.compile("CWWKE09(21W|12W|13E|14W|15W|16W)");
            List<String> j2secIssues = new ArrayList<String>();
            for (String errorInLog : errorsInLogs)
                if (j2secPattern.matcher(errorInLog).find()) {
                    j2secIssues.add(errorInLog);
                }

            // There were unexpected errors in logs, print them
            // and set an exception to return
            StringBuilder sb = new StringBuilder("Errors/warnings were found in server ");
            sb.append(getServerName());
            sb.append(" logs:");
            if (!j2secIssues.isEmpty()) {
                // When things go wrong with j2sec, a LOT of things tend to go wrong, so just leave a pointer
                // to the nicely formatted ACE report instead of putting every single issue in the exception msg
                sb.append("\n <br>");
                sb.append("Java 2 security issues were found in logs");
                boolean showJ2secErrors = true;
                // If an ACE-report will be generated....
                if (isJava2SecurityEnabled()) {
                    sb.append("  See autoFVT/ACE-report-*.log for details.");
                    if (j2secIssues.size() > 25)
                        showJ2secErrors = false;
                }
                if (showJ2secErrors) {
                    for (String j2secIssue : j2secIssues) {
                        sb.append("\n <br>");
                        sb.append(j2secIssue);
                    }
                }
                errorsInLogs.removeAll(j2secIssues);
            }
            for (String errorInLog : errorsInLogs) {
                sb.append("\n <br>");
                sb.append(errorInLog);
                Log.info(c, method, "Error/warning found: " + errorInLog);
            }
            ex = new Exception(sb.toString());
        }

        if (ex == null)
            Log.info(c, method, "No unexpected errors or warnings found in server logs.");
        else
            throw ex;
    }

    protected void clearMessageCounters() {
        //this is because we will be getting a new log file
        stopApplicationMessages.set(0);
        startApplicationMessages.set(0);
    }

    public void restartServer() throws Exception {
        stopServer();
        startServer();
    }

    /**
     * This method is protected as from now on the Custom
     * JUnit runner will call this method at the end of the testing
     * in order to ensure all server's are tidied.
     */
    protected void postTestTidy() throws Exception {
        if (!isTidy) {
            stopServer();
        }
        isTidy = true;
    }

    /**
     * This method is intended to be called from LibertyServerFactory
     * only when a cached server instance which has previously been
     * used and stopped is to be re-used.
     */
    void unTidy() {
        isTidy = false;
        needsPostTestRecover = true;
    }

    boolean isTidy() {
        return isTidy;
    }

    boolean needsPostTestRecover() {
        return needsPostTestRecover;
    }

    public void setNeedsPostRecover(boolean b) {
        needsPostTestRecover = b;
    }

    /**
     * This method is used to tidy away the server logs at the start.
     */
    protected void preStartServerLogsTidy() throws Exception {
        //should be .../liberty/usr/servers/<server>/logs
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getServerRoot() + "/logs");

        // Look for javacore/heapdump/snap. These are collected by stop/archive. We don't need
        // to collect them in the archive of every subsequent server run.
        List<String> files = listLibertyServerRoot(null, null);
        for (String name : files) {
            if (name.startsWith("javacore*") || name.startsWith("heapdump*") || name.startsWith("Snap*") || name.startsWith(serverToUse + ".dump")) {
                deleteFileFromLibertyInstallRoot(name);
            }
        }
    }

    /**
     * This method is used to archive server logs after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     */
    public void postStopServerArchive() throws Exception {
        final String method = "postStopServerArchive";
        Log.entering(c, method);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        Date d = new Date(System.currentTimeMillis());

        String runLevel = RepeatTestFilter.getRepeatActionsAsString();

        String logDirectoryName = "";
        if (runLevel == null || runLevel.isEmpty()) {
            logDirectoryName = pathToAutoFVTOutputServersFolder + "/" + serverToUse + "-" + sdf.format(d);
        } else {
            logDirectoryName = pathToAutoFVTOutputServersFolder + "/" + serverToUse + "-" + runLevel + "-" + sdf.format(d);
        }
        LocalFile logFolder = new LocalFile(logDirectoryName);
        RemoteFile serverFolder = new RemoteFile(machine, serverRoot);

        runJextract(serverFolder);

        // Copy the log files: try to move them instead if we can
        recursivelyCopyDirectory(serverFolder, logFolder, true, true, true);

        deleteServerMarkerFile();

        Log.exiting(c, method);
    }

    protected void runJextract(RemoteFile serverFolder) throws Exception {
        RemoteFile[] files = serverFolder.list(false);
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
                    ProgramOutput output = machine.execute(cmd, parms, serverFolder.getAbsolutePath(), envVars);
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
                if (l.equals(OSGI_DIR_NAME) || l.startsWith(".s")) {
                    // skip the osgi framework cache, and runtime artifacts: too big / too racy
                    Log.finest(c, "recursivelyCopyDirectory", "Skipping workarea element " + l);
                    continue;
                }
            }

            if (remoteDirectory.getName().equals("messaging")) {
                Log.finest(c, "recursivelyCopyDirectory", "Skipping message store element " + l);
                continue;
            }

            RemoteFile toCopy = new RemoteFile(machine, remoteDirectory, l);
            LocalFile toReceive = new LocalFile(destination, l);
            String absPath = toCopy.getAbsolutePath();
            Log.finest(c, "recursivelyCopyDirectory", "Getting: " + absPath);

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
                            //If we're only getting logs, skip jars, wars, ears, zips, unless they are server dump zips
                            || (absPath.endsWith(".zip") && !toCopy.getName().contains(serverToUse + ".dump")))) {
                        Log.finest(c, "recursivelyCopyDirectory", "Skipping: " + absPath);
                        continue;
                    }

                    // We're only going to attempt to move log files. Because of ffdc log checking, we
                    // can't move those. But we should move other log files..
                    boolean isLog = (absPath.contains("logs") && !absPath.contains("ffdc"))
                                    || toCopy.getName().contains("javacore")
                                    || toCopy.getName().contains("heapdump")
                                    || toCopy.getName().contains("Snap")
                                    || toCopy.getName().contains(serverToUse + ".dump");

                    boolean isConfigBackup = absPath.contains("serverConfigBackups");

                    if (moveFile && (isLog || isConfigBackup)) {
                        boolean copied = false;

                        // If we're local, try to rename the file instead..
                        if (machine.isLocal() && toCopy.rename(toReceive)) {
                            copied = true; // well, we moved it, but it counts.
                            Log.finest(c, "recursivelyCopyDirectory", "MOVE: " + l + " to " + toReceive.getAbsolutePath());
                        }

                        if (!copied && toReceive.copyFromSource(toCopy)) {
                            // copy was successful, clean up the source log
                            toCopy.delete();
                            Log.finest(c, "recursivelyCopyDirectory", "MOVE: " + l + " to " + toReceive.getAbsolutePath());
                        }
                    } else {
                        toReceive.copyFromSource(toCopy);
                        Log.finest(c, "recursivelyCopyDirectory", "COPY: " + l + " to " + toReceive.getAbsolutePath());
                    }
                } catch (Exception e) {
                    Log.finest(c, "recursivelyCopyDirectory", "unable to copy or move " + l + " to " + toReceive.getAbsolutePath());
                    // Ignore on request and carry on copying the rest of the files
                    if (!ignoreFailures) {
                        throw e;
                    }
                }
            }

        }
    }

    /**
     * This method will copy a file from the server root into the AutoFVT {@link #pathToAutoFVTTestFiles}/tmp folder.
     * If you are copying a directory and some of the files cannot be copied due to an error then these errors will
     * be ignored, this can happen if the server is still active and the files are locked by another process.
     *
     * If copying a file the destination will be overwritten.
     *
     * @param  pathInServerRoot The path to the file or directory in the server root, must not start with a "/"
     * @param  destination      The place within the temp folder to store this file, must not start with a "/"
     * @return                  the LocalFile of the copied RemoteFile
     * @throws Exception
     */
    public LocalFile copyFileToTempDir(String pathInServerRoot, String destination) throws Exception {
        return copyFileToTempDir(new RemoteFile(machine, serverRoot + "/" + pathInServerRoot), destination);
    }

    /**
     * This method will copy a file from the install root into the AutoFVT {@link #pathToAutoFVTTestFiles}/tmp folder.
     * If you are copying a directory and some of the files cannot be copied due to an error then these errors will
     * be ignored, this can happen if the server is still active and the files are locked by another process.
     *
     * If copying a file the destination will be overwritten.
     *
     * @param  pathInInstallRoot The path to the file or directory in the install root, must not start with a "/"
     * @param  destination       The place within the temp folder to store this file, must not start with a "/"
     * @return                   the LocalFile of the copied RemoteFile
     * @throws Exception
     */
    public LocalFile copyInstallRootFileToTempDir(String pathInInstallRoot, String destination) throws Exception {
        return copyFileToTempDir(new RemoteFile(machine, installRoot + "/" + pathInInstallRoot), destination);
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

    public String getServerRoot() {
        return serverRoot;
    }

    public String getOsgiWorkAreaRoot() {
        return serverRoot + "/workarea" + "/" + OSGI_DIR_NAME;
    }

    public String getServerSharedPath() {
        return serverRoot + "/../../shared/";
    }

    /**
     * Get the collective dir under the server resources dir. For instance,
     * this is where the collective trust stores are located.
     *
     * @return the path
     */
    public String getCollectiveResourcesPath() {
        return serverRoot + "/resources/collective/";
    }

    public void setServerRoot(String serverRoot) {
        this.serverRoot = serverRoot;
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

    // Note: This method does not use a tmp file if the destination file already exists!  See comments
    // and logic of copyFileIntoLiberty().  Use setServerConfigurationFile() for updating server.xml
    // if the file pre-exists so that a tmp file / move is performed vs a copy.  This helps
    // avoid the scenario of parsing a partial config file which results in a XML Parsing error.
    public void copyFileToLibertyServerRootUsingTmp(String path, String relPathTolocalFile) throws Exception {
        LocalFile localFileToCopy = new LocalFile(LibertyServerUtils.makeJavaCompatible(relPathTolocalFile, machine));
        LibertyFileManager.copyFileIntoLiberty(machine, path, localFileToCopy.getName(), relPathTolocalFile, false, serverRoot);
    }

    public void copyApplicationToServer(String appName) throws Exception {
        copyApplicationToServer("publish/files/apps", appName);
    }

    public void copyApplicationToServer(String pathToApp, String appName) throws Exception {
        copyFileToLibertyServerRootUsingTmp(serverRoot + "/apps", (pathToApp + "/" + appName));
    }

    public void copyFileToLibertyServerRoot(String fileName) throws Exception {
        copyFileToLibertyServerRootUsingTmp(serverRoot, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    public void copyFileToLibertyServerRoot(String extendedPath, String fileName) throws Exception {
        copyFileToLibertyServerRootUsingTmp(serverRoot + "/" + extendedPath, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    /**
     * Copies a file into the ${server.config.dir} of a Liberty server.
     *
     * @param fromDir  The directory of the file to copy.
     * @param toDir    Any extra path beyond ${server.config.dir} for the destination.
     *                     For example, for a destination of ${server.config.dir}/test/ you would use toServerDir=test
     * @param fileName The name of the file to copy. The file name will be unchanged form source to dest
     */
    public void copyFileToLibertyServerRoot(String fromDir, String toDir, String fileName) throws Exception {
        if (toDir == null)
            toDir = "";
        copyFileToLibertyServerRootUsingTmp(serverRoot + "/" + toDir, (fromDir + "/" + fileName));
    }

    public void renameLibertyServerRootFile(String oldFileName, String newFileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, serverRoot + "/" + oldFileName, serverRoot + "/" + newFileName);
    }

    public void renameLibertyInstallRootFile(String oldFileName, String newFileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, installRoot + "/" + oldFileName, installRoot + "/" + newFileName);
    }

    public RemoteFile getFileFromLibertyInstallRoot(String filePath) throws Exception {
        final String method = "getFileFromLibertyInstallRoot";
        Log.entering(c, method);
        return getFileFromLiberty(installRoot + "/" + filePath);
    }

    public RemoteFile getFileFromLibertyServerRoot(String filePath) throws Exception {
        final String method = "getFileFromLibertyServerRoot";
        Log.entering(c, method);
        return getFileFromLiberty(serverRoot + "/" + filePath);
    }

    /* not called */public RemoteFile getFileFromLibertySharedDir(String filePath) throws Exception {
        final String method = "getFileFromLibertySharedDir";
        Log.entering(c, method);
        return getFileFromLiberty(getServerSharedPath() + filePath);
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

    public boolean fileExistsInLibertyServerRoot(String filePath) throws Exception {
        final String method = "fileExistsInLibertyServerRoot";
        Log.entering(c, method);
        return libertyFileExists(serverRoot + "/" + filePath);
    }

    /* not called */public boolean fileExistsInLibertySharedDir(String filePath) throws Exception {
        final String method = "fileExistsInLibertySharedDir";
        Log.entering(c, method);
        return libertyFileExists(getServerSharedPath() + filePath);
    }

    protected boolean libertyFileExists(String fullPath) throws Exception {
        boolean exists = LibertyFileManager.libertyFileExists(machine, fullPath);
        Log.info(c, "libertyFileExists", "File: " + fullPath + " exists " + exists);
        return exists;
    }

    public String getServerName() {
        return serverToUse;
    }

    public void deleteFileFromLibertyInstallRoot(String filePath) throws Exception {
        LibertyFileManager.deleteLibertyFile(machine, (installRoot + "/" + filePath));
    }

    public void deleteDirectoryFromLibertyInstallRoot(String directoryPath) throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, (installRoot + "/" + directoryPath));
    }

    public void deleteDirectoryFromLibertyServerRoot(String directoryPath) throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, (serverRoot + "/" + directoryPath));
    }

    public void deleteFileFromLibertyServerRoot(String filePath) throws Exception {
        LibertyFileManager.deleteLibertyFile(machine, (serverRoot + "/" + filePath));
    }

    public RemoteFile getServerBootstrapPropertiesFile() throws Exception {
        return new RemoteFile(machine, serverRoot + "/bootstrap.properties");
    }

    /**
     * Non-recursively list the contents of the server install root directory, or if the relativePath
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
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(path, machine), filter);
    }

    /**
     * Non-recursively list the contents of the server install root directory, or if the relativePath
     * parameter is non-null, the indicated directory (relative the the install root). If filter is
     * non-null, return only those directory names or filenames that contain the filter string.
     *
     * @param  relativeDir path to a directory relative to the install root directory, should not begin with path separator, may be null.
     * @param  filter      string to filter the results by, returned file and directory names must contain this, may be null.
     * @return             a list of file and directory names indicating the contents of the specified directory.
     * @throws Exception
     */
    public ArrayList<String> listLibertyServerRoot(String relativeDir, String filter) throws Exception {
        String path = serverRoot;
        if (relativeDir != null && !relativeDir.equals("")) {
            path = path + "/" + relativeDir;
        }

        path = LibertyServerUtils.makeJavaCompatible(path, machine);

        return listDirectoryContents(path, filter);
    }

    /**
     * Non-recursively list the contents of the autoFVT test directory, or if the relativePath
     * parameter is non-null, the indicated directory (relative the the install root). If filter is
     * non-null, return only those directory names or filenames that contain the filter string.
     *
     * @param  relativeDir path to a directory relative to the autoFVT test directory, should not begin with path separator, may be null.
     * @param  filter      string to filter the results by, returned file and directory names must contain this, may be null.
     * @return             a list of file and directory names indicating the contents of the specified directory.
     * @throws Exception
     */
    public ArrayList<String> listAutoFVTTestFiles(Machine machine, String relativeDir, String filter) throws Exception {
        String path = pathToAutoFVTTestFiles;
        if (relativeDir != null && !relativeDir.equals("")) {
            path = path + relativeDir;
        }

        path = LibertyServerUtils.makeJavaCompatible(path, machine);

        RemoteFile serverDir = new RemoteFile(machine, path);
        return listDirectoryContents(serverDir, filter);
    }

    /**
     * Method for returning the directory contents as a list of Strings representing first level file/dir names
     *
     * @return                   ArrayList of File/Directory names
     *                           that exist at the first level i.e. it's not recursive. If it's a directory the String in the list is prefixed with a /
     * @throws TopologyException
     */
    protected ArrayList<String> listDirectoryContents(RemoteFile serverDir) throws Exception {
        return listDirectoryContents(serverDir, null);

    }

    protected ArrayList<String> listDirectoryContents(String path, String fileName) throws Exception {

        RemoteFile serverDir = new RemoteFile(machine, path);
        return listDirectoryContents(serverDir, fileName);

    }

    protected ArrayList<String> listDirectoryContents(RemoteFile serverDir, String fileName) throws Exception {

        final String method = "serverDirectoryContents";
        Log.entering(c, method);
        if (!serverDir.isDirectory() || !serverDir.exists())
            throw new TopologyException("The specified directoryPath \'"
                                        + serverDir.getAbsolutePath() + "\' was not a directory");

        RemoteFile[] firstLevelFiles = serverDir.list(false);
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

    public ArrayList<String> listFFDCFiles(String server) throws Exception {
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(serverRoot + "/logs/ffdc", machine), "ffdc");
    }

    public ArrayList<String> listFFDCSummaryFiles(String server) throws Exception {
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(serverRoot + "/logs/ffdc", machine), "exception_summary");
    }

    /* not called */public int getOsgiConsolePort() {
        return osgiConsolePort;
    }

    /**
     * @return the httpDefaultPort
     */
    public int getHttpDefaultPort() {
        return httpDefaultPort;
    }

    /**
     * @param httpDefaultPort
     *                            the httpDefaultPort to set
     */
    public void setHttpDefaultPort(int httpDefaultPort) {
        this.httpDefaultPort = httpDefaultPort;
    }

    /**
     * @return the httpDefaultSecurePort
     */
    public int getHttpDefaultSecurePort() {
        return httpDefaultSecurePort;
    }

    /**
     * @param httpDefaultSecurePort
     *                                  the httpDefaultSecurePort to set
     */
    public void setHttpDefaultSecurePort(int httpDefaultSecurePort) {
        this.httpDefaultSecurePort = httpDefaultSecurePort;
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
            case WC_defaulthost:
                ret = getHttpDefaultPort();
                break;
            case WC_defaulthost_secure:
                ret = getHttpDefaultSecurePort();
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

    public void setServerStartTimeout(long timeout) {
        serverStartTimeout = timeout;
    }

    /* not called */public long getServerStartTimeout() {
        return serverStartTimeout;
    }

    public String getBootstrapKey() {
        return serverTopologyID;
    }

    /**
     * Method used to autoinstall apps in
     * publish/servers/<serverName>/dropins folder found in the FAT project
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
     * Install a bundle.
     *
     * @deprecated Build bundles from ${project}/test-bundles and invoke {@link #installSystemBundle(String)} instead to install them from the built location.
     */
    @Deprecated
    public void installBundle(String name) throws Exception {
        Log.info(c, "installBundle", "Installing bundle '" + name + "'");
        String path;
        if (name.contains("/") || name.contains("\\")) {
            path = name;
        } else {
            path = "publish/bundles/" + name + ".jar";
        }

        Assert.assertFalse("Server should not be started when installing a bundle", isStarted());
        copyFileToLibertyInstallRoot("lib/", path);
    }

    /**
     * Install a feature.
     *
     * @deprecated Place feature manifests in S{project}/publish/features and invoke {@link #installSystemFeature(String)} instead.
     */
    @Deprecated
    public void installFeature(String name) throws Exception {
        Log.info(c, "installFeature", "Installing feature '" + name + "'");
        String path;
        if (name.contains("/") || name.contains("\\")) {
            path = name;
        } else {
            path = "publish/features/" + name + ".mf";
        }

        Assert.assertFalse("Server should not be started when installing a bundle", isStarted());
        copyFileToLibertyInstallRoot("lib/features/", path);
    }

    /**
     * Uninstall a bundle
     *
     * @deprecated Use {@link #uninstallSystemBundle(String)} instead.
     */
    @Deprecated
    public void uninstallBundle(String name) throws Exception {
        Log.info(c, "uninstallBundle", "Uninstalling bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a bundle", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/lib/" + name + ".jar");
    }

    /**
     * Uninstall a feature
     *
     * @deprecated Use {@link #uninstallSystemFeature(String)} instead.
     */
    @Deprecated
    public void uninstallFeature(String name) throws Exception {
        Log.info(c, "uninstallFeature", "Uninstalling feature '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/lib/features/" + name + ".mf");
    }

    /**
     * Install a bundle as a system bundle, assuming the bundle is
     * to be found in publish/bundles/&lt;name>.jar
     * <p>
     * To use this most effectively, place your bundle code under test-bundles/bundle.symbolic.name/.
     * The structure under here reflects the structure of a bundle project and uses the same ant
     * logic to create the bundle. Look at the mxbeans fat project for an example.
     *
     * @param name the name of the bundle, without the <code>.jar</code> suffix
     */
    public void installSystemBundle(String name) throws Exception {
        Log.info(c, "installSystemBundle", "Installing system bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a bundle", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/lib", "publish/bundles/" + name + ".jar");
    }

    /**
     * Install a feature as a system feature, assuming the feature is
     * to be found in publish/features/&lt;name>.mf
     *
     * @param name the name of the feature, without the <code>.mf</code> suffix
     */
    public void installSystemFeature(String name) throws Exception {
        Log.info(c, "installSystemFeature", "Installing system feature '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/lib/features", "publish/features/" + name + ".mf");
    }

    /**
     * Install a feature translation file to the system feature directory,
     * assuming the feature translation file is to be found in publish/features/l10n/&lt;name>.mf
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void installSystemFeatureL10N(String name) throws Exception {
        Log.info(c, "installSystemFeatureL10N", "Installing system feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature translation", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/lib/features/l10n", "publish/features/l10n/" + name + ".properties");
    }

    /**
     * Uninstall a system bundle.
     *
     * @param name the name of the bundle, without the <code>.jar</code> suffix
     */
    public void uninstallSystemBundle(String name) throws Exception {
        Log.info(c, "uninstallSystemBundle", "Uninstalling system bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a bundle", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/lib/" + name + ".jar");
    }

    /**
     * Uninstall a system feature.
     *
     * @param name the name of the feature, without the <code>.mf</code> suffix
     */
    public void uninstallSystemFeature(String name) throws Exception {
        Log.info(c, "uninstallSystemFeature", "Uninstalling system feature '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/lib/features/" + name + ".mf");
    }

    /**
     * Uninstall a system feature translation file.
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void uninstallSystemFeatureL10N(String name) throws Exception {
        Log.info(c, "uninstallSystemFeatureL10N", "Uninstalling system feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature translation", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/lib/features/l10n/" + name + ".properties");
    }

    /**
     * Install a bundle as a user extension bundle, assuming the bundle is
     * to be found in publish/bundles/&lt;name>.jar
     * <p>
     * To use this most effectively, place your bundle code under test-bundles/bundle.symbolic.name/.
     * The structure under here reflects the structure of a bundle project and uses the same ant
     * logic to create the bundle. Look at the mxbeans fat project for an example.
     *
     * @param name the name of the bundle, without the <code>.jar</code> suffix
     */
    public void installUserBundle(String name) throws Exception {
        Log.info(c, "installUserBundle", "Installing user bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a bundle", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/usr/extension/lib", "publish/bundles/" + name + ".jar");
    }

    /**
     * Install a feature as a user extension feature, assuming the feature is
     * to be found in publish/features/&lt;name>.mf
     *
     * @param name the name of the feature, without the <code>.mf</code> suffix
     */
    public void installUserFeature(String name) throws Exception {
        Log.info(c, "installUserFeature", "Installing user feature '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/usr/extension/lib/features", "publish/features/" + name + ".mf");
    }

    /**
     * Install a feature translation to the user extension feature directory,
     * assuming the feature translation file is to be found in publish/features/l10n/&lt;name>.mf
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void installUserFeatureL10N(String name) throws Exception {
        Log.info(c, "installUserFeatureL10N", "Installing user feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature translation", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/usr/extension/lib/features/l10n", "publish/features/l10n/" + name + ".properties");
    }

    /**
     * Install a feature packaged as an ESA into the server runtime.
     *
     * @param  loc                    the name of the product extension. If set to null then "usr" is assumed
     * @param  esa                    the name of the feature.
     * @param  additionalFeatureFiles - More ESA files that need to be copied to the machine prior to running the install
     * @throws Exception
     */
    public ProgramOutput installFeature(String loc, String feature, String... additionalFeatureFiles) throws Exception {
        return installFeatureWithProgramArgs(loc, feature, new String[0], additionalFeatureFiles);
    }

    /**
     * Install a feature packaged as an ESA into the server runtime.
     *
     * @param  loc                    the name of the product extension. If set to null then "usr" is assumed
     * @param  esa                    the name of the feature.
     * @param  additionalProgramArgs  - extra args to pass into the program when running
     * @param  additionalFeatureFiles - More ESA files that need to be copied to the machine prior to running the install
     * @throws Exception
     */
    public ProgramOutput installFeatureWithProgramArgs(String loc, String feature, String[] additionalProgramArgs, String... additionalFeatureFiles) throws Exception {
        if (loc == null) {
            loc = "usr";
        }
        Log.info(c, "installFeatureWithProgramArgs", "Installing feature '" + feature + "' to '" + loc + "'");

        List<String> pathsToDelete = new ArrayList<String>();
        String featureFileName = feature + ".esa";
        String featurePathName = installRoot + "/" + featureFileName;
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot, "publish/features/" + featureFileName);
        pathsToDelete.add(featurePathName);

        for (String featureName : additionalFeatureFiles) {
            String fileName = featureName + ".esa";
            String pathName = installRoot + "/" + fileName;
            pathsToDelete.add(pathName);
            LibertyFileManager.copyFileIntoLiberty(machine, installRoot, "publish/features/" + fileName);
        }

        ProgramOutput po = doInstallFeature(featurePathName, loc, additionalProgramArgs);

        for (String pathName : pathsToDelete) {
            LibertyFileManager.deleteLibertyFile(machine, pathName);
        }

        return po;
    }

    /**
     * Installs a feature from a remote file (ESA).
     *
     * @param  feature   The ESA file
     * @return           The output from the {@link Machine#execute(String, String[], String)} call
     * @throws Exception
     */
    public ProgramOutput installFeature(RemoteFile feature) throws Exception {
        Log.info(c, "installFeatureWithProgramArgs", "Installing feature '" + feature.getAbsolutePath() + "'");
        return installFeature(feature, new String[0]);
    }

    /**
     * Installs a feature from a remote file (ESA).
     *
     * @param  feature   The ESA file
     * @param  args      arguments to pass when installing the feature
     * @return           The output from the {@link Machine#execute(String, String[], String)} call
     * @throws Exception
     */
    public ProgramOutput installFeature(RemoteFile feature, String[] args) throws Exception {
        Log.info(c, "installFeatureWithProgramArgs", "Installing feature '" + feature.getAbsolutePath() + "'");
        return doInstallFeature(feature.getAbsolutePath(), "usr", args);
    }

    /**
     * Actually run the install command on the remote machine
     *
     * @param  featurePathName       The path to the feature ESA file
     * @param  loc                   The loc to use
     * @param  additionalProgramArgs Any additonal program args to include
     * @return                       The output from the {@link Machine#execute(String, String[], String)} call
     * @throws Exception
     */
    protected ProgramOutput doInstallFeature(String featurePathName, String loc, String[] additionalProgramArgs) throws Exception {
        // Always have the accept license header as we do not run the command in an interactive way.
        // This is ok from a testing point of view as the interactive code is re-used from the
        // self extracting JAR file where it is already being well tested
        String[] args = new String[4 + additionalProgramArgs.length];
        args[0] = "install";
        args[1] = "--to=" + loc;
        args[2] = "--acceptLicense";
        for (int pos = 0; pos < additionalProgramArgs.length; pos++) {
            args[pos + 3] = additionalProgramArgs[pos];
        }
        args[3 + additionalProgramArgs.length] = featurePathName;
        Log.info(c, "installUserFeature", "Using args " + Arrays.toString(args));
        ProgramOutput po = machine.execute(installRoot + "/bin/featureManager", args, installRoot);
        return po;
    }

    /**
     * Given a sample name that corresponds to a sample in a jar named <sample-name>.jar in
     * the FAT files directory, runs the self extractor with the --downloadDependencies and
     * --acceptLicense flag in order to create a working copy of the sample server for test.
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
            throw new Exception("Could not install sample server - return code " + po.getReturnCode());
        }

        //Move the test server xml into sample.xml
        RemoteFile sampleServerFile = LibertyFileManager.createRemoteFile(machine, getServerRoot() + "/sample.xml");
        LibertyFileManager.moveLibertyFile(getServerConfigurationFile(), sampleServerFile);
        //And upload the FAT server XML that will include sample.xml
        LibertyFileManager.copyFileIntoLiberty(machine, getServerRoot(), "server.xml", "productSampleServer.xml");

        //Move the test server bootstrap.properties into sample.properties if it exists
        RemoteFile serverBootStrapProps = new RemoteFile(machine, getServerRoot() + "/bootstrap.properties");
        if (serverBootStrapProps.exists()) {
            //This is optional
            RemoteFile samplePropertiesFile = LibertyFileManager.createRemoteFile(machine, getServerRoot() + "/sample.properties");
            LibertyFileManager.moveLibertyFile(serverBootStrapProps, samplePropertiesFile);
            //And upload the FAT server properties that will include the sample properties
            LibertyFileManager.copyFileIntoLiberty(machine, getServerRoot(), "bootstrap.properties", "productSample.properties");
        } else {
            //Just take the testports, no include of existing config needed
            LibertyFileManager.copyFileIntoLiberty(machine, getServerRoot(), "bootstrap.properties", "productSample_noBootstrap.properties");
        }

        for (String pathName : pathsToDelete) {
            LibertyFileManager.deleteLibertyFile(machine, pathName);
        }

        return po;
    }

    /**
     * Uninstall a user extension bundle
     *
     * @param name the name of the bundle, without the <code>.jar</code> suffix
     */
    public void uninstallUserBundle(String name) throws Exception {
        Log.info(c, "uninstallUserBundle", "Uninstalling user bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a bundle", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/usr/extension/lib/" + name + ".jar");
    }

    /**
     * Uninstall a user extension feature
     *
     * @param name the name of the feature, without the <code>.mf</code> suffix
     */
    public void uninstallUserFeature(String name) throws Exception {
        Log.info(c, "uninstallUserFeature", "Uninstalling user feature '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/usr/extension/lib/features/" + name + ".mf");
    }

    /**
     * Uninstall a user feature translation file.
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void uninstallUserFeatureL10N(String name) throws Exception {
        Log.info(c, "uninstallUserFeatureL10N", "Uninstalling user feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature translation", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/usr/extension/lib/features/l10n/" + name + ".properties");
    }

    /**
     * Install a bundle as a product extension bundle, assuming the bundle is
     * to be found in publish/productbundles/&lt;name>.jar
     *
     * @param productName the name of the product the bundle belongs to
     * @param name        the name of the bundle, without the <code>.jar</code> suffix
     */
    public void installProductBundle(String productName, String name) throws Exception {
        Log.info(c, "installProductBundle", "Installing product '" + productName + "' bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a bundle", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRootParent + "/" + productName + "/lib", "publish/productbundles/" + name + ".jar");
    }

    /**
     * Install a feature as a product extension feature, assuming the feature is
     * to be found in publish/productfeatures/&lt;name>.mf
     *
     * @param productName the name of the product the feature belongs to
     * @param name        the name of the feature, without the <code>.mf</code> suffix
     */
    public void installProductFeature(String productName, String name) throws Exception {
        Log.info(c, "installProductFeature", "Installing product '" + productName + "' feature '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRootParent + "/" + productName + "/lib/features", "publish/productfeatures/" + name + ".mf");
    }

    /**
     * Install a feature translation file to the product extension feature directory,
     * assuming the feature translation file is to be found in publish/features/l10n/&lt;name>.mf
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void installProductFeatureL10N(String productName, String name) throws Exception {
        Log.info(c, "installProductFeatureL10N", "Installing product '" + productName + "' feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when installing a feature translation", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRootParent + "/" + productName + "/lib/features/l10n", "publish/productfeatures/l10n/" + name + ".properties");
    }

    /**
     * Create a product extension, assuming the properties file is
     * to be found in publish/productproperties/&lt;productName>.properties
     *
     * @param productName the name of the product
     */
    public void installProductExtension(String productName) throws Exception {
        Log.info(c, "installProductExtension", "Installing product '" + productName + "'");
        Assert.assertFalse("Server should not be started when installing an extension", isStarted());
        LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/etc/extensions", "publish/productproperties/" + productName + ".properties");
    }

    /**
     * Uninstall a product extension bundle
     *
     * @param productName the name of the product the bundle belongs to
     * @param name        the name of the bundle, without the <code>.jar</code> suffix
     */
    public void uninstallProductBundle(String productName, String name) throws Exception {
        Log.info(c, "uninstallProductBundle", "Uninstalling product '" + productName + "'bundle '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a bundle", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRootParent + "/" + productName + "/lib/" + name + ".jar");
    }

    /**
     * Uninstall a product extension feature
     *
     * @param productName the name of the product the feature belongs to
     * @param name        the name of the feature, without the <code>.mf</code> suffix
     */
    public void uninstallProductFeature(String productName, String name) throws Exception {
        Log.info(c, "uninstallProductFeature", "Uninstalling product '" + productName + "', feature '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRootParent + "/" + productName + "/lib/features/" + name + ".mf");
    }

    /**
     * Uninstall a product feature translation file.
     *
     * @param name the name of the feature translation properties, without the <code>.properties</code> suffix.
     *                 The file name should be the subsystem symbolic name of the feature.
     */
    public void uninstallProductFeatureL10N(String productName, String name) throws Exception {
        Log.info(c, "uninstallProductFeatureL10N", "Uninstalling product '" + productName + "' feature translation '" + name + "'");
        Assert.assertFalse("Server should not be started when uninstalling a feature translation", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRootParent + "/" + productName + "/lib/features/l10n/" + name + ".properties");
    }

    /**
     * Uninstall a product extension. This will delete the wlp/etc/extensions/productName.properties file,
     * and all contents under the productName directory (where productName is a peer of wlp)
     *
     * @param productName the name of the product the feature belongs to
     * @param name        the name of the product.
     */
    public void uninstallProductExtension(String productName) throws Exception {
        Log.info(c, "uninstallProductExtension", "Uninstalling product '" + productName + "'");
        Assert.assertFalse("Server should not be started when uninstalling a product extension", isStarted());
        LibertyFileManager.deleteLibertyFile(machine, installRoot + "/etc/extensions/" + productName + ".properties");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, (installRootParent + "/" + productName));
    }

    /**
     * Install the extended content image jar into this existing server.
     *
     * @return           true, if the install was successful
     * @throws Exception
     */
    public boolean installExtendedImage() throws Exception {
        Log.info(c, "installExtendedImage", "Looking for extended image to install...");
        String jarName = null;

        // Find the extended image under the autoFVT/publish/images location
        LocalFile imagesDirectory = new LocalFile("publish/images");
        RemoteFile[] imageFiles = imagesDirectory.list(false);
        for (RemoteFile imageFile : imageFiles) {
            String imageName = imageFile.getName();
            if (imageName.endsWith("jar") && imageName.contains("extended")) {
                jarName = imageName;
                Log.info(c, "installExtendedImage", "Using the following file as the extended image to install: " + jarName);
                break;
            }
        }

        // If the extended image was found, copy it to the server machine and
        // extract it over this server.
        if (jarName != null) {
            String jarPath = LibertyFileManager.copyFileIntoLiberty(machine, installRoot + "/tmp", "publish/images/" + jarName);
            RemoteFile jarFile = new RemoteFile(machine, jarPath);

            Log.info(c, "installExtendedImage", "Issuing the command to install the extended content.");
            ProgramOutput po = machine.execute(machineJava + "/bin/java", new String[] { "-jar", jarFile.getAbsolutePath(), "--acceptLicense", "install", installRoot });
            assertEquals("Installing Liberty extended edition should have worked. Program output:\n" + po.getStdout() + "\nErr output:\n" + po.getStderr(),
                         0, po.getReturnCode());
            return true;
        } else {
            Log.warning(c, "Didn't find a file to use as an extended image...");
            return false;
        }
    }

    public String getHostname() {
        return hostName;
    }

    protected ApplicationType getApplictionType(String appName) throws Exception {
        ApplicationType type = null;
        if (appName.endsWith("zip") || appName.endsWith("ZIP")) {
            type = ApplicationType.ZIP;
        } else if (appName.endsWith("ear") || appName.endsWith("EAR")) {
            type = ApplicationType.EAR;
        } else if (appName.endsWith("war") || appName.endsWith("WAR")) {
            type = ApplicationType.WAR;
        } else if (appName.endsWith("eba") || appName.endsWith("EBA")) {
            type = ApplicationType.EBA;
        } else if (appName.endsWith("js") || appName.endsWith("js")) {
            type = ApplicationType.JS;
        } else if (appName.endsWith("e") || appName.endsWith("jsar")) {
            type = ApplicationType.JS;
        }

        if (type == null) {
            //Application type not recognised
            throw new TopologyException("Can't install the application " + appName
                                        + " as the application type is not recognised.  We only support WAR, EAR, ZIP or EBA");
        }
        return type;
    }

    protected String getJvmOptionsFilePath() {
        return this.getServerRoot() + "/" + JVM_OPTIONS_FILE_NAME;
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
        File tmpFile = File.createTempFile("jvm", "options");
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

    public Properties getBootstrapProperties() {
        Properties props = new Properties();

        try {
            String serverEnv = FileUtils.readFile(getServerRoot() + "/bootstrap.properties");
            props.load(new StringReader(serverEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }

        return props;
    }

    public void addEnvVar(String key, String value) {
        if (!Pattern.matches("[a-zA-Z_]+[a-zA-Z0-9_]*", key)) {
            throw new IllegalArgumentException("Invalid environment variable key '" + key +
                                               "'. Environment variable keys must consist of characers [a-zA-Z0-9_] " +
                                               "in order to work on all OSes.");
        }
        if (this.isStarted())
            throw new RuntimeException("Cannot add env vars to a running server");
        envVars.put(key, value);
    }

    public Properties getServerEnv() {
        Properties props = new Properties();

        props.put("JAVA_HOME", getMachineJavaJDK());

        // First load ${wlp.install.dir}/etc/server.env
        try {
            String serverEnv = FileUtils.readFile(getInstallRoot() + "/etc/server.env");
            props.load(new StringReader(serverEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }

        // Then load ${server.config.dir}/server.env
        try {
            String serverEnv = FileUtils.readFile(getServerRoot() + "/server.env");
            props.load(new StringReader(serverEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
        }

        return props;
    }

    public void deleteDropinDefaultConfiguration(String fileName) throws Exception {
        deleteDropinConfiguration(fileName, true);
    }

    public void deleteDropinOverrideConfiguration(String fileName) throws Exception {
        deleteDropinConfiguration(fileName, false);
    }

    private void deleteDropinConfiguration(String fileName, boolean isDefault) throws Exception {
        String location = getServerRoot() + "/configDropins/defaults/" + fileName;
        if (!isDefault)
            location = getServerRoot() + "/configDropins/overrides/" + fileName;

        LibertyFileManager.deleteLibertyFile(machine, location);
    }

    public void deleteAllDropinConfigurations() throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getServerRoot() + "/configDropins");
    }

    public void addDropinDefaultConfiguration(String fileName) throws Exception {
        addDropinConfiguration(fileName, true);
    }

    public void addDropinOverrideConfiguration(String fileName) throws Exception {
        addDropinConfiguration(fileName, false);
    }

    private void addDropinConfiguration(String fileName, boolean isDefault) throws Exception {
        String location = getServerRoot() + "/configDropins";
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

        Log.finer(c, "replaceServerConfiguration", "isUnix=" + isUnix + " lastConfigLessThanOneSecAgo=" + lastConfigLessThanOneSecAgo);
        if (lastConfigLessThanOneSecAgo && isUnix) {
            // Due to a java limitation on Unix, we need to wait at least
            // 1 second between config updates so the server can see it.
            // See https://www-01.ibm.com/support/docview.wss?uid=swg21446506
            // Note that the above page says that it affects versions up to 1.6, but if you look at the sun bug it is not fixed until java 8.
            Log.finer(c, "replaceServerConfiguration", "Sleeping for 1 second to work around Unix / JDK limitation fixed in Java 8");
            Thread.sleep(1000);
        }
    }

    /**
     * Reconfigures the server to use the new configuration file provided (relative to the autoFVT test files directory), waits for the
     * configuration update to be processed, and waits for all of the specified messages (if any). Note: The autoFVT test files directory
     * is populated using the files in {@code <FAT-root>/publish/files/}, so the configuration file specified should be relative to that
     * directory in the FAT project. For example, specifying {@code "configs/server_test1.xml"} as the config file will use the file
     * located at {@code <FAT-root>/publish/files/configs/server_test1.xml}.
     */
    public void reconfigureServer(String newConfigFile, String... waitForMessages) throws Exception {
        Log.info(c, "reconfigureServer", "Reconfiguring server to use new config: " + newConfigFile);
        setMarkToEndOfLog();

        setServerConfigurationFile(newConfigFile);
        waitForConfigUpdateInLogUsingMark(installedApplications, waitForMessages);
    }

    /**
     * Replaces the server configuration. This encapsulates the necessary logic
     * to deal with system / JDK idiosyncrasies.
     *
     * @param  fileName
     * @throws Exception
     */
    protected void replaceServerConfiguration(String fileName) throws Exception {
        waitIfNeeded();

        LibertyFileManager.moveFileIntoLiberty(machine, getServerRoot(), "server.xml", fileName);
        lastConfigUpdate = System.currentTimeMillis();
    }

    /**
     * Replaces the server configuration which is using a non default server.xml file name (ex, myServer.xml).
     * This encapsulates the necessary logic to deal with system / JDK idiosyncrasies.
     *
     * @param  srcFile   the source configuration file name
     * @param  destFile  the destination configuration file name
     * @throws Exception
     */
    protected void replaceServerConfiguration(String srcFile, String destFile) throws Exception {
        waitIfNeeded();

        LibertyFileManager.moveFileIntoLiberty(machine, getServerRoot(), destFile, srcFile);
        lastConfigUpdate = System.currentTimeMillis();
    }

    /**
     * Replaces the server admin-metadata configuration. This encapsulates the necessary logic
     * to deal with system / JDK idiosyncrasies.
     *
     * @param  fileName
     * @throws Exception
     */
    public void replaceAdminMetadataConfiguration(String fileName) throws Exception {
        waitIfNeeded();

        LibertyFileManager.moveFileIntoLiberty(machine, getServerRoot(), "admin-metadata.xml", pathToAutoFVTTestFiles + "/" + fileName);
        lastConfigUpdate = System.currentTimeMillis();
    }

    /**
     * Replaces the server configuration. This encapsulates the necessary logic
     * to deal with system / JDK idiosyncrasies.
     *
     * @param  fileName
     * @throws Exception
     */
    public void replaceAdminMetadataServerConfiguration(String fileName) throws Exception {
        waitIfNeeded();

        LibertyFileManager.moveFileIntoLiberty(machine, getServerRoot(), "server.xml", pathToAutoFVTTestFiles + "/" + fileName);
        lastConfigUpdate = System.currentTimeMillis();
    }

    /**
     * This will put the named file into the root directory of the server and name it server.xml. As the file name is changed if you want to copy files for use in an include
     * statement or if the location of the config file is being changed using the was.configroot.uri property or --config-root command line then you should use the
     * {@link #copyFileToLibertyInstallRoot(String)} method.
     * <br/>
     * Note: The provided file name is relative to the autoFVT test files directory.
     *
     * @param  fileName  The name of the file from the FVT test suite
     * @throws Exception
     */
    public void setServerConfigurationFile(String fileName) throws Exception {
        replaceServerConfiguration(pathToAutoFVTTestFiles + "/" + fileName);
        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
    }

    /**
     * This will put the named file into the root directory of the server and name it the value of destFile
     * (ie not server.xml as the single parameter version of this method above).
     * <br/>
     * Note: The provided srcFile name is relative to the autoFVT test files directory.
     *
     * @param  srcFile
     * @param  destFile
     * @throws Exception
     */
    public void setServerConfigurationFile(String srcFile, String destFile) throws Exception {
        replaceServerConfiguration(pathToAutoFVTTestFiles + "/" + srcFile, destFile);
        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call

    }

    /**
     * Puts the named file into the root directory of the server and names it server.xml. If the file path is not absolute, it is
     * assumed to exist under the server root directory.
     */
    public void setServerConfigurationFromFilePath(String filePath) throws Exception {
        if (filePath != null && !filePath.startsWith(getServerRoot())) {
            filePath = getServerRoot() + File.separator + filePath;
        }
        Log.info(c, "setServerConfigurationFile", "Using path: " + filePath);
        replaceServerConfiguration(filePath);
        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
    }

    /**
     * This will save the current server configuration, so that it can be restored later on via the
     * restoreServerConfiguration method.
     *
     * @throws Exception
     */
    public void saveServerConfiguration() throws Exception {
        try {
            savedServerXml = new RemoteFile(machine, serverRoot + "/savedServerXml" + System.currentTimeMillis() + ".xml");
            getServerConfigurationFile().copyToDest(savedServerXml);
        } catch (Exception e) {
            savedServerXml = null;
            throw e;
        }
    }

    /**
     * This will restore the server configuration that was saved by a prior call to the
     * saveServerConfiguration method.
     *
     * @throws Exception
     */
    public void restoreServerConfiguration() throws Exception {
        if (savedServerXml == null) {
            throw new RuntimeException("The server configuration cannot be restored because it was never saved via the saveServerConfiguration method.");
        }
        Log.info(c, "restoreServerConfiguration", savedServerXml.getName());
        getServerConfigurationFile().copyFromSource(savedServerXml);
    }

    /**
     * This will restore the server configuration and wait for all apps to be ready
     *
     * @throws Exception
     */
    public void restoreServerConfigurationAndWaitForApps(String... extraMsgs) throws Exception {
        restoreServerConfiguration();
        waitForConfigUpdateInLogUsingMark(listAllInstalledAppsForValidation(), extraMsgs);
    }

    public String getServerConfigurationPath() {
        return this.getServerRoot() + "/" + SERVER_CONFIG_FILE_NAME;
    }

    public RemoteFile getServerConfigurationFile() throws Exception {
        return LibertyFileManager.getLibertyFile(machine, getServerConfigurationPath());
    }

    /**
     * This will load the {@link ServerConfiguration} from the default config file returned from {@link #getServerConfigurationFile()}.
     *
     * @return           The loaded {@link ServerConfiguration}
     * @throws Exception
     */
    public ServerConfiguration getServerConfiguration() throws Exception {
        RemoteFile file = getServerConfigurationFile();
        return getServerConfiguration(file);
    }

    /**
     * This gets the {@link ServerConfiguration} for the supplied XML file.
     *
     * @param  file      The file to load the server configuration from
     * @return           The loaded {@link ServerConfiguration}
     * @throws Exception
     */
    public ServerConfiguration getServerConfiguration(RemoteFile file) throws Exception {
        return ServerConfigurationFactory.getInstance().unmarshal(file.openForReading());
    }

    public void updateServerConfiguration(File serverConfig) throws Exception {
        replaceServerConfiguration(serverConfig.getAbsolutePath());

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Server configuration updated from: " + serverConfig);
            logServerConfiguration(Level.INFO, false);
        }
    }

    /**
     * This updates the supplied file with the supplied config.
     *
     * @param  serverConfig The config to store to the file
     * @param  file         The file to store the config to
     * @throws Exception
     */
    public void updateServerConfiguration(ServerConfiguration serverConfig, RemoteFile file) throws Exception {
        // write contents to a temporary file
        RemoteFile newServerFile = LibertyFileManager.createRemoteFile(machine, getServerConfigurationPath() + ".tmp");
        OutputStream os = newServerFile.openForWriting(false);
        ServerConfigurationFactory.getInstance().marshal(serverConfig, os);

        if (newServerFile.length() == file.length()) {
            serverConfig.setDescription(serverConfig.getDescription() + " (this is some random text to make the file size bigger)");
            os = newServerFile.openForWriting(false);
            ServerConfigurationFactory.getInstance().marshal(serverConfig, os);
        }

        // replace the file
        // This logic does not need to be time protected (as we do in method
        // replaceServerConfiguration) because of the "extra random text" logic
        // above. Even if the timestamp would not be changed, the size out be.
        LibertyFileManager.moveLibertyFile(newServerFile, file);

        if (LOG.isLoggable(Level.INFO) && logOnUpdate) {
            LOG.info("Server configuration updated:");
            logServerConfiguration(Level.INFO, false);
        }
    }

    /**
     * This stores the supplied content to the default server XML file returned from {@link #getServerConfigurationFile()}.
     *
     * @param  serverConfig The configuration to store
     * @throws Exception
     */
    public void updateServerConfiguration(ServerConfiguration serverConfig) throws Exception {
        updateServerConfiguration(serverConfig, getServerConfigurationFile());
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
    protected void logServerConfiguration(Level level, boolean singleLine) {
        String method = "logServerConfiguration";
        BufferedReader reader = null;
        try {
            StringWriter stringWriter = null;
            PrintWriter printWriter = null;
            reader = new BufferedReader(new InputStreamReader(this.getServerConfigurationFile().openForReading()));
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
            LOG.logp(level, CLASS_NAME, method, "Failed to read " + this.getServerConfigurationPath() + ".  Exception: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.logp(level, CLASS_NAME, method, "Failed to close reader for " + this.getServerConfigurationPath() + ".  Exception: " + e.getMessage());
                }
            }
        }
    }

    public RemoteFile getConsoleLogFile() throws Exception {
        // Find the currently configured/in-use console log file.
        final RemoteFile remoteFile;
        if (machineOS == OperatingSystem.ZOS) {
            remoteFile = new RemoteFile(machine, consoleAbsPath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = new RemoteFile(machine, consoleAbsPath);
        }
        return remoteFile;
    }

    public RemoteFile getDefaultLogFile() throws Exception {
        //Set path to server log assuming the default setting.
        // ALWAYS RETURN messages.log -- tests assume they can look for INFO+ messages.
        RemoteFile file = LibertyFileManager.getLibertyFile(machine, messageAbsPath);
        if (file == null) {
            throw new IllegalStateException("Unable to find default log file, path=" + messageAbsPath);
        }
        return file;
    }

    public boolean defaultTraceFileExists() throws Exception {
        return LibertyFileManager.libertyFileExists(machine, traceAbsPath);
    }

    protected String getDefaultLogPath() {
        try {
            RemoteFile file = getDefaultLogFile();
            return file.getAbsolutePath();
        } catch (Exception ex) {
            return "DefaultLogFile";
        }
    }

    public RemoteFile getDefaultTraceFile() throws Exception {
        return LibertyFileManager.getLibertyFile(machine, traceAbsPath);
    }

    public boolean defaultLogFileExists() throws Exception {
        return LibertyFileManager.libertyFileExists(machine, messageAbsPath);
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
     * This method will search the given file on this server for the specified expression.
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
     * This method will search the given file on this server for the specified expression.
     * The path given is relative to the server root directory.
     *
     * @param  regexp    pattern to search for.
     * @param  filePath  the pathname relative to the server root directory.
     * @return           A list of the lines in the file that contains the matching
     *                   pattern. No match results in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInFileInLibertyServerRoot(String regexp, String filePath) throws Exception {

        /*
         * On z/os, the console log will be produced in EBCDIC. We know other logs are ASCII, and
         * so we can comfortably special case the console.
         */
        final RemoteFile remoteFile;
        String absolutePath = serverRoot + "/" + filePath;
        if (machineOS == OperatingSystem.ZOS && absolutePath.equalsIgnoreCase(consoleAbsPath)) {
            remoteFile = new RemoteFile(machine, absolutePath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = LibertyFileManager.getLibertyFile(machine, absolutePath);
        }
        List<String> strings = LibertyFileManager.findStringsInFile(regexp, remoteFile);
        return strings;
    }

    /**
     * This method will search the output and trace files for this server
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

    /**
     * This method will search the output and trace files for this server
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
     * This method will search the output and trace files for this server
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
     * This method will search the output and trace files for this server
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
     * This method will search the trace files for this server
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
     * This method will search for the provided expression in the log file
     * on an incremental basis. It starts with reading the file at the offset where
     * the last mark was set (or the beginning of the file if no mark has been set)
     * and reads until the end of the file.
     *
     * @param  regexp    pattern to search for
     * @return           A list of the lines in the log file which contain the matching
     *                   pattern. No matches result in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogsUsingMark(String regexp, String filePath) throws Exception {
        final RemoteFile remoteFile;
        String absolutePath = serverRoot + "/" + filePath;
        if (machineOS == OperatingSystem.ZOS && absolutePath.equalsIgnoreCase(consoleAbsPath)) {
            remoteFile = new RemoteFile(machine, absolutePath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = LibertyFileManager.getLibertyFile(machine, absolutePath);
        }
        return findStringsInLogsUsingMark(regexp, remoteFile);
    }

    /**
     * This method will search for the provided expression in the log file
     * on an incremental basis. It starts with reading the
     * file at the offset where the last mark was set (or the beginning of the file
     * if no mark has been set) and reads until the end of the file.
     *
     * @param  regexp    pattern to search for
     * @param  logFile   RemoteFile for log file to search
     * @return           A list of the lines in the trace files which contain the matching
     *                   pattern. No matches result in an empty list.
     * @throws Exception
     */
    public List<String> findStringsInLogsUsingMark(String regexp, RemoteFile logFile) throws Exception {

        List<String> matches = new ArrayList<String>();
        LogSearchResult newOffsetAndMatches;

        Long offset = getMarkOffset(logFile.getAbsolutePath());
        newOffsetAndMatches = LibertyFileManager.findStringsInFile(regexp, logFile, offset);
        matches.addAll(newOffsetAndMatches.getMatches()); // get the list of matches found

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
     * Reset the mark and offset values for logs back to the start of the JVM's run.
     */
    public void resetLogMarks() {
        logMonitor.resetLogMarks();
    }

    /**
     * Note: This method doesn't set the offset values to the beginning of the file per se,
     * rather this method sets the list of logs and their offset values to null. When one
     * of the findStringsInLogsAndTrace...(...) methods are called, it will recreate the
     * list of logs and set each offset value to 0L - the start of the file.
     */
    public void clearLogMarks() {
        logMonitor.clearLogMarks();
    }

    /**
     * Reset the marks and offset values for the logs back to the start of the JVM's run.
     *
     * @deprecated Using log offsets is deprecated in favor of using log marks.
     *             For all new test code, use the following methods: {@link #resetLogMarks()}, {@link #setMarkToEndOfLog(RemoteFile...)},
     *             {@link #waitForStringInLogUsingMark(String)} and {@link #getMarkOffset(String)}.
     */
    @Deprecated
    public void resetLogOffsets() {
        resetLogMarks();
    }

    /**
     * Set the mark offset to the end of the log file.
     *
     * @param log files to mark. If none are specified, the default log file is marked.
     */
    public void setMarkToEndOfLog(RemoteFile... logFiles) throws Exception {
        Log.info(c, "setMarkToEndOfLog", "Setting mark to the end of logs (if null, messages.log): " + logFiles);
        logMonitor.setMarkToEndOfLog(logFiles);
    }

    /**
     * Set the mark offset to the end of the default trace file (i.e. trace.log).
     *
     * @throws Exception
     */
    public void setTraceMarkToEndOfDefaultTrace() throws Exception {
        Log.info(c, "setTraceMarkToEndOfDefaultTrace", "Setting mark to the end of trace.log");
        setMarkToEndOfLog(getDefaultTraceFile());
    }

    /**
     * Get the mark offset for the specified log file.
     */
    protected Long getMarkOffset(String logFile) {
        return logMonitor.getMarkOffset(logFile);
    }

    /**
     * Get the offset into a log or trace file of the last message inspected.
     *
     * If the file name does not exist in the offsets, then create an entry for it and
     * set the offset for that file to '0'.
     *
     * @param      String value of the file name
     * @return            Long containing the offset into the file of the last message inspected
     * @deprecated        Using log offsets is deprecated in favor of using log marks.
     *                    For all new test code, use the following methods: {@link #resetLogMarks()}, {@link #setMarkToEndOfLog(RemoteFile...)},
     *                    {@link #waitForStringInLogUsingMark(String)} and {@link #getMarkOffset(String)}.
     */
    @Deprecated
    protected Long getLogOffset(String logFile) {

        String method = "getLogOffset";
        Log.finer(c, "getLogOffset", logFile);

        if (!logOffsets.containsKey(logFile)) {
            Log.finer(c, method, "file does not exist in logOffsets, set initial offset");
            logOffsets.put(logFile, 0L);
        }

        return logOffsets.get(logFile);
    }

    /**
     * Update the log offset for the specified log file to the offset provided.
     *
     * @deprecated Using log offsets is deprecated in favor of using log marks.
     *             For all new test code, use the following methods: {@link #resetLogMarks()},
     *             {@link #setMarkToEndOfLog(RemoteFile...)},
     *             {@link #waitForStringInLogUsingMark(String)} and
     *             {@link #getMarkOffset(String)}.
     */
    @Deprecated
    public void updateLogOffset(String logFile, Long newLogOffset) {
        @SuppressWarnings("unused")
        Long oldLogOffset = logOffsets.put(logFile, newLogOffset);
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

            for (String line : findStringsInFileInLibertyServerRoot(".*((CWWKZ0)|(J2CA7))00[139]I: .*", "logs/messages.log"))
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
        final long timeout = LOG_SEARCH_TIMEOUT_CONFIG_UPDATE;

        final List<String> matchingLines = new LinkedList<String>();
        final List<String> watchFor = new LinkedList<String>();
        String firstLine = null;
        String lastLine = null;
        boolean hitEof = false;
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
                    Log.info(LibertyServer.class, methodName, "waited " + waited + "ms" +
                                                              ", startedAppNames=" + startedAppNames +
                                                              ", appNames=" + appNames +
                                                              ", contains? " + startedAppNames.containsAll(appNames) +
                                                              ", watchFor=" + watchFor);

                LogSearchResult newOffsetAndMatches = LibertyFileManager.findStringsInFileCommon(watchFor, 1, logFile, offset);

                if (firstLine == null)
                    firstLine = newOffsetAndMatches.getFirstLine();

                //Make the last-line sticky so we see the actual text searched last.
                //If we get an EOF return (null) note it, but don't remove the last text seen.
                if (newOffsetAndMatches.getLastLine() == null)
                    hitEof = true;
                else
                    lastLine = newOffsetAndMatches.getLastLine();
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
            Log.info(LibertyServer.class, methodName, "Started waiting for CWWKG001[7-8]I and messages matching regexps "
                                                      + Arrays.asList(regexps) + " at " + formatter.format(new Date(startTime))
                                                      + " and finished at " + formatter.format(new Date(endTime))
                                                      + ". Found: " + matchingLines);

            Log.info(LibertyServer.class, methodName, "First line searched: [ " + firstLine + " ]");
            Log.info(LibertyServer.class, methodName, "Last line searched:  [ " + lastLine + " ]");
            if (hitEof)
                Log.info(LibertyServer.class, methodName, "Last line searching reached end of file, preceding last line was the last line of text seen.");
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

        FeatureDependencyProcessor.validateTestedFeatures(this, logFile);

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
     * Waits for each of the regexes in the provided list in the default log from the last mark. Each search will time out after
     * a sensible period of time has elapsed.
     *
     * @throws Exception Thrown if any of the messages in the provided list are not found.
     */
    public void waitForStringsInLogUsingMark(List<String> messages) throws Exception {
        if (messages == null) {
            return;
        }
        for (String msg : messages) {
            String matchingLine = waitForStringInLogUsingMark(msg);
            if (matchingLine != null) {
                Log.info(getClass(), "waitForStringsInLogUsingMark", "Found message [" + msg + "]: " + matchingLine);
            } else {
                throw new Exception("Failed to find [" + msg + "] in the server log for server " + getServerName());
            }
        }
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     * <p>The best practice for this method is as follows:
     * <tt><p>
     * // Set the mark to the current end of log<br/>
     * server.setMarkToEndOfLog();<br/>
     * // Do something, e.g. config change<br/>
     * server.setServerConfigurationFile("newServer.xml");<br/>
     * // Wait for message that was a result of the config change<br/>
     * server.waitForStringInLogUsingMark("CWWKZ0009I");<br/>
     * </p></tt></p>
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
        return logMonitor.waitForStringInLogUsingMark(regexp, timeout);
    }

    /**
     * @param  regexp
     * @param  serverConfigurationFile
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
            Log.info(LibertyServer.class, "waitForMultipleStringsInLog",
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
            Log.info(LibertyServer.class, "waitForStringInLogUsingLastOffset",
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
        return logMonitor.waitForStringInLogUsingMark(regexp, intendedTimeout, extendedTimeout, outputFile);
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark
     * and verify that the regex does not show up in the logs during the
     * specfied duration.
     *
     * @param  timeout Timeout (in milliseconds)
     * @return         line that matched the regexp
     */
    public String verifyStringNotInLogUsingMark(String regexToSearchFor, long timeout) {
        try {
            return verifyStringNotInLogUsingMark(regexToSearchFor, timeout, getDefaultLogFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark
     * and verify that the regex does not show up in the logs during the
     * specfied duration.
     *
     * @param  timeout Timeout (in milliseconds)
     * @return         line that matched the regexp
     */
    public String verifyStringNotInLogUsingMark(String regexToSearchFor, long timeout, RemoteFile logFileToSearch) {
        return logMonitor.verifyStringNotInLogUsingMark(regexToSearchFor, timeout, logFileToSearch);
    }

    /**
     * Wait for the specified regexp in the default logs from the last mark.
     * The offset is also incremented every time this method is called.
     *
     * TODO: This is a temporary version of this method that will be used for negative
     * checks. Remove this method and update the verifyStringNotInLogUsingMark method to use
     * the waitForStringInLogUsingMark method eventually.
     *
     * @param  regexp          a regular expression to search for
     * @param  intendedTimeout a timeout, in milliseconds, within which the wait should complete. Exceeding this is a soft fail.
     * @param  extendedTimeout a timeout, in milliseconds, within which the wait must complete. Exceeding this is a hard fail.
     * @param  outputFile      file to check
     * @return                 line that matched the regexp
     */
    protected String waitForStringInLogUsingMarkWithException(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile) {
        return logMonitor.waitForStringInLogUsingMarkWithException(regexp, intendedTimeout, extendedTimeout, outputFile);
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
        return logMonitor.waitForMultipleStringsInLogUsingMark(numberOfMatches, regexp, timeout, outputFile);
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
            RemoteFile outputFile = new RemoteFile(machine, messageAbsPath);
            int oldNumber = counter.getAndIncrement();
            int newNumber = oldNumber + 1;
            int numberFound = waitForMultipleStringsInLog(newNumber, message_code, serverStartTimeout, outputFile);
            //waitForStringInLog(REMOVE_APP_MESSAGE_CODE, serverStartTimeout, outputFile);
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
        final String START_APP_MESSAGE_CODE = "CWWKZ0001I:.*" + app;
        Log.info(c, method, "Adding installed app: " + app + " for validation");
        installedApplications.add(app);

        if (isStarted) {
            searchForMessages(START_APP_MESSAGE_CODE, "installApp", startApplicationMessages);
        }
    }

    public void removeInstalledAppForValidation(String app) {
        final String method = "removeInstalledAppForValidation";
        final String REMOVE_APP_MESSAGE_CODE = "CWWKZ0009I:.*" + app;
        Log.info(c, method, "Removing installed app: " + app + " for validation");
        installedApplications.remove(app);

        if (isStarted) {
            searchForMessages(REMOVE_APP_MESSAGE_CODE, "uninstallApp", stopApplicationMessages);
        }
    }

    public void removeAllInstalledAppsForValidation() {
        final String method = "removeInstalledAppForValidation";
        final String REMOVE_APP_MESSAGE_CODE = "CWWKZ0009I";
        Log.info(c, method, "Removing following list of installed application for validation");
        for (String app : installedApplications) {
            Log.info(c, method, " -" + app);
        }
        installedApplications.clear();

        if (isStarted) {
            searchForMessages(REMOVE_APP_MESSAGE_CODE, "uninstallApp", stopApplicationMessages);
        }
    }

    public Set<String> listAllInstalledAppsForValidation() {
        final String method = "listAllInstalledAppsForValidation";
        Log.info(c, method, "Returning list of installed application for validation");
        for (String app : installedApplications) {
            Log.info(c, method, " -" + app);
        }
        return installedApplications;
    }

    /**
     * Returns true if the server has been successfully started, and either
     * hasn't been stopped or hit exceptions during server stop.
     *
     * @return
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Sets the server's state to "started". The server has been started by some
     * other means, and we are simply telling this class about it so that the server
     * can be stopped later.
     */
    public void setStarted() {
        isStarted = true;
    }

    /**
     * Sets the server's state flag to the state specified by the isStarted parameter.
     * The isStarted flag may not always be correct, if the server happens to be started
     * external from this class. This method allows for management of the state externally.
     *
     * @param isStarted
     */
    public void setStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }

    /**
     * This method will check the server state and reset the state based on the results of the
     * status operation.
     *
     * @throws Exception
     */
    public int resetStarted() throws Exception {
        ProgramOutput serverStatusOutput = executeServerScript("status", null);
        int rc = serverStatusOutput.getReturnCode();
        if (rc == 0) {
            // Server is still running when rc == 0
            isStarted = true;

            //Setup the server logs assuming the default setting.
            messageAbsPath = logsRoot + messageFileName;
            consoleAbsPath = logsRoot + consoleFileName;
            traceAbsPath = logsRoot + traceFileName;

        } else {
            // Server is stopped when rc == 1.  Any other value means server
            // is in a bad way, but we still will treat it as not started.
            isStarted = false;
        }

        return rc;
    }

    /**
     * Start the server.
     *
     * @param  cleanStart   if true, the server will be started with a clean start
     * @param  validateApps if true, block until all of the registered apps have started
     * @throws Exception
     */
    public void startServer(boolean cleanStart, boolean validateApps) throws Exception {
        startServerAndValidate(true, cleanStart, validateApps);
    }

    public void deleteAllDropinApplications() throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getServerRoot() + "/dropins");
        LibertyFileManager.createRemoteFile(machine, getServerRoot() + "/dropins");
    }

    /**
     * Restart a drop-ins application.
     *
     * Rename the application to move it out of the drop-ins folder, then wait for a
     * log message that shows that the the application was shut down, then rename
     * the application to move it back into the drop-ins folder, then wait
     * for a log message that shows that the application was started.
     *
     * Because a single log may show multiple application start and multiple application
     * stop messages, the log message detection for this operation uses
     * {@link #waitForStringInLogUsingMark(String)}.
     *
     * @param  appFileName The name of the file of the application, for example, "snoop.war".
     *
     * @return             True or false telling if the application was successfully restarted.
     *
     * @throws Exception   Thrown in case of a failure of the restart.
     */
    public boolean restartDropinsApplication(String appFileName) throws Exception {
        final String method = "restartDropinsApplication";

        String appName = appFileName.substring(0, appFileName.lastIndexOf("."));
        String appInDropinsPath = serverRoot + "/dropins/" + appFileName;
        String appExcisedPath = serverRoot + "/" + appFileName;

        // Allow two attempts to rename the liberty file: Zip file caching may keep
        // application archives active after the application has quiesced, up to
        // the long zip caching pending interval.  See
        //   open-liberty/dev/com.ibm.ws.artifact.zip/src/
        //   com/ibm/ws/artifact/zip/cache/
        //    ZipCachingProperties.java
        // The default largest pending close time is specified
        // by property <code>zip.reaper.slow.pend.max</code>.  The default value
        // is 200 NS.  The retry interval is set at twice that.

        setMarkToEndOfLog(); // Only want messages which follow the app removal.

        // Logging in 'renameLibertyFile'.
        if (!LibertyFileManager.renameLibertyFile(machine, appInDropinsPath, appExcisedPath)) { // throws Exception
            Log.info(c, method, "Unable to move " + appFileName + " out of dropins, failing.");
            return false;
        } else {
            Log.info(c, method, appFileName + " successfully moved out of dropins, waiting for message...");
        }

        // The following app stop message does not necessarily indicate that the app has been completely removed.
        // We'll wait for 1s to ensure that the "restarted" app is recognized as a new rather than updated app.
        // If we don't wait here, in rare cases a CWWKZ0003I will be printed instead of CWWKZ0001I for the app.
        Thread.sleep(1000);

        String stopMsg = waitForStringInLogUsingMark("CWWKZ0009I:.*" + appName); // throws Exception
        if (stopMsg == null) {
            return false;
        }

        // Detection of the stop message means the mark was updated.  There is no need
        // to set the mark explicitly.

        // Logging in 'renameLibertyFile'.
        if (!LibertyFileManager.renameLibertyFile(machine, appExcisedPath, appInDropinsPath)) { // throws Exception
            Log.info(c, method, "Unable to move " + appFileName + " back into dropins, failing.");
            return false;
        } else {
            Log.info(c, method, appFileName + " successfully moved back into dropins, waiting for message...");
        }

        String startMsg = waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName); // throws Exception
        if (startMsg == null) {
            return false;
        }

        // Detection of the start message means the mark was updated.  Subsequent log
        // log detection which uses the mark will start immediately following the
        // start message.

        return true;
    }

    /**
     *
     * Removes one or more applications from dropins.
     *
     * This does no clever testing using log files, it assumes you're using it before the server starts
     *
     * @param  fileNames the file name or names of the application, e.g. snoop.war
     * @return           {@code true} if the applications were moved successfully, {@code false} otherwise.
     * @throws Exception
     */
    public boolean removeDropinsApplications(String... fileNames) throws Exception {

        boolean allSucceeded = true;

        for (String fileName : fileNames) {

            String dropinsFilePath = serverRoot + "/dropins/" + fileName;
            String nonDropinsFilePath = serverRoot + "/" + fileName;

            allSucceeded = allSucceeded && LibertyFileManager.renameLibertyFile(machine, dropinsFilePath, nonDropinsFilePath);
        }

        return allSucceeded;
    }

    /**
     *
     * Removes one or more applications from dropins and wait for them to stop
     *
     * @param  fileNames the file name or names of the application, e.g. snoop.war
     * @return           {@code true} if the applications were moved successfully, {@code false} otherwise.
     * @throws Exception
     */
    public boolean removeAndStopDropinsApplications(String... fileNames) throws Exception {
        boolean allSucceeded = true;

        for (String fileName : fileNames) {
            allSucceeded = allSucceeded && removeAndStopDropinsApplication(fileName);
        }

        return allSucceeded;
    }

    /**
     *
     * Removes an application from dropins and waits for it to stop
     *
     * @param  fileNames the file name or names of the application, e.g. snoop.war
     * @return           {@code true} if the applications were moved successfully, {@code false} otherwise.
     * @throws Exception
     */
    private boolean removeAndStopDropinsApplication(String appFileName) throws Exception {
        final String method = "removeAndStopDropinsApplication";

        setMarkToEndOfLog();

        String appName = appFileName.substring(0, appFileName.lastIndexOf("."));
        String appInDropinsPath = serverRoot + "/dropins/" + appFileName;
        String nonDropinsFilePath = serverRoot + "/" + appFileName;

        if (!LibertyFileManager.renameLibertyFile(machine, appInDropinsPath, nonDropinsFilePath)) { // throws Exception
            Log.info(c, method, "Unable to move " + appFileName + " out of dropins, failing.");
            return false;
        } else {
            Log.info(c, method, appFileName + " successfully moved out of dropins, waiting for message...");
        }

        String stopMsg = waitForStringInLogUsingMark("CWWKZ0009I:.*" + appName); // throws Exception

        if (stopMsg == null) {
            return false;
        }

        return true;
    }

    /**
     * Restores one or more applications to dropins that has been removed by removeDropinsApplication.
     *
     * This assumes that the server is now running and checks waits for the app to startup
     * in the logs.
     *
     * @param  fileNames the file name or names of the application, e.g. snoop.war
     * @return           {@code true} if the applications were started successfully, {@code false} otherwise.
     * @throws Exception
     */
    public boolean restoreDropinsApplications(String... fileNames) throws Exception {
        final String method = "restartDropinsApplication";

        boolean allSucceeded = true;

        for (String fileName : fileNames) {
            String appName = fileName.substring(0, fileName.lastIndexOf("."));
            String dropinsFilePath = serverRoot + "/dropins/" + fileName;
            String nonDropinsFilePath = serverRoot + "/" + fileName;

            String startMsg = null;

            if (LibertyFileManager.renameLibertyFile(machine, nonDropinsFilePath, dropinsFilePath)) {
                Log.info(c, method, fileName + " successfully moved back into dropins, waiting for message...");
                startMsg = waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName);
                allSucceeded = allSucceeded && (startMsg != null);
            } else {
                Log.info(c, method, "Unable to move " + fileName + " back into dropins, failing.");
                allSucceeded = false;
            }
        }
        return allSucceeded;
    }

    public void setLogsRoot(String root) {
        this.logsRoot = root;
    }

    public String getLogsRoot() {
        return logsRoot;
    }

    public void setExtraArgs(List<String> extraArgs) {
        this.extraArgs.clear();
        this.extraArgs.addAll(extraArgs);
    }

    /**
     * Issues a server script command against this server
     *
     * @param  command      command name
     * @param  optionalArgs any optional args needed by the command
     * @throws Exception    if the operation fails
     * @return              the output of the command
     */
    public ProgramOutput executeServerScript(String command, String[] optionalArgs) throws Exception {
        final String method = "executeServerScript";
        Log.info(c, method, "Running server script with command=" + command, optionalArgs);

        String cmd = installRoot + "/bin/server";

        // organize parms properly - the command name comes first, followed by the server name, followed
        // by an optional arguments
        String[] parms;
        if (optionalArgs == null) {
            parms = new String[2];
        } else {
            parms = new String[2 + optionalArgs.length];
            System.arraycopy(optionalArgs, 0, parms, 2, optionalArgs.length);
        }
        parms[0] = command;
        parms[1] = serverToUse;

        Properties envVars = null;
        if (customUserDir) {
            envVars = new Properties();
            envVars.setProperty("WLP_USER_DIR", userDir);
        }

        return LibertyServerUtils.execute(machine, machineJava, envVars, cmd, parms);
    }

    /**
     * If the server is running, this will execute:
     * WLP/bin/server javadump SERVERNAME
     *
     * which should generate a javacore / thread dump
     */
    public ProgramOutput javadumpThreads() throws Exception {
        final String method = "javadumpThreads";
        Log.info(c, method, "Acquiring thread dump");

        String cmd = installRoot + "/bin/server";
        String[] parms = { "javadump", serverToUse };

        Properties envVars = new Properties();
        envVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            envVars.setProperty("WLP_USER_DIR", userDir);
        Log.info(c, method, "Using additional env props: " + envVars.toString());

        ProgramOutput output = machine.execute(cmd, parms, envVars);
        String stdout = output.getStdout();
        Log.info(c, method, "Server javadump output: " + stdout);
        Log.info(c, method, "Return code from javadump is: " + output.getReturnCode());

        return output;
    }

    /**
     * If the server is running, this will execute:
     * WLP/bin/server dump SERVERNAME
     *
     * which should generate a zip file with contents from the
     * server's directory and introspectors (includes threads). This will contain
     * information about registered services, etc. Which can help identify missing
     * dependencies.
     */
    public ProgramOutput serverDump() throws Exception {
        return serverDump(null);
    }

    /**
     * If the server is running, this will execute:
     * WLP/bin/server dump SERVERNAME --include=&lt;includeParameter&gt;
     *
     * which should generate a zip file with contents from the
     * server's directory and introspectors (includes threads). This will contain
     * information about registered services, etc. Which can help identify missing
     * dependencies.
     *
     * @param includeParameter server dump parameter -include=value
     */
    public ProgramOutput serverDump(String includeParameter) throws Exception {

        final String method = "serverDump";
        Log.info(c, method, "Acquiring full dump");

        String cmd = installRoot + "/bin/server";
        String[] parms;
        if (includeParameter == null)
            parms = new String[] { "dump", serverToUse };
        else
            parms = new String[] { "dump", serverToUse, "--include=" + includeParameter };

        Properties envVars = new Properties();
        envVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            envVars.setProperty("WLP_USER_DIR", userDir);
        Log.info(c, method, "Using additional env props: " + envVars.toString());

        ProgramOutput output = machine.execute(cmd, parms, envVars);
        String stdout = output.getStdout();
        Log.info(c, method, "Server dump output: " + stdout);
        Log.info(c, method, "Return code from dump is: " + output.getReturnCode());

        return output;
    }

    public void setupForRestConnectorAccess() throws Exception {
        if (isStarted) {
            throw new IllegalStateException("Must call setupForRestConnectorAccess BEFORE starting the server");
        }

        //if the key file was generated by another test using the same server then it will be backed up in tmp
        //copy it back to the server...
        LocalFile keyFile = new LocalFile(pathToAutoFVTTestFiles + "/tmp/key.jks");
        if (keyFile.exists())
            keyFile.copyToDest(new RemoteFile(getMachine(), getServerRoot() + "/resources/security/key.jks"));
        else {
            keyFile = new LocalFile(pathToAutoFVTTestFiles + "/tmp/key.p12");
            if (keyFile.exists())
                keyFile.copyToDest(new RemoteFile(getMachine(), getServerRoot() + "/resources/security/key.p12"));
        }

        // Set up the trust store
        //System.setProperty("javax.net.ssl.trustStore", getServerRoot() + "/resources/security/key.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "Liberty");

        checkForRestConnector.set(true);
    }

    /**
     * Creates a JMX rest connection to the server using the following user name, password and keystore password:
     * "theUser", "thePassword", "Liberty".
     *
     * If you need to connect with different values, use {@link #getJMXRestConnector(String, String, String)}.
     *
     * @return           JMXConnector connected to the server
     * @throws Exception If anything goes wrong!
     */
    public JMXConnector getJMXRestConnector() throws Exception {
        return getJMXRestConnector(getHttpDefaultSecurePort());
    }

    /**
     * Creates a JMX rest connection to the server using the following user name, password and keystore password:
     * "theUser", "thePassword", "Liberty".
     *
     * If you need to connect with different values, use {@link #getJMXRestConnector(String, String, String)}.
     *
     * @return           JMXConnector connected to the server
     * @throws Exception If anything goes wrong!
     */
    public JMXConnector getJMXRestConnector(int port) throws Exception {
        final String userName = "theUser";
        final String password = "thePassword";
        final String keystorePassword = "Liberty";

        return getJMXRestConnector(userName, password, keystorePassword, port);
    }

    /**
     * Creates a JMX rest connection to the server.
     *
     * @param  userName         The admin user
     * @param  password         The admin user password
     * @param  keystorePassword The keystore password used to open the server's key.jks
     * @return                  JMXConnector connected to the server
     * @throws Exception        If anything goes wrong!
     */
    public JMXConnector getJMXRestConnector(String userName, String password, String keystorePassword) throws Exception {
        return getJMXRestConnector(userName, password, keystorePassword, getHttpDefaultSecurePort());
    }

    private JMXConnector getJMXRestConnector(String userName, String password, String keystorePassword, int port) throws Exception {
        String METHOD_NAME = "getJMXRestConnector";
        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { userName, password });
        environment.put("com.ibm.ws.jmx.connector.client.disableURLHostnameVerification", true);
        environment.put("com.ibm.ws.jmx.connector.client.readTimeout", 2 * 60 * 1000);

        // Load the keystore file from the file system.
        KeyStore keyStore = KeyStore.getInstance("JKS");
        String path = getServerRoot() + "/resources/security/key.jks";
        File keyFile = new File(path);
        if (!keyFile.exists()) {
            path = getServerRoot() + "/resources/security/key.p12";
            keyFile = new File(path);
        }

        FileInputStream is = new FileInputStream(keyFile);
        byte[] fileBytes = new byte[(int) keyFile.length()];
        is.read(fileBytes);

        // Load the file to the Keystore object as type JKS (default).
        // If the load fails with an IOException, try to load it as type PKCS12.
        // Note that in java 9, dynamically generated keystores using java's keytool will be of type PKCS12 because
        // that is the new default. See this link for more information: http://openjdk.java.net/jeps/229
        // The code below will handle paltform differences and JDK level differences.
        try {
            Log.info(c, METHOD_NAME, "Loading keystore: " + path + " as type: " + keyStore.getType() + ". Bytes read from file: " + fileBytes.length);
            keyStore.load(new ByteArrayInputStream(fileBytes), keystorePassword.toCharArray());
        } catch (IOException ioe) {
            keyStore = KeyStore.getInstance("PKCS12");
            Log.info(c, METHOD_NAME, "Loading keystore: " + path + " as type: " + keyStore.getType() + ". Bytes read from file: " + fileBytes.length);
            keyStore.load(new ByteArrayInputStream(fileBytes), keystorePassword.toCharArray());
        }

        is.close();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        environment.put("com.ibm.ws.jmx.connector.client.CUSTOM_SSLSOCKETFACTORY", sslContext.getSocketFactory());

        JMXServiceURL url = new JMXServiceURL("REST", getHostname(), port, "/IBMJMXConnectorREST");

        JMXConnector jmxConnector = JMXConnectorFactory.connect(url, environment);
        Log.info(c, METHOD_NAME, "Created JMX connector to server with URL: " + url + " Connector: " + jmxConnector);

        return jmxConnector;
    }

    /**
     * Retrieves an {@link ApplicationMBean} for a particular application on this server
     *
     * @param  applicationName the name of the application to operate on
     * @return                 an {@link ApplicationMBean}
     * @throws JmxException    if the object name for the input application cannot be constructed
     */
    public ApplicationMBean getApplicationMBean(String applicationName) throws JmxException {
        return new ApplicationMBean(getJmxServiceUrl(), applicationName);
    }

    /**
     * Get the JMX connection URL of this server
     *
     * @return              a {@link JMXServiceURL} that allows you to invoke MBeans on the server
     * @throws JmxException
     *                          if the server can't be found,
     *                          the localConnector-1.0 feature is not enabled,
     *                          or the address file is not valid
     */
    public JMXServiceURL getJmxServiceUrl() throws JmxException {
        return JmxServiceUrlFactory.getInstance().getUrl(this);
    }

    /**
     * Restarts an application via its MBean
     *
     * @param  applicationName the application to be restarted
     * @throws JmxException
     */
    public void restartApplication(String applicationName) throws JmxException {
        getApplicationMBean(applicationName).restart();
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
    public String waitForStringInTraceUsingLastOffset(String regexp) {
        return waitForStringInTraceUsingLastOffset(regexp, LOG_SEARCH_TIMEOUT);
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
    public String waitForStringInTraceUsingLastOffset(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingLastOffset(regexp, timeout, getMostRecentTraceFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in default log file due to exception " + e);
            return null;
        }
    }

    /**
     * Wait for the specified regex in the most recent trace log from the last mark.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     *
     * @param  regexp a regular expression to search for
     * @return        the matching line in the log, or null if no matches
     *                appear before the timeout expires
     */
    public String waitForStringInTraceUsingMark(String regexp) {
        return waitForStringInTraceUsingMark(regexp, LOG_SEARCH_TIMEOUT);
    }

    /**
     * Wait for the specified regex in the most recent trace log from the last mark.
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
    public String waitForStringInTraceUsingMark(String regexp, long timeout) {
        try {
            return waitForStringInLogUsingMark(regexp, timeout, getMostRecentTraceFile());
        } catch (Exception e) {
            Log.warning(c, "Could not find string in trace log file due to exception " + e);
            return null;
        }
    }

    public int getHttpSecondaryPort() {
        return httpSecondaryPort;
    }

    public void setHttpSecondaryPort(int httpSecondaryPort) {
        this.httpSecondaryPort = httpSecondaryPort;
    }

    public int getHttpSecondarySecurePort() {
        return httpSecondarySecurePort;
    }

    public void setHttpSecondarySecurePort(int httpSecondarySecurePort) {
        this.httpSecondarySecurePort = httpSecondarySecurePort;
    }

    /**
     * Add to a list of strings that will be ignored when the server logs are
     * checked for errors on shutdown. The list is cleared every time the server
     * is stopped.
     *
     * @param regexes
     */
    public void addIgnoredErrors(List<String> regexes) {
        ignoredErrors.addAll(regexes);
    }

    /**
     * Populates a list with a fixed set error and warning messages to be ignored for
     * those buckets that choose to care about error or warning messages when the server
     * is stopped.
     *
     * Some history:
     * Starting in 2Q15, the default behavior was made for buckets to
     * care about waring/error messages. Buckets prior to 2Q15 and those that did not
     * care about those messages were excluded from the default behavior by being listed
     * under ant_build\resources\configuration\exemptServersList.txt.
     *
     * Issue:
     * Buckets that care about error/warning messages now fail sporadically because
     * of messages that show in the logs for reasons completely external to the FAT
     * being run.
     *
     * Workaround:
     * Create a new global list to account for failures that are proven to be
     * not test case specific and could apply to all buckets that are sensitive to
     * error/warning messages. It is true that this list could become really long
     * as unforseen and unrelated issues take place, but the expectation is that if
     * error/warning messages are seen, the first course of action would be to
     * report/fix the issue being exposed by the message. In certain cases; however,
     * the errors reported could be unpredictable and could not be prevented reliably.
     * It is only then that you may concider adding it to the list.
     */
    private void populateFixedListOfMessagesToIgnore() {

        // Added to stop iFix/testFix builds failing when listing warning message of testFix installed,
        // of course there is a test fix installed ...it is a test fix build
        fixedIgnoreErrorsList.add("CWWKF0014W:");
        // Added due to build break defect 168264. See defect for more details.
        fixedIgnoreErrorsList.add("CWWKF0017E.*cik.ext.product1.properties");
        // Added due to build break defect 221453.
        fixedIgnoreErrorsList.add("CWWKG0011W");
    }

    public boolean isJava2SecurityEnabled() {
        boolean globalEnabled = GLOBAL_JAVA2SECURITY || GLOBAL_DEBUG_JAVA2SECURITY;
        if (!globalEnabled)
            return false;

        // Allow servers to opt-out of j2sec by setting websphere.java.security.exempt=true in their ${server.config.dir}/bootstrap.properties
        boolean isJava2SecExempt = "true".equalsIgnoreCase(getBootstrapProperties().getProperty("websphere.java.security.exempt"));
        Log.info(c, "isJava2SecurityEnabled", "Is server " + getServerName() + " Java 2 Security exempt?  " + isJava2SecExempt);
        return !isJava2SecExempt;
    }

    /**
     * No longer using bootstrap properties to update server config for database rotation.
     * Instead look at using the fattest.databases module
     */
    @Deprecated
    public void configureForAnyDatabase() throws Exception {
        ServerConfiguration config = this.getServerConfiguration();
        config.updateDatabaseArtifacts();
        this.updateServerConfiguration(config);
    }

    public boolean isIBMJVM() {
        return javaInfo.vendor() == JavaInfo.Vendor.IBM;
    }

    public boolean isOracleJVM() {
        return javaInfo.vendor() == JavaInfo.Vendor.SUN_ORACLE;
    }

    public void useSecondaryHTTPPort() {
        setHttpDefaultPort(getHttpSecondaryPort());
        setHttpDefaultSecurePort(getHttpSecondarySecurePort());
    }

    public void setConsoleLogName(String consoleLogName) {
        this.consoleFileName = consoleLogName;
    }

    public void setAdditionalSystemProperties(Map<String, String> additionalSystemProperties) {
        this.additionalSystemProperties = additionalSystemProperties;
    }

    public void clearAdditionalSystemProperties() {
        this.additionalSystemProperties.clear();
    }

    /**
     * Sets the flag that prevents FFDCs associated with this server from being collected and
     * reported to the logic that checks for unexpected FFDCs during server stop.
     *
     * NOTE: The use of this method to disable unexpected FFDC checking is discouraged unless
     * there is a real need for it. The value of this variable is true by default.
     *
     * @param ffdcChecking. False to disable unexpected FFDC checking. True, otherwise.
     */
    public void setFFDCChecking(boolean ffdcChecking) {
        this.ffdcChecking = ffdcChecking;
    }

    /**
     * Returns true if unexpected FFDC checking is enabled. False, otherwise.
     *
     * @return True if unexpected FFDC checking is enabled. False, otherwise.
     */
    public boolean getFFDCChecking() {
        return this.ffdcChecking;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcGetDefaultLogFile()
     */
    @Override
    public RemoteFile lmcGetDefaultLogFile() throws Exception {
        return getDefaultLogFile();
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcClearLogOffsets()
     */
    @Override
    public void lmcClearLogOffsets() {
        logOffsets.clear();
        originOffsets.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcResetLogOffsets()
     */
    @Override
    public void lmcResetLogOffsets() {
        logOffsets = new HashMap<String, Long>(originOffsets);
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcSetOriginLogOffsets()
     */
    @Override
    public void lmcSetOriginLogOffsets() {
        originOffsets = new HashMap<String, Long>(logOffsets);
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcUpdateLogOffset(java.lang.String, java.lang.Long)
     */
    @Override
    public void lmcUpdateLogOffset(String logFile, Long newLogOffset) {
        updateLogOffset(logFile, newLogOffset);
    }

    public void setBvtPortPropertyName(String propertyName) {
        bvtPortPropertyName = propertyName;
    }

    public void setBvtSecurePortPropertyName(String propertyName) {
        bvtSecurePortPropertyName = propertyName;
    }

    public int getBvtPort() {
        if (bvtPortPropertyName != null) {
            return Integer.getInteger(bvtPortPropertyName);
        } else {
            return getHttpDefaultPort();
        }
    }

    public int getBvtSecurePort() {
        if (bvtSecurePortPropertyName != null) {
            return Integer.getInteger(bvtSecurePortPropertyName);
        } else {
            return getHttpDefaultSecurePort();
        }
    }

    @Override
    public String toString() {
        return serverToUse + " : " + super.toString();
    }
}
