/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.soe_reporting.SOEHttpPostUtil;
import com.ibm.ws.fat.util.ACEScanner;
import com.ibm.ws.fat.util.Props;
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
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTestAction;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyFileManager.LogSearchResult;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;
import componenttest.topology.utils.PrivHelper;
import componenttest.topology.utils.ServerFileUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

public class LibertyServer implements LogMonitorClient {

    protected static final Class<?> c = LibertyServer.class;
    protected static final String CLASS_NAME = c.getName();
    protected static Logger LOG = Logger.getLogger(CLASS_NAME); // why don't we always use the Logger directly?
    private final static String LS = System.getProperty("line.separator");
    public final static String LIBERTY_ERROR_REGEX = "^.*[EW] .*\\d{4}[EW]:.*$";

    /** How frequently we poll the logs when waiting for something to happen */
    protected static final int WAIT_INCREMENT = 300;
    private static final String SPECIAL_CHARS = "\\`$\"'!&|;()<>*?[]{} ";

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
    public static final boolean DEFAULT_PRE_CLEAN = true;
    public static final boolean DEFAULT_CLEANSTART = Boolean.parseBoolean(PrivHelper.getProperty("default.clean.start", "true"));
    public static final boolean DEFAULT_VALIDATE_APPS = true;
    protected static final String RELEASE_MICRO_VERSION = PrivHelper.getProperty("micro.version");
    protected static final String TMP_DIR = PrivHelper.getProperty("java.io.tmpdir");
    public static boolean validateApps = DEFAULT_VALIDATE_APPS;

    protected static final JavaInfo javaInfo = JavaInfo.forCurrentVM();

    protected static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");
    protected static final boolean GLOBAL_JAVA2SECURITY = javaInfo.MAJOR > 17 ? false : Boolean.parseBoolean(PrivHelper.getProperty("global.java2.sec", "false"));
    protected static final boolean GLOBAL_DEBUG_JAVA2SECURITY = javaInfo.MAJOR > 17 ? false : FAT_TEST_LOCALRUN //
                    ? Boolean.parseBoolean(PrivHelper.getProperty("global.debug.java2.sec", "true")) //
                    : Boolean.parseBoolean(PrivHelper.getProperty("global.debug.java2.sec", "false"));

    //should the check for repeated features throw an exception
    protected static final String REPEAT_FEATURE_CHECK_ERROR_PROP = "fat.test.repeat.feature.check.error";
    protected static final boolean REPEAT_FEATURE_CHECK_ERROR = Boolean.parseBoolean(PrivHelper.getProperty(REPEAT_FEATURE_CHECK_ERROR_PROP, "true"));

    //FIPS 140-3
    protected static final boolean GLOBAL_FIPS_140_3 = Boolean.parseBoolean(PrivHelper.getProperty("global.fips_140-3", "false"));

    protected static final String GLOBAL_TRACE = PrivHelper.getProperty("global.trace.spec", "").trim();
    protected static final String GLOBAL_JVM_ARGS = PrivHelper.getProperty("global.jvm.args", "").trim();

    protected static final boolean DO_COVERAGE = PrivHelper.getBoolean("test.coverage");
    protected static final String JAVA_AGENT_FOR_JACOCO = PrivHelper.getProperty("javaagent.for.jacoco");

    protected static final int SERVER_START_TIMEOUT = (FAT_TEST_LOCALRUN ? 15 : 120) * 1000;
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
    protected static final String OPENLIBERTY_PROPERTIES_FILE_NAME = "openliberty.properties";
    protected static final String COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY = "com.ibm.websphere.productVersion";

    protected static final String EBCDIC_CHARSET_NAME = "IBM1047";

    protected volatile boolean isStarted = false;
    public volatile boolean startedWithJavaSecurity = false;
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

    private boolean isTidy = false;

    private boolean needsPostTestRecover = true;

    private boolean logOnUpdate = true;

    protected boolean debuggingAllowed = true;

    private Properties openLibertyProperties;

    private String openLibertyVersion;

    private String archiveMarker = null;

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
     * @return the installRootParent
     */
    public String getInstallRootParent() {
        return installRootParent;
    }

    /**
     * @return the release micro version
     */
    public String getMicroVersion() {
        return RELEASE_MICRO_VERSION;

        // The micro version is set by by 'cnf\build.gradle':
        //   inputs.file('resources/bnd/liberty-release.props')
        // And by 'wlp-gradle/subprojects/fat.gradle':
        //   bndProps.setProperty('micro.version', bnd.get('libertyBundleMicroVersion'))
        //
        // The micro version is used as a suffix to the base name of
        // library jars, for example:
        //
        // libertyBundleMicroVersion=78
        //
        // -rw-rw-rw-  1 874973897 874973897  348465 06-12 22:12 com.ibm.ws.kernel.boot.archive_1.0.78.jar
        // -rw-rw-rw-  1 874973897 874973897  829676 06-13 16:11 com.ibm.ws.kernel.boot_1.0.78.jar
        // -rw-rw-rw-  1 874973897 874973897  829671 06-28 12:07 com.ibm.ws.kernel.boot_1.0.78.jar
        //
        // Errors can occur if the micro version does not match the names under 'wlp/lib'.
        // This occurs rarely during official builds, possibly because of the splicing of
        // a newer WAS liberty build with an older open liberty build.
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
    protected final LogMonitor logMonitor;

    public LogMonitor getLogMonitor() {
        return logMonitor;
    }

    private boolean newLogsOnStart = FileLogHolder.NEW_LOGS_ON_START_DEFAULT;

    public void setCheckpoint(CheckpointPhase phase) {
        setCheckpoint(phase, true, null);
    }

    /**
     * When server.start is executed, perform a
     *
     * <pre>
     * <code> "bin/server checkpoint --at=phase"</code>, followed by
     * <code> "bin/server start"</code>
     * </pre>
     *
     * @param phase         The phase at which to take the checkpoint. Must be non-null.
     * @param autoRestore   if true initiate restore as part of serverStart
     * @param beforeRestore beforeRestore lambda is called just before the server start
     */
    public void setCheckpoint(CheckpointPhase phase, boolean autoRestore, Consumer<LibertyServer> beforeRestoreLambda) {
        checkpointInfo = new CheckpointInfo(phase, autoRestore, beforeRestoreLambda);
    }

    public void setCheckpoint(CheckpointInfo checkpointInfo) {
        this.checkpointInfo = checkpointInfo;
    }

    public void unsetCheckpoint() {
        checkpointInfo = null;
    }

    private CheckpointInfo checkpointInfo;

    public boolean isCriuRestoreDisableRecovery() {
        return checkpointInfo.criuRestoreDisableRecovery;
    }

    public void setCriuRestoreDisableRecovery(boolean value) {
        checkpointInfo.criuRestoreDisableRecovery = value;
    }

    public LibertyServer addCheckpointRegexIgnoreMessage(String regEx) {
        checkpointInfo.checkpointRegexIgnoreMessages.add(regEx);
        return this;
    }

    public LibertyServer addCheckpointRegexIgnoreMessages(String... regExs) {
        for (String regEx : regExs) {
            checkpointInfo.checkpointRegexIgnoreMessages.add(regEx);
        }
        return this;
    }

    public void addEnvVarsForCheckpoint(Map<String, String> props) throws Exception {
        File serverEnvFile;
        if (fileExistsInLibertyServerRoot("server.env")) {
            serverEnvFile = new File(getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        } else {
            serverEnvFile = new File(getServerRoot() + "/" + "server.env");
            serverEnvFile.createNewFile();
        }
        Properties mergeProps = new Properties();
        try (InputStream in = new FileInputStream(serverEnvFile)) {
            mergeProps.load(in);
        }
        mergeProps.putAll(props);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(serverEnvFile), "8859_1"))) {
            for (Entry<Object, Object> entry : mergeProps.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue());
                bw.newLine();
            }
        }
    }

    public static class CheckpointInfo {
        final Consumer<LibertyServer> defaultPreCheckpointLambda = (LibertyServer s) -> {
            Log.debug(c, "No preCheckpointLambda supplied.");
        };
        final Consumer<LibertyServer> defaultPostCheckpointLambda = (LibertyServer s) -> {
            Log.debug(c, "No postCheckpointLambda supplied.");
        };

        public CheckpointInfo(CheckpointPhase phase, boolean autorestore, Consumer<LibertyServer> postCheckpointLambda) {
            this(phase, autorestore, false, false, postCheckpointLambda);
        }

        public CheckpointInfo(CheckpointPhase phase, boolean autorestore, boolean expectCheckpointFailure, boolean expectRestoreFailure,
                              Consumer<LibertyServer> postCheckpointLambda) {
            this(phase, autorestore, expectCheckpointFailure, expectRestoreFailure, null, postCheckpointLambda);
        }

        public CheckpointInfo(CheckpointPhase phase, boolean autorestore, boolean expectCheckpointFailure, boolean expectRestoreFailure,
                              Consumer<LibertyServer> preCheckpointLambda, Consumer<LibertyServer> postCheckpointLambda) {
            if (phase == null) {
                throw new IllegalArgumentException("Phase must not be null");
            }

            this.checkpointPhase = phase;
            this.autoRestore = autorestore;
            this.expectCheckpointFailure = expectCheckpointFailure;
            this.expectRestoreFailure = expectRestoreFailure;
            if (preCheckpointLambda == null) {
                this.preCheckpointLambda = defaultPreCheckpointLambda;
            } else {
                this.preCheckpointLambda = (LibertyServer svr) -> {
                    Log.debug(c, "Begin execution of supplied preCheckpointLambda.");
                    preCheckpointLambda.accept(svr);
                    Log.debug(c, "Excecution of supplied preCheckpointLambda complete.");
                };
            }
            if (postCheckpointLambda == null) {
                this.postCheckpointLambda = defaultPostCheckpointLambda;
            } else {
                this.postCheckpointLambda = (LibertyServer svr) -> {
                    Log.debug(c, "Begin execution of supplied postCheckpointLambda.");
                    postCheckpointLambda.accept(svr);
                    Log.debug(c, "Excecution of supplied postCheckpointLambda complete.");
                };
            }
        }

        String phaseToCommandLineArg() {
            return phaseArgument.length() > 0 ? phaseArgument : checkpointPhase.name();
        }

        /*
         * parameters to configure a checkpoint/restore test
         */
        private final CheckpointPhase checkpointPhase; //Phase to checkpoint
        private String phaseArgument = ""; //phase string on command line. Added strictly to allow validity testing the bin/server inputs
        private final boolean autoRestore; // weather or not to perform restore after checkpoint
        //AN optional function executed after checkpoint but before restore
        private final Consumer<LibertyServer> preCheckpointLambda;
        private final Consumer<LibertyServer> postCheckpointLambda;
        private final boolean expectCheckpointFailure;
        private final boolean expectRestoreFailure;
        /*
         * save intermediate results of ongoing checkpoint restore test
         */
        // TODO these booleans don't seem to ever get set to true
        private final boolean validateApps = false;
        private final boolean validateTimedExit = false;
        //Check log on serverStop for unintentional app restart after restore.
        private boolean assertNoAppRestartOnRestore = true;

        public CheckpointInfo setPhaseArgument(String pa) {
            phaseArgument = pa;
            return this;
        }

        /**
         * @return the assertNoAppRestartOnRestore
         */
        public boolean isAssertNoAppRestartOnRestore() {
            return assertNoAppRestartOnRestore;
        }

        /**
         * @param assertNoAppRestartOnRestore the assertNoAppRestartOnRestore to set
         */
        public void setAssertNoAppRestartOnRestore(boolean assertNoAppRestartOnRestore) {
            this.assertNoAppRestartOnRestore = assertNoAppRestartOnRestore;
        }

        /**
         * If true, auto-recovery is disabled. Otherwise, auto-recovery is enabled. The default is true (disabled).
         * <p>
         * Auto-recovery will launch a clean start of the server if a checkpoint restore fails.
         */
        private boolean criuRestoreDisableRecovery = true;

        private Properties checkpointEnv = null;

        /**
         * Set of regular expressions to match against lines to ignore in the post checkpoint log files. Error / Warning messages found
         * in the post checkpoint log not matching any of these expressions will result in test failure
         */
        private final List<String> checkpointRegexIgnoreMessages = new ArrayList<String>();

        public void setCheckpointEnv(Properties checkpointEnv) {
            this.checkpointEnv = checkpointEnv;
        }

        public Properties getCheckpointEnv() {
            return this.checkpointEnv;
        }

    }

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
     * @param  bootstrap               The bootstrap properties for this server
     * @param  deleteServerDirIfExist  If true and if the specified server name already exists on the file system, it will be deleted
     * @param  usePreviouslyConfigured If true do not tidy existing server
     * @param  winServiceOption
     * @throws Exception
     */
    LibertyServer(String serverName, Bootstrap bootstrap,
                  boolean deleteServerDirIfExist, boolean usePreviouslyConfigured,
                  LibertyServerFactory.WinServiceOption winServiceOption) throws Exception {

        final String method = "setup";
        Log.entering(c, method);

        this.serverTopologyID = bootstrap.getValue("ServerTopologyID");
        this.hostName = bootstrap.getValue("hostName");
        this.machineJava = bootstrap.getValue(hostName + ".JavaHome");

        if (serverName != null) {
            this.serverToUse = serverName;
            this.pathToAutoFVTNamedServer += serverToUse + "/";
        } else {
            this.serverToUse = bootstrap.getValue("serverName");
            if (this.serverToUse == null || this.serverToUse.trim().equals("")) {
                this.serverToUse = DEFAULT_SERVER;
            }
        }

        if (winServiceOption == LibertyServerFactory.WinServiceOption.ON) {
            this.runAsAWindowService = true;
        } else {
            this.runAsAWindowService = false;
        }

        String newLogsOnStartProperty = bootstrap.getValue(FileLogHolder.NEW_LOGS_ON_START_PROPERTY);
        if (newLogsOnStartProperty != null) {
            this.newLogsOnStart = Boolean.parseBoolean(newLogsOnStartProperty);
        }

        // This is the only case where we will allow the messages.log name to  be changed
        // by the fat framework -- because we want to look at messasges.log for start/stop/blah
        // messags, we shouldn't be pointing it all over everywhere else. For those FAT tests
        // that need a messages file in an alternate location, they should set the corresponding
        // com.ibm.ws.logging.message.file.name property in bootstrap.properties
        String nonDefaultLogFile = bootstrap.getValue("NonDefaultConsoleLogFileName");
        if (nonDefaultLogFile != null && nonDefaultLogFile.startsWith("SERVER_NAME/")) {
            this.relativeLogsRoot = "/logs/" + serverToUse + "/";
            this.messageFileName = nonDefaultLogFile.substring(12);
        } else {
            this.relativeLogsRoot = "/logs/";
            this.messageFileName = DEFAULT_MSG_FILE;
        }

        try {
            this.osgiConsolePort = Integer.parseInt(bootstrap.getValue("osgi.console"));
        } catch (Exception e) {
            Log.debug(c, "No osgi.console set in bootstrap.properties.  Will use default value: " + this.osgiConsolePort);
        }

        try {
            this.httpDefaultPort = Integer.parseInt(bootstrap.getValue("http.Default.Port"));
        } catch (Exception e) {
            Log.debug(c, "No http.Default.Port set in bootstrap.properties.  Will use default value: " + this.httpDefaultPort);
        }

        try {
            this.httpDefaultSecurePort = Integer.parseInt(bootstrap.getValue("http.Default.Secure.Port"));
        } catch (Exception e) {
            Log.debug(c, "No http.Default.Secure.Port set in bootstrap.properties.  Will use default value: " + this.httpDefaultSecurePort);
        }

        try {
            this.iiopDefaultPort = Integer.parseInt(bootstrap.getValue("IIOP"));
        } catch (Exception e) {
            Log.debug(c, "No iiop.Default.Port set in bootstrap.properties.  Will use default value: " + this.iiopDefaultPort);
        }

        if (this.machineJava == null) {
            throw new IllegalArgumentException("No " + this.hostName + ".JavaHome was set in " + bootstrap);
        }
        this.installRoot = bootstrap.getValue("libertyInstallPath");
        if (this.installRoot == null) {
            throw new IllegalArgumentException("No installRoot was set in " + bootstrap);
        }

        // TODO: Verify the micro version matches the files under 'installRoot/lib'.

        // Allow user directory name to be provided in bootstrap properties.
        // It is optional and if it is not set, setup() will set it.
        this.userDir = bootstrap.getValue("libertyUserDir");

        // Populate the fixed set error and warning messages to be ignored for those
        // buckets that choose to care about error or warning messages when the server
        // is stopped.
        this.populateFixedListOfMessagesToIgnore();

        // In the multinode test environment, one of the hosts may be the local machine
        // and therefore should be mapped to LocalMachine.  Simplicity only does this
        // mapping if the host name is "localhost".  For now a check is added here
        // to compare the host name to the local host name.  This is guarded by a
        // property "checkIfLocalHost" just in case someone doesn't want this behavior.
        String checkIfLocalHost = bootstrap.getValue("checkIfLocalHost");
        if (checkIfLocalHost != null && hostName.equals(InetAddress.getLocalHost().getHostName())) {
            this.machine = Machine.getLocalMachine();
            // Do not update hostName because Machine will say localhost!
        } else {
            this.machine = LibertyServerUtils.createMachine(bootstrap);
            // Update hostName to maintain old behavior of LibertyServer.getHostname()
            // which asked the machine.
            this.hostName = this.machine.getHostname();
        }

        this.logMonitor = new LogMonitor(this);

        this.setup(deleteServerDirIfExist, usePreviouslyConfigured);

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
        if (serverToUse == null) {
            serverToUse = DEFAULT_SERVER;
        }

        machineOS = machine.getOperatingSystem();
        installRoot = LibertyServerUtils.makeJavaCompatible(installRoot, machine);
        // Set default usr directory if not already set.
        if (userDir == null)
            userDir = installRoot + "/usr";
        else
            customUserDir = true;
        serverRoot = userDir + "/servers/" + serverToUse;
        serverOutputRoot = serverRoot;
        logsRoot = serverOutputRoot + relativeLogsRoot;
        messageAbsPath = logsRoot + messageFileName;
        traceAbsPath = logsRoot + traceFileName;

        // delete existing server directory if requested:
        if (deleteServerDirIfExist) {
            RemoteFile serverDir = machine.getFile(serverRoot);
            if (serverDir.exists() && !serverDir.delete()) {
                Exception ex = new TopologyException("Unable to delete pre-existing server directory: " + serverRoot);
                Log.error(c, "setup - User requested that we delete pre-existing server directory, but this operation failed - " + serverRoot, ex);
                throw ex;
            }
        }

        File installRootfile = new File(installRoot);
        installRootParent = installRootfile.getParent();

        // Now it sets all OS specific stuff
        machineJava = LibertyServerUtils.makeJavaCompatible(machineJava, machine);

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
            RemoteFile applicationsFolder = machine.getFile(userDir + "/shared/apps");
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
        RemoteFile serverXML = machine.getFile(serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
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
     * Set the platform / feature elements of the server.xml to the new platform / features values specified.
     * Each String should be the only platform or feature name, e.g. jakartaee-10.0 or servlet-3.0. If the
     * server.xml has no <featureManager> tag this method will simply duplicate the existing server.xml content.
     *
     * @param  newPlatforms
     * @param  newFeatures
     * @return
     * @throws Exception
     */
    public void changePlatformsAndFeatures(List<String> newPlatforms, List<String> newFeatures) throws Exception {
        RemoteFile serverXML = machine.getFile(serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        LocalFile tempServerXML = new LocalFile(SERVER_CONFIG_FILE_NAME);

        Writer w = new OutputStreamWriter(tempServerXML.openForWriting(false));
        InputStream originalOutput = serverXML.openForReading();
        InputStreamReader in2 = new InputStreamReader(originalOutput);
        Scanner s2 = new Scanner(in2);

        while (s2.hasNextLine()) {
            String line = s2.nextLine();
            // We've reached  the platform/feature elements
            if (line.contains("<featureManager>")) {
                // Skip until we reach the end tag
                while (s2.hasNextLine()) {
                    line = s2.nextLine();
                    if (line.contains("</featureManager>")) {
                        break;
                    }
                }

                // Now write the <featureManager> snippet to the temp xml
                w.write("   <featureManager>");
                w.write("\n");
                if (newPlatforms != null || newPlatforms.size() > 0) {
                    for (String platform : newPlatforms) {
                        w.write("               <platform>" + platform.trim() + "</platform>");
                        w.write("\n");
                    }
                }
                if (newFeatures != null || newFeatures.size() > 0) {
                    for (String feature : newFeatures) {
                        w.write("               <feature>" + feature.trim() + "</feature>");
                        w.write("\n");
                    }
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
        boolean rc = tempServerXML.copyToDest(serverXML, false, true);
        tempServerXML.delete();
    }

    /**
     * Copies the server.xml to the server.
     *
     * @throws Exception
     */
    public void refreshServerXMLFromPublish() throws Exception {
        RemoteFile serverXML = machine.getFile(serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        LocalFile publishServerXML = new LocalFile(PATH_TO_AUTOFVT_SERVERS + "/" + getServerName() + "/" + SERVER_CONFIG_FILE_NAME);

        publishServerXML.copyToDest(serverXML, false, true);
    }

    /**
     * Swaps in a different server.xml file from the server directory.
     *
     * @param  fileName  the name of a server.xml file found in the wlp/usr/[server]/ directory
     * @throws Exception
     */
    public void swapInServerXMLFromPublish(String fileName) throws Exception {
        RemoteFile serverXML = machine.getFile(serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        LocalFile publishServerXML = new LocalFile(PATH_TO_AUTOFVT_SERVERS + "/" + getServerName() + "/" + fileName);

        Log.info(c, "swapInServerXMLFromPublish", "Reconfiguring server to use config file: " + publishServerXML);
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

        String thisMethod = "reconfigureServerUsingExpandedConfiguration";
        ServerFileUtils serverFileUtils = new ServerFileUtils();
        String newServerCfg = serverFileUtils.expandAndBackupCfgFile(this, configDir + "/" + newConfig, testName);
        Log.info(c, thisMethod, "Reconfiguring server to use new config: " + newConfig);
        if (resetMark) {
            setMarkToEndOfLog();
        }
        replaceServerConfiguration(newServerCfg);

        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
        waitForConfigUpdateInLogUsingMark(listAllInstalledAppsForValidation(), waitForMessages);

        // wait for ssl port restart
        waitForSSLRestart();

    }

    public void waitForSSLRestart() throws Exception {

        String thisMethod = "waitForSSLRestart";
        // look for the "CWWKO0220I: TCP Channel defaultHttpEndpoint-ssl has stopped listening for requests on host " message
        // if we find it, then wait for "CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host"
        String sslStopMsg = waitForStringInLogUsingMark("CWWKO0220I:.*defaultHttpEndpoint-ssl.*", 500);
        if (sslStopMsg != null) {
            String sslStartMsg = waitForStringInLogUsingMark("CWWKO0219I:.*defaultHttpEndpoint-ssl.*");
            if (sslStartMsg == null) {
                Log.warning(c, "SSL may not have started properly - future failures may be due to this");
            } else {
                Log.info(c, thisMethod, "SSL appears have restarted properly");
            }
        } else {
            Log.info(c, thisMethod, "Did not detect a restart of the SSL port");
        }

    }

    /**
     * Wait for the server to state that it is listening on its SSL port
     *
     * @throws Exception
     */
    public void waitForSSLStart() throws Exception {
        //wait for "CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host"
        String sslStartMsg = waitForStringInLogUsingMark("CWWKO0219I:.*defaultHttpEndpoint-ssl.*");
        if (sslStartMsg == null) {
            RuntimeException rx = new RuntimeException("Timed out waiting for the server to initialize defaultHttpEndpoint-ssl");
            Log.error(c, "waitForSSLStart", rx);
            throw rx;
        }
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
     * @param consoleFileNameLog name that should be used for console log. It can be helpful
     *                               to have a console log file name that is related to (or describes) the test
     *                               case the server is used for.
     * @param cleanStart         if true, the server will be started with a clean start
     * @param preCleanServer     if true, the server directory will be reset before
     *                               the server is started (reverted to vanilla backup).
     * @param validateTimedExit  if true, the server will make sure that timedexit-1.0 is enabled
     */
    public void startServer(String consoleFileNameLog,
                            boolean cleanStart, boolean preCleanServer, boolean validateTimedExit) throws Exception {
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
     * @return                 the output of the start command
     */
    public ProgramOutput startServerExpectFailure(String consoleFileNameLog, boolean preClean, boolean cleanStart) throws Exception {
        this.consoleFileName = consoleFileNameLog;
        return startServerAndValidate(preClean, cleanStart, false, true, true);
    }

    /**
     * Given a formatted "CWWKO0221E" error string, parse out a port number and use it to invoke
     * printProcessHoldingPort
     *
     * Example error (newlines added for readability):
     *
     * [5/11/21 17:16:41:009 GMT] 00000026 com.ibm.ws.tcpchannel.internal.TCPPort
     * E CWWKO0221E: TCP Channel defaultHttpEndpoint-ssl initialization did not succeed.
     * The socket bind did not succeed for host * and port 8020.
     * The port might already be in use.
     * Exception Message: EDC8115I Address already in use.
     *
     * @param errorString in the format of "CWWKO0221E"
     */
    protected void printProcessHoldingPort(String errorString) {
        final String m = "printProcessHoldingPort";
        String portIndexString = "port ";
        int start = errorString.indexOf(portIndexString);
        if (start > 0) {
            start += portIndexString.length();
            int end = errorString.indexOf(".", start);
            if ((end - start) > 0) {
                try {
                    int port = Integer.parseInt(errorString.substring(start, end));
                    if (port > 0) {
                        printProcessHoldingPort(port);
                        return;
                    }
                } catch (NumberFormatException nfe) {
                    Log.info(c, m, "Failed to find a port number, cannot log the process holding the port");
                }
            }
        }
        Log.info(c, m, "Failed to find a port number, cannot log the process holding the port");
    }

    public void printProcesses() {
        printProcesses(machine);
    }

    public static void printProcesses(Machine host) {
        printProcesses(host, "");
    }

    public static void printProcesses(Machine host, String prefix) {
        final String m = "printProcesses";

        String timeStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
        String fileName = "processes-" + timeStamp + ".txt";
        if (prefix != null && !prefix.isEmpty()) {
            fileName = prefix + "-" + fileName;
        }
        Props properties = Props.getInstance();

        Log.info(c, m, "Printing processes to file: " + fileName);

        String filePath = properties.getFileProperty(Props.DIR_LOG).getAbsolutePath() + File.separator + fileName;
        PortDetectionUtil detector = PortDetectionUtil.getPortDetector(host);
        try {
            String processes = detector.listProcesses();
            if (processes != null) {
                try (PrintStream stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(filePath)), true, "UTF-8")) {

                    // Remove useless numbers and whitespace
                    StringTokenizer st = new StringTokenizer(processes, LS);
                    while (st.hasMoreTokens()) {
                        String s = st.nextToken().trim();
                        if (!s.matches("^\\d+$")) {
                            stream.println(s.replaceAll("\\s+", " "));
                        }
                    }
                } catch (Exception ex) {
                    Log.error(c, m, ex, "Caught exception while trying to list processes");
                }
            } else {
                Log.info(c, m, "Could not list processes");
            }
        } catch (Exception ex) {
            Log.error(c, m, ex, "Caught exception while trying to list processes");
        }
    }

    public void printProcessHoldingPort(int port) {
        final String m = "printProcessHoldingPort";
        try {
            PortDetectionUtil detector = PortDetectionUtil.getPortDetector(machine);
            Log.info(c, m, detector.determineCommandLineForPid(getPid()));
        } catch (Exception ex) {
            Log.error(c, m, ex, "Caught exception while trying to detect the process holding port " + port);
        }
    }

    public String getPid() {
        PortDetectionUtil detector = PortDetectionUtil.getPortDetector(machine);
        try {
            return detector.determinePidForPort(httpDefaultPort);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCommandLine() {
        PortDetectionUtil detector = PortDetectionUtil.getPortDetector(machine);
        try {
            return detector.determineCommandLineForPid(detector.determinePidForPort(httpDefaultPort));
        } catch (Exception e) {
            return null;
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
            Log.error(c, "checkPortsOpen", ex, "http default port (" + httpDefaultPort + ") is currently bound");
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
                    Log.error(c, "checkPortsOpen", ioe, "Failed to close socket. Port " + httpDefaultPort + " will still be bound.");
                }
            }
        }
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  cleanStart      if true, the server will be started with a clean start
     * @param  useValidateApps if true, block until all of the registered apps have started
     * @param  preCleanServer  if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                 the output of the start command
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart, boolean useValidateApps) throws Exception {
        return startServerAndValidate(preClean, cleanStart, useValidateApps, false);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  cleanStart      if true, the server will be started with a clean start
     * @param  useValidateApps if true, block until all of the registered apps have started
     * @param  preCleanServer  if true, the server directory will be reset before the server is started (reverted to vanilla backup).
     * @throws Exception
     * @return                 the output of the start command
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart, boolean useValidateApps, boolean expectStartFailure) throws Exception {
        return startServerAndValidate(preClean, cleanStart, useValidateApps, expectStartFailure, true);
    }

    /**
     * Start the server and validate that the server was started
     *
     * @param  preClean           if true, the server directory will be reset before
     *                                the server is started (reverted to vanilla backup).
     * @param  cleanStart         if true, the server will be started with a clean start
     * @param  useValidateApps    if true, block until all of the registered apps have started
     * @param  expectStartFailure if true, a the server is not expected to start
     *                                due to a failure
     * @param  validateTimedExit  if true, the server will make sure that timedexit-1.0 is enabled
     * @throws Exception
     */
    public ProgramOutput startServerAndValidate(boolean preClean, boolean cleanStart,
                                                boolean useValidateApps, boolean expectStartFailure,
                                                boolean validateTimedExit) throws Exception {
        return startServerWithArgs(preClean, cleanStart, useValidateApps, expectStartFailure, "start", null, validateTimedExit);
    }

    public enum IncludeArg {
        MINIFY, ALL, USR, RUNNABLE, MINIFYRUNNABLE;

        public String getIncludeString() {
            if (this.equals(MINIFYRUNNABLE)) {
                return "--include=" + "minify,runnable";
            } else {
                return "--include=" + toString().toLowerCase();
            }
        }
    }

    public void packageServer(IncludeArg include, String otherPackageArgs, String osFilter) throws Exception {
        ArrayList<String> args = setArgsExtended(include, otherPackageArgs, osFilter);
        startServerWithArgs(true, false, false, false, "package", args, true);
    }

    public void packageServer(IncludeArg include, String osFilter) throws Exception {
        ArrayList<String> args = setArgs(include, osFilter);
        startServerWithArgs(true, false, false, false, "package", args, true);
    }

    public void packageServerWithCleanStart(IncludeArg include, String osFilter) throws Exception {
        ArrayList<String> args = setArgs(include, osFilter);
        startServerWithArgs(true, true, false, false, "package", args, true);
    }

    protected ArrayList<String> setArgs(IncludeArg include, String osFilter) {
        ArrayList<String> args = new ArrayList<String>();

        args.add(include.getIncludeString());

        if (osFilter != null) {
            args.add("--os=" + osFilter);
        }
        return args;
    }

    protected ArrayList<String> setArgsExtended(IncludeArg include, String otherPackageArgs, String osFilter) {
        ArrayList<String> args = setArgs(include, osFilter);
        args.add(otherPackageArgs);

        return args;
    }

    public ProgramOutput startServerWithArgs(boolean preClean, boolean cleanStart,
                                             boolean useValidateApps, boolean expectStartFailure,
                                             String serverCmd, List<String> args,
                                             boolean validateTimedExit) throws Exception {
        final String method = "startServerWithArgs";
        Log.info(c, method, ">>> STARTING SERVER: " + getServerName());
        Log.info(c, method,
                 "Starting " + getServerName() + "; preClean=" + preClean + ", clean=" + cleanStart + ", validateApps=" + useValidateApps + ", expectStartFailure="
                            + expectStartFailure
                            + ", cmd=" + serverCmd + ", args=" + args);

        if (serverCleanupProblem) {
            throw new Exception("The server was not cleaned up on the previous test.");
        }

        //if we're (re-)starting then we must be untidy!
        isTidy = false;

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

        final Properties useEnvVars = new Properties();

        useEnvVars.putAll(envVars);
        if (!useEnvVars.isEmpty())
            Log.info(c, method, "Adding env vars: " + useEnvVars);
        envVars.clear();

        if (additionalSystemProperties != null && additionalSystemProperties.size() > 0) {
            useEnvVars.putAll(additionalSystemProperties);
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
            useEnvVars.setProperty("DEBUG_PORT", debugInfo.debugPort); // Not sure what this does.  It's not read by the FAT framework, for example. Was it meant to be usable for trace/debug?
            useEnvVars.setProperty("WLP_DEBUG_ADDRESS", debugInfo.debugPort);
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
        useEnvVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            useEnvVars.setProperty("WLP_USER_DIR", userDir);

        // Pick up global JVM args (forced by build properties)
        String JVM_ARGS = GLOBAL_JVM_ARGS;

        // Always set tmp dir.
        JVM_ARGS += " -Djava.io.tmpdir=" + TMP_DIR;

        // 207555: A number of FAT buckets establish a lot of secure connections and drain the entropy pool of /dev/random on Linux when
        // running Oracle/Sun JVMs - this results in buckets timing out as they wait for the entropy pool to be repopulated. Additionally,
        // from Java 9 onwards, IBM JDKs will also exhibit the same behaviour as it will start to use /dev/random by default.
        // The fix is thus to ensure we use the pseudorandom entropy pool (/dev/urandom) (which is also valid for Windows/zOS).
        JVM_ARGS += " -Djava.security.egd=file:/dev/urandom";

        JavaInfo info = JavaInfo.forServer(this);
        // Debug for a highly intermittent problems on j9/semeru JVMs.
        if (info.VENDOR == Vendor.IBM || info.VENDOR == Vendor.OPENJ9) {
            JVM_ARGS += " -Xdump:system+java+snap:events=systhrow,filter=\"java/lang/NoSuchMethodError#com/ibm/ws/classloading/internal/AppClassLoader.<init>*\",msg_filter=\"*getPrivateLibraries*\",request=exclusive+prepwalk";
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
        if (isJava2SecurityEnabled() && !isEE11OrLaterEnabled()) {
            RemoteFile f = getServerBootstrapPropertiesFile();
            addJava2SecurityPropertiesToBootstrapFile(f, GLOBAL_DEBUG_JAVA2SECURITY);
            String reason = GLOBAL_JAVA2SECURITY ? "GLOBAL_JAVA2SECURITY" : "GLOBAL_DEBUG_JAVA2SECURITY";
            Log.info(c, "startServerWithArgs", "Java 2 Security enabled for server " + getServerName() + " because " + reason + "=true");
            startedWithJavaSecurity = true;
        } else {
            boolean bootstrapHasJava2SecProps = false;
            // Check if "websphere.java.security" has been added to bootstrapping.properties
            // as some tests will add it for their own security enable tests
            RemoteFile f = getServerBootstrapPropertiesFile();
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
                Log.info(c, "startServerWithArgs", "caught exception checking bootstap.properties file for Java 2 Security properties, e: ", e.getMessage());
            } finally {
                if (reader != null)
                    reader.close();
            }

            startedWithJavaSecurity = bootstrapHasJava2SecProps;
            if (bootstrapHasJava2SecProps) {
                if (info.majorVersion() >= 18 && info.majorVersion() <= 23) {
                    // If we are running on Java 18 through 23, then we need to explicitly enable the security manager
                    Log.info(c, "startServerWithArgs", "Java 18 + Java2Sec requested, setting -Djava.security.manager=allow");
                    JVM_ARGS += " -Djava.security.manager=allow";
                } else if (info.majorVersion() >= 24) {
                    // Security manager not available in Java 24+
                    LOG.severe("The server is configured to run with Java 2 security enabled, but the security manager is permanently disabled in Java versions 24 and later.  The security manager cannot be set!");
                    throw new RuntimeException("The security manager is permanently disabled in Java versions 24 and later.  When running FATs, use @MaximumJavaLevel(javaLevel = 23) or disable Java 2 security to prevent this server from failing to start when running in Java 24 or later.");
                }
            }
        }

        //FIPS 140-3
        // if we have FIPS 140-3 enabled, and the matched java/platform, add JVM Arg
        if (isFIPS140_3EnabledAndSupported(info)) {
            // TODO: `getJvmOptionsAsMap()` should be added to JVM_ARGS outside of this if-block so that we always run it.
            // During FIPS 140-3 development, we found test scenarios where jvm.options is set before server start and the file is ignored.
            // So that we can test FIPS 140-3 without causing issues unrelated to FIPS, we have put it inside this if-block, for now.
            Map<String, String> combined = this.getJvmOptionsAsMap();
            combined.putAll(this.getFipsJvmOptions(info, false));
            JVM_ARGS += getJvmArgString(combined);
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
                        RemoteFile x = machine.getFile(serverRoot + "/" + fileName);
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

        useEnvVars.setProperty("JVM_ARGS", JVM_ARGS);

        // This takes the custom console file name used for tests into consideration
        useEnvVars.setProperty("LOG_DIR", logsRoot);
        useEnvVars.setProperty("LOG_FILE", consoleFileName);

        Log.info(c, method, "Using additional env props: " + useEnvVars);

        Log.finer(c, method, "Starting Server with command: " + cmd);

        configureLTPAKeys(info);

        // Create a marker file to indicate that we're trying to start a server
        createServerMarkerFile();

        if (doCheckpoint()) {
            // save off envVars for checkpoint
            checkpointInfo.setCheckpointEnv((Properties) useEnvVars.clone());
            checkpointInfo.preCheckpointLambda.accept(this);
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
            String workDir = new File(serverOutputRoot).getAbsolutePath();
            localMachine.executeAsync(cmd, parameters, workDir, useEnvVars, redirect);
            Log.info(c, method, "Started server process in debug mode");
            output = null;
        } else {
            if (machine instanceof LocalMachine) {
                // Running the server start asynchronously because it appears that the start
                // process is hanging from time to time. We can probably remove this when we fix
                // the issue causing the process to hang.
                final BlockingQueue<ProgramOutput> outputQueue = new LinkedBlockingQueue<ProgramOutput>();

                Runnable execServerCmd = null;

                if (runAsAWindowService == false) {
                    final String[] params = doCheckpoint() ? checkpointAdjustParams(parametersList) : parameters;
                    execServerCmd = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                outputQueue.put(machine.execute(cmd, params, useEnvVars));
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
                                Log.info(c, method, "runAsAWindowService RegisterService parms: " + registerServiceParmList);
                                final String[] registerServiceparameters = registerServiceParmList.toArray(new String[] {});
                                outputQueue.put(machine.execute(cmd, registerServiceparameters, useEnvVars));

                                Log.info(c, method, "runAsAWindowService StartService    parms: " + startServiceParmList);
                                final String[] startServiceparameters = startServiceParmList.toArray(new String[] {});
                                outputQueue.put(machine.execute(cmd, startServiceparameters, useEnvVars));
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
                    if (!doCheckpoint()) {
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
                    } else {
                        // we're taking a server checkpoint and the process has not exited
                        Log.warning(c, "The launch of bin/server to create a checkpoint did not exit within " +
                                       SCRIPT_TIMEOUT_IN_MINUTES + " minutes.");
                        // at this point we have limited info about what happened since the STDOUT, STDERR and rc
                        // from the attempted fork of bin/server have not returned within the time limit.
                        // Probe the checkpoint dir structure
                        try {
                            assertCheckpointDirAsExpected(true);
                            Log.warning(c, "There are some expected checkpoint files in the checkpoint directory.");
                        } catch (AssertionError ae) {
                            Log.debug(c, "Got an expected assertion error: " + ae);
                        }
                    }
                }
            } else {
                // If the machine is remote we can execute the command directly.
                // RXA has its own timeouts in case the command "hangs".
                output = machine.execute(cmd, parameters, useEnvVars);
            }
            boolean shouldFail = doCheckpoint() ? checkpointInfo.expectCheckpointFailure : expectStartFailure;
            int rc = output.getReturnCode();
            if (rc != 0) {
                if (shouldFail) {
                    Log.info(c, method, "EXPECTED: Server didn't start");
                    deleteServerMarkerFile();
                    Log.exiting(c, method);
                    return output;
                } else {
                    Log.info(c, method, "Response from script is: " + output.getStdout());
                    Log.info(c, method, "Return code from script is: " + rc);
                }
            } else {
                if (shouldFail && doCheckpoint()) {
                    Exception fail = new Exception("Checkpoint should have failed.");
                    Log.error(c, fail.getMessage(), fail);
                    throw fail;
                }
            }
            if (doCheckpoint()) {
                checkpointValidate(output, expectStartFailure);
                checkpointInfo.postCheckpointLambda.accept(this);
                if (checkpointInfo.autoRestore) {
                    output = checkpointRestore(false);
                } else {
                    return output;
                }
            }
        }

        // Validate the server and apps started - if they didn't, that
        // method will throw an appropriate exception

        if ("start".equals(serverCmd)) {
            validateServerStarted(output, useValidateApps, expectStartFailure, validateTimedExit);
            isStarted = true;
        }

        Log.exiting(c, method);
        return output;
    }

    /**
     * @param  fipsOpts, a Map containing jvm argument name/value pairs
     * @return           A string that starts with a space and contains key/value pairs represented by 'key=value' and separated by spaces
     */
    private String getJvmArgString(Map<String, String> fipsOpts) {
        StringJoiner joiner = new StringJoiner(" ", " ", "");
        for (String key : fipsOpts.keySet()) {
            String value = fipsOpts.get(key);
            if (value != null && !value.isEmpty()) {
                joiner.add(String.format("%s=%s", escapeCharacters(key), escapeCharacters(value)));
            } else {
                joiner.add(key);
            }
        }
        return joiner.toString();
    }

    private String escapeCharacters(String input) {
        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) > -1) {
                builder.append("\\");
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String[] checkpointAdjustParams(List<String> parametersList) {
        final String method = "checkpointFixParams";
        Log.info(c, method, "checkpointFixUpParameters: " + parametersList);
        ArrayList<String> checkpointParams = new ArrayList<String>();
        //exclude actions run, debug, package, ...
        boolean isLaunch = "start".equals(parametersList.get(0));
        for (int i = 0; i < parametersList.size(); i++) {
            if (i == 0 && isLaunch) {
                checkpointParams.add("checkpoint");
            } else if (i == 2 && isLaunch) {
                checkpointParams.add("--at=" + checkpointInfo.phaseToCommandLineArg());
                checkpointParams.add(parametersList.get(i));
            } else {
                checkpointParams.add(parametersList.get(i));
            }
        }
        if (parametersList.size() == 2 && isLaunch) {
            checkpointParams.add("--at=" + checkpointInfo.checkpointPhase);
        }
        String[] ret = checkpointParams.toArray(new String[checkpointParams.size()]);
        Log.info(c, method, "checkpointFixParams: " + checkpointParams);
        return ret;
    }

    /**
     * After a checkpoint image has been created and basic validation
     */
    public ProgramOutput checkpointRestore() throws Exception {
        return checkpointRestore(true);
    }

    public ProgramOutput checkpointRestore(boolean validate) throws Exception {
        String method = "checkpointRestore";
        //Launch restore cmd mimic the process used to launch the checkpointing operation w.r.t
        // polling timeout on the launch
        String cmd = installRoot + "/bin/server start " + serverToUse;
        final BlockingQueue<ProgramOutput> restoreProgramOutputQueue = new LinkedBlockingQueue<ProgramOutput>();
        Runnable execRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Properties restoreEnv = (Properties) checkpointInfo.getCheckpointEnv().clone();
                    if (checkpointInfo.criuRestoreDisableRecovery) {
                        restoreEnv.setProperty("CRIU_RESTORE_DISABLE_RECOVERY", "true");
                    }
                    Log.info(c, method, "Restoring with cmd: " + cmd + " and env:" + restoreEnv);
                    restoreProgramOutputQueue.put(machine.execute(cmd, new String[0], restoreEnv));
                } catch (Exception e) {
                    Log.info(c, method, "Exception while attempting to restore a server: " + e.getMessage());
                }
            }
        };
        new Thread(execRunnable).start();
        //Poll for script completion
        final int scriptTimeout = 5;
        ProgramOutput output = null;
        try {
            output = restoreProgramOutputQueue.poll(scriptTimeout, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Log.error(c, method, e);
        }
        if (output == null) {
            Log.warning(c, "The output is null");
            fail("Failed to restore: no output");
        } else if (output.getReturnCode() != 0) {
            Log.info(c, method, "Restore failed with RC:" + output.getReturnCode());
            Log.info(c, method, "Restore stdout: " + output.getStdout());
            Log.info(c, method, "Restore stderr: " + output.getStderr());
            if (!checkpointInfo.expectRestoreFailure) {
                fail("Failed to restore: " + output.getStdout() + " " + output.getStderr());
            } else {
                return output;
            }
        }

        // recalculate the messages and trace logs in case the logsRoot changed
        messageAbsPath = logsRoot + messageFileName;
        traceAbsPath = logsRoot + traceFileName;

        //The restore operation returned 0. Verify that running server is from a checkpoint restore and not from a
        // failed restore recovery, unless auto-recovery is enabled
        if (checkpointInfo.criuRestoreDisableRecovery && failedRestore()) {
            // Did not find restore message; assume it failed;
            // The return code is 0 indicating the server started, likely recovered
            // set as started but then stop the server
            setStarted();
            try {
                stopServer();
            } catch (Exception e) {
                Log.error(c, method, e);
                // we don't want to fail if stop fails
            }
            fail("The server did not restore successfully");
        }
        if (validate) {
            validateServerStarted(output, checkpointInfo.validateApps, checkpointInfo.expectRestoreFailure,
                                  checkpointInfo.validateTimedExit);
            Log.info(c, method, "Restored from checkpoint, mark server as started.");
            if (output.getReturnCode() == 0) {
                setStarted();
            }
        }

        //TODO - consider validating that the application has started before running tests

        return output;
    }

    /**
     * After a checkpoint the server should be stopped, There should be no errors and the
     * checkpoint dir should look normal
     *
     * @param output
     */
    private void checkpointValidate(ProgramOutput output, boolean expectCheckpointFailure) throws Exception {
        String method = "checkpointValidate";
        Log.info(c, method, method);
        try {
            resetStarted();
            if (!expectCheckpointFailure) {
                assertEquals("Checkpoint operation return code should be zero", 0, output.getReturnCode());
            }
            if (isStarted) {
                Exception fail = new Exception("Server should not be started after a checkpoint operation");
                Log.error(c, "Server should not be started after a checkpoint operation", fail);
                throw fail;
            }
            assertCheckpointDirAsExpected(true);
            try {
                checkLogsForErrorsAndWarnings(checkpointInfo.checkpointRegexIgnoreMessages.toArray(new String[checkpointInfo.checkpointRegexIgnoreMessages.size()]));
            } catch (Exception exc) {
                Log.error(c, "Server logs should not contain unexpected errors after a checkpoint operation", exc);
                throw exc;
            }
            assertNotNull("'CWWKC0451I: A server checkpoint was requested...' message not found in log.",
                          waitForStringInLogUsingMark("CWWKC0451I:", 0));
        } catch (AssertionError | Exception err) {
            final String errInfo = (err instanceof AssertionError) ? "AssertionError" : "Exception";
            Log.info(c, method, "errInfo: " + err);
            if (isStarted) {
                Log.info(c, method, "Stop running server after checkpointValidate " + errInfo);
                stopServer(!POST_ARCHIVES);
            }
            postStopServerArchive();
            throw err;
        }
        Log.exiting(c, method);
    }

    /**
     * Check for obvious missing files or structural problems with checkpoint dir layout.
     * expected layout is
     *
     * <pre>
     *     workarea/
     *         checkpoint/
     *             image/
     *                 inventory.img
     *                 fdinfo*.img
     *                 core-*.img
     *                 ...
     *             workarea/
     *                 osgi.eclipse/
     *                 platform/
     *         osgi.eclipse/
     *         platform/
     * </pre>
     *
     * @throws Exception
     */
    public void assertCheckpointDirAsExpected(boolean log) throws Exception {
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb, Locale.US);
        String fmt = "%3$10d %2$tD-%2$tT %1$s";

        RemoteFile workarea = machine.getFile(serverRoot + "/workarea");
        assertTrue("Missing top level workarea dir", workarea.isDirectory());
        if (log) {
            Log.warning(c, "Log workarea directory contents");

            for (RemoteFile rf : workarea.list(false)) {
                fm.format(fmt, rf.getAbsolutePath(), new Long(rf.lastModified()), rf.length());
                Log.warning(c, sb.toString());
                sb.setLength(0);
            }
        }
        RemoteFile cpDir = machine.getFile(workarea, "checkpoint");
        assertTrue("Missing checkpoint dir", cpDir.isDirectory());
        if (log) {
            for (RemoteFile rf : cpDir.list(false)) {
                fm.format(fmt, rf.getAbsolutePath(), new Long(rf.lastModified()), rf.length());
                Log.warning(c, sb.toString());
                sb.setLength(0);
            }
        }
        RemoteFile workareaCheckpoint = machine.getFile(cpDir, "workarea");
        assertTrue("Missing workarea backup dir", workareaCheckpoint.isDirectory());
        assertTrue("checkpoint workarea dir has no files",
                   workareaCheckpoint.list(false).length > 1 /* a somewhat arbitrary min count */);
        RemoteFile imgDir = machine.getFile(cpDir, "image");
        assertTrue("checkpoint image dir has no files",
                   imgDir.list(false).length > 2 /* a somewhat arbitrary min count */);
        if (log) {
            for (RemoteFile rf : imgDir.list(false)) {
                fm.format(fmt, rf.getAbsolutePath(), new Long(rf.lastModified()), rf.length());
                Log.warning(c, sb.toString());
                sb.setLength(0);
            }
        }
    }

    private boolean failedRestore() throws Exception {
        final String method = "failedRestore";
        final String RESTORE_MESSAGE_CODE = "CWWKC0452I";
        Log.info(c, method, "Checking for restore message: " + RESTORE_MESSAGE_CODE);

        // The console log is where to check first because its location
        // cannot change on restore.  The messages one may change while restoring
        // that makes the file the restore message is in not predictable.
        RemoteFile logToCheck = getConsoleLogFile();
        // App validation needs the info messages in messages.log
        if (!logToCheck.exists() || consoleLogOff()) {
            // try the messages log
            Log.info(c, method, "WARNING: console.log does not exist-- trying app verification step with messages.log");
            logToCheck = machine.getFile(messageAbsPath);
        }

        String found = waitForStringInLog(RESTORE_MESSAGE_CODE, logToCheck);
        if (found == null) {
            Log.info(c, method, "Error: server did not restore successfully.");
            return true;
        }
        Log.info(c, method, "Found restore message:" + found);
        return false;
    }

    private boolean consoleLogOff() {
        return "OFF".equals(getBootstrapProperties().get("com.ibm.ws.logging.console.log.level"));
    }

    /**
     * @return
     */
    private boolean doCheckpoint() {
        return (checkpointInfo != null);
    }

    /**
     * Clear any log marks and then set log marks to messages.log
     * or trace.log if those exist. See issue 4364
     *
     * @throws Exception
     */
    public void initializeAnyExistingMarks() throws Exception {
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

    public void validateAppsLoaded() throws Exception {
        validateAppsLoaded(getDefaultLogFile());
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
                serverDump("thread");

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

    protected void validateServerStarted(ProgramOutput output, boolean useValidateApps,
                                         boolean expectStartFailure, boolean validateTimedExit) throws Exception {
        final String method = "validateServerStarted";

        final String START_MESSAGE_CODE = "CWWKF0011I";

        boolean serverStarted = false;

        Log.info(c, method, "Waiting up to " + (serverStartTimeout / 1000)
                            + " seconds for server confirmation:  "
                            + START_MESSAGE_CODE + " to be found in " + consoleAbsPath);

        RemoteFile messagesLog = machine.getFile(messageAbsPath);
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

            if (checkForRestConnector.get()) {
                //since this is going to connect to the secure port, that needs to be ready
                //before an attempt to make the JMX connection
                Log.info(c, method, "Checking that the JMX RestConnector is available and secured");
                assertNotNull("CWWKO0219I.*ssl not received", waitForStringInLogUsingMark("CWWKO0219I.*ssl"));

                assertNotNull("IBMJMXConnectorREST app did not report as ready", waitForStringInLogUsingMark("CWWKT0016I.*IBMJMXConnectorREST"));

                assertNotNull("Security service did not report it was ready", waitForStringInLogUsingMark("CWWKS0008I"));

                assertNotNull("The JMX REST connector message was not found", waitForStringInLogUsingMark("CWWKX0103I"));

                //backup the key file
                try {
                    copyFileToTempDir("resources/security/key.jks", "key.jks");
                } catch (Exception e) {
                    copyFileToTempDir("resources/security/key.p12", "key.p12");
                }
            }
        } catch (Exception e) {
            Log.error(c, method, e, "Exception thrown confirming server started in " + consoleAbsPath);
            postStopServerArchive(RETRY, ALLOW_FAILURES);
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
                serverDump("thread");
                postStopServerArchive(RETRY, ALLOW_FAILURES);
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
        if (useValidateApps) {
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
        RemoteFile messagesLog = machine.getFile(messageAbsPath);
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

    public ProgramOutput stopServer(String... ignoredFailuresRegExps) throws Exception {
        return stopServer(POST_ARCHIVES, ignoredFailuresRegExps);
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

    public ProgramOutput stopServer(boolean postStopServerArchive, String... ignoredFailuresRegExps) throws Exception {
        return stopServer(postStopServerArchive, !FORCE_STOP, ignoredFailuresRegExps);
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
            Log.info(c, method, "<<< DUMPING SERVER: " + getServerName());

            if (!isStarted) {
                Log.info(c, method, "Server " + serverToUse + " is not running (stop called previously).");
                return null;
            }

            String cmd = installRoot + "/bin/server";
            String[] parameters = new String[] { "dump", serverToUse };

            //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
            Properties useEnvVars = new Properties();
            useEnvVars.setProperty("JAVA_HOME", machineJava);
            if (customUserDir)
                useEnvVars.setProperty("WLP_USER_DIR", userDir);
            Log.finer(c, method, "Using additional env props: " + useEnvVars);

            final ProgramOutput output = machine.execute(cmd, parameters, machine.getWorkDir(), useEnvVars, 300);

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
                    final RemoteFile dumpFile = machine.getFile(dumpPath);
                    Log.info(c, method, "Copying RemoteFile " + dumpFile + " to " + pathToAutoFVTTestFiles + "/tmp/" + destination);
                    lf = copyFileToTempDir(dumpFile, destination);
                }
            } else {
                Log.info(c, method, "Matcher failed.");
            }

        } finally {
            Log.info(c, method, "<<< SERVER DUMP COMPLETE: " + getServerName() + " , localFile = " + lf);
        }

        return lf;
    }

    /**
     * Stops the server and checks for any warnings or errors that appeared in logs.
     * If warnings/errors are found, an exception will be thrown after the server stops.
     *
     * @param  postStopServerArchive  true to collect server log files after the server is stopped; false to skip this step (sometimes, FATs back up log files on their own, so this
     *                                    would be redundant)
     * @param  forceStop              Force the server to stop, skipping the quiesce (default/usual value should be false)
     * @param  ignoredFailuresRegExps A list of reg expressions corresponding to warnings or errors that should be ignored.
     *                                    If regIgnore is null, logs will not be checked for warnings/errors
     * @return                        the output of the stop command
     * @throws Exception              if the stop operation fails or there are warnings/errors found in server
     *                                    logs that were not in the list of ignored warnings/errors.
     */
    public ProgramOutput stopServer(boolean postStopServerArchive, boolean forceStop, String... ignoredFailuresRegExps) throws Exception {
        return stopServer(postStopServerArchive, forceStop, SKIP_ARCHIVES, ignoredFailuresRegExps);
    }

    /**
     * Stops the server and checks for any warnings or errors that appeared in logs.
     * If warnings/errors are found, an exception will be thrown after the server stops.
     *
     * @param  postStopServerArchive  true to collect server log files after the server is stopped; false to skip this step (sometimes, FATs back up log files on their own, so this
     *                                    would be redundant)
     * @param  forceStop              Force the server to stop, skipping the quiesce (default/usual value should be false)
     * @param  skipArchives           Skip postStopServer collection of archives (WARs, EARs, JARs, etc.) - only used if postStopServerArchive is true
     * @param  ignoredFailuresRegExps A list of reg expressions corresponding to warnings or errors that should be ignored.
     *                                    If ignoredFailuresRegExps is null, logs will not be checked for warnings/errors
     * @return                        the output of the stop command
     * @throws Exception              if the stop operation fails or there are warnings/errors found in server
     *                                    logs that were not in the list of ignored warnings/errors.
     */
    public ProgramOutput stopServer(boolean postStopServerArchive, boolean forceStop, boolean skipArchives,
                                    String... ignoredFailuresRegExps) throws Exception {

        List<String> failuresRegExps = Arrays.asList(LIBERTY_ERROR_REGEX);
// TFB: TODO: The parameter 'skipArchives' is *NOT* used.
//            This is probably a bug, but cannot be adjusted without running
//            regression testsi.
        return stopServer(postStopServerArchive, forceStop, SKIP_ARCHIVES,
                          failuresRegExps, ignoredFailuresRegExps);
    }

    public ProgramOutput stopServer(boolean postStopServerArchive, boolean forceStop, boolean skipArchives,
                                    List<String> failuresRegExps, String... ignoredFailuresRegExps) throws Exception {
        return stopServer(!IGNORE_STOPPED, postStopServerArchive, forceStop, skipArchives, !SKIP_FEATURE_CHECK,
                          failuresRegExps, ignoredFailuresRegExps);
    }

    public ProgramOutput stopServerAlways(String... ignoredFailures) throws Exception {
        return stopServer(IGNORE_STOPPED, POST_ARCHIVES, FORCE_STOP, SKIP_ARCHIVES, !SKIP_FEATURE_CHECK,
                          Collections.emptyList(), ignoredFailures);
    }

    public static final boolean IGNORE_STOPPED = true;
    public static final boolean FORCE_STOP = true;
    public static final boolean POST_ARCHIVES = true;
    public static final boolean SKIP_ARCHIVES = true;
    public static final boolean SKIP_FEATURE_CHECK = true;

    /**
     * Stops the server and checks for any warnings or errors that appeared in logs.
     * If warnings/errors are found, an exception will be thrown after the server stops.
     *
     * @param  ignoreStopped          true to perform the stop even if the server is marked as "not running'"
     * @param  postStopServerArchive  true to collect server log files after the server is stopped; false to skip this step
     *                                    (sometimes, FATs back up log files on their own, so this would be redundant)
     * @param  forceStop              Force the server to stop, skipping the quiesce (default/usual value should be false)
     * @param  skipArchives           Skip postStopServer collection of archives (WARs, EARs, JARs, etc.)
     *                                    Only used if postStopServerArchive is true
     * @param  skipFeatureCheck       Skip repeat feature set checking
     * @param  ignoredFailuresRegExps A list of reg expressions corresponding to warnings or errors that should be ignored.
     *                                    If ignoredFailuresRegExps is null, logs will not be checked for warnings/errors
     * @param  failuresRegExps        A list of reg expressions corresponding to warnings or errors that should be treated
     *                                    as test failures.
     * @return                        the output of the stop command
     * @throws Exception              if the stop operation fails or there are warnings/errors found in server
     *                                    logs that were not in the list of ignored warnings/errors.
     */
    public ProgramOutput stopServer(boolean ignoreStopped, boolean postStopServerArchive, boolean forceStop, boolean skipArchives, boolean skipFeatureCheck,
                                    List<String> failuresRegExps, String... ignoredFailuresRegExps) throws Exception {
        final String method = "stopServer";
        Log.info(c, method, "<<< STOPPING SERVER: " + getServerName());

        boolean commandPortEnabled = true;

        // Don't use 'ignoreStopped' if the server was successfully started.
        // 'ignoreStopped' changes stop processing only when the server was
        // not successfully started.

        if (isStarted) {
            ignoreStopped = false;
        }

        try {
            // 'ignoreStopped' means trying the stop, even though the server
            // doesn't think that it was started.
            if (!isStarted && !ignoreStopped) {
                Log.info(c, method, "Server " + serverToUse + " is not running (stop called previously).");
                // The checkpointEnv will be set if a checkpoint was done.
                // The server may never have been successfully started because
                // the checkpoint failed or the restore failed.
                // We archive the server in this case to ensure we get the possible error logs
                if (checkpointInfo == null || checkpointInfo.getCheckpointEnv() == null) {
                    postStopServerArchive = false;
                }
                return null;
            }

            String cmd = installRoot + "/bin/server";
            String[] parameters;
            if (forceStop) {
                parameters = new String[] { "stop", serverToUse, "--force" };
            } else {
                parameters = new String[] { "stop", serverToUse };
            }

            // Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be
            // set to the same Java as the build/runtime environment.

            Properties useEnvVars = new Properties();
            useEnvVars.setProperty("JAVA_HOME", machineJava);
            if (customUserDir) {
                useEnvVars.setProperty("WLP_USER_DIR", userDir);
            }
            Log.finer(c, method, "Using additional env props: " + useEnvVars);

            ProgramOutput output = null;

            if (!runAsAWindowService) {
                output = machine.execute(cmd, parameters, machine.getWorkDir(), useEnvVars, 300);
            } else {
                ArrayList<String> parametersList = new ArrayList<String>();
                for (int i = 0; i < parameters.length; i++) {
                    parametersList.add(parameters[i]);
                }
                ArrayList<String> stopServiceParmList = makeParmList(parametersList, 2);
                ArrayList<String> removeServiceParmList = makeParmList(parametersList, 3);
                String[] stopServiceParameters = stopServiceParmList.toArray(new String[] {});
                String[] removeServiceParameters = removeServiceParmList.toArray(new String[] {});

                try {
                    output = machine.execute(cmd, stopServiceParameters, machine.getWorkDir(), useEnvVars, 300);
                } finally {
                    output = machine.execute(cmd, removeServiceParameters, machine.getWorkDir(), useEnvVars, 300);
                }
            }

            String stdout = output.getStdout();
            Log.info(c, method, "Stop Server Response: " + stdout);
            if (output.getReturnCode() != 0)
                Log.info(c, method, "Return code from script is: " + output.getReturnCode());

            // This step can be reached with '!isStarted' if 'ignoreStopped' is true.
            // Don't test the log for messages: The prior start is already known
            // to not have been successful.
            if (!isStarted) {
                return output;
            }

            isStarted = false;

            if (stdout.contains("is not running")) {
                return output;
            }

            if (stdout.contains("command port is disabled")) {
                // Mark that the command port is not enabled
                commandPortEnabled = false;
                throw new RuntimeException("Cannot stop server because command port is disabled.");
            }

            RemoteFile log = isStartedConsoleLogLevelOff ? machine.getFile(messageAbsPath) : getConsoleLogFile();
            // Actually waits for the stop message
            waitForStringInLog("CWWKE0036I:", SERVER_STOP_TIMEOUT, log);

            int serverStopRC = output.getReturnCode();
            if (serverStopRC != 0) {
                throw new RuntimeException("Server stop failed with RC " + serverStopRC + ".\n" +
                                           "Stdout:\n" + output.getStdout() + "\n" +
                                           "Stderr:\n" + output.getStderr());
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

            isTidy = true;

            if (!skipFeatureCheck) {
                checkServerRepeatFeatures();
            }
            checkLogsForErrorsAndWarnings(failuresRegExps, ignoredFailuresRegExps);

            if (doCheckpoint() && checkpointInfo.isAssertNoAppRestartOnRestore() &&
                checkpointInfo.checkpointPhase == CheckpointPhase.AFTER_APP_START) {
                //If server restored from an AFTER_APP_START checkpoint, then we do not expect to see starting application message.
                // It would have started pre-checkpoint.
                // If present, it may mean a bug in how config changes are handled by checkpoint.
                // We intentionally only make this check if the test will not otherwise fail due to unexpected error messages
                // already found.
                List<String> appsRestarted = findStringsInLogs("CWWKZ0018I: Starting application");
                if (!appsRestarted.isEmpty()) {
                    StringBuffer sb = new StringBuffer("Unexpected application restart messages found after restore:");
                    sb.append(getServerName());
                    sb.append(" logs:");
                    for (String applicationRestarted : appsRestarted) {
                        sb.append("\n <br>");
                        sb.append(applicationRestarted);
                        Log.info(c, method, "Unexpected application restart in retored server found in log " +
                                            getDefaultLogFile() + ": " + applicationRestarted);
                    }
                    throw new Exception(sb.toString());
                }
            }

            return output;
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
            }

            if (startedWithJavaSecurity) {
                try {
                    new ACEScanner(this).run();
                } catch (Throwable t) {
                    LOG.logp(Level.WARNING, c.getName(), "stopServer", "Caught exception trying to scan for AccessControlExceptions", t);
                }
                startedWithJavaSecurity = false;
            }

            // If the prior stop did not occur successfully, archive collection
            // will usually have been done.
            //
            // There might be additional logs, but also, the logs will be truncated
            // because of the prior collection step.
            if (!ignoreStopped && postStopServerArchive) {
                postStopServerArchive(RETRY, skipArchives);
            }

            // Delete marker for stopped server
            // deleteServerMarkerFile();
        }
    }

    @Deprecated
    protected void checkLogsForErrorsAndWarnings(String... ignoredFailuresRegExps) throws Exception {
        checkLogsForErrorsAndWarnings(Arrays.asList(LIBERTY_ERROR_REGEX), ignoredFailuresRegExps);
    }

    /**
     * Checks server logs for any lines containing errors or warnings that
     * do not match any regular expressions provided in regIgnore.
     *
     * @param  failuresRegExps        A list of reg expressions corresponding to warnings or errors that should be treated as test failures.
     * @param  ignoredFailuresRegExps A list of regex strings for errors/warnings that
     *                                    may be safely ignored.
     * @return                        A list of lines containing errors/warnings from server logs
     */
    protected void checkLogsForErrorsAndWarnings(List<String> failuresRegExps, String... ignoredFailuresRegExps) throws Exception {
        final String method = "checkLogsForErrorsAndWarnings";

        // Get all warnings and errors in logs - default to an empty list
        List<String> errorsInLogs = new ArrayList<String>();
        try {
            for (String failureRegExp : failuresRegExps) {
                errorsInLogs.addAll(findStringsInLogs(failureRegExp)); // uses getDefaultLogFile()
            }
            if (!errorsInLogs.isEmpty()) {
                // There were unexpected errors in logs, print them
                // and set an exception to return
                StringBuffer sb = new StringBuffer("Errors/warnings were found in server ");
                sb.append(getServerNameWithRepeatAction());
                sb.append(" logs:");
                for (String errorInLog : errorsInLogs) {
                    sb.append("\n <br>");
                    sb.append(errorInLog);
                    Log.info(c, method, "Error/warning found in log " + getDefaultLogFile() + ": " + errorInLog);
                    if (errorInLog.contains("CWWKO0221E")) {
                        printProcessHoldingPort(errorInLog);
                    }
                }
            }
        } catch (Exception e) {
            Log.warning(getClass(), "While checking for log errors and warnings, findStringsInLogs caused an exception: " + e.getMessage());
        }

        // Compile set of regex's using input list and universal ignore list
        List<Pattern> ignorePatterns = new ArrayList<Pattern>();
        if (ignoredFailuresRegExps != null && ignoredFailuresRegExps.length != 0) {
            for (String ignoreRegEx : ignoredFailuresRegExps) {
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
            sb.append(getServerNameWithRepeatAction());
            sb.append(" logs:");
            if (!j2secIssues.isEmpty()) {
                // When things go wrong with j2sec, a LOT of things tend to go wrong, so just leave a pointer
                // to the nicely formatted ACE report instead of putting every single issue in the exception msg
                sb.append("\n <br>");
                sb.append("Java 2 security issues were found in logs");
                boolean showJ2secErrors = true;
                // If an ACE-report will be generated....
                if (startedWithJavaSecurity) {
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
                if (errorInLog.contains("CWWKO0221E")) {
                    printProcessHoldingPort(errorInLog);
                }
            }
            ex = new Exception(sb.toString());
        }

        if (ex == null)
            Log.info(c, method, "No unexpected errors or warnings found in server logs.");
        else
            throw ex;
    }

    //servers which are exempt from checking repeat features in automated builds
    //the vast majority of servers should eventually be removed once the tests are fixed
    //the test will still fail when run locally
    private static final String[] EXEMPT_SERVERS = {
                                                     "cdi20EEServer", //com.ibm.ws.cdi.1.0_fat_EE

                                                     "EclipseLinkServer", //com.ibm.ws.jpa.tests.eclipselink_jpa_2.1_fat

                                                     "com.ibm.ws.jpa.el.defaultds.fat.server", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.fat.dsoverride", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.fat.emlocking", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.el.defaultds.fat.server", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.fat.dserror", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.fat.emlocking", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "ConcurrentEnhancementVerification", //com.ibm.ws.jpa.tests.jpa_fat
                                                     "com.ibm.ws.jpa.fat.ejbpassivation", //com.ibm.ws.jpa.tests.jpa_fat

                                                     "ApplicationProcessorServer", //io.openliberty.microprofile.openapi.2.0.internal_fat
                                                     "OpenAPITestServer", //io.openliberty.microprofile.openapi.2.0.internal_fat
                                                     "OpenAPIMergeTestServer", //io.openliberty.microprofile.openapi.2.0.internal_fat
                                                     "OpenAPIMergeWithServletTestServer", //io.openliberty.microprofile.openapi.2.0.internal_fat

                                                     "SimpleRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "ContextRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "CustomContextRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "ConcurrentRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "SharedLibRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "CheckpointSimpleRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat
                                                     "JsonbRxMessagingServer", //com.ibm.ws.microprofile.reactive.messaging_fat

                                                     "mpRestClient10.remoteServer", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient11.async", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient10.basic", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient10.collections", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient10.handleresponses", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient10.headerPropagation", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient13.ssl", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient12.jsonbContext", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient11.produceConsume", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient10.props", //com.ibm.ws.microprofile.rest.client_fat
                                                     "mpRestClient20.sse", //com.ibm.ws.microprofile.rest.client_fat

                                                     "opentracingFATServer1", //com.ibm.ws.opentracing.1.x_fat
                                                     "opentracingFATServer3", //com.ibm.ws.opentracing.1.x_fat
                                                     "opentracingFATServer4", //com.ibm.ws.opentracing.1.x_fat

                                                     "RequestTimingServer", //com.ibm.ws.request.timing_fat
                                                     "HungRequestTimingServer", //com.ibm.ws.request.timing.hung_fat

                                                     "com.ibm.ws.rest.handler.config.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.config.openapi.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.config.audit.feature.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.config.appdef.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.config.jca.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.config.jms.fat", //com.ibm.ws.rest.handler.config_fat
                                                     "com.ibm.ws.rest.handler.validator.jdbc.fat", //com.ibm.ws.rest.handler.validator_fat
                                                     "com.ibm.ws.rest.handler.validator.jca.fat", //com.ibm.ws.rest.handler.validator_fat
                                                     "com.ibm.ws.rest.handler.validator.jms.fat", //com.ibm.ws.rest.handler.validator_fat
                                                     "com.ibm.ws.rest.handler.validator.openapi.fat", //com.ibm.ws.rest.handler.validator_fat

                                                     "com.ibm.ws.scaling.member.fat.member1", //com.ibm.ws.scaling.member_fat
                                                     "com.ibm.ws.scaling.member.fat.controller1", //com.ibm.ws.scaling.member_fat

                                                     "com.ibm.ws.ui.fat", //com.ibm.ws.ui_rest_fat

                                                     "com.ibm.ws.jaxrs.fat.exceptionMappingWithOT", //com.ibm.ws.jaxrs.2.0_fat

                                                     "RequestTimingServer", //com.ibm.ws.request.timing_fat

                                                     "MPServer41", //io.openliberty.microprofile41.internal_fat
                                                     "MPServer", //io.openliberty.microprofile.internal_fat

    };
    private static final Set<String> EXEMPT_SERVERS_SET = new HashSet<String>(Arrays.asList(EXEMPT_SERVERS));

    /**
     * After the server has shutdown, check that the features used at runtime matched those expected by any FeatureReplacementAction which may have been active.
     *
     * @throws Exception
     */
    protected void checkServerRepeatFeatures() throws Exception {
        String method = "checkServerRepeatFeatures";

        RepeatTestAction action = RepeatTestFilter.getMostRecentRepeatAction();
        if (action instanceof FeatureReplacementAction) {
            FeatureReplacementAction featureReplacementAction = (FeatureReplacementAction) action;

            //only check the features if this server was included in the FeatureReplacementAction
            String serverName = getServerName();

            Set<String> servers = featureReplacementAction.getServers();
            //only check the features if the FeatureReplacementAction applies to this server
            if (!servers.contains(FeatureReplacementAction.NO_SERVERS) &&
                (servers.contains(serverName) || servers.contains(FeatureReplacementAction.ALL_SERVERS))) {

                Set<String> expectedFeatures = new HashSet<>(featureReplacementAction.getAddFeatures()); //the expected features, if present
                expectedFeatures.addAll(featureReplacementAction.getAlwaysAddFeatures());
                List<Set<String>> installedFeatures = getInstalledFeatures(); //the features actually installed at runtime
                for (Set<String> installedFeatureSet : installedFeatures) {

                    //expected feature -> actual feature
                    Map<String, String> unexpectedFeatures = getUnexpectedFeatures(expectedFeatures, installedFeatureSet);

                    if (unexpectedFeatures.size() > 0) {
                        String message = "Runtime features were not of the expected version for repeat action (Server: " + serverName + ", Action: " + action.getID() + ").\n";
                        for (Map.Entry<String, String> entry : unexpectedFeatures.entrySet()) {
                            message = message + "Expected: " + entry.getKey() + ", Actual: " + entry.getValue() + ".\n";
                        }
                        message = message
                                  + "This is usually caused by a feature not being explicitly set in the FAT's server.xml such that FeatureReplacementAction does not replace it properly.";

                        //if this is a local run then always throw an exception
                        //if not local then check if the server is exempt
                        //if not exempt then throw exception, otherwise just output a message
                        //check for exempt servers should eventually be removed
                        if (REPEAT_FEATURE_CHECK_ERROR) {
                            if (FAT_TEST_LOCALRUN) {
                                message = message + "\nYou should also ensure that the test server has been removed from LibertyServer.EXEMPT_SERVERS.";
                                throw new Exception(message);
                            } else {
                                if (!EXEMPT_SERVERS_SET.contains(serverName)) {
                                    throw new Exception(message);
                                } else {
                                    Log.info(c, method, message);
                                }
                            }
                        } else {
                            Log.info(c, method, message);
                        }
                    }
                }
            }
        }
        //re-instate this message once the exempt servers are removed
        //Log.info(c, method, "No invalid replacement features found.");
    }

    /**
     * Get a map of any features which are not at the expected version. e.g.
     *
     * if the expected features are
     * mpConfig-1.2, cdi-2.0, servlet-3.0, appSecurity-3.0
     *
     * and the installed features are
     * mpConfig-1.2, cdi-3.0, servlet-3.0, appSecurity-2.0, appSecurity-3.0
     *
     * then the returned map would only contain one element of
     * cdi-2.0 -> cdi-3.0
     *
     * Note that more than one version of appSecurity was installed but one of them was the expected version, so it was not flagged.
     *
     * @param  expectedFeatures
     * @param  installedFeatures
     * @return
     */
    public static Map<String, String> getUnexpectedFeatures(Collection<String> expectedFeatures, Collection<String> installedFeatures) {
        Map<String, String> featureMap = new HashMap<>();

        //compare each installed feature to the expected ones
        for (String installedFeature : installedFeatures) {
            //ignore versionless features
            int dash = installedFeature.indexOf("-");
            if (dash > -1) {
                String versionlessInstalledFeature = removeFeatureVersion(installedFeature);
                for (String replacementFeature : expectedFeatures) {
                    String versionlessReplacementFeature = removeFeatureVersion(replacementFeature);
                    if (versionlessReplacementFeature.equalsIgnoreCase(versionlessInstalledFeature)) {
                        //if the featureMap does not yet contain an entry for the replacementFeature then add it
                        if (!featureMap.containsKey(replacementFeature)) {
                            featureMap.put(replacementFeature, installedFeature);
                        } //if the feature is already in the featureMap, replace it if the installed feature did not match
                        else if (!featureMap.get(replacementFeature).equalsIgnoreCase(replacementFeature)) {
                            featureMap.put(replacementFeature, installedFeature);
                        }
                    }
                }
            }
        }

        //now extract the features which don't match
        Map<String, String> unexpectedFeatures = new HashMap<>();
        for (Entry<String, String> entry : featureMap.entrySet()) {
            String replacementFeature = entry.getKey();
            String installedFeature = entry.getValue();
            if (!replacementFeature.equalsIgnoreCase(installedFeature)) {
                unexpectedFeatures.put(replacementFeature, installedFeature);
            }
        }

        return unexpectedFeatures;
    }

    /**
     * Remove the version suffix and return only the base name of a feature, everything up to but not including the dash.
     * If the feature is already versionless (no dash) return as is.
     *
     * e.g.
     * servlet-6.0 -> servlet
     * servlet -> servlet
     *
     * @param  feature The short feature name
     * @return         The versionless feature name
     */
    public static final String removeFeatureVersion(String feature) {
        String baseFeatureName = feature;
        int dash = feature.indexOf("-");
        if (dash > -1) {
            baseFeatureName = feature.substring(0, dash); //the feature name, up to but not including the dash
        }
        return baseFeatureName;
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
     *
     * The operation will be retried
     */
    public void postStopServerArchive() throws Exception {
        postStopServerArchive(RETRY, !ALLOW_FAILURES, NOT_ARCHIVE);
    }

    /**
     * This method is used to archive server logs after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     *
     * @param retry if true and the operation fails, retry
     */
    public void postStopServerArchive(boolean retry) throws Exception {
        postStopServerArchive(retry, !ALLOW_FAILURES, NOT_ARCHIVE);
    }

    /**
     * This method is used to archive server logs after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     *
     * @param retry if true and the operation fails, retry
     */
    public void postStopServerArchive(boolean retry, boolean ignoreFailures) throws Exception {
        postStopServerArchive(retry, ignoreFailures, NOT_ARCHIVE);
    }

    public static final boolean RETRY = true;
    public static final boolean ALLOW_FAILURES = true;
    public static final boolean NOT_ARCHIVE = true;

    /**
     * This method is used to archive server logs and archives after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     *
     * @param retry          if true and the operation fails, retry
     * @param ignoreFailures if true, allow file collection steps to fail
     * @param skipArchives   whether or not to skip packaging of archive files (JARs, WARs, EARs, etc.)
     */
    public void postStopServerArchive(boolean retry, boolean ignoreFailures, boolean skipArchives) throws Exception {
        final String method = "postStopServerArchive";
        Log.entering(c, method);
        printProcesses();

        // 'ignoreFailures' is set when performing these operations after a failed startup.
        //
        // For example:
        // java.io.IOException: Error: Failed [ copy and delete ]
        //   of remote [ C:/dev/repos-pub/ol-baw/dev/build.image/wlp/usr/servers/Servlet3toHealth/logs/console.log ]
        //   to local [ C:/dev/repos-pub/ol-baw/dev/com.ibm.ws.kernel.feature.resolver_fat/build/libs/autoFVT/output/servers/Servlet3toHealth-15-02-2024-10-25-17/logs/console.log ]
        // at componenttest.topology.impl.LibertyServer.recursivelyCopyDirectory(LibertyServer.java:3681)
        //
        // A failed startup may lead to a zombie server process, which might never release files,
        // leading to an infinite loop.
        //
        // The value is passed into _postStopServerArchive: The intent is to collect
        // as many files as possible.  That means having the recursive file processing
        // not throw an exception on a failure.  The alternative, which would be to
        // have 'retry' be false, would avoid an infinite loop, but would result in
        // incomplete file collection.

        // Don't retry more than 30s.  If the processing fails because
        // of locked files from a zombie process, the locks might never be removed.

        int nextWait = 500;
        int totalWait = 0;
        final int totalWaitLimit = 30000;

        while (true) {
            try {
                _postStopServerArchive(ignoreFailures, skipArchives);
                break;
            } catch (FileNotFoundException ex) {
                Log.error(c, method, ex, "Failed to archive " + getServerName() + " because of missing files. ");
                break; // The file is never going to appear, so break here.
            } catch (Exception e) {
                Log.error(c, method, e, "Server " + getServerName() + " may still be running.");
                printProcesses();
            }
            if (!retry) {
                break;
            }

            if (totalWait > totalWaitLimit) {
                Log.warning(c, "Server " + getServerName() + ": Wait [ " + totalWait + " ] exceeds maximum [ " + totalWaitLimit + " ]" +
                               ": Failing retries; the server may still be running.");
                break;
            }

            Log.warning(c, "Server " + getServerName() + " may still be running; pausing [ " + nextWait + " ] then retrying.");

            try {
                Thread.sleep(nextWait);
            } catch (Exception x) {
                Log.error(c, method, x);
            }

            totalWait += nextWait;
            nextWait *= 2;
        }

        Log.exiting(c, method);
    }

    /**
     * This method is used to archive server logs after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     *
     * @param ignoreFailures Control parameter: Should failures to copy or move files be ignored?
     * @param skipArchives   Control parameter: Should skippable archives be skipped during file
     *                           collection steps. See {@link #isSkippableArchive(String, String, String)}.
     */
    private void _postStopServerArchive(boolean ignoreFailures, boolean skipArchives) throws Exception {
        final String method = "_postStopServerArchive";
        Log.entering(c, method, skipArchives);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        Date d = new Date(System.currentTimeMillis());

        String logDirectoryName = "";
        logDirectoryName = pathToAutoFVTOutputServersFolder + "/" + getServerNameWithRepeatAction() + "-" + sdf.format(d);
        LocalFile logFolder = new LocalFile(logDirectoryName);
        RemoteFile serverFolder = machine.getFile(serverRoot);

        runJextract(serverFolder);

        // Copy the log files: try to move them instead if we can
        recursivelyCopyDirectory(serverFolder, logFolder, ignoreFailures, skipArchives, true);

        deleteServerMarkerFile();

        // create archive marker file
        if (archiveMarker != null) {
            try {
                new File(new LocalFile(logFolder, archiveMarker).getAbsolutePath()).createNewFile();
            } catch (Exception e) {
                // avoid blowing up on any exception here creating the archive marker
                Log.error(c, "_postStopServerArchive", e);
            }
        }
        Log.exiting(c, method);
    }

    public String getPathToAutoFVTOutputServersFolder() {
        return pathToAutoFVTOutputServersFolder;
    }

    protected void runJextract(RemoteFile serverFolder) throws Exception {
        RemoteFile[] files = serverFolder.list(false);
        if (files != null) {
            for (RemoteFile file : files) {
                String filename = file.getAbsolutePath();
                if (filename.endsWith(".dmp")) {
                    Properties useEnvVars = new Properties();
                    useEnvVars.setProperty("JAVA_HOME", machineJava);
                    Log.info(c, "runJextract", "Running jextract on file: " + filename);

                    String outputFilename = filename + ".zip.DMP"; //adding .DMP to ensure it is collected even when not collecting archives
                    String cmd = machineJava + "/bin/jextract";
                    String[] parms = new String[] { filename, outputFilename };
                    ProgramOutput output = machine.execute(cmd, parms, serverFolder.getAbsolutePath(), useEnvVars);
                    Log.info(c, "runJextract stdout", output.getStdout());
                    Log.info(c, "runJextract stderr", output.getStderr());
                    Log.info(c, "runJextract", "rc = " + output.getReturnCode());
                }
            }
        }
    }

    protected void recursivelyCopyDirectory(RemoteFile remoteFile,
                                            LocalFile logFolder,
                                            boolean ignoreFailures) throws Exception {

        recursivelyCopyDirectory(remoteFile, logFolder, ignoreFailures, false, false);
    }

    private boolean isSkippableArchive(String srcPath, String dstName, String dumpName) {
        // Don't skip zips which are intended for a server dump.

        if (srcPath.endsWith(".jar") ||
            srcPath.endsWith(".war") ||
            srcPath.endsWith(".ear") ||
            srcPath.endsWith(".rar")) {
            return true;
        } else if (srcPath.endsWith(".zip")) {
            return (!dstName.contains(dumpName));
        } else {
            return false;
        }
    }

    private boolean isLog(String localPath, String remoteName, String dumpName) {
        // Only non-FFDC log files are moved.
        //
        // FFDC log files cannot be moved because they must remain for FFDC checking.

        if (localPath.contains("logs")) {
            return (!localPath.contains("ffdc"));

        } else {
            return (remoteName.contains("javacore") ||
                    remoteName.contains("heapdump") ||
                    remoteName.contains("Snap") ||
                    remoteName.contains(dumpName));
        }
    }

    public void recursivelyCopyDirectory(RemoteFile remoteSrcDir,
                                         LocalFile localDstDir,
                                         boolean ignoreFailures, boolean skipArchives, boolean moveFile) throws Exception {

        String method = "recursivelyCopyDirectory";

        Log.finest(c, method, "Remote source directory: " + remoteSrcDir +
                              "\n  Local destination directory: " + localDstDir +
                              "\n  ignore failures: " + ignoreFailures +
                              "\n  skip archives: " + skipArchives +
                              "\n  move file: " + moveFile);

        String remoteSrcDirPath = remoteSrcDir.getAbsolutePath();
        String remoteSrcDirName = remoteSrcDir.getName();

        String localDstDirPath = localDstDir.getAbsolutePath();

        localDstDir.mkdirs();

        if (!localDstDir.exists()) {
            String msg = "Error: Failed to create local [ " + localDstDirPath + " ] to receive remote [ " + remoteSrcDirPath + " ]";
            Log.info(c, method, msg);
            if (ignoreFailures) {
                return;
            } else {
                throw new IOException(msg);
            }
        }

        boolean isLocal = machine.isLocal();

        String dumpName = serverToUse + ".dump";

        boolean isWorkarea = remoteSrcDirName.equals("workarea");
        boolean isCheckpoint = !isWorkarea && remoteSrcDirName.equals("checkpoint");
        boolean isMessaging = !isWorkarea && !isCheckpoint && remoteSrcDirName.equals("messaging");

        for (String remoteSrcFileName : listDirectoryContents(remoteSrcDir)) {
            String skipReason = null;
            if (isWorkarea) {
                if (remoteSrcFileName.equals(OSGI_DIR_NAME) || remoteSrcFileName.startsWith(".s")) {
                    skipReason = "workarea element"; // too big / too racy
                }
            } else if (isCheckpoint) {
                if (remoteSrcFileName.equals("image")) {
                    skipReason = "checkpoint/image element"; // too big
                }
            } else if (isMessaging) {
                skipReason = "message store element"; // ?
            }
            if (skipReason != null) {
                Log.finest(c, method, "Skip [ " + remoteSrcFileName + " ]: " + skipReason);
                continue;
            }

            RemoteFile remoteSrcFile = machine.getFile(remoteSrcDir, remoteSrcFileName);
            LocalFile localDstFile = new LocalFile(localDstDir, remoteSrcFileName);

            if (remoteSrcFile.isDirectory()) {
                recursivelyCopyDirectory(remoteSrcFile, localDstFile, ignoreFailures, skipArchives, moveFile);

            } else {
                String remoteSrcFilePath = remoteSrcFile.getAbsolutePath();

                Log.finest(c, method, "Remote source file [ " + remoteSrcFilePath + " ]");

                if (remoteSrcFilePath.endsWith(".log")) {
                    LogPolice.measureUsedTrace(remoteSrcFile.length());
                }

                if (skipArchives && isSkippableArchive(remoteSrcFilePath, remoteSrcFileName, dumpName)) {
                    Log.finest(c, method, "Skip [ " + remoteSrcFilePath + " ]: Archive");
                    continue;
                }

                String localDstFilePath = localDstFile.getAbsolutePath();
                Log.finest(c, method, "Local destination file [ " + localDstFilePath + " ]");

                String opDesc = "remote [ " + remoteSrcFilePath + " ] to local [ " + localDstFilePath + " ]";

                boolean isLog = moveFile && isLog(remoteSrcFilePath, remoteSrcFileName, dumpName);
                boolean isConfigBackup = moveFile && !isLog && remoteSrcFilePath.contains("serverConfigBackups");

                String opName = null;
                IOException failure = null;

                try {
                    boolean success = false;

                    if (moveFile && (isLog || isConfigBackup)) {
                        if (isLocal) {
                            opName = "rename";
                            success = remoteSrcFile.rename(localDstFile);
                            if (!success) {
                                Log.info(c, method, "Error: Failed rename of " + opDesc + "; falling back to copy and delete");
                            }
                        }
                        if (!success) {
                            opName = "copy and delete";
                            success = localDstFile.copyFromSource(remoteSrcFile) && remoteSrcFile.delete();
                        }
                    } else {
                        opName = "copy";
                        success = localDstFile.copyFromSource(remoteSrcFile);
                    }

                    if (!success) {
                        failure = new IOException("Error: Failed [ " + opName + " ] of " + opDesc);
                    }

                } catch (Exception e) {
                    failure = new IOException("Error: Failed [ " + opName + " ] of " + opDesc, e);
                }

                if (failure != null) {
                    if (!ignoreFailures) {
                        throw failure;
                    } else {
                        Log.error(c, method, failure, "Ignoring failure during transfer of [ " + remoteSrcDirPath + " ] to [ " + localDstDirPath + " ]");
                    }
                } else {
                    Log.finest(c, method, "Successful [ " + opName + " ]" + " of " + opDesc);
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
        return copyFileToTempDir(machine.getFile(serverRoot + "/" + pathInServerRoot), destination);
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

    /* not called */
    public void setMachineJava(String machineJava) {
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
     * @param fileName The name of the file to copy. The file name will be unchanged from source to dest
     */
    public void copyFileToLibertyServerRoot(String fromDir, String toDir, String fileName) throws Exception {
        if (toDir == null)
            toDir = "";
        copyFileToLibertyServerRootUsingTmp(serverRoot + "/" + toDir, (fromDir + "/" + fileName));
    }

    /**
     * Copies a file from the oldAbsolutePath to the newAbsolutePath in the Liberty server.
     *
     * @param  oldAbsolutePath The absolute path of the file to copy.
     * @param  newAbsolutePath The absolute path of the destination.
     * @param  fileName        The name of the file to copy. The file name will be unchanged from source to dest
     *
     * @throws Exception
     */
    public void copyFileToAbsolutePathInLibertyServer(String oldAbsolutePath, String newAbsolutePath, String fileName) throws Exception {
        copyFileToLibertyServerRootUsingTmp(newAbsolutePath, (oldAbsolutePath + "/" + fileName));
    }

    public void renameLibertyServerRootFile(String oldFileName, String newFileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, serverRoot + "/" + oldFileName, serverRoot + "/" + newFileName);
    }

    /**
     * Renames a file from the oldAbsolutePath to the newAbsolutePath in the Liberty server.
     *
     * @param  oldAbsolutePath The absolute path of the file to copy.
     * @param  newAbsolutePath The absolute path of the destination.
     * @param  fileName        The name of the file to rename. The file name will be unchanged from source to dest
     *
     * @throws Exception
     */
    public void renameFileToAbsolutePathInLibertyServerRootFile(String oldAbsolutePath, String newAbsolutePath, String fileName) throws Exception {
        LibertyFileManager.renameLibertyFile(machine, (oldAbsolutePath + "/" + fileName), (newAbsolutePath + "/" + fileName));
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

    public RemoteFile getFileFromLibertyServerWithAbsoluteFilePath(String absoluteFilePath) throws Exception {
        final String method = "getFileFromLibertyServerWithAbsoluteFilePath";
        Log.entering(c, method);
        return getFileFromLiberty(absoluteFilePath);
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

    public String getServerNameWithRepeatAction() {
        String repeatActionString = RepeatTestFilter.getRepeatActionsAsString();
        if (repeatActionString == null || repeatActionString.isEmpty()) {
            return serverToUse;
        } else {
            return serverToUse + "-" + repeatActionString;
        }
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

    public void deleteFileFromAbsolutePathInLibertyServer(String absolutePath) throws Exception {
        LibertyFileManager.deleteLibertyFile(machine, absolutePath);
    }

    public RemoteFile getServerBootstrapPropertiesFile() throws Exception {
        return machine.getFile(serverRoot + "/bootstrap.properties");
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
    public ArrayList<String> listAutoFVTTestFiles(Machine useMachine, String relativeDir, String filter) throws Exception {
        String path = pathToAutoFVTTestFiles;
        if (relativeDir != null && !relativeDir.equals("")) {
            path = path + relativeDir;
        }

        path = LibertyServerUtils.makeJavaCompatible(path, useMachine);

        RemoteFile serverDir = useMachine.getFile(path);
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

        RemoteFile serverDir = machine.getFile(path);
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
        Log.debug(c, "Current list of trace logs: " + files);

        if (files == null || files.isEmpty()) {
            return null;
        }

        RemoteFile rf = null;
        long maxLastModified = 0;
        int nameLength = 0;
        for (int i = 0; i < files.size(); i++) {
            final RemoteFile f = getTraceFile(files.get(i));
            Log.debug(c, "Trace file " + f + "[modified: " + f.lastModified() + "]");
            if (f.lastModified() > maxLastModified ||
                f.lastModified() == maxLastModified && f.getName().length() < nameLength) {
                maxLastModified = f.lastModified();
                nameLength = f.getName().length();
                rf = f;
            }
        }

        return rf;
    }

    public ArrayList<String> listFFDCFiles(String server) throws Exception {
        //This method should respect that the logs folder can be changed, by using getLogsRoot()
        //However, there are some tests in WS-CD that fail after making this change and will need to be fixed first
        //return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(getLogsRoot() + "ffdc", machine), "ffdc");
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(serverRoot + "/logs/ffdc", machine), "ffdc");
    }

    public ArrayList<String> listFFDCSummaryFiles(String server) throws Exception {
        //This method should respect that the logs folder can be changed, by using getLogsRoot()
        //However, there are some tests in WS-CD that fail after making this change and will need to be fixed first
        //return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(getLogsRoot() + "ffdc", machine), "exception_summary");
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(serverRoot + "/logs/ffdc", machine), "exception_summary");
    }

    public ArrayList<String> listDDLFiles(String server) throws Exception {
        return listDirectoryContents(LibertyServerUtils.makeJavaCompatible(serverRoot + "/ddl", machine), "ddl");
    }

    /* not called */
    public int getOsgiConsolePort() {
        return osgiConsolePort;
    }

    public int getHttpDefaultPort() {
        return httpDefaultPort;
    }

    public void setHttpDefaultPort(int httpDefaultPort) {
        this.httpDefaultPort = httpDefaultPort;
    }

    public int getHttpDefaultSecurePort() {
        return httpDefaultSecurePort;
    }

    public void setHttpDefaultSecurePort(int httpDefaultSecurePort) {
        this.httpDefaultSecurePort = httpDefaultSecurePort;
    }

    /**
     * If set the archiveMarker will be used to create an empty marker file
     * in the server archive location. This allows for archive servers
     * to be located easily according to a test marker name.
     *
     * @param archiveMarker the name of the marker file to be created each
     *                          time a server is archived
     */
    public void setArchiveMarker(String archiveMarker) {
        this.archiveMarker = archiveMarker;
    }

    /* not called */
    public int getIiopDefaultPort() {
        return iiopDefaultPort;
    }

    /* not called */
    public void setIiopDefaultPort(int iiopDefaultPort) {
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
        addInstalledAppForValidation(appName);
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
        RemoteFile serverBootStrapProps = machine.getFile(getServerRoot() + "/bootstrap.properties");
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
            RemoteFile jarFile = machine.getFile(jarPath);

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

    protected String getJvmOptionsFilePath() {
        return getServerRoot() + "/" + JVM_OPTIONS_FILE_NAME;
    }

    protected RemoteFile getJvmOptionsFile() throws Exception {
        return LibertyFileManager.createRemoteFile(machine, getJvmOptionsFilePath());
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
        List<String> options = getJvmOptions();
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
        RemoteFile file = getJvmOptionsFile();
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
        setJvmOptions(optionList);
    }

    public void setJvmOptions(List<String> options) throws Exception {
        // Step 1: Write options to local temporary file
        File tmpFile = File.createTempFile("jvm", "options");
        LOG.info("Writing temporary " + JVM_OPTIONS_FILE_NAME + " file: " + tmpFile);
        LOG.info(JVM_OPTIONS_FILE_NAME + ": " + options); // tell the user which properties you're setting
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
            out.write("#Updated by " + getClass().getName() + " on " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
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
            remoteFile = getJvmOptionsFile(); // won't return null
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
            // Ignore
        }

        return props;
    }

    public void addBootstrapProperties(Map<String, String> properties) throws Exception {
        Properties existing = getBootstrapProperties();
        properties.forEach((k, v) -> existing.put(k, v));

        RemoteFile serverBootStrapProps = machine.getFile(getServerRoot() + "/bootstrap.properties");
        try (OutputStream out = serverBootStrapProps.openForWriting(false)) {
            existing.store(out, null);
        }
    }

    public void addEnvVar(String key, String value) {
        if (!Pattern.matches("[a-zA-Z_]+[a-zA-Z0-9_]*", key)) {
            throw new IllegalArgumentException("Invalid environment variable key '" + key +
                                               "'. Environment variable keys must consist of characers [a-zA-Z0-9_] " +
                                               "in order to work on all OSes.");
        }
        if (isStarted())
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
            // Ignore
        }

        // Then load ${server.config.dir}/server.env
        try {
            String serverEnv = FileUtils.readFile(getServerRoot() + "/server.env");
            props.load(new StringReader(serverEnv.replace("\\", "\\\\")));
        } catch (IOException ignore) {
            // Ignore
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
            savedServerXml = machine.getFile(serverRoot + "/savedServerXml" + System.currentTimeMillis() + ".xml");
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
        // wait for ssl port restart
        waitForSSLRestart();
    }

    public String getServerConfigurationPath() {
        return getServerRoot() + "/" + SERVER_CONFIG_FILE_NAME;
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

        if (LOG.isLoggable(Level.INFO) && logOnUpdate) {
            LOG.info("Server configuration before update:");
            logServerConfiguration(Level.INFO, false);
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
            reader = new BufferedReader(new InputStreamReader(getServerConfigurationFile().openForReading()));
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
            LOG.logp(level, CLASS_NAME, method, "Failed to read " + getServerConfigurationPath() + ".  Exception: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.logp(level, CLASS_NAME, method, "Failed to close reader for " + getServerConfigurationPath() + ".  Exception: " + e.getMessage());
                }
            }
        }
    }

    public RemoteFile getConsoleLogFile() throws Exception {
        // Find the currently configured/in-use console log file.
        final RemoteFile remoteFile;
        if (consoleAbsPath == null) {
            return null;
        }
        if (machineOS == OperatingSystem.ZOS) {
            remoteFile = machine.getFile(consoleAbsPath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = machine.getFile(consoleAbsPath);
        }
        return remoteFile;
    }

    public RemoteFile getDefaultLogFile() throws Exception {
        //Set path to server log assuming the default setting.
        // ALWAYS RETURN messages.log -- tests assume they can look for INFO+ messages.
        try {
            RemoteFile file = LibertyFileManager.getLibertyFile(machine, messageAbsPath);
            if (file == null) {
                throw new IllegalStateException("Unable to find default log file, path=" + messageAbsPath);
            }
            return file;
        } catch (FileNotFoundException e) {
            if (isStarted) {
                String msg = e.getMessage() + " and the server was started. Has it been left running from a previous repeat?";
                Exception e2 = new FileNotFoundException(msg);
                e2.initCause(e);
                throw e2;
            }
            throw e;
        }
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
            remoteFile = machine.getFile(absolutePath, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            remoteFile = LibertyFileManager.getLibertyFile(machine, absolutePath);
        }
        List<String> strings = LibertyFileManager.findStringsInFile(regexp, remoteFile);
        return strings;
    }

    /**
     * This method will search the messages.log for this server
     * for the specified expression.
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
     * This method will search {@code logFile} for the specified expression.
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
            remoteFile = machine.getFile(absolutePath, Charset.forName(EBCDIC_CHARSET_NAME));
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
        logMonitor.setMarkToEndOfLog(logFiles);
    }

    /**
     * Set the mark offset to the end of the default trace file (i.e. trace.log).
     *
     * @throws Exception
     */
    public void setTraceMarkToEndOfDefaultTrace() throws Exception {
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

            //This method should respect that the logs folder can be changed, by using findStringsInLogs()
            //However, there are some tests in WS-CD that fail after making this change and will need to be fixed first
            //for (String line : findStringsInLogs(".*((CWWKZ0)|(J2CA7))00[139]I: .*"))
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

    private static final String INSTALL_FEATURE_MESSAGE_PREFIX = "CWWKF0012I: The server installed the following features:";

    /**
     * Returns sets of the features which were installed at runtime startup, based on the CWWKF0012I message in messages.log.
     * This message can occur multiple times, hence multiple sets
     *
     * e.g.
     * CWWKF0012I: The server installed the following features: [bells-1.0, cdi-4.0, componenttest-2.0, concurrent-3.0, jndi-1.0, mpConfig-3.1, mpContextPropagation-1.3,
     * mpFaultTolerance-4.0, servlet-6.0, timedexit-1.0].
     *
     * @return           sets of the features installed at runtime
     * @throws Exception
     */
    public List<Set<String>> getInstalledFeatures() throws Exception {
        List<Set<String>> installedFeatures = new ArrayList<>();

        for (String line : findStringsInLogs(INSTALL_FEATURE_MESSAGE_PREFIX)) {
            Set<String> installedFeatureSet = new HashSet<>();
            installedFeatureSet.addAll(getInstalledFeaturesFromLogMessage(line));
            installedFeatures.add(installedFeatureSet);
        }

        return installedFeatures;
    }

    /**
     * Returns a set of the features which were installed at runtime, based on the CWWKF0012I message from the messages.log file.
     *
     * Only a line containing a CWWKF0012I message should be passed in. It may be prefixed with timestamps etc.
     * e.g.
     * [04/07/2024, 15:26:31:119 BST] 00000035 com.ibm.ws.kernel.feature.internal.FeatureManager A
     * CWWKF0012I: The server installed the following features: [bells-1.0, cdi-4.0, componenttest-2.0,
     * concurrent-3.0, jndi-1.0, mpConfig-3.1, mpContextPropagation-1.3, mpFaultTolerance-4.0, servlet-6.0, timedexit-1.0].
     *
     * @return           a set of the features installed at runtime
     * @throws Exception
     */
    public static final Set<String> getInstalledFeaturesFromLogMessage(String installedFeaturesMessage) {
        Set<String> installedFeatures = new HashSet<>();
        int prefixIndex = installedFeaturesMessage.indexOf(INSTALL_FEATURE_MESSAGE_PREFIX);
        if (prefixIndex > -1) {
            String trimmed = installedFeaturesMessage.substring(prefixIndex); //strip off the date & time etc
            int openBracketIndex = trimmed.indexOf("[");
            if (openBracketIndex > -1) {
                trimmed = trimmed.substring(openBracketIndex + 1); //strip off everything up to and including the first "["
                int closeBracketIndex = trimmed.indexOf("]");
                if (closeBracketIndex > -1) {
                    trimmed = trimmed.substring(0, closeBracketIndex);// remove "]."
                    String[] features = trimmed.split(", ");
                    for (String feature : features) {
                        installedFeatures.add(feature);
                    }
                } else {
                    Log.warning(LibertyServer.class, "Installed Features Message CWWKF0012I was not formatted as expected: " + installedFeaturesMessage);
                }
            } else {
                Log.warning(LibertyServer.class, "Installed Features Message CWWKF0012I was not formatted as expected: " + installedFeaturesMessage);
            }
        } else {
            Log.warning(LibertyServer.class, "Installed Features Message CWWKF0012I was not formatted as expected: " + installedFeaturesMessage);
        }
        return installedFeatures;
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
                                //There used to be a break here but if a user passed in a
                                //pattern that overlapped with one of the ones above only
                                //one was removed. The watchFor list is usually small.
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
            Log.warning(c, "Could not read log file: " + outputFile + " due to exception " + e);
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
    public String waitForStringInLogUsingLastOffset(String regexp, long intendedTimeout, RemoteFile outputFile) {
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
            Log.warning(c, "Could not read log file: " + outputFile + " due do exception " + e);
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
    public String waitForStringInLogUsingMark(String regexp, long intendedTimeout, RemoteFile outputFile) {
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
     * Check for multiple instances of the regex in log using mark
     *
     * @param  numberOfMatches number of matches required
     * @param  regexp          a regular expression to search for
     * @param  outputFile      file to check
     * @return                 number of matches found
     */
    public int waitForMultipleStringsInLogUsingMark(int numberOfMatches, String regexp, RemoteFile outputFile) {
        return logMonitor.waitForMultipleStringsInLogUsingMark(numberOfMatches, regexp, LOG_SEARCH_TIMEOUT, outputFile);
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

        if (f == null) {
            Log.info(c, "waitForStringInTrace", "Failed to getMostRecentTraceFile(). Server " + getServerName() + " is probably stopping.");
            return null;
        }

        Log.info(c, "waitForStringInTrace", "Waiting for \"" + regexp + "\" to be found in " + f);

        if (timeout > 0) {
            return waitForStringInLog(regexp, timeout, f);
        } else {
            return waitForStringInLog(regexp, f);
        }
    }

    private enum AppState {
        STARTED,
        STOPPED
    }

    /**
     * Wait for an application to be started or stopped
     *
     * @param  appName          the application name to wait for
     * @param  state            whether we're waiting for it to start or stop
     * @param  timeout          the timeout in ms
     * @throws RuntimeException if the app does not reach the expected state within the timeout
     */
    private void waitForAppState(String appName, AppState state, long timeout) {
        Log.info(c, "waitForAppState", "Starting wait for " + appName + " to be " + state);
        AppState currentState = null;
        String lastMessage = null;
        long waited = 0;
        while (waited <= timeout) {
            try {
                List<String> strings = findStringsInLogs("CWWKZ000(1|9)I:.*" + appName);
                if (!strings.isEmpty()) {
                    lastMessage = strings.get(strings.size() - 1);
                    if (lastMessage.contains("CWWKZ0001I")) {
                        currentState = AppState.STARTED;
                    } else {
                        currentState = AppState.STOPPED;
                    }
                } else {
                    currentState = AppState.STOPPED; // If there's no started message, assume it's stopped
                }
                if (currentState == state) {
                    break;
                }
                Thread.sleep(LogMonitor.WAIT_INCREMENT);
                waited += LogMonitor.WAIT_INCREMENT;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (currentState != state) {
            if (throwExceptionOnAppStateError()) {
                throw new RuntimeException("Timed out waiting for " + appName + " to be in state " + state + ". Actual state: " + currentState + ", Last log message:"
                                           + lastMessage);
            } else {
                Log.info(c, "waitForAppState", "Application " + appName + " did not reach " + state + " in " + waited + "ms");
            }
        } else {
            Log.info(c, "waitForAppState", "Application " + appName + " reached " + state + " in " + waited + "ms");
        }
    }

    protected boolean throwExceptionOnAppStateError() {
        return true;
    }

    public void addInstalledAppForValidation(String app) {
        final String method = "addInstalledAppForValidation";
        Log.info(c, method, "Adding installed app: " + app + " for validation");
        installedApplications.add(app);

        if (isStarted) {
            waitForAppState(app, AppState.STARTED, APP_START_TIMEOUT);
        }
    }

    public void removeInstalledAppForValidation(String app) {
        final String method = "removeInstalledAppForValidation";
        Log.info(c, method, "Removing installed app: " + app + " for validation");
        installedApplications.remove(app);

        if (isStarted) {
            waitForAppState(app, AppState.STOPPED, LOG_SEARCH_TIMEOUT);
        }
    }

    public void removeAllInstalledAppsForValidation() {
        final String method = "removeInstalledAppForValidation";
        Log.info(c, method, "Removing following list of installed application for validation");
        for (String app : installedApplications) {
            Log.info(c, method, " -" + app);
        }
        List<String> appsRemoved = new ArrayList<>(installedApplications);
        installedApplications.clear();

        if (isStarted) {
            for (String app : appsRemoved) {
                waitForAppState(app, AppState.STOPPED, LOG_SEARCH_TIMEOUT);
            }
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
     * @param  cleanStart      if true, the server will be started with a clean start
     * @param  useValidateApps if true, block until all of the registered apps have started
     * @throws Exception
     */
    public void startServer(boolean cleanStart, boolean useValidateApps) throws Exception {
        startServerAndValidate(true, cleanStart, useValidateApps);
    }

    public void deleteAllDropinApplications() throws Exception {
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, getServerRoot() + "/dropins");
        LibertyFileManager.createRemoteFile(machine, getServerRoot() + "/dropins").mkdir();
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
        removeInstalledAppForValidation(appName);

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

        Properties useEnvVars = null;
        if (customUserDir) {
            useEnvVars = new Properties();
            useEnvVars.setProperty("WLP_USER_DIR", userDir);
        }

        return LibertyServerUtils.execute(machine, machineJava, useEnvVars, cmd, parms);
    }

    /**
     * Issues a server script command against this server with environment variables
     *
     * @param  command
     * @param  optionalArgs
     * @param  envVars
     * @return
     * @throws Exception
     */
    public ProgramOutput executeServerScript(String command, String[] optionalArgs, Properties envVars) throws Exception {
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

        Properties _envVars = null;
        if (envVars != null) {
            _envVars = new Properties();
            _envVars.putAll(envVars);
        }

        if (customUserDir) {
            _envVars = new Properties();
            _envVars.setProperty("WLP_USER_DIR", userDir);
        }

        if (!_envVars.isEmpty())
            Log.info(c, method, "Adding env vars: " + _envVars);

        return LibertyServerUtils.execute(machine, machineJava, _envVars, cmd, parms);
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

        Properties useEnvVars = new Properties();
        useEnvVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            useEnvVars.setProperty("WLP_USER_DIR", userDir);
        Log.info(c, method, "Using additional env props: " + useEnvVars);

        ProgramOutput output = machine.execute(cmd, parms, useEnvVars);
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

        Properties useEnvVars = new Properties();
        useEnvVars.setProperty("JAVA_HOME", machineJava);
        if (customUserDir)
            useEnvVars.setProperty("WLP_USER_DIR", userDir);
        Log.info(c, method, "Using additional env props: " + useEnvVars);

        ProgramOutput output = machine.execute(cmd, parms, machine.getWorkDir(), useEnvVars, 300);
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
            keyFile.copyToDest(getMachine().getFile(getServerRoot() + "/resources/security/key.jks"));
        else {
            keyFile = new LocalFile(pathToAutoFVTTestFiles + "/tmp/key.p12");
            if (keyFile.exists())
                keyFile.copyToDest(getMachine().getFile(getServerRoot() + "/resources/security/key.p12"));
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
            RemoteFile f = getMostRecentTraceFile();

            Log.info(c, "waitForStringInTrace", "Waiting for \"" + regexp + "\" to be found in " + f);
            return waitForStringInLogUsingMark(regexp, timeout, f);
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

        boolean isJava2SecExempt = !serverNeedsToRunWithJava2Security();
        Log.info(c, "isJava2SecurityEnabled", "Is server " + getServerName() + " Java 2 Security exempt?  " + isJava2SecExempt);
        return !isJava2SecExempt;
    }

    protected boolean serverNeedsToRunWithJava2Security() {
        // Allow servers to opt-out of j2sec by setting
        // websphere.java.security.exempt=true
        // in their ${server.config.dir}/bootstrap.properties
        return !"true".equalsIgnoreCase(getBootstrapProperties().getProperty("websphere.java.security.exempt"));
    }

    private boolean isEE11OrLaterEnabled() throws Exception {
        if (JakartaEEAction.isEE11OrLaterActive()) {
            return true;
        }

        if (JakartaEEAction.isEE9OrLaterActive()) {
            return false;
        }

        // EE 11 which doesn't support Java security manager can run with Java 17.  As such we need to return false even if we are running
        // with Java security enabled in the build.

        RemoteFile serverXML = machine.getFile(serverRoot + "/" + SERVER_CONFIG_FILE_NAME);
        InputStreamReader in = new InputStreamReader(serverXML.openForReading());
        try (Scanner s = new Scanner(in)) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.contains("<featureManager>")) {//So has reached featureSets
                    while (s.hasNextLine()) {
                        line = s.nextLine();
                        if (line.contains("</featureManager>"))
                            break;

                        line = line.replaceAll("<feature>", "");
                        line = line.replaceAll("</feature>", "");
                        line = line.trim();
                        String lowerCaseFeatureName = line.toLowerCase();

                        // special case data-1.1 until JakartaEE12Action is created when EE 12 is more of a thing.
                        if (JakartaEE11Action.EE11_ONLY_FEATURE_SET_LOWERCASE.contains(lowerCaseFeatureName) || "data-1.1".equals(lowerCaseFeatureName)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // FIPS 140-3
    public boolean isFIPS140_3EnabledAndSupported(JavaInfo serverJavaInfo, boolean logOutput) throws IOException {
        String methodName = "isFIPS140_3EnabledAndSupported";
        boolean isIBMJVM8 = (serverJavaInfo.majorVersion() == 8) && (serverJavaInfo.VENDOR == Vendor.IBM);
        boolean isIBMJVM17 = (serverJavaInfo.majorVersion() == 17) && (serverJavaInfo.VENDOR == Vendor.IBM);
        if (logOutput && GLOBAL_FIPS_140_3) {
            Log.info(c, methodName, "Liberty server is running JDK version: " + serverJavaInfo.majorVersion()
                                    + " and vendor: " + serverJavaInfo.VENDOR);
            if (isIBMJVM8) {
                Log.info(c, methodName, "global build properties FIPS_140_3 is set for server " + getServerName() +
                                        " and IBM java 8 is available to run with FIPS 140-3 enabled.");
            } else if (isIBMJVM17) {
                Log.info(c, methodName, "global build properties FIPS_140_3 is set for server " + getServerName() +
                                        " and IBM java 17 is available to run with FIPS 140-3 enabled.");
            } else {
                Log.info(c, methodName, "The global build properties FIPS_140_3 is set for server " + getServerName() +
                                        ",  but no IBM java 8 or java 17 on liberty server to run with FIPS 140-3 enabled.");
            }
        }
        return GLOBAL_FIPS_140_3 && (isIBMJVM8 || isIBMJVM17);
    }

    public boolean isFIPS140_3EnabledAndSupported() throws IOException {
        return isFIPS140_3EnabledAndSupported(JavaInfo.forServer(this), true);
    }

    public boolean isFIPS140_3EnabledAndSupported(JavaInfo info) throws IOException {
        return isFIPS140_3EnabledAndSupported(info, true);
    }

    /**
     * No longer using bootstrap properties to update server config for database rotation.
     * Instead look at using the fattest.databases module
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void configureForAnyDatabase() throws Exception {
        ServerConfiguration config = getServerConfiguration();
        config.updateDatabaseArtifacts();
        updateServerConfiguration(config);
    }

    @SuppressWarnings("deprecation")
    public boolean isIBMJVM() {
        return javaInfo.vendor() == JavaInfo.Vendor.IBM;
    }

    @SuppressWarnings("deprecation")
    public boolean isOracleJVM() {
        return javaInfo.vendor() == JavaInfo.Vendor.SUN_ORACLE;
    }

    public void useSecondaryHTTPPort() {
        setHttpDefaultPort(getHttpSecondaryPort());
        setHttpDefaultSecurePort(getHttpSecondarySecurePort());
    }

    public void setConsoleLogName(String consoleLogName) {
        this.consoleFileName = consoleLogName;
        this.consoleAbsPath = logsRoot + consoleLogName;
    }

    public void setAdditionalSystemProperties(Map<String, String> additionalSystemProperties) {
        this.additionalSystemProperties = additionalSystemProperties;
    }

    public void clearAdditionalSystemProperties() {
        this.additionalSystemProperties = null;
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
        return ffdcChecking;
    }

    @Override
    public RemoteFile lmcGetDefaultLogFile() throws Exception {
        return getDefaultLogFile();
    }

    @Override
    public void lmcClearLogOffsets() {
        logOffsets.clear();
        originOffsets.clear();
    }

    @Override
    public void lmcResetLogOffsets() {
        logOffsets = new HashMap<String, Long>(originOffsets);
    }

    @Override
    public void lmcSetOriginLogOffsets() {
        originOffsets = new HashMap<String, Long>(logOffsets);
    }

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

    private Boolean checkpointSupported;

    /**
     * <pre>
     *   bin/server start --checkpoint==[PHASE];
     *   bin/server restore
     * </pre>
     *
     * @return           true if the server has requisite support for the following operations, false otherwise.
     *
     * @throws Exception we may attempt to fork a new jvm to test for support. An exception typically
     *                       means a Failure around forking the new process.
     */
    public boolean getCheckpointSupported() {
        if (checkpointSupported == null) {
            // Check if criu supported. Needed to run checkpoint/restore tests.
            if ("LINUX".equals(machineOS.name().trim().toUpperCase())) {
                JavaInfo jinfo;
                try {
                    jinfo = JavaInfo.fromPath(machineJava);
                } catch (IOException e) {
                    LOG.warning("Unable to detect platform support for criu: " + e.getMessage());
                    return false; //no cache of checkpointSupported let it keep trying.
                }
                checkpointSupported = jinfo.isCriuSupported();
            } else {
                checkpointSupported = false;
            }
        }
        return checkpointSupported;
    }

    protected String getOpenLibertyPropertiesFilePath() {
        return getInstallRoot() + "/lib/versions/" + OPENLIBERTY_PROPERTIES_FILE_NAME;
    }

    protected RemoteFile getOpenLibertyPropertiesFile() throws Exception {
        return LibertyFileManager.createRemoteFile(machine, getOpenLibertyPropertiesFilePath());
    }

    /**
     * @return the contents of the wlp/lib/versions/openliberty.properties file
     */
    public Properties getOpenLibertyProperties() {
        if (openLibertyProperties == null) {
            openLibertyProperties = new Properties();
            InputStream is = null;
            try {
                is = getOpenLibertyPropertiesFile().openForReading();
                openLibertyProperties.load(is);
            } catch (Exception e) {
                LOG.warning("Unable to read openliberty.properties file: " + e.getMessage());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOG.warning("Unable to close input stream for openliberty.properties file: " + e.getMessage());
                    }
                }
            }
        }
        return openLibertyProperties;
    }

    /**
     * @return the product version value from the openliberty.properties file
     */
    public String getOpenLibertyVersion() {
        if (openLibertyVersion == null) {
            openLibertyVersion = (String) getOpenLibertyProperties().get(COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY);
        }
        return openLibertyVersion;
    }

    public String getEnvVar(String var) {
        return envVars.get(var);
    }

    public void configureLTPAKeys(JavaInfo info) throws IOException, InterruptedException {

        if (isFIPS140_3EnabledAndSupported(info)) {
            String serverSecurityDir = serverRoot + File.separator + "resources" + File.separator + "security";
            File ltpaFIPSKeys = new File(serverSecurityDir, "ltpaFIPS.keys");
            File ltpaKeys = new File(serverSecurityDir, "ltpa.keys");
            String serverName = getServerName();
            boolean fipsKeyExists = ltpaFIPSKeys.exists();

            if (!ltpaKeys.exists() && !fipsKeyExists) {
                Log.info(this.getClass(), "configureLTPAKeys",
                         "FIPS 140-3 global build properties are set for server " + serverName
                                                               + ", but neither ltpa.keys nor ltpaFIPS.keys is found in " + serverSecurityDir);
            } else {
                Log.info(this.getClass(), "configureLTPAKeys",
                         "FIPS 140-3 global build properties are set for server " + serverName
                                                               + ", swapping ltpaFIPS.keys into ltpa.keys");
            }

            if (fipsKeyExists) {
                Files.move(ltpaFIPSKeys.toPath(), ltpaKeys.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // Log.info(this.getClass(), "configureLTPAKeys",
                //         "Waiting for 2 seconds after updating ltpa.keys ...");
                // Thread.sleep(2000);
            }
            if (ltpaKeys.exists()) {
                // Log the content of ltpa.keys
                String content = FileUtils.readFile(ltpaKeys.getAbsolutePath());
                Log.info(this.getClass(), "configureLTPAKeys", "Content of ltpa.keys: " + content);
            }
        }
    }

    public void configureLTPAKeys() throws IOException, InterruptedException {
        configureLTPAKeys(JavaInfo.forServer(this));
    }

    private Map<String, String> getFipsJvmOptions(JavaInfo info, boolean includeGlobalArgs) throws Exception, IOException {
        Map<String, String> opts = new HashMap<>();
        opts.putAll(this.getJvmOptionsAsMap()); //Add all current JVM option so we don't unintentionally clear any set by tests.
        if (isFIPS140_3EnabledAndSupported(info, false)) {
            if (info.majorVersion() == 17) {
                Log.info(c, "getFipsJvmOptions",
                         "FIPS 140-3 global build properties is set for server " + getServerName()
                                                 + " with IBM Java 17, adding required JVM arguments to run with FIPS 140-3 enabled");
                opts.put("-Dsemeru.fips", "true");
                opts.put("-Dsemeru.customprofile", "OpenJCEPlusFIPS.FIPS140-3-withPKCS12");
                opts.put("-Dcom.ibm.fips.mode", "140-3");
            } else if (info.majorVersion() == 8) {
                Log.info(c, "getFipsJvmOptions", "FIPS 140-3 global build properties is set for server "
                                                 + getServerName()
                                                 + " with IBM Java 8, adding JVM arguments -Xenablefips140-3, ...,  to run with FIPS 140-3 enabled");
                opts.put("-Xenablefips140-3", null);
                opts.put("-Dcom.ibm.jsse2.usefipsprovider", "true");
                opts.put("-Dcom.ibm.jsse2.usefipsProviderName", "IBMJCEPlusFIPS");
                opts.put("-Dcom.ibm.fips.mode", "140-3");

            }
            if (includeGlobalArgs) {
                opts.put("-Dglobal.fips_140-3", "true");
                opts.put("-Dcom.ibm.ws.beta.edition", "true");
            }
        }
        return opts;
    }

    public void setKeysAndJVMOptsForFips() throws Exception {
        // Enable FIPS on members via jvm.options file. This way when the controller starts / joins members
        // the appropriate FIPS jvm arguments will be configured.
        JavaInfo info = JavaInfo.forServer(this);
        if (isFIPS140_3EnabledAndSupported(info)) {
            this.configureLTPAKeys(info);
            Map<String, String> jvm_opts = this.getJvmOptionsAsMap();
            Map<String, String> combined = new HashMap(jvm_opts);
            combined.putAll(this.getFipsJvmOptions(info, true));
            if (!combined.isEmpty() && !combined.equals(jvm_opts)) {
                this.setJvmOptions(combined);
            }
        }
    }
}
