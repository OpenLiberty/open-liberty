/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.rules.repeater;

import static componenttest.custom.junit.runner.CheckpointSupportFilter.CHECKPOINT_ONLY_PROPERTY_NAME;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Rule to run the tests with normal server run and checkpoint/restore on each run.
 * Note that if this rule is used in a FAT that has repeat rules then each
 * repeat will also attempt a run with checkpoint.
 */
public class CheckpointRule implements TestRule {
    private static final AtomicBoolean IS_ACTIVE = new AtomicBoolean();
    public static final String ID = "CHECKPOINT_RULE";

    /**
     * Returns true if the test is running with the checkpoint scenario
     *
     * @return true if doing a checkpoint test
     */
    public static boolean isActive() {
        return IS_ACTIVE.get();
    }

    /**
     * The mode the server is running in to execute the test
     */
    public enum ServerMode {
        /**
         * A normal server launch
         */
        NORMAL,
        /**
         * A server that has a checkpoint/restore to launch
         */
        CHECKPOINT
    }

    private static void log(String method, String msg) {
        Log.info(CheckpointRule.class, method, msg);
    }

    /**
     * A function that acts like @BeforeClass. This is an optional
     * function for the rule. If set it gets call once for each repeat done.
     * But it does not get called between the normal and checkpoint runs of
     * the tests
     *
     * @param <T>
     */
    @FunctionalInterface
    public static interface ClassSetup<T extends Throwable> {
        /**
         * Does the class setup before running the tests in normal and checkpoint mode
         *
         * @throws T
         */
        public void call() throws T;
    }

    /**
     * A function that does setup of a {@code LibertyServer}.
     *
     * @param <T>
     */
    @FunctionalInterface
    public static interface ServerSetup<T extends Throwable> {
        /**
         * Setup the server
         *
         * @param  mode The mode the server will be run for the test
         * @return      the unstarted server
         * @throws T
         */
        public LibertyServer call(ServerMode mode) throws T;
    }

    /**
     * A function that starts the server.
     *
     * @param <T>
     */
    @FunctionalInterface
    public static interface ServerStart<T extends Throwable> {
        /**
         * Starts the server.
         *
         * @param  mode   the mode the server is started in
         * @param  server the server to start
         * @throws T
         */
        public void call(ServerMode mode, LibertyServer server) throws T;
    }

    @FunctionalInterface
    /**
     * Function to stop the server and cleans up.
     *
     * @param <T>
     */
    public static interface ServerTearDown<T extends Throwable> {
        /**
         * Stops the server and cleans up.
         *
         * @param  mode   the mode the server was running for the test
         * @param  server the server to stop
         * @throws T
         */
        public void call(ServerMode mode, LibertyServer server) throws T;
    }

    @FunctionalInterface
    /**
     * A function that acts like @AfterClass. This is an optional
     * function for the rule. If set it gets call once for each repeat done.
     * But it does not get called between the normal and checkpoint runs of
     * the tests
     *
     * @param <T>
     */
    public static interface ClassTearDown<T extends Throwable> {
        public void call() throws T;
    }

    private ClassSetup<?> classSetup;
    private ServerSetup<?> serverSetup;
    private ServerStart<?> serverStart;
    private ServerTearDown<?> serverTearDown;
    private ClassTearDown<?> classTearDown;

    private boolean beta = false;
    private String consoleLogName = "console.log";
    private CheckpointPhase checkpointPhase = CheckpointPhase.AFTER_APP_START;
    private Consumer<LibertyServer> postCheckpointLambda;
    private final Set<String> unsupportedRepeatIDs = new HashSet<>(Arrays.asList(EE6FeatureReplacementAction.ID, EE7FeatureReplacementAction.ID));
    private String[] checkpointIgnoreMessages;
    private boolean runNormalTests = true;
    private boolean checkrestart;

    /**
     * Sets the optional function to do class setup before running the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  classSetup
     * @return
     */
    public <T extends Throwable> CheckpointRule setClassSetup(ClassSetup<T> classSetup) {
        this.classSetup = classSetup;
        return this;
    }

    /**
     * Sets the mandatory function that does server setup for the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  serverSetup
     * @return
     */
    public <T extends Throwable> CheckpointRule setServerSetup(ServerSetup<T> serverSetup) {
        this.serverSetup = serverSetup;
        return this;
    }

    /**
     * Sets the mandatory function that starts the server for the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  serverSetup
     * @return
     */
    public <T extends Throwable> CheckpointRule setServerStart(ServerStart<T> serverStart) {
        this.serverStart = serverStart;
        return this;
    }

    /**
     * Sets the mandatory function that stops the server for the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  serverTearDown
     * @return
     */
    public <T extends Throwable> CheckpointRule setServerTearDown(ServerTearDown<T> serverTearDown) {
        this.serverTearDown = serverTearDown;
        return this;
    }

    /**
     * Sets the optional function to do class tear down after running the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  classSetup
     * @return
     */
    public <T extends Throwable> CheckpointRule setClassTearDown(ClassTearDown<T> classTearDown) {
        this.classTearDown = classTearDown;
        return this;
    }

    /**
     * Sets the beta flag for running the tests when in checkpoint mode.
     *
     * @param  beta
     * @return
     */
    public CheckpointRule setBeta(boolean beta) {
        this.beta = beta;
        return this;
    }

    /**
     * Adds repeat IDs that are not supported when running in checkpoint mode.
     * If the specified repeat IDs are running then the checkpoint mode will be
     * skipped for the tests.
     *
     * @param  unsupportedRepeatIDs
     * @return
     */
    public CheckpointRule addUnsupportedRepeatIDs(String... unsupportedRepeatIDs) {
        this.unsupportedRepeatIDs.addAll(Arrays.asList(unsupportedRepeatIDs));
        return this;
    }

    /**
     * The {@code CheckpointPhase} to run the checkpoint tests with.
     *
     * @param  checkpointPhase
     * @return
     */
    public CheckpointRule setCheckpointPhase(CheckpointPhase checkpointPhase) {
        this.checkpointPhase = checkpointPhase;
        return this;
    }

    /**
     * The optional function to run post checkpoint before the server is restored when running
     * the test in checkpoint mode
     *
     * @param  postCheckpointLambda
     * @return
     */
    public CheckpointRule setPostCheckpointLambda(Consumer<LibertyServer> postCheckpointLambda) {
        this.postCheckpointLambda = postCheckpointLambda;
        return this;
    }

    public CheckpointRule setAssertNoAppRestartOnRestore(boolean checkrestart) {
        this.checkrestart = checkrestart;
        return this;
    }

    /**
     * Sets the console log name. When the test is run in checkpoint mode then CHECKPOINT_RULE will be prepended to the name.
     * The default name is console.log
     *
     * @param  consoleLogName
     * @return
     */
    public CheckpointRule setConsoleLogName(String consoleLogName) {
        this.consoleLogName = consoleLogName;
        return this;
    }

    /**
     * Adds regular expressions to match messages to ignore from the server
     *
     * @param  regExs regular expression to match server messages
     * @return
     */
    public CheckpointRule addCheckpointRegexIgnoreMessages(String... regExs) {
        this.checkpointIgnoreMessages = regExs;
        return this;
    }

    /**
     * Sets if the tests should be run on a normal server which has no checkpoint done.
     *
     * @param  runNormalTests
     * @return
     */
    public CheckpointRule setRunNormalTests(boolean runNormalTests) {
        this.runNormalTests = runNormalTests;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description desc) {
        assertNotNull("Must set ServerSetup", serverSetup);
        assertNotNull("Must set ServerStart", serverStart);
        assertNotNull("Must set serverTearDown", serverTearDown);
        return new CheckpointStatement(base);
    }

    /**
     * Statement class - performs the before/after operations around a
     * call to the base Statement's evaulate() method (which runs the test).
     */
    private class CheckpointStatement extends Statement {

        /**
         * A reference to the Statement that this Statement wraps around.
         */
        private final Statement base;

        private LibertyServer server;
        private Map<String, String> originalJvmOptions = null;
        private Properties originalBootstrapProperties = null;

        /**
         *
         * @param base The Statement that this Statement wraps around.
         */
        public CheckpointStatement(Statement base) {
            this.base = base;
        }

        /**
         * This method is called by the test runner in order to execute the tests.
         *
         */
        @Override
        public void evaluate() throws Throwable {
            if (classSetup != null) {
                classSetup.call();
            }
            try {
                if (runNormalTests) {
                    evaluate(ServerMode.NORMAL);
                }
                evaluate(ServerMode.CHECKPOINT);
            } finally {
                if (classTearDown != null) {
                    classTearDown.call();
                }
            }
        }

        private void evaluate(ServerMode mode) throws Throwable {
            log("evaluate", "Running evaluate for test mode: " + mode);
            if (mode == ServerMode.CHECKPOINT) {
                if (!isCheckpointSupported()) {
                    log("evaluate", "Skipping evaluate for mode: " + mode);
                    return;
                }
            } else {
                if (Boolean.getBoolean(CHECKPOINT_ONLY_PROPERTY_NAME)) {
                    log("evaluate", "Skipping evaluate for mode: " + mode);
                    return;
                }
            }
            IS_ACTIVE.set(mode == ServerMode.CHECKPOINT);
            try {
                try {
                    log("evaluate", "Server setup for mode: " + mode);
                    server = serverSetup.call(mode);
                    if (mode == ServerMode.CHECKPOINT) {
                        checkpointSetup();
                    } else {
                        server.setConsoleLogName(consoleLogName);
                    }
                    log("evaluate", "Starting server for mode: " + mode);
                    serverStart.call(mode, server);
                    log("evaluate", "Running tests for mode: " + mode);
                    base.evaluate();
                } finally {
                    try {
                        serverTearDown.call(mode, server);
                    } finally {
                        if (mode == ServerMode.CHECKPOINT) {
                            checkpointTearDown();
                        }
                    }
                }
            } finally {
                IS_ACTIVE.set(false);
            }
        }

        private boolean isCheckpointSupported() {
            if (JavaInfo.forCurrentVM().isCriuSupported()) {
                return !RepeatTestFilter.isAnyRepeatActionActive(unsupportedRepeatIDs.toArray(new String[0]));
            }
            return false;
        }

        /**
         * checkpoint/restore server setup
         */
        private void checkpointSetup() throws Exception {
            log("checkpointSetup", "Setting up the server for checkpoint");
            setJvmOptions();
            configureBootStrapProperties();

            String logName = ID + "_" + consoleLogName;
            log("checkpointSetup", "Configuring checkpoint phase '" + checkpointPhase + "' with log name: " + logName);
            CheckpointInfo checkpointInfo = new CheckpointInfo(checkpointPhase, true, postCheckpointLambda);
            checkpointInfo.setAssertNoAppRestartOnRestore(checkrestart);
            server.setConsoleLogName(logName);
            server.setCheckpoint(checkpointInfo);
            if (checkpointIgnoreMessages != null) {
                server.addCheckpointRegexIgnoreMessages(checkpointIgnoreMessages);
            }
        }

        private void checkpointTearDown() throws Exception {
            server.unsetCheckpoint();
            restoreJvmOptions();
            restoreBootstrapProperties();
        }

        private void setJvmOptions() throws Exception {
            if (beta) {
                log("setJvmOptions", "Setting the beta flag for checkpoint run jvm.options");
                Map<String, String> options = server.getJvmOptionsAsMap();
                originalJvmOptions = new HashMap<String, String>(options);
                options.put("-Dcom.ibm.ws.beta.edition", "true");
                server.setJvmOptions(options);
            }
        }

        private void restoreJvmOptions() throws Exception {
            if (originalJvmOptions != null) {
                log("restoreJvmOptions", "Restoring original jvm.options");
                server.setJvmOptions(originalJvmOptions);
                originalJvmOptions = null;
            }
        }

        private void restoreBootstrapProperties() throws Exception {
            if (originalBootstrapProperties != null) {
                log("restoreBootstrapProperties", "Restoring original bootstrap.properties");
                try (OutputStream out = new FileOutputStream(server.getServerBootstrapPropertiesFile().getAbsolutePath())) {
                    originalBootstrapProperties.store(out, "");
                    originalBootstrapProperties = null;
                }
            }
        }

        private void configureBootStrapProperties() throws Exception {
            log("configureBootStrapProperties", "Configuring bootstrap.properties for checkpoint");
            Properties bootStrapProperties = server.getBootstrapProperties();
            originalBootstrapProperties = new Properties();
            originalBootstrapProperties.putAll(bootStrapProperties);

            //Exempting security and removing all security bootstrap properties.
            bootStrapProperties.put("websphere.java.security.exempt", "true");
            bootStrapProperties.remove("websphere.java.security");
            bootStrapProperties.remove("websphere.java.security.norethrow");
            bootStrapProperties.remove("websphere.java.security.unique");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(server.getServerBootstrapPropertiesFile().getAbsolutePath()))) {
                for (String key : bootStrapProperties.stringPropertyNames()) {
                    String value = bootStrapProperties.getProperty(key);
                    writer.write(key + "=" + value);
                    writer.newLine();
                }
            }
        }

    }
}
