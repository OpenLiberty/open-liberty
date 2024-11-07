/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.rules.repeater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * Rule to run the tests with normal server run and checkpoint/restore on each EE repetition
 */
public class CheckpointRule implements TestRule {
    public static void log(String method, String msg) {
        Log.info(CheckpointRule.class, method, msg);
    }

    @FunctionalInterface
    public static interface Action<T extends Throwable> {
        public void call() throws T;
    }

    private Action<?> initialSetup;

    private Action<?> finalTearDown;

    private Supplier<LibertyServer> serverSupplier;

    private boolean beta;

    private CheckpointPhase checkpointPhase = CheckpointPhase.AFTER_APP_START;

    private Consumer<LibertyServer> postCheckpointLambda;

    /**
     * @param  server The LibertyServer to use
     *
     * @return        this
     */
    public CheckpointRule setServerSupplier(Supplier<LibertyServer> serverSupplier) {
        this.serverSupplier = serverSupplier;
        return this;
    }

    /**
     * @param  initialSetup Executed on each EE repetition.
     *
     * @return              this
     */
    public <T extends Throwable> CheckpointRule setInitialSetup(Action<T> initialSetup) {
        this.initialSetup = initialSetup;
        return this;
    }

    /**
     * @param  finalTearDown Executed after each EE repetition.
     *
     * @return               this
     */
    public <T extends Throwable> CheckpointRule setFinalTearDown(Action<T> finalTearDown) {
        this.finalTearDown = finalTearDown;
        return this;
    }

    public CheckpointRule setBeta(boolean beta) {
        this.beta = beta;
        return this;
    }

    public CheckpointRule setCheckpointPhase(CheckpointPhase checkpointPhase) {
        this.checkpointPhase = checkpointPhase;
        return this;
    }

    public CheckpointRule setPostCheckpointLambda(Consumer<LibertyServer> postCheckpointLambda) {
        this.postCheckpointLambda = postCheckpointLambda;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description desc) {
        return new CheckpointStatement(base, serverSupplier);
    }

    /**
     * Statement class - performs the before/after operations around a
     * call to the base Statement's evaulate() method (which runs the test).
     */
    protected class CheckpointStatement extends Statement {

        /**
         * A reference to the Statement that this Statement wraps around.
         */
        private final Statement base;

        private final Supplier<LibertyServer> serverSupplier;

        private LibertyServer server;

        /**
         *
         * @param base The Statement that this Statement wraps around.
         */
        public CheckpointStatement(Statement base, Supplier<LibertyServer> serverSupplier) {
            this.base = base;
            this.serverSupplier = serverSupplier;
        }

        public void setServer() {
            this.server = serverSupplier.get();
        }

        /**
         * This method is called by the test runner in order to execute the tests.
         *
         */
        @Override
        public void evaluate() throws Throwable {

            log("evaluate", "running initial setup");

            if (initialSetup != null) {
                initialSetup.call();
            }

            //Set server after the initialSetup is done.
            setServer();

            try {
                runTests();

            } finally {

                if (finalTearDown != null) {
                    finalTearDown.call();
                }
                unsetCheckpoint();
            }
        }

        private void unsetCheckpoint() {
            unsetCheckpointId();
            server.unsetCheckpoint();
        }

        /**
         * Run all tests with normal server run and checkpoint/restore
         */
        protected void runTests() throws Throwable {
            // normal server run
            base.evaluate();

            if (isCheckpointSupported()) {
                //Stop the server before running tests again for InstantOn.
                if (finalTearDown != null) {
                    finalTearDown.call();
                } else {
                    server.stopServer();
                }

                checkpointSetup();
                //checkpoint/restore run
                base.evaluate();
            }
        }

        private boolean isCheckpointSupported() {
            //Skipping EmptyAction repeat action for doing checkpoint/restore for now. EmptyAction could be running EE7 or EE6 features.
            return JavaInfo.forCurrentVM().isCriuSupported() &&
                   !RepeatTestFilter.isAnyRepeatActionActive(EmptyAction.ID, EE6FeatureReplacementAction.ID, EE7FeatureReplacementAction.ID);
        }

        /**
         * checkpoint/restore server setup
         */
        private void checkpointSetup() throws Exception {
            setCheckpointId();
            setJvmOptions();
            configureBootStrapProperties();

            CheckpointInfo checkpointInfo = new CheckpointInfo(checkpointPhase, true, postCheckpointLambda);
            server.setCheckpoint(checkpointInfo);
            server.startServer();
        }

        private void setCheckpointId() {
            FeatureReplacementAction action = (FeatureReplacementAction) RepeatTestFilter.getMostRecentRepeatAction();
            action.withID(action.getID() + "_Checkpoint");
        }

        private void unsetCheckpointId() {
            if (FeatureReplacementAction.isCheckpointRepeatActionActive()) {
                FeatureReplacementAction action = (FeatureReplacementAction) RepeatTestFilter.getMostRecentRepeatAction();
                String id = action.getID().substring(0, action.getID().indexOf("_Checkpoint"));
                action.withID(id);
            }
        }

        private void setJvmOptions() throws Exception {
            if (beta) {
                Map<String, String> options = server.getJvmOptionsAsMap();
                options.put("-Dcom.ibm.ws.beta.edition", "true");
                server.setJvmOptions(options);
            }
        }

        public void configureBootStrapProperties() throws Exception {
            Properties bootStrapProperties = new Properties();
            File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
            if (bootStrapPropertiesFile.isFile()) {
                try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
                    bootStrapProperties.load(in);
                }
            }
            Map<String, String> properties = new HashMap<>();
            //Exempting security and removing all security bootstrap properties.
            properties.put("websphere.java.security.exempt", "true");
            bootStrapProperties.remove("websphere.java.security");
            bootStrapProperties.remove("websphere.java.security.norethrow");
            bootStrapProperties.remove("websphere.java.security.unique");

            bootStrapProperties.putAll(properties);
            try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
                bootStrapProperties.store(out, "");
            }
        }

    }
}
