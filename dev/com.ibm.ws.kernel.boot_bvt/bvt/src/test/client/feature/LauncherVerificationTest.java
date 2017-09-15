/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.client.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

import test.common.SharedOutputManager;

public class LauncherVerificationTest {

    private static final String WLP_USER_DIR = "WLP_USER_DIR";

    private static final String PRODUCT_EXT_FEATURE_MANIFEST = "prodtest1-1.0.mf";

    private static final String PRODUCT_EXTENSION_JAR = "test.prod.extensions_1.0.0.jar";

    /**
     * Environment variable used to test the UNIX scripts on Windows (it is
     * never used in production or by any builds). For a typical Cygwin
     * installation, export WLP_CYGWIN_HOME=c:/cygwin before running Ant.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    private static final String BOOT_SERVER = "com.ibm.ws.kernel.boot.bvt";
    private static final String BOOT_DUMP_REGEX = "com\\.ibm\\.ws\\.kernel\\.boot\\.bvt\\.dump-.*\\.zip";
    private static final String PACKAGE_REGEX = "com\\.ibm\\.ws\\.kernel\\.boot\\.bvt\\.zip";
    private static final String NOTEXIST_SERVER = "notexist";
    private static final String ENV_SERVER = "com.ibm.ws.kernel.boot.env.bvt";
    private static final String ALL_USR_FEATURE_MF = "packageAllUsrFeature.mf";
    private static final String USR_USR_FEATURE_MF = "packageUsrUsrFeature.mf";

    private static final Pattern DUMP_THREAD_PATTERN = Pattern.compile("java(dump|core)\\..*\\.txt");
    private static final Pattern DUMP_HEAP_PATTERN = Pattern.compile("heapdump\\..*\\.phd|java\\..*\\.hprof");
    private static final Pattern DUMP_SYSTEM_PATTERN = Pattern.compile("core\\..*\\.dmp");

    private static final String PRODUCT_EXT_PROPERTIES = "test.properties";

    private static ScheduledExecutorService schedExec = Executors.newSingleThreadScheduledExecutor();

    private static PrintStream testLogStream = System.out;

    // default timeout 2 minutes
    private static int TIMEOUT = 2 * 60 * 1000;

    private static String installDir;
    private static String installParent;
    private static String serverScript;
    private static String serversDir;
    private static String usrDir;
    private static String agentToolJar;
    private static String agentLibJar;
    private static String launchToolJar;
    private static String launchLibJar;
    private static String javaHome;
    private static String java;
    private static String osName;
    private static boolean isWindows;
    private static boolean isMac;
    private static boolean isJ9;
    private static int counter;

    @Rule
    public SharedOutputManager sharedOutputManager = SharedOutputManager.getInstance();

    @Rule
    public TestRule dumpScriptsOnFail = new TestRule() {
        @Override
        public Statement apply(final Statement statement, Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        statement.evaluate();
                    } catch (Throwable t) {
                        if (serverOutputDir != null) {
                            dumpScriptLogs(serverOutputDir);
                        }
                        if (serverLogDir != null) {
                            dumpScriptLogs(serverLogDir);
                        }
                    }
                }
            };
        }

    };

    private static ProcessBuilder builder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        javaHome = System.getProperty("java.home");

        // Check for windows..
        osName = System.getProperty("os.name", "unknown").toLowerCase();
        isWindows = osName.indexOf("windows") >= 0;
        isMac = osName.indexOf("mac os") >= 0;
        isJ9 = System.getProperty("java.vm.name", "unknown").contains("J9");

        installDir = System.getProperty("install.dir", "build/bvt/wlp (&+)");
        File file = new File(installDir);
        if (!file.isDirectory()) {
            fail("Could not find wlp install dir");
        }

        installDir = file.getAbsolutePath();

        installParent = file.getParentFile().getAbsolutePath();

        serverScript = installDir + File.separatorChar + "bin" + File.separatorChar + "server";
        agentToolJar = installDir + File.separatorChar + "bin" + File.separatorChar + "tools" + File.separatorChar + "ws-javaagent.jar";
        agentLibJar = installDir + File.separatorChar + "lib" + File.separatorChar + "bootstrap-agent.jar";
        launchToolJar = installDir + File.separatorChar + "bin" + File.separatorChar + "tools" + File.separatorChar + "ws-server.jar";
        launchLibJar = installDir + File.separatorChar + "lib" + File.separatorChar + "ws-launch.jar";
        serversDir = installDir + File.separatorChar + "usr" + File.separatorChar + "servers";
        usrDir = installDir + File.separatorChar + "usr";
        java = javaHome + File.separatorChar + "bin" + File.separatorChar + "java";
    }

    private static <T> Matcher<T> matchIf(boolean match, Matcher<T> matcher) {
        return match ? matcher : Matchers.not(matcher);
    }

    private static String findStringInListOutput(String regexp, List<String> lines) throws IOException {
        for (String line : lines) {
            if (line.matches(regexp)) {
                return line;
            }
        }
        return null;
    }

    @Rule
    public TestName testName = new TestName();

    enum LauncherMode {
        /** bin/server */
        SCRIPT,
        /** -javaagent:bin/tools/ws-javaagent.jar -jar bin/tools/ws-server.jar */
        TOOL_JAR,
        /** -javaagent:lib/bootstrap-agent.jar -jar lib/ws-launch.jar */
        LIB_JAR,
    }

    /**
     * The mode for launching the server.
     */
    private LauncherMode launcherMode = LauncherMode.SCRIPT;

    /**
     * The output directory for the server being used by a test method.
     */
    private File serverOutputDir;

    /**
     * The log directory for the server being used by a test method.
     */
    private File serverLogDir;

    @Before
    public void setUp() throws Exception {
        serverOutputDir = serverLogDir = null;
        File logFile = new File(serversDir + File.separatorChar + BOOT_SERVER, testName.getMethodName() + ".log");

        // autoflush this log file
        testLogStream = new PrintStream(new FileOutputStream(logFile), true);

        // Use the same format the logging files use to help match up w/ server log files..
        testLogStream.println(DataFormatHelper.formatCurrentTime() + "setUp");

        // Allocate new process builder
        builder = new ProcessBuilder();
        builder.redirectErrorStream(true);

        // Set active directory (install dir)
        builder.directory(new File(installDir));
        builder.environment().put("JAVA_HOME", javaHome);
        if (isWindows) {
            // To fix server.bat exit codes on Windows XP.
            builder.environment().put("EXIT_ALL", "1");
        }

        testLogStream.println("installDir   = " + installDir);
        testLogStream.println("serverScript = " + serverScript);
        testLogStream.println("agentJar     = " + agentLibJar);
        testLogStream.println("launchJar    = " + launchLibJar);
        testLogStream.println("serversDir   = " + serversDir);
        testLogStream.println("java         = " + java);
    }

    @After
    public void tearDown() throws Exception {
        testLogStream.println(DataFormatHelper.formatCurrentTime() + "tearDown");
        testLogStream.flush();
        testLogStream.close();
    }

    private void dumpScriptLogs(File logDir) {
        if (logDir != null && logDir.isDirectory()) {
            for (String logName : new String[] { "status.log", "start.log", "stop.log", "package.log", "dump.log", "javadump.log", "console.log", "messages.log" }) {
                File logFile = new File(logDir, "logs" + File.separator + logName);
                if (logFile.exists()) {
                    testLogStream.println("-- " + logFile + " -------------------------------------------------");

                    try {
                        FileInputStream input = new FileInputStream(logFile);
                        try {
                            byte[] buf = new byte[4096];
                            for (int read; (read = input.read(buf)) != -1;) {
                                testLogStream.write(buf, 0, read);
                            }
                        } finally {
                            if (input != null) {
                                input.close();
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private void waitForFileCreate(File file) throws InterruptedException {
        int iterations = TIMEOUT / 500;
        for (int i = 0; i < iterations && !file.exists(); i++) {
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Waiting for " + file + " to be created...");
            Thread.sleep(500);
        }

        Assert.assertTrue(file.toString() + " should exist", file.exists());
    }

    private void waitForFileDelete(File file) throws InterruptedException {
        int iterations = TIMEOUT / 500;
        for (int i = 0; i < iterations && file.exists(); i++) {
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Waiting for " + file + " to be deleted..");
            Thread.sleep(500);
        }

        Assert.assertFalse(file.toString() + " should have been deleted", file.exists());
    }

    private void command(String command, String server, String... args) {
        command(launcherMode, command, server, args);
    }

    private void command(LauncherMode launcherMode, String command, String server, String... args) {
        command(builder, launcherMode, command, server, args);
    }

    private void command(ProcessBuilder builder, LauncherMode launcherMode, String command, String server, String... args) {
        ArrayList<String> newArgs = new ArrayList<String>();
        if (launcherMode == LauncherMode.SCRIPT) {
            if (isWindows) {
                if (WLP_CYGWIN_HOME == null) {
                    newArgs.add(serverScript + ".bat");
                } else {
                    newArgs.add(WLP_CYGWIN_HOME + "/bin/sh");
                    newArgs.add("-x");
                    newArgs.add(serverScript);
                }
            } else {
                newArgs.add("/bin/sh");
                newArgs.add("-x");
                newArgs.add(serverScript);
            }

            if (command != null) {
                newArgs.add(command);
                if (server != null) {
                    newArgs.add(server);
                }
            }
        } else {
            newArgs.add(java);
            if (launcherMode == LauncherMode.LIB_JAR) {
                newArgs.add("-javaagent:" + agentLibJar);
                newArgs.add("-jar");
                newArgs.add(launchLibJar);
            } else {
                newArgs.add("-javaagent:" + agentToolJar);
                newArgs.add("-jar");
                newArgs.add(launchToolJar);
            }
            if (server != null) {
                newArgs.add(server);
            }
            if (!command.equals("start")) {
                newArgs.add("--" + command);
            }
        }

        newArgs.addAll(Arrays.asList(args));

        newArgs.add("--");
        newArgs.add(testName.getMethodName());

        builder.command(newArgs);
    }

    protected int doEverything(final Process p, final String commandLine, String assertDescr, int... expected) throws IOException, InterruptedException {
        counter++;

        testLogStream.println(); // pad to make these easier to pick out
        testLogStream.println(DataFormatHelper.formatCurrentTime() + assertDescr);
        testLogStream.println("[" + counter + "] +=== INVOKE " + commandLine);

        StreamCopier copier = new StreamCopier(p.getInputStream(), "[" + counter + "] ");
        copier.start();

        // Create something that will interrupt this thread if something happens
        // in one of the two methods below (waitFor / doJoin)...
        WakeUp w = new WakeUp(Thread.currentThread());
        ScheduledFuture<?> future = schedExec.schedule(w, TIMEOUT, TimeUnit.MILLISECONDS);

        int exitVal = p.waitFor();
        copier.doJoin();

        // clear the future
        future.cancel(false);
        testLogStream.println("[" + counter + "] ---+ RC=" + exitVal + ", expected=" + Arrays.toString(expected));
        testLogStream.println(DataFormatHelper.formatCurrentTime() + "RC=" + exitVal + ", " + assertDescr);

        boolean foundExitVal = false;
        for (int expectedVal : expected) {
            if (exitVal == expectedVal) {
                foundExitVal = true;
                break;
            }
        }

        Assert.assertTrue(assertDescr + ",  RC=" + exitVal + ", expected=" + Arrays.toString(expected), foundExitVal);
        return exitVal;
    }

    private static class StreamCopier extends Thread {
        private final BufferedReader reader;
        private final String prefix;
        private boolean joined;
        private boolean terminated;

        StreamCopier(InputStream input, String prefix) {
            this.reader = new BufferedReader(new InputStreamReader(input));
            this.prefix = prefix;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                for (String line; (line = reader.readLine()) != null;) {
                    synchronized (this) {
                        if (joined) {
                            // The main thread was notified that the process
                            // ended and has already given up waiting for
                            // output from the foreground process.
                            break;
                        }

                        testLogStream.println(prefix.concat(line));
                    }
                }
            } catch (IOException ex) {
                throw new Error(ex);
            } finally {
                if (isWindows) {
                    synchronized (this) {
                        terminated = true;
                        notifyAll();
                    }
                }
            }
        }

        public void doJoin() throws InterruptedException {
            if (isWindows) {
                // Windows doesn't disconnect background processes (start /b)
                // from the console of foreground processes, so waiting until
                // the end of output from server.bat means waiting until the
                // server process itself ends.  We can't wait that long, so we
                // wait one second after .waitFor() ends.  Hopefully this will
                // be long enough to copy all the output from the script.

                synchronized (this) {
                    long begin = System.nanoTime();
                    long end = begin + TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
                    long duration = end - begin;
                    while (!terminated && duration > 0) {
                        TimeUnit.NANOSECONDS.timedWait(this, duration);
                        duration = end - System.nanoTime();
                    }

                    // If the thread didn't end after waiting for a second,
                    // then assume it's stuck in a blocking read.  Oh well,
                    // it's a daemon thread, so it'll go away eventually.  Let
                    // it know that we gave up to avoid spurious output in case
                    // it eventually wakes up.
                    joined = true;
                }
            } else {
                super.join();
            }
        }
    }

    private static class WakeUp implements Runnable {
        private final Thread t;

        public WakeUp(Thread t) {
            this.t = t;
        }

        @Override
        public void run() {
            t.interrupt();
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "*** THREAD INTERRUPTED DUE TO TIMEOUT / WAKE UP");
        }
    }

    private void startServer(String desc, String serverName, int... expected) throws IOException, InterruptedException {
        startServer(desc, launcherMode, serverName, expected);
    }

    private void startServer(String desc, LauncherMode launcherMode, String serverName, int... expected) throws IOException, InterruptedException {
        if (launcherMode == LauncherMode.SCRIPT) {
            command(launcherMode, "start", serverName);
            // doEverything increments counter...
            builder.environment().put("LOG_FILE", "console." + (counter + 1) + ".log");
            Process p = builder.start();
            doEverything(p, builder.command().toString(), desc, expected);
        } else {
            testLogStream.println(DataFormatHelper.formatCurrentTime() + desc);

            // First, check if the server is running.
            int[] statusExpected = new int[expected.length];
            for (int i = 0; i < expected.length; i++) {
                // "server status" returns 0 for running and 1 for stopped, so
                // for "server start", we need to invert these two exit codes.
                if (expected[i] == ReturnCode.OK.getValue()) {
                    statusExpected[i] = ReturnCode.REDUNDANT_ACTION_STATUS.getValue();
                } else if (expected[i] == ReturnCode.REDUNDANT_ACTION_STATUS.getValue()) {
                    statusExpected[i] = ReturnCode.OK.getValue();
                } else {
                    statusExpected[i] = expected[i];
                }
            }
            int statusVal = serverStatus(desc + " (--status)", launcherMode, serverName, statusExpected);
            if (statusVal != ReturnCode.REDUNDANT_ACTION_STATUS.getValue()) {
                return;
            }

            // Create the .sLock file if it doesn't exist to avoid racing with
            // the background process.
            File sLockFile = new File(serversDir, serverName + "/workarea/.sLock");
            boolean sLockFileCreated = sLockFile.createNewFile();
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Created " + sLockFile + " = " + sLockFileCreated);

            // Delete the .sCommand file if it exists to avoid racing with the
            // backgound process.
            File sCommandFile = new File(serversDir, serverName + "/workarea/.sCommand");
            boolean sCommandFileDeleted = sCommandFile.delete();
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Deleted " + sCommandFile + " = " + sCommandFileDeleted);

            // Second, start the server process in the background.
            command(launcherMode, "start", serverName);
            testLogStream.println(); // pad to make these easier to pick out
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Starting server " + serverName);
            testLogStream.println("+=== INVOKE (background) " + builder.command());

            File savedDirectory = builder.directory();
            builder.directory(serverOutputDir);
            try {
                final Process serverProcess = builder.start();
                final StreamCopier copier = new StreamCopier(serverProcess.getInputStream(), "@ ");
                copier.start();

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            int exitVal = serverProcess.waitFor();
                            testLogStream.println("@ ---+ RC=" + exitVal);
                            copier.doJoin();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }.start();
            } finally {
                builder.directory(savedDirectory);
            }

            // Finally, verify the start status.
            command(launcherMode, "status:start", serverName);
            Process p = builder.start();
            doEverything(p, builder.command().toString(), desc + " (--status:start)", ReturnCode.OK.getValue());

            checkServerCommandFile(desc, serverName, true);
        }
    }

    private int serverStatus(String desc, LauncherMode launcherMode, String serverName, int... expected) throws IOException, InterruptedException {
        command(launcherMode, "status", serverName);
        Process p = builder.start();
        int statusVal = doEverything(p, builder.command().toString(), desc, expected);
        if (statusVal == ReturnCode.OK.getValue()) {
            checkServerCommandFile(desc, serverName, true);
        }

        if (launcherMode == LauncherMode.SCRIPT) {
            // Test status:fast.  This should never return SERVER_UNKNOWN_STATUS.
            int[] fastExpected = Arrays.copyOf(expected, expected.length);
            int fastExpectedOut = 0;
            for (int expectedVal : expected) {
                if (expectedVal != ReturnCode.SERVER_UNKNOWN_STATUS.getValue()) {
                    fastExpected[fastExpectedOut++] = expectedVal;
                }
            }
            if (fastExpectedOut != fastExpected.length) {
                fastExpected = Arrays.copyOf(fastExpected, fastExpectedOut);
            }

            command(launcherMode, "status:fast", serverName);
            p = builder.start();
            doEverything(p, builder.command().toString(), desc + " (status:fast)", fastExpected);
        }

        return statusVal;
    }

    private void checkServerCommandFile(String desc, String serverName, boolean expectedExists) {
        File sCommandFile = new File(serversDir, serverName + File.separatorChar + "workarea" + File.separatorChar + ".sCommand");
        testLogStream.println("Checking " + sCommandFile);
        Assert.assertEquals(desc + " (.sCommand)", expectedExists, sCommandFile.exists());
    }

    private void stopServer(String desc, String serverName, int... expected) throws IOException, InterruptedException {
        command("stop", serverName);
        Process p = builder.start();
        int statusVal = doEverything(p, builder.command().toString(), desc, expected);
        if (statusVal == ReturnCode.OK.getValue()) {
            checkServerCommandFile(desc, serverName, false);
        }
    }

    private void javaDumpServer(String desc, String serverName, String include, int[] expected, Pattern[] dumpFilePatterns,
                                int[] numExpectedDumps) throws IOException, InterruptedException {
        String[] filenamesBeforeArray = serverOutputDir.list();
        Set<String> filenamesBefore = filenamesBeforeArray == null ? Collections.<String> emptySet() : new HashSet<String>(Arrays.asList(filenamesBeforeArray));

        // javadump on HotSpot-derived JVMs relies on Java attach, and Java
        // attach is unreliable on Mac.
        if (isMac) {
            int[] newExpected = Arrays.copyOf(expected, expected.length + 1);
            newExpected[expected.length] = ReturnCode.ERROR_SERVER_DUMP.getValue();
            expected = newExpected;
        }

        boolean success = false;
        try {
            command("javadump", serverName, include == null ? new String[0] : new String[] { "--include=" + include });
            Process p = builder.start();
            int exitVal = doEverything(p, builder.command().toString(),
                                       desc,
                                       expected);
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Command returned with exitVal=" + exitVal);
            success = exitVal != ReturnCode.ERROR_SERVER_DUMP.getValue();
        } finally {
            int[] numDumps = new int[dumpFilePatterns.length];
            List<String> unmatched = new ArrayList<String>();

            File[] files = serverOutputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (!filenamesBefore.contains(fileName)) {
                        boolean matched = false;
                        for (int i = 0; i < dumpFilePatterns.length; i++) {
                            if (dumpFilePatterns[i].matcher(fileName).matches()) {
                                numDumps[i]++;
                                matched = true;

                                // Avoid leaving huge dump files in the output
                                // dir that will get included in "server dump"
                                // and that cause bvt.xml to fail the build.
                                if (file.delete()) {
                                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Deleted " + file.getAbsolutePath());
                                } else {
                                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Failed to delete " + file.getAbsolutePath());
                                }
                            }
                        }

                        if (!matched) {
                            unmatched.add(fileName);
                        }
                    }
                }
            }

            if (success) {
                for (int i = 0; i < numExpectedDumps.length; i++) {
                    testLogStream.println(DataFormatHelper.formatCurrentTime()
                                          + "Given file pattern: " + dumpFilePatterns[i].toString()
                                          + " ASSERT that we have " + numExpectedDumps[i]
                                          + " files which match. We had " + numDumps[i]);

                    Assert.assertEquals(desc + " (expected javadump file " + dumpFilePatterns[i].toString() + ")",
                                        numExpectedDumps[i], numDumps[i]);
                }
                testLogStream.println(DataFormatHelper.formatCurrentTime() + "ASSERT that we have no unmatched files. We had " + unmatched);

                Assert.assertTrue(desc + " (unexpected javadump files " + unmatched + ")", unmatched.isEmpty());
            }
        }
    }

    private void doTestExistingServer(String m) throws Throwable {
        Process p = null;
        String serverDir = serversDir + File.separatorChar + BOOT_SERVER;
        serverOutputDir = new File(serverDir);

        // 0: start the server (but BVT might have already started it), expected="0" or "1"
        startServer("0: Starting the server (if necessary) should return '0' or '1'",
                    BOOT_SERVER,
                    ReturnCode.OK.getValue(),
                    ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        // 1: check status of started server, expected="0"
        serverStatus("1: Started server status should return '0'",
                     launcherMode,
                     BOOT_SERVER,
                     ReturnCode.OK.getValue());

        // 2: start the started server, expected="1"
        startServer("2: Trying to start a running server should return '1'",
                    BOOT_SERVER,
                    ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        if (launcherMode != LauncherMode.SCRIPT) {
            // 2b: start the started server using the script, expected="1"
            startServer("2b: Trying to start a running server (with script) should return '1'",
                        LauncherMode.SCRIPT,
                        BOOT_SERVER,
                        ReturnCode.REDUNDANT_ACTION_STATUS.getValue());
        }

        if (launcherMode == LauncherMode.SCRIPT || launcherMode == LauncherMode.TOOL_JAR) {
            // 3: package a running server, expected="3"
            command("package", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "3: Trying to package a running server should return '3'",
                         ReturnCode.SERVER_ACTIVE_STATUS.getValue());

            // 4: dump a running server, expected="0"
            command("dump", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "4: Trying to dump a running server should return '0'",
                         ReturnCode.OK.getValue());

            // Dump java threads from a running server, expected="0"
            javaDumpServer("Trying to dump java threads for a running server should return '0'",
                           BOOT_SERVER,
                           null,
                           new int[] { ReturnCode.OK.getValue() },
                           new Pattern[] { DUMP_THREAD_PATTERN },
                           new int[] { 1 });

            // Dumping java heap from a running server, expected="0"
            javaDumpServer("Trying to dump java heap for a running server should return '0'",
                           BOOT_SERVER,
                           "heap",
                           new int[] { ReturnCode.OK.getValue() },
                           new Pattern[] { DUMP_THREAD_PATTERN, DUMP_HEAP_PATTERN },
                           new int[] { 1, 1 });

            // Dumping java system dump from a running server, expected="0"
            // System dumps are only supported on J9.
            javaDumpServer("Trying to dump java system dump for a running server should return '0'",
                           BOOT_SERVER,
                           "system",
                           new int[] { isJ9 ? ReturnCode.OK.getValue() : ReturnCode.REDUNDANT_ACTION_STATUS.getValue() },
                           new Pattern[] { DUMP_THREAD_PATTERN, DUMP_SYSTEM_PATTERN },
                           new int[] { 1, isJ9 ? 1 : 0 });
        }

        /********************************************************
         * Then, following steps all operates on a server with a missing .sLock.
         ********************************************************/

        if (!isWindows) {
            // 5: Move key file from the workarea
            //    *** CHANGED IN 8.5.5: JVM Attach fallback was removed.
            //        If the .sLock file or workarea is missing, we now assume the server has stopped.
            File sLockFile = new File(serverDir, "workarea/.sLock");
            File sLockRenamedFile = new File(serverDir, ".sLock.bvt");
            File sCommand = new File(serverDir, "workarea/.sCommand");
            File sCommandRenamedFile = new File(serverDir, ".sCommand.bvt");
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Renaming " + sLockFile + " to " + sLockRenamedFile);
            boolean sLockRenamed = sLockFile.renameTo(sLockRenamedFile);
            Assert.assertEquals("5a: Renaming .sLock file", !isWindows, sLockRenamed);
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Renaming " + sCommand + " to " + sCommandRenamedFile);
            boolean sCommandRenamed = sCommand.renameTo(sCommandRenamedFile);
            Assert.assertTrue("5b: Renaming .sCommand file", sCommandRenamed);

            boolean success = false;
            try {
                // 6: Check status of running server with missing files, expected="0"
                // Status is unreliable with missing workarea due to Java attach.
                serverStatus("6: Checking status for a running server (missing .sLock/.sCommand) should return '1'",
                             launcherMode,
                             BOOT_SERVER,
                             ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

                // 7: Start already running server with missing files: THIS WILL START ANOTHER SERVER!
                startServer("7: Trying to start a running server (re-create .sLock/.sCommand) should return '0'",
                            BOOT_SERVER,
                            ReturnCode.OK.getValue());

                // 8: stop the server, expected="0".
                stopServer("8: Stopping a running server (replacement workarea) should return '0'",
                           BOOT_SERVER,
                           ReturnCode.OK.getValue());

                success = true;
            } finally {
                try {
                    // 9a-c: Restore the workarea to test a properly stopped server.
                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Deleting replacement .sLock file " + sLockFile);
                    boolean sLockDeleted = sLockFile.delete();
                    Assert.assertEquals("9a: Deleted .sLock file", !isWindows, sLockDeleted);

                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Renaming " + sLockRenamedFile + " to " + sLockFile);
                    sLockRenamed = sLockRenamedFile.renameTo(sLockFile);
                    Assert.assertTrue("9b: Restoring .sLock file", sLockRenamed);
                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Renaming " + sCommandRenamedFile + " to " + sCommand);
                    sCommandRenamed = sCommandRenamedFile.renameTo(sCommand);
                    Assert.assertEquals("9c: Restoring .sCommand file", !isWindows, sCommandRenamed);
                } catch (Throwable t) {
                    if (success) {
                        throw t;
                    }
                    t.printStackTrace();
                }
            }
        }

        /********************************************************
         * Then, following steps all operates the stopped server.
         ********************************************************/
        // 10: stop the server, expected="0"
        stopServer("10: Stopping a server should return '0' (or maybe '1' on non-Windows)",
                   BOOT_SERVER,
                   ReturnCode.OK.getValue());

        // 11: stop the stopped server, expected="1"
        command("stop", BOOT_SERVER);
        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "11: Stopping a stopped server should return '1'",
                     ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        // 12: check the status of the stopped server, expected="1"
        serverStatus("12: Status check for a stopped server should return '1'",
                     launcherMode,
                     BOOT_SERVER,
                     ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        // 12b: check the fast status of the stopped server with an
        // existing .sCommand (as if the server process died), expected="1"
        if (launcherMode == LauncherMode.SCRIPT) {
            File sCommandFile = new File(serverDir, "workarea/.sCommand");
            Assert.assertTrue("Create " + sCommandFile.getAbsolutePath(), sCommandFile.createNewFile());
            try {
                command(launcherMode, "status:fast", BOOT_SERVER);
                p = builder.start();
                doEverything(p, builder.command().toString(),
                             "12b: Status check for a killed server (with .sCommand) should return '1'",
                             ReturnCode.REDUNDANT_ACTION_STATUS.getValue());
            } finally {
                // The script should do this anyway, but...
                if (!sCommandFile.delete()) {
                    testLogStream.println(DataFormatHelper.formatCurrentTime() + "Did not delete " + sCommandFile.getAbsolutePath());
                }
            }
        }

        if (launcherMode == LauncherMode.SCRIPT || launcherMode == LauncherMode.TOOL_JAR) {
            // 13: Package the stopped server, expected="0"
            command("package", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "13.1: Packaging a server, should return '0'",
                         ReturnCode.OK.getValue());
            // Package with --include option
            command("package", BOOT_SERVER, new String[] { "--include=all" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "13.2: Packaging a server with --include=all, should return '0'",
                         ReturnCode.OK.getValue());
            command("package", BOOT_SERVER, new String[] { "--include=usr" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "13.3: Packaging a server with --include=usr, should return '0'",
                         ReturnCode.OK.getValue());

            // 14: Dump the stopped server, expected="0"
            command("dump", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "14: Dumping a server, should return '0'",
                         ReturnCode.OK.getValue());

            // Dump java threads for a stopped server, expected="4"
            javaDumpServer("Dumping java threads for a stopped server, should return '4'",
                           BOOT_SERVER,
                           null,
                           new int[] { ReturnCode.SERVER_INACTIVE_STATUS.getValue() },
                           new Pattern[0],
                           new int[0]);
        }

    }

    /*
     * Following tests deal with the dump command executed during shutdown.
     *
     * a) Start the server
     * b) Block the shutdown and prepare for test
     * c) Kick off a shutdown on its own thread
     * d) Dump the server
     * e) Unblock the shutdown
     */
    @Test
    public void testDumpDuringShutdown() throws Throwable {
        Process p = null;
        String serverDir = serversDir + File.separatorChar + BOOT_SERVER;
        serverOutputDir = new File(serverDir);

        File preventDeactivate = new File(serverOutputDir, "TestBundleDeactivatePrevented.txt");
        Thread stopThread = null;
        final Throwable[] stopThreadError = new Throwable[1];

        try {
            // Clean up if the test failed previously.
            File deactivating = new File(serverOutputDir, "TestBundleDeactivate.txt");
            Assert.assertTrue("Should be able to delete " + deactivating,
                              !deactivating.exists() || deactivating.delete());

            // Start the server
            startServer("Starting the server (if necessary) should return '0' or '1'",
                        BOOT_SERVER,
                        ReturnCode.OK.getValue(),
                        ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

            // Prevent the server from stopping.
            Assert.assertTrue("Should be able to create " + preventDeactivate,
                              preventDeactivate.exists() || preventDeactivate.createNewFile());

            // Stop the server in a background thread.
            stopThread = new Thread() {
                @Override
                public void run() {
                    try {
                        stopServer("Stopping a running server should return '0'",
                                   BOOT_SERVER,
                                   ReturnCode.OK.getValue());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        stopThreadError[0] = t;
                    }
                }
            };
            stopThread.start();

            // Wait for server to begin stopping.
            waitForFileCreate(deactivating);

            // Dumping the server during shutdown should work.
            command("dump", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Dumping a shutting down server should return '0'",
                         ReturnCode.OK.getValue());

            // Allow the server to finish stopping.
            Assert.assertTrue("Should be able to delete " + preventDeactivate,
                              preventDeactivate.delete());

            // Wait for the server to finish stopping.
            Thread stopThreadTmp = stopThread;
            stopThread = null;
            stopThreadTmp.join();
            if (stopThreadError[0] != null) {
                throw stopThreadError[0];
            }
        } finally {
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Deleting " + preventDeactivate + " = " + preventDeactivate.delete());

            if (stopThread != null) {
                try {
                    stopThread.join();
                    if (stopThreadError[0] != null) {
                        stopThreadError[0].printStackTrace();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testScriptExistingServer() throws Throwable {
        doTestExistingServer(testName.getMethodName());
    }

    @Test
    public void testToolJarExistingServer() throws Throwable {
        launcherMode = LauncherMode.TOOL_JAR;
        doTestExistingServer(testName.getMethodName());
    }

    @Test
    public void testLibJarExistingServer() throws Throwable {
        launcherMode = LauncherMode.LIB_JAR;
        doTestExistingServer(testName.getMethodName());
    }

    @Test
    public void testUserDirComma() throws Throwable {
        File usrDirFile = new File(usrDir);
        builder.environment().put(WLP_USER_DIR, new File(usrDirFile.getParentFile(), usrDirFile.getName() + ",comma").getAbsolutePath());
        startServer("Starting the default server should return '0'",
                    null,
                    ReturnCode.OK.getValue(),
                    ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        stopServer("Stopping the default server should return '0'",
                   null,
                   ReturnCode.OK.getValue());
    }

    @Test
    public void testJVMAbendAutoClean() throws Throwable {
        launcherMode = LauncherMode.TOOL_JAR;
        doJVMAbendAutoClean(testName.getMethodName());
    }

    /**
     * Test the creation of a server using the template option
     * and a relative root directory of the template project
     * Expect success
     */
    @Test
    public void testCreateServerWithTemplate1() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "templateTestRelative:templateTest", false, true);
    }

    /**
     * Test the creation of a server using the template option
     * and an absolute path to the root directory of the template project
     * Expect success
     */
    @Test
    public void testCreateServerWithTemplate2() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "templateTestAbsolute:templateTest", false, true);

    }

    /**
     * Test the creation of a server using the template option
     * and a nonexistent properties file name
     * Expect failure
     */
    @Test
    public void testCreateServerWithTemplate3() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "unknownExtension:templateTest", true, true);
    }

    /**
     * Test the creation of a server using the template option
     * and a nonexistent template directory
     * Expect failure
     */
    @Test
    public void testCreateServerWithTemplate4() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "templateTestRelative:unknownTemplate", true, true);
    }

    /**
     * Test the creation of a server using the template option
     * and a local template directory under wlp
     * Expect success
     */
    @Test
    public void testCreateServerWithTemplate5() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "templateTest", false, false);
    }

    /**
     * Test the creation of a server using the template option
     * and a local template directory under wlp
     * Expect success
     */
    @Test
    public void testCreateServerWithTemplate6() throws Throwable {
        doTestCreateServerWithTemplate(testName.getMethodName(), "unknownTemplate", true, false);
    }

    /**
     * This test creates a server from a template option,
     * verifies it is a valid server, and verifies that it
     * contains the correct template sub-directory (or not if
     * failure is expected).
     */
    private void doTestCreateServerWithTemplate(String m, String templateValue, boolean expectFailure, boolean extensionDir) throws Throwable {
        Process p = null;
        String serverDir = serversDir + File.separatorChar + m;
        serverOutputDir = new File(serverDir);

        // Guard against un-cleaned-up BVT run
        recursiveDelete(serverOutputDir, false);

        Assert.assertFalse("Server directory should not exist to start with", serverOutputDir.exists());

        // 1: create the "template" server
        command("create", m, "--template=" + templateValue);
        p = builder.start();
        if (expectFailure) {
            doEverything(p, builder.command().toString(),
                         "1: Expected error creating server from template, should return '24'",
                         ReturnCode.LAUNCH_EXCEPTION.getValue());

            Assert.assertFalse("Server directory should not exist after failed create", serverOutputDir.exists());

            // 2: check the status immediately after create, expected="1"
            serverStatus("2: Status of server that doesn't exist should return '2'",
                         launcherMode,
                         m,
                         ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

            Assert.assertFalse("Server directory should not exist after status check", serverOutputDir.exists());
        } else {
            doEverything(p, builder.command().toString(),
                         "1: Create new server from template should return '0'",
                         ReturnCode.OK.getValue());

            Assert.assertTrue("Server directory should exist after create", serverOutputDir.exists());

            // 2: check the status immediately after create, expected="1"
            serverStatus("2: Status of existing server should return '1'",
                         launcherMode,
                         m,
                         ReturnCode.REDUNDANT_ACTION_STATUS.getValue());
        }

        // 3: check server contents
        boolean successfulCopy = new File(serverDir, "directoryToFind").exists();
        boolean foundExtensionDir = successfulCopy && new File(serverDir, "extensionDir").exists();

        if (expectFailure) {
            Assert.assertFalse("3: Template content should not exist in server directory", successfulCopy);
        } else {
            Assert.assertTrue("3: Template content should exist in server directory", successfulCopy);
        }

        if (!expectFailure && extensionDir) {
            // If the server should have been created, and it was using an extension template, we should
            // have found the extension directory as well.
            Assert.assertTrue("4: Server directory should contain extensionDir", foundExtensionDir);
        } else {
            Assert.assertFalse("4: Server directory should not contain extensionDir", foundExtensionDir);
        }
    }

    private void doJVMAbendAutoClean(String m) throws Throwable {
        // To simulate a JVM Abend, create a sRunning file so that server thinks
        // the JVM abended the last time it ran.  Also, create test file in workarea to
        // test that clean of the workspace occurs (it should be deleted after server starts up).
        // This also tests to make sure that if the file existed when the server started, it
        // then correctly deleted it even though it wasn't the original creator of the file.

        // 1: Make sure server is stopped
        stopServer("1: Make sure server is stopped, should return '0', '5' or '1'",
                   BOOT_SERVER,
                   ReturnCode.OK.getValue(),
                   ReturnCode.SERVER_UNKNOWN_STATUS.getValue(),
                   ReturnCode.REDUNDANT_ACTION_STATUS.getValue()); // already stopped

        // Create sRunning and test file

        File sRunning = new File(serversDir, BOOT_SERVER +
                                             File.separatorChar +
                                             "workarea" +
                                             File.separatorChar +
                                             BootstrapConstants.SERVER_RUNNING_FILE);
        File testFile = new File(serversDir, BOOT_SERVER +
                                             File.separatorChar +
                                             "workarea" +
                                             File.separatorChar +
                                             "test.file");
        File workareaDir = new File(serversDir +
                                    File.separatorChar +
                                    BOOT_SERVER +
                                    File.separatorChar +
                                    "workarea");

        workareaDir.mkdirs(); // The success of this is validated by the next two file creates...
        Assert.assertTrue("Create: " + sRunning, sRunning.createNewFile());
        Assert.assertTrue("Create: " + testFile, testFile.createNewFile());

        // 2: Start the server
        startServer("2: Starting the server, should return '0' or '1'",
                    BOOT_SERVER,
                    ReturnCode.OK.getValue(),
                    ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        // 3: stop the server, expected="0" or "5".
        stopServer("3: Stopping a running server, should return '0' or '5'",
                   BOOT_SERVER,
                   ReturnCode.OK.getValue(),
                   ReturnCode.SERVER_UNKNOWN_STATUS.getValue());

        this.waitForFileDelete(testFile);
        this.waitForFileDelete(sRunning);
    }

    private void doTestMissingServer(String m) throws Throwable {
        Process p = null;
        String serverDir = serversDir + File.separatorChar + NOTEXIST_SERVER;
        serverOutputDir = new File(serverDir);

        // Guard against un-cleaned-up BVT run
        recursiveDelete(serverOutputDir, false);
        Assert.assertFalse("Server directory should not exist to start with", serverOutputDir.exists());

        // 1: status of the notexist server, expected="2"
        serverStatus("1: Status of non-existent server should return '2'",
                     launcherMode,
                     NOTEXIST_SERVER,
                     ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

        // 2: start the notexist server, expected="2"
        startServer("2: Start of non-existent server should return '2'",
                    NOTEXIST_SERVER,
                    ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

        // 3: stop the notexist server, expected="2"
        command("stop", NOTEXIST_SERVER);
        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "3: Stop of non-existent server should return '2'",
                     ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

        // 4: Try to package a non-existent server, expected="2"
        command("package", NOTEXIST_SERVER);
        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "4: Trying to package a non-existent server should return '2'",
                     ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

        // 5: Try to dump the non-existent server, expected="2"
        command("dump", NOTEXIST_SERVER);
        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "5: Dump a non-existent server should return '2'",
                     ReturnCode.SERVER_NOT_EXIST_STATUS.getValue());

        // Try to dump java threads the non-existent server, expected="2"
        javaDumpServer("Dump java threads for a non-existent server, should return '2'",
                       NOTEXIST_SERVER,
                       null,
                       new int[] { ReturnCode.SERVER_NOT_EXIST_STATUS.getValue() },
                       new Pattern[0],
                       new int[0]);

        try {
            // 6: create the "notexist" server, expected="0"
            command("create", NOTEXIST_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "6: Create non-existent server should return '0'",
                         ReturnCode.OK.getValue());

            // 7: check the status immediately after create, expected="1"
            serverStatus("7: Status of existing server should return '1'",
                         launcherMode,
                         NOTEXIST_SERVER,
                         ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

            // 8: create the now existing server, expected="1"
            command("create", NOTEXIST_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "8: Create existing server should return '1'",
                         ReturnCode.REDUNDANT_ACTION_STATUS.getValue());
        } finally {
            recursiveDelete(new File(serverDir));
        }
    }

    private void recursiveDelete(File file) {
        recursiveDelete(file, true);
    }

    private void recursiveDelete(File file, boolean assertDeleted) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    recursiveDelete(child);
                }
            }
            testLogStream.println(DataFormatHelper.formatCurrentTime() + "Deleting " + file);
            Assert.assertTrue("Delete " + file, file.delete());
        } else if (assertDeleted) {
            Assert.fail("Expected file did not exist to delete: " + file);
        }
    }

    @Test
    public void testScriptMissingServer() throws Throwable {
        doTestMissingServer(testName.getMethodName());
    }

    @Test
    public void testToolJarMissingServer() throws Throwable {
        launcherMode = LauncherMode.TOOL_JAR;
        doTestMissingServer(testName.getMethodName());
    }

    @Test
    public void testList() throws IOException {
        command("list", "");
        builder.environment().put(WLP_USER_DIR, new File("bvt/test data/noservers").getAbsolutePath());
        Process p = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;

        boolean found = false;
        StringBuilder buffer = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            found |= line.startsWith("There are no servers defined in the user directory");
            buffer.append(line);
            buffer.append("\r\n");
        }

        assertTrue("List should return message indicating no servers exist: " + buffer, found);

        builder.environment().put(WLP_USER_DIR, new File("bvt/test data/servers").getAbsolutePath());
        p = builder.start();
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        buffer = new StringBuilder();

        found = false;

        while (!!!found && (line = reader.readLine()) != null) {
            found |= line.startsWith("The following servers are defined relative to the user directory");
            buffer.append(line);
            buffer.append("\r\n");
        }
        assertTrue("List should return message indicating servers exist: " + buffer, found);
        assertEquals("There should be an empty line here", "", reader.readLine());
        assertEquals("server1", reader.readLine());
        assertEquals("server2", reader.readLine());
    }

    @Test
    public void testSchemaGenCommand() throws Throwable {
        Process p = null;
        serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
        // start the server
        startServer("Starting the server (if necessary) should return '0' or '1'",
                    BOOT_SERVER,
                    ReturnCode.OK.getValue(),
                    ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

        command("schemagen", BOOT_SERVER);

        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "Trying to generate schema for a running server should return '0'",
                     ReturnCode.OK.getValue());

        List<File> schemaFiles = getMatchingFiles(serverOutputDir, "server.xsd");
        Assert.assertTrue("Expecting schema file", schemaFiles.size() > 0);

        File schemaFile = schemaFiles.get(schemaFiles.size() - 1);

        Assert.assertTrue(schemaFile.exists());
        Assert.assertTrue(schemaFile.length() > 0);

        command("stop", BOOT_SERVER);
        p = builder.start();
        doEverything(p, builder.command().toString(),
                     "Stopping a server should return '0'",
                     ReturnCode.OK.getValue());

    }

    @Test
    public void testDump() throws Throwable {
        Process p = null;
        // following two are for lock test
        FileOutputStream fos = null;
        FileChannel fc = null;
        try {
            serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);

            // start the server
            startServer("Starting the server (if necessary) should return '0' or '1'",
                        BOOT_SERVER,
                        ReturnCode.OK.getValue(),
                        ReturnCode.REDUNDANT_ACTION_STATUS.getValue());

            // Create security-sensitive files.
            File serverDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
            File resourcesDir = new File(serverDir, "resources");
            File securityResourcesDir = new File(resourcesDir, "security");
            List<File> sensitiveFiles = new ArrayList<File>();
            // Everything in resources/security is sensitive.
            sensitiveFiles.add(new File(securityResourcesDir, "test-dump-security.txt"));
            // Files named *.jks are sensitive.
            sensitiveFiles.add(new File(serverDir, "test-dump-security.jks"));
            // Files named *.p12 are sensitive.
            sensitiveFiles.add(new File(serverDir, "test-dump-security.p12"));

            for (File sensitiveFile : sensitiveFiles) {
                File dir = sensitiveFile.getParentFile();
                Assert.assertTrue(dir.toString(), dir.mkdirs() || dir.isDirectory());
                Assert.assertTrue(sensitiveFile.toString(), sensitiveFile.createNewFile());
            }

            // Create locking files, test the dump could still finish successfully
            File testLock = new File(serverOutputDir, ".testlock");
            fos = new FileOutputStream(testLock);
            fc = fos.getChannel();

            FileLock serverLock = null;
            for (int i = 0; i < 6; i++) { // try for a short period of time to obtain the lock
                serverLock = fc.tryLock();
                if (serverLock != null) {
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
            Assert.assertTrue("Can not create a test lock file.", serverLock != null);

            // run dump cmd
            if (isMac) {
                command("dump", BOOT_SERVER, "--include=heap");
            } else if (isJ9) {
                command("dump", BOOT_SERVER, "--include=heap,thread,system");
            } else {
                command("dump", BOOT_SERVER, "--include=heap,thread");
            }

            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Trying to dump a running server should return '0'",
                         ReturnCode.OK.getValue());

            // get the dump zip
            List<File> dumpZipFiles = getMatchingFiles(serverDir, BOOT_DUMP_REGEX);
            Assert.assertTrue("Expecting at least one dump file", dumpZipFiles.size() > 0);

            File dumpFile = dumpZipFiles.get(dumpZipFiles.size() - 1);

            // Ensure security-sensitive files were not included in the dump and that java dumps were.
            boolean threadDumpExpected = false;
            boolean heapDumpExpected = false;
            boolean systemDumpExpected = false;

            ZipFile zipFile = new ZipFile(dumpFile);
            try {
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();

                    if (DUMP_THREAD_PATTERN.matcher(entryName).matches()) {
                        threadDumpExpected = true;
                    } else if (DUMP_HEAP_PATTERN.matcher(entryName).matches()) {
                        heapDumpExpected = true;
                    } else if (DUMP_SYSTEM_PATTERN.matcher(entryName).matches()) {
                        systemDumpExpected = true;
                    }

                    for (File sensitiveFile : sensitiveFiles) {
                        String sensitiveName = sensitiveFile.getName();
                        Assert.assertFalse(entryName + " should not match " + sensitiveName, entryName.contains(sensitiveName));
                    }
                }
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }

            if (!!!isMac) {
                Assert.assertTrue("Could not find expected javacore dump file", threadDumpExpected);
                if (isJ9) {
                    Assert.assertTrue("Could not find expected java system core dump file", systemDumpExpected);
                }
            }

            Assert.assertTrue("Could not find expected java heap dump file", heapDumpExpected);

            // stop server
            command("stop", BOOT_SERVER);
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Stopping a server should return '0'",
                         ReturnCode.OK.getValue());
        } finally {
            if (!Utils.tryToClose(fc)) // try to close the wrapping-channel first.
                Utils.tryToClose(fos);
        }
    }

    @Test
    public void testPackageAllWithUsrFeature() throws Throwable {
        Process p = null;
        File usrBvtFeatureFile = null;
        boolean usrBvtFeatureFileCreated = false;
        try {
            File serverDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
            serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);

            stopServer("Make sure server is stopped",
                       BOOT_SERVER,
                       ReturnCode.OK.getValue(),
                       ReturnCode.REDUNDANT_ACTION_STATUS.getValue()); // already stopped

            usrBvtFeatureFile = new File(usrDir + File.separatorChar + "extension" + File.separatorChar + "lib" +
                                         File.separatorChar + "features" + File.separatorChar + ALL_USR_FEATURE_MF);

            File usrFeaturesDir = new File(usrDir + File.separatorChar + "extension" + File.separatorChar + "lib" +
                                           File.separatorChar + "features");

            usrFeaturesDir.mkdirs(); // The success of this is validated by the next file create
            usrBvtFeatureFileCreated = usrBvtFeatureFile.createNewFile();
            testLogStream.println("Created " + usrBvtFeatureFile + " = " + usrBvtFeatureFileCreated);
            Assert.assertTrue("Create: " + usrBvtFeatureFile, usrBvtFeatureFileCreated);

            // Package with --include option
            command("package", BOOT_SERVER, new String[] { "--include=all" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Packaging a server with --include=all, should return '0'",
                         ReturnCode.OK.getValue());

            // get the server zip
            List<File> packageZipFiles = getMatchingFiles(serverDir, PACKAGE_REGEX);
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            // Ensure usr feature is in the zip.
            ZipFile zipFile = new ZipFile(packageFile);
            try {
                boolean usrMf = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    if (entryName.contains(ALL_USR_FEATURE_MF) == true) {
                        usrMf = true;
                    }
                }
                Assert.assertTrue("Expecting " + ALL_USR_FEATURE_MF + " in package file", usrMf);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } finally {
            if (usrBvtFeatureFile != null && usrBvtFeatureFileCreated == true) {
                boolean usrBvtFeatureFileDeleted = usrBvtFeatureFile.delete();
                testLogStream.println("Deleted " + usrBvtFeatureFile + " = " + usrBvtFeatureFileDeleted);
            }
        }
    }

    @Test
    public void testPackageUsrWithUsrFeature() throws Throwable {
        Process p = null;
        File usrBvtFeatureFile = null;
        boolean usrBvtFeatureFileCreated = false;
        try {
            File serverDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
            serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);

            stopServer("Make sure server is stopped",
                       BOOT_SERVER,
                       ReturnCode.OK.getValue(),
                       ReturnCode.REDUNDANT_ACTION_STATUS.getValue()); // already stopped

            usrBvtFeatureFile = new File(usrDir + File.separatorChar + "extension" + File.separatorChar + "lib" +
                                         File.separatorChar + "features" + File.separatorChar + USR_USR_FEATURE_MF);

            File usrFeaturesDir = new File(usrDir + File.separatorChar + "extension" + File.separatorChar + "lib" +
                                           File.separatorChar + "features");

            usrFeaturesDir.mkdirs(); // The success of this is validated by the next file create
            usrBvtFeatureFileCreated = usrBvtFeatureFile.createNewFile();
            testLogStream.println("Created " + usrBvtFeatureFile + " = " + usrBvtFeatureFileCreated);
            Assert.assertTrue("Create: " + usrBvtFeatureFile, usrBvtFeatureFileCreated);

            // Package with --include option
            command("package", BOOT_SERVER, new String[] { "--include=usr" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Packaging a server with --include=usr, should return '0'",
                         ReturnCode.OK.getValue());

            // get the server zip
            List<File> packageZipFiles = getMatchingFiles(serverDir, PACKAGE_REGEX);
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            ZipFile zipFile = new ZipFile(packageFile);
            try {
                boolean usrMf = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();

                    // Ensure only wlp/usr/ files are in the zip.
                    String entryName = entry.getName();
                    Assert.assertTrue("Not expecting " + entryName, entryName.startsWith("wlp/usr/"));

                    // Ensure usr feature is in the zip.
                    if (entryName.contains(USR_USR_FEATURE_MF) == true) {
                        usrMf = true;
                    }
                }
                Assert.assertTrue("Expecting " + USR_USR_FEATURE_MF + " in package file", usrMf);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } finally {
            if (usrBvtFeatureFile != null && usrBvtFeatureFileCreated == true) {
                boolean usrBvtFeatureFileDeleted = usrBvtFeatureFile.delete();
                testLogStream.println("Deleted " + usrBvtFeatureFile + " = " + usrBvtFeatureFileDeleted);
            }
        }
    }

    @Test
    public void testPackageWithLooseApps() throws Throwable {
        //Process p = null;
        File usrBvtFeatureFile = null;
        boolean usrBvtFeatureFileCreated = false;
        try {
            File serverDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
            serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);

            stopServer("Make sure server is stopped",
                       BOOT_SERVER,
                       ReturnCode.OK.getValue(),
                       ReturnCode.REDUNDANT_ACTION_STATUS.getValue()); // already stopped

            // Package with --include option
            command("package", BOOT_SERVER, new String[] { "--include=usr" });
            //p = builder.start();
            List<String> cmdOutputLines = getExecuteLines(builder, ReturnCode.OK.getValue());
//            doEverything(p, builder.command().toString(),
//                         "Packaging a server with --include=usr, should return '0'",
//                         ReturnCode.OK.getValue());

            // check for warning message indicating that loose config file with invalid path exists:
            assertNotNull("Did not find expected warning about invalid loose config file",
                          findStringInListOutput(".*CWWKE0095W.*AppsLooseWeb-InvalidRelPaths.*", cmdOutputLines));

            // get the server zip
            List<File> packageZipFiles = getMatchingFiles(serverDir, PACKAGE_REGEX);
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            ZipFile zipFile = new ZipFile(packageFile);
            try {
                ZipEntry entry = zipFile.getEntry("wlp/usr/servers/com.ibm.ws.kernel.boot.bvt/apps/Apps Loose Web.war");
                assertNotNull("There should have been an Apps Loose Web.war file in the package", entry);
                entry = zipFile.getEntry("wlp/usr/servers/com.ibm.ws.kernel.boot.bvt/apps/AppsLooseWeb.war");
                assertNotNull("There should have been an AppsLooseWeb.war file in the package", entry);
                entry = zipFile.getEntry("wlp/usr/servers/com.ibm.ws.kernel.boot.bvt/apps/AppsLooseWeb-RelPaths.war");
                assertNotNull("There should have been an AppsLooseWeb-RelPaths.war file in the package", entry);
                entry = zipFile.getEntry("wlp/usr/servers/com.ibm.ws.kernel.boot.bvt/apps/AppsLooseWeb-InvalidRelPaths.war.xml");
                assertNotNull("There should have been an AppsLooseWeb-InvalidRelPaths.war.xml file in the package", entry);
                entry = zipFile.getEntry("wlp/usr/servers/com.ibm.ws.kernel.boot.bvt/apps/AppsLooseWeb-Signed.war");
                assertNotNull("There should have been an AppsLooseWeb-Signed.war file in the package", entry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } finally {
            if (usrBvtFeatureFile != null && usrBvtFeatureFileCreated == true) {
                boolean usrBvtFeatureFileDeleted = usrBvtFeatureFile.delete();
                testLogStream.println("Deleted " + usrBvtFeatureFile + " = " + usrBvtFeatureFileDeleted);
            }
        }
    }

    @Test
    public void testInvalidJVMOptions() throws Throwable {
        File serverConfigDir = new File(serversDir, ENV_SERVER);
        File serverJVMOptionsFile = new File(serverConfigDir, "jvm.options");

        try {
            // Specify various strings in jvm.options using the wrong format.
            // We use an otherwise invalid JVM option to prevent the JVM from
            // starting in case the scripts fail to detect the issue.
            List<String> badOptions = new ArrayList<String>();

            // Missing leading "-".
            badOptions.add("badoption");

            // EN-DASH and EM-DASH (e.g., copied from richtext)
            badOptions.add("\u2013badoption");
            badOptions.add("\u2014badoption");

            // Leading whitespace is ignored by server.bat.
            if (!isWindows || WLP_CYGWIN_HOME != null) {
                badOptions.add(" -badoption");
                badOptions.add("\t-badoption");
            }

            for (String badOption : badOptions) {
                testLogStream.println("Writing jvm.options with \"" + badOption + "\"");
                PrintWriter pw = new PrintWriter(serverJVMOptionsFile);
                pw.println(badOption);
                pw.close();

                command("run", ENV_SERVER);
                List<String> lines = getExecuteLines(builder, 32);
                Assert.assertTrue(lines.toString(), lines.get(0).contains("CWWKE0086E"));
            }
        } finally {
            serverJVMOptionsFile.delete();
        }
    }

    /**
     * Test server current working directory, server.env, and jvm.options handling.
     */
    @Test
    public void testEnvironmentAndJVMOptions() throws Throwable {

        File etcDir = new File(installDir, "etc");
        etcDir.mkdir();

        File installJVMOptionsFile = new File(etcDir, "jvm.options");
        File installServerEnvFile = new File(etcDir, "server.env");

        File serverConfigDir = new File(serversDir, ENV_SERVER);
        File serversOutputDir = new File(serverConfigDir, "output");
        serverLogDir = new File(serverConfigDir, "LOG_DIR");
        File serverServerEnvFile = new File(serverConfigDir, "server.env");
        File serverJVMOptionsFile = new File(serverConfigDir, "jvm.options");

        Process p = null;
        try {
            PrintWriter pw = new PrintWriter(installServerEnvFile);
            pw.println("WLP_KERNEL_BOOT_BVT_INSTALL=install");
            pw.println("WLP_KERNEL_BOOT_BVT_OVERRIDE=install");
            pw.close();

            pw = new PrintWriter(installJVMOptionsFile);
            pw.println("-Dcom.ibm.ws.kernel.boot.bvt.install=install");
            pw.println("-Dcom.ibm.ws.kernel.boot.bvt.override=install");
            pw.close();

            serverOutputDir = new File(serversOutputDir, ENV_SERVER);

            pw = new PrintWriter(serverServerEnvFile);
            pw.println("#comment");
            pw.println(); // empty line
            pw.println("WLP_OUTPUT_DIR=" + serversOutputDir.getAbsolutePath());
            pw.write("WLP_KERNEL_BOOT_BVT_CR=cr\r\n"); // not last line of file
            pw.println("WLP_KERNEL_BOOT_BVT_OVERRIDE=server");
            pw.println("WLP_KERNEL_BOOT_BVT_SERVER=server");
            pw.println("WLP_KERNEL_BOOT_BVT_CHARS=a b\\c");
            pw.close();

            pw = new PrintWriter(serverJVMOptionsFile);
            pw.println("#comment");
            pw.println(); // empty line
            pw.write("-Dcom.ibm.ws.kernel.boot.bvt.cr=cr\r\n"); // not last line of file
            pw.println("-Dcom.ibm.ws.kernel.boot.bvt.override=server");
            pw.println("-Dcom.ibm.ws.kernel.boot.bvt.server=server");
            pw.println("-Dcom.ibm.ws.kernel.boot.bvt.chars=a b\\c");
            pw.close();

            // Ensure JVM_ARGS is used for the server process.  Ensure multiple
            // arguments can be specified.  Ensure quotes can be used.
            StringBuilder jvmArgs = new StringBuilder();
            jvmArgs.append("-Dcom.ibm.ws.kernel.boot.bvt.jvmarg1=1");
            jvmArgs.append(' ');
            jvmArgs.append("-Dcom.ibm.ws.kernel.boot.bvt.jvmarg2=2");
            jvmArgs.append(' ');
            jvmArgs.append("-Dcom.ibm.ws.kernel.boot.bvt.jvmargquoted=\"a b\"");
            builder.environment().put("JVM_ARGS", jvmArgs.toString());

            for (String testPrefix : new String[] { "ServerAndInstall", "InstallOnly", "SpecifyLogDir" }) {
                testLogStream.println("Test prefix: " + testPrefix);
                testLogStream.println("Server output dir: " + serverOutputDir.getAbsolutePath());
                File started = new File(serverOutputDir, "started.txt");
                started.delete();

                if (testPrefix.equals("SpecifyLogDir")) {
                    builder.environment().put("LOG_DIR", serverLogDir.getAbsolutePath());
                }

                command("start", ENV_SERVER);
                String consoleName = "console." + (counter + 1) + ".log";
                builder.environment().put("LOG_FILE", consoleName);
                p = builder.start();
                testLogStream.println("Environment: " + builder.environment());
                doEverything(p, builder.command().toString(),
                             "Start of server should return '0'",
                             ReturnCode.OK.getValue());

                waitForFileCreate(started);

                runTestOnServer("testJVMArgs");
                runTestOnServer("test" + testPrefix + "ServerEnv");
                runTestOnServer("test" + testPrefix + "JVMOptions");

                command("stop", ENV_SERVER);
                p = builder.start();
                doEverything(p, builder.command().toString(),
                             "Stop of server should return '0'",
                             ReturnCode.OK.getValue());

                if (testPrefix.equals("SpecifyLogDir")) {
                    Assert.assertTrue("console.log file should exist in LOG_DIR", new File(serverLogDir, consoleName).exists());
                    Assert.assertTrue("messages.log file should exist in LOG_DIR", new File(serverLogDir, "messages.log").exists());
                    builder.environment().remove("LOG_DIR");
                } else {
                    Assert.assertTrue(consoleName + " file should exist in server output dir", new File(serverOutputDir, "logs/" + consoleName).exists());
                }

                serverServerEnvFile.delete();
                serverJVMOptionsFile.delete();
                serverOutputDir = serverConfigDir;
            }
        } catch (Throwable t) {
            dumpScriptLogs(serverLogDir);
            throw t;
        } finally {
            installJVMOptionsFile.delete();
            installServerEnvFile.delete();
        }
    }

    private void runTestOnServer(String test) {
        try {
            String port = System.getProperty("HTTP_default", "8000");
            URL url = new URL("http://localhost:" + port + "/com.ibm.ws.kernel.boot.bvt?test=" + test);
            testLogStream.println("Opening " + url);
            InputStream in = url.openConnection().getInputStream();
            try {
                while (in.read() != -1);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            testLogStream.println("Exception reading response: " + ex);
            throw new AssertionError(ex);
        }
    }

    @Test
    public void testIBMJavaOptions() throws Throwable {
        File jresDir = new File("bvt/test data/jres");
        File jreDir = new File(jresDir, "unknown");

        ProcessBuilder builder = new ProcessBuilder();
        builder.environment().put("JAVA_HOME", jreDir.getAbsolutePath());

        for (String action : new String[] { "version", "run" }) {
            boolean client = "version".equals(action);

            testLogStream.println("Testing action " + action);
            command(builder, LauncherMode.SCRIPT, action, null);

            for (int i = 0; i < 2; i++) {
                // Ensure -Xdummy remains if specified, or ensure the script
                // exports IBM_JAVA_OPTIONS if it wasn't before.
                boolean addDummyOption = i == 1;
                testLogStream.println(addDummyOption ? "Testing with -Xdummy" : "Testing without -Xdummy");
                if (addDummyOption) {
                    builder.environment().put("IBM_JAVA_OPTIONS", "-Xdummy");
                } else {
                    builder.environment().remove("IBM_JAVA_OPTIONS");
                }

                String ibmJavaOptions = getIBMJavaOptions(builder);
                Assert.assertThat(ibmJavaOptions, matchIf(addDummyOption, Matchers.containsString("-Xdummy")));
                Assert.assertThat(ibmJavaOptions, matchIf(client, Matchers.containsString("-Xnoaot")));
                Assert.assertThat(ibmJavaOptions, matchIf(client, Matchers.containsString("-Xquickstart")));
            }
        }
    }

    @Test
    public void testShareClassesCacheDirPerm() throws Throwable {
        if (isWindows && WLP_CYGWIN_HOME == null) {
            return;
        }

        File jresDir = new File("bvt/test data/jres");

        ProcessBuilder builder = new ProcessBuilder();
        command(builder, LauncherMode.SCRIPT, "version", null);
        builder.environment().put("IBM_JAVA_OPTIONS", "-Xdummy");
        String origPath = builder.environment().get("PATH");

        // Windows does not have (or need) cacheDirPerm, so the script
        // always sets the environment variable.  Test that behavior, and
        // then lie to the server script about our platform.
        if (WLP_CYGWIN_HOME != null) {
            String javaHome = new File(jresDir, "unknown").getAbsolutePath();
            testLogStream.println("Testing JAVA_HOME=" + javaHome + " (cygwin)");
            builder.environment().put("JAVA_HOME", javaHome);

            String output = getIBMJavaOptions(builder);
            Assert.assertTrue("unknown should contain -Xshareclasses", output.contains("-Xshareclasses"));
            Assert.assertFalse("unknown should not contain cacheDirPerm", output.contains("cacheDirPerm"));

            // Override the OSTYPE environment variable.
            builder.environment().put("OSTYPE", "unknown");

            // jres/bin has a uname that lies the server script.
            origPath = new File(jresDir, "bin").getAbsolutePath() + File.pathSeparatorChar + origPath;
            builder.environment().put("PATH", origPath);
            testLogStream.println("New PATH=" + origPath);
        }

        for (String jre : new String[] {
                                         "unknown",
                                         "ibm-1.5.0",
                                         "ibm-1.6.0",
                                         "ibm-1.6.0sr1",
                                         "ibm-1.6.0sr9",
                                         "ibm-1.6.0-2.6" }) {
            String javaHome = new File(jresDir, jre).getAbsolutePath();
            testLogStream.println("Testing JAVA_HOME=" + javaHome);
            builder.environment().put("JAVA_HOME", javaHome);

            String ibmJavaOptions = getIBMJavaOptions(builder);
            Assert.assertTrue(jre + " should contain -Xdummy", ibmJavaOptions.contains("-Xdummy"));
            Assert.assertFalse(jre + " should not contain -Xshareclasses", ibmJavaOptions.contains("-Xshareclasses"));
        }

        for (String jre : new String[] {
                                         "ibm-1.6.0sr10",
                                         "ibm-1.6.0sr10-zos",
                                         "ibm-1.6.0sr11",
                                         "ibm-1.6.0-2.6sr1",
                                         "ibm-1.6.0-2.6sr2",
                                         "ibm-1.7.0" }) {
            String javaHome = new File(jresDir, jre).getAbsolutePath();
            testLogStream.println("Testing JAVA_HOME=" + javaHome);
            builder.environment().put("JAVA_HOME", javaHome);

            String ibmJavaOptions = getIBMJavaOptions(builder);
            Assert.assertTrue(jre + " should contain -Xdummy", ibmJavaOptions.contains("-Xdummy"));
            Assert.assertTrue(jre + " should contain -Xshareclasses", ibmJavaOptions.contains("-Xshareclasses"));
            Assert.assertTrue(jre + " should contain cacheDirPerm", ibmJavaOptions.contains("cacheDirPerm"));
        }

        builder.environment().remove("JAVA_HOME");
        builder.environment().remove("JRE_HOME");

        // Ensure -Xshareclasses is not added for "java" in PATH.
        for (String binDir : new String[] {
                                            "unknown/bin",
                                            "ibm-1.5.0/bin",
                                            "ibm-1.5.0/jre/bin" }) {
            String path = new File(jresDir, binDir).getAbsolutePath() + File.pathSeparator + origPath;
            testLogStream.println("Testing PATH=" + path);
            builder.environment().put("PATH", path);

            String ibmJavaOptions = getIBMJavaOptions(builder);
            Assert.assertTrue(binDir + " should contain -Xdummy", ibmJavaOptions.contains("-Xdummy"));
            Assert.assertFalse(binDir + " should not contain -Xshareclasses", ibmJavaOptions.contains("-Xshareclasses"));
        }

        // Ensure -Xshareclasses is added for "java" in PATH.
        for (String binDir : new String[] {
                                            "ibm-1.6.0sr10/bin",
                                            "ibm-1.6.0sr10/jre/bin" }) {
            String path = new File(jresDir, binDir).getAbsolutePath() + File.pathSeparator + origPath;
            testLogStream.println("Testing PATH=" + path);
            builder.environment().put("PATH", path);

            String ibmJavaOptions = getIBMJavaOptions(builder);
            Assert.assertTrue(binDir + " should contain -Xdummy", ibmJavaOptions.contains("-Xdummy"));
            Assert.assertTrue(binDir + " should contain -Xshareclasses", ibmJavaOptions.contains("-Xshareclasses"));
        }
    }

    private static String getIBMJavaOptions(ProcessBuilder builder) throws IOException, InterruptedException {
        String output = execute(builder, 0).trim();
        testLogStream.println(output);
        String prefix = "IBM_JAVA_OPTIONS=";
        Assert.assertTrue("expected IBM_JAVA_OPTIONS: " + output, output.startsWith(prefix));
        return output.substring(prefix.length());
    }

    private static String execute(ProcessBuilder builder, Integer expected) throws IOException, InterruptedException {
        final Process p = builder.start();

        Thread stderrCopier = new Thread() {
            @Override
            public void run() {
                try {
                    InputStream err = p.getErrorStream();
                    byte[] buf = new byte[4096];
                    for (int read; (read = err.read(buf)) != -1;) {
                        testLogStream.write(buf, 0, read);
                    }
                } catch (IOException ex) {
                    throw new Error(ex);
                }
            }
        };
        stderrCopier.start();

        InputStreamReader reader = new InputStreamReader(p.getInputStream());
        char[] buf = new char[4096];
        StringBuilder output = new StringBuilder();
        for (int read; (read = reader.read(buf)) != -1;) {
            output.append(buf, 0, read);
        }

        // Create something that will interrupt this thread if something happens
        // in one of the two methods below (waitFor / doJoin)...
        WakeUp w = new WakeUp(Thread.currentThread());
        ScheduledFuture<?> future = schedExec.schedule(w, TIMEOUT, TimeUnit.MILLISECONDS);

        stderrCopier.join();

        p.waitFor();

        future.cancel(false);

        if (expected != null) {
            int exitValue = p.exitValue();
            if (exitValue != expected) {
                throw new IOException(builder.command() + " failed (" + exitValue + "): " + output);
            }
        }

        return output.toString().trim();
    }

    private static List<String> getExecuteLines(ProcessBuilder builder, Integer expected) throws IOException, InterruptedException {
        String output = execute(builder, expected);

        List<String> lines = new ArrayList<String>();
        boolean inEval = false;
        int carryover = 0;

        for (String line : output.split("[\r\n]+")) {
            // Filter sh -x trace output.
            if (inEval) {
                testLogStream.println("(trace eval) " + line);
                if (line.trim().equals("'")) {
                    inEval = false;
                }
            } else if (line.equals("+ eval '")) {
                inEval = true;
                testLogStream.println("(trace eval) " + line);
            } else if (carryover > 0) {
                carryover--;
                testLogStream.println("(trace) " + line);
            } else if (line.startsWith("+") || line.equals("'")) {
                int index = 0;
                index = line.indexOf("+", index + 1);
                while (index != -1) {
                    index = line.indexOf("+", index + 1);
                    carryover++;
                }
                testLogStream.println("(trace) " + line);
            } else if (isMac && lines.isEmpty() && line.matches("objc\\[\\d+\\]: Class JavaLaunchHelper is implemented in both .*")) { // 123011
                testLogStream.println("(mac workaround) " + line);
            } else {
                lines.add(line);
                testLogStream.println(line);
            }
        }

        testLogStream.println();

        return lines;
    }

    public static List<File> getMatchingFiles(File root, final String filterExpr) {
        if (root == null)
            return Collections.emptyList();

        File fileList[];
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file != null && file.isFile()) {
                    String name = file.getName();
                    if (name.matches(filterExpr))
                        return true;
                }
                return false;
            }
        };

        fileList = root.listFiles(filter);
        if (fileList == null)
            return Collections.emptyList();
        else {
            List<File> list = Arrays.asList(fileList);
            if (list.size() > 1) {
                Collections.sort(list);
            }
            return list;
        }
    }

    @Test
    public void testHelp() throws Throwable {
        List<String> usageOutput = getHelpOutput(LauncherMode.SCRIPT, null);
        Assert.assertTrue("Usage help should contain help in the text", findMatchingLine(usageOutput, "\\s*help"));

        List<String> actionsOutput = getHelpOutput(LauncherMode.SCRIPT, "invalidaction");
        Assert.assertTrue("There should be a message about invalid parameter", findMatchingLine(actionsOutput, "CWWKE0013E: .*"));

        final String[] options = new String[] { "--archive", "--clean", "--include" };

        List<String> helpOutput = getHelpOutput(LauncherMode.SCRIPT, "help");
        for (String cmd : new String[] { "create", "debug", "dump", "javadump", "package", "run", "start", "status", "stop", "version" }) {
            Assert.assertTrue("Full script help should contain " + cmd, findMatchingLine(helpOutput, "\\s*" + cmd + "\\s*"));
        }
        for (String option : options) {
            Assert.assertTrue("Full script help should contain " + option, findMatchingLine(helpOutput, "\\s*" + option + "(=.*|$)"));
        }

        List<String> javaHelpOutput = getHelpOutput(LauncherMode.TOOL_JAR, "help");
        for (String option : new String[] { "--create", "--dump", "--javadump", "--package", "--status", "--stop", "--version" }) {
            Assert.assertTrue("Full java --help should contain " + option, findMatchingLine(javaHelpOutput, "\\s*" + option + "\\s*"));
        }
        for (String option : new String[] { "--debug", "--run", "--start" }) {
            Assert.assertFalse("Full java --help should not contain " + option, findMatchingLine(javaHelpOutput, "\\s*" + option + "\\s*"));
        }
        for (String option : options) {
            Assert.assertTrue("Full java --help should contain " + option, findMatchingLine(helpOutput, "\\s*" + option + "(=.*|$)"));
        }
    }

    private List<String> getHelpOutput(LauncherMode launcherMode, String action) throws IOException, InterruptedException {
        command(launcherMode, action, null);
        System.out.println("----- Running server script with action " + action);
        List<String> lines = getExecuteLines(builder, 0);

        Assert.assertTrue("Help output should contain at least one line", lines.size() >= 1);
        String usageLine = null;
        int i = 0;
        for (String l : lines) {
            System.out.println(l);
            ++i;
            if (l.startsWith("Usage")) {
                usageLine = l;
                break;
            }
        }

        Assert.assertNotNull("Usage line should be present in help", usageLine);
        Assert.assertTrue("Usage line should be first or second (if there is an error): " + i, i <= 2);

        if (launcherMode == LauncherMode.SCRIPT) {
            Assert.assertTrue("Usage line should include server script name", usageLine.matches(".*[\\\\/]server(\\.bat)? .*"));
        } else {
            Assert.assertTrue("Usage line should include -javaagent", usageLine.contains("-javaagent"));
        }

        return lines;
    }

    private boolean findMatchingLine(List<String> lines, String regex) {
        Pattern pattern = Pattern.compile(regex);
        for (String line : lines) {
            if (pattern.matcher(line).find()) {
                testLogStream.println("Found line matching regex " + regex + ": " + line);
                return true;
            }
        }

        testLogStream.println("Did not find line matching " + regex);
        return false;
    }

    @Test
    public void testMissingJava() throws Throwable {
        command("stop", BOOT_SERVER);
        builder.environment().put("JAVA_HOME", new File("javanotexist").getAbsolutePath());
        List<String> output = getExecuteLines(builder, null);
        Assert.assertFalse("expected non-empty output", output.isEmpty());
    }

    @Test
    public void testPackageAllWithProductExtension() throws Throwable {
        Process p = null;
        File extPropertiesFile = null;
        File featureFile = null;
        File featureJar = null;
        File extDirFile = null;

        boolean propertiesFileCreated = false;
        boolean featureFileCreated = false;
        boolean featureJarCreated = false;
        try {
            File serverDir = new File(serversDir + File.separatorChar + BOOT_SERVER);
            serverOutputDir = new File(serversDir + File.separatorChar + BOOT_SERVER);

            stopServer("Make sure server is stopped",
                       BOOT_SERVER,
                       ReturnCode.OK.getValue(),
                       ReturnCode.REDUNDANT_ACTION_STATUS.getValue()); // already stopped

            String extPropertiesDir = installDir + File.separatorChar + "etc" + File.separatorChar + "extensions";

            File extDir = new File(extPropertiesDir);
            extDir.mkdirs();

            extPropertiesFile = new File(extPropertiesDir +
                                         File.separatorChar + PRODUCT_EXT_PROPERTIES);

            Properties props = new Properties();
            props.put("com.ibm.websphere.productId", "myProduct");
            props.put("com.ibm.websphere.productInstall", "wlpExtension" + File.separatorChar + "producttest");
            props.store(new FileOutputStream(extPropertiesFile), "Product Extension Test Properties");
            propertiesFileCreated = true;

            testLogStream.println("Created " + extPropertiesFile);

            String extensionDir = installParent + File.separatorChar + "wlpExtension" + File.separatorChar +
                                  "producttest" + File.separatorChar + "lib";

            extDirFile = new File(extensionDir);
            extDirFile.mkdirs();
            testLogStream.println("Created directory " + extensionDir);

            File featureDirFile = new File(extensionDir + File.separatorChar + "features");
            featureDirFile.mkdirs();
            testLogStream.println("Created directory " + featureDirFile);

            featureFile = new File(extensionDir + File.separatorChar + "features" + File.separatorChar + PRODUCT_EXT_FEATURE_MANIFEST);
            featureJar = new File(extensionDir + File.separatorChar + PRODUCT_EXTENSION_JAR);

            featureFileCreated = featureFile.createNewFile();
            testLogStream.println("Created file + " + featureFile + " = " + featureFileCreated);

            featureJarCreated = featureJar.createNewFile();
            testLogStream.println("Created file + " + featureJar + " = " + featureJarCreated);

            // Package with --include option
            command("package", BOOT_SERVER, new String[] { "--include=all" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Packaging a server with --include=usr, should return '0'",
                         ReturnCode.OK.getValue());

            // get the server zip
            List<File> packageZipFiles = getMatchingFiles(serverDir, PACKAGE_REGEX);
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            // Ensure usr feature is in the zip.
            ZipFile zipFile = new ZipFile(packageFile);
            try {
                boolean propsFilePresent = false;
                boolean featureFilePresent = false;
                boolean featureJarFilePresent = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    if (entryName.contains(PRODUCT_EXT_PROPERTIES) == true) {
                        propsFilePresent = true;
                    } else if (entryName.contains(PRODUCT_EXT_FEATURE_MANIFEST) == true) {
                        featureFilePresent = true;
                    } else if (entryName.contains(PRODUCT_EXTENSION_JAR) == true) {
                        featureJarFilePresent = true;
                    }
                }
                Assert.assertTrue("Expecting " + PRODUCT_EXT_PROPERTIES + " in package file", propsFilePresent);
                Assert.assertTrue("Expecting " + PRODUCT_EXT_FEATURE_MANIFEST + " in package file", featureFilePresent);
                Assert.assertTrue("Expecting " + PRODUCT_EXTENSION_JAR + " in package file", featureJarFilePresent);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } finally {
            if (extPropertiesFile != null && propertiesFileCreated) {
                boolean propertiesFileDeleted = extPropertiesFile.delete();
                testLogStream.println("Deleted " + extPropertiesFile + " = " + propertiesFileDeleted);
            }
            if (featureFileCreated && featureFile != null) {
                boolean featureFileDeleted = featureFile.delete();
                testLogStream.println("Deleted " + featureFile + " = " + featureFileDeleted);
            }
            if (featureJarCreated && featureJar != null) {
                boolean featureJarDeleted = featureJar.delete();
                testLogStream.println("Deleted " + featureJar + " = " + featureJarDeleted);
            }
            if (extDirFile != null) {
                extDirFile.delete();
            }
        }
    }

    @Test
    public void testPackageWLP() throws Throwable {
        Process p = null;
        File extPropertiesFile = null;
        File featureFile = null;
        File featureJar = null;
        File extDirFile = null;

        boolean propertiesFileCreated = false;
        boolean featureFileCreated = false;
        boolean featureJarCreated = false;
        try {
            String extPropertiesDir = installDir + File.separatorChar + "etc" + File.separatorChar + "extensions";

            File extDir = new File(extPropertiesDir);
            extDir.mkdirs();

            extPropertiesFile = new File(extPropertiesDir +
                                         File.separatorChar + PRODUCT_EXT_PROPERTIES);

            Properties props = new Properties();
            props.put("com.ibm.websphere.productId", "myProduct");
            props.put("com.ibm.websphere.productInstall", "wlpExtension" + File.separatorChar + "producttest");
            props.store(new FileOutputStream(extPropertiesFile), "Product Extension Test Properties");
            propertiesFileCreated = true;

            testLogStream.println("Created " + extPropertiesFile);

            String extensionDir = installParent + File.separatorChar + "wlpExtension" + File.separatorChar +
                                  "producttest" + File.separatorChar + "lib";

            extDirFile = new File(extensionDir);
            extDirFile.mkdirs();
            testLogStream.println("Created directory " + extensionDir);

            File featureDirFile = new File(extensionDir + File.separatorChar + "features");
            featureDirFile.mkdirs();
            testLogStream.println("Created directory " + featureDirFile);

            featureFile = new File(extensionDir + File.separatorChar + "features" + File.separatorChar + PRODUCT_EXT_FEATURE_MANIFEST);
            featureJar = new File(extensionDir + File.separatorChar + PRODUCT_EXTENSION_JAR);

            featureFileCreated = featureFile.createNewFile();
            testLogStream.println("Created file + " + featureFile + " = " + featureFileCreated);

            featureJarCreated = featureJar.createNewFile();
            testLogStream.println("Created file + " + featureJar + " = " + featureJarCreated);

            // Package with --include option
            command("package", null, new String[] { "--include=wlp" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Packaging runtime, should return '0'",
                         ReturnCode.OK.getValue());

            // get wlp zip
            File wlpDir = new File(installDir);
            List<File> packageZipFiles = getMatchingFiles(wlpDir, "wlp.zip");
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            // Ensure usr is not in wlp.zip
            ZipFile zipFile = new ZipFile(packageFile);
            try {
                boolean propsFilePresent = false;
                boolean featureFilePresent = false;
                boolean featureJarFilePresent = false;
                boolean usrFileNotPresent = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    testLogStream.println("EntryName = " + entryName);
                    if (entryName.contains(PRODUCT_EXT_PROPERTIES) == true) {
                        propsFilePresent = true;
                    } else if (entryName.contains(PRODUCT_EXT_FEATURE_MANIFEST) == true) {
                        featureFilePresent = true;
                    } else if (entryName.contains(PRODUCT_EXTENSION_JAR) == true) {
                        featureJarFilePresent = true;
                    } else if (!(entryName.contains("usr") == true)) {
                        usrFileNotPresent = true;
                    }
                }
                Assert.assertTrue("Expecting " + PRODUCT_EXT_PROPERTIES + " in package file", propsFilePresent);
                Assert.assertTrue("Expecting " + PRODUCT_EXT_FEATURE_MANIFEST + " in package file", featureFilePresent);
                Assert.assertTrue("Expecting " + PRODUCT_EXTENSION_JAR + " in package file", featureJarFilePresent);
                Assert.assertTrue("Do not expecting usr in package file", usrFileNotPresent);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } finally {
            if (extPropertiesFile != null && propertiesFileCreated) {
                boolean propertiesFileDeleted = extPropertiesFile.delete();
                testLogStream.println("Deleted " + extPropertiesFile + " = " + propertiesFileDeleted);
            }
            if (featureFileCreated && featureFile != null) {
                boolean featureFileDeleted = featureFile.delete();
                testLogStream.println("Deleted " + featureFile + " = " + featureFileDeleted);
            }
            if (featureJarCreated && featureJar != null) {
                boolean featureJarDeleted = featureJar.delete();
                testLogStream.println("Deleted " + featureJar + " = " + featureJarDeleted);
            }
            if (extDirFile != null) {
                extDirFile.delete();
            }
        }
    }

    @Test
    public void testPackageWLPWithArchiveOption() throws Throwable {
        Process p = null;
        try {
            String archiveName = "myPackage.zip";
            // Package with --include=wlp and --archive=myPackage.zip option
            command("package", null, new String[] { "--archive=" + archiveName + " --include=wlp" });
            p = builder.start();
            doEverything(p, builder.command().toString(),
                         "Packaging runtime, should return '0'",
                         ReturnCode.OK.getValue());

            // get the zip file
            File wlpDir = new File(installDir);
            List<File> packageZipFiles = getMatchingFiles(wlpDir, archiveName);
            Assert.assertTrue("Expecting at least one package file", packageZipFiles.size() > 0);

            File packageFile = packageZipFiles.get(packageZipFiles.size() - 1);

            // Ensure usr is not in the zip file
            ZipFile zipFile = new ZipFile(packageFile);
            try {
                boolean usrFileNotPresent = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    testLogStream.println("EntryName = " + entryName);
                    if (!(entryName.contains("usr") == true)) {
                        usrFileNotPresent = true;
                    }
                }
                Assert.assertTrue("Do not expecting usr in package file", usrFileNotPresent);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (Exception e) {
        }
    }
}