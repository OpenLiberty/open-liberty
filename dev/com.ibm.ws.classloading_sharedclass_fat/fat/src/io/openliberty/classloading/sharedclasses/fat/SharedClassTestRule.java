/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.fat;

import static componenttest.topology.utils.FATServletClient.runTest;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_NAME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.classloading.sharedclasses.fat.FATSuite.TestMethod;

public class SharedClassTestRule implements TestRule {

    /**
     * The mode the server is running in to execute the test
     */
    public enum ServerMode {
        storeInCache,
        loadFromCache,
        modifyAppClasses
    }

    private static void log(String method, String msg) {
        Log.info(SharedClassTestRule.class, method, msg);
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

    private ServerSetup<?> serverSetup;
    private LibertyServer server;
    private String consoleLogName = "console.log";
    private ServerMode serverMode = ServerMode.storeInCache;

    public ServerMode serverMode() {
        return serverMode;
    }
    /**
     * Sets the mandatory function that does server setup for the store and load mode for the test
     *
     * @param  <T>
     * @param  serverSetup
     * @return
     */
    public <T extends Throwable> SharedClassTestRule setServerSetup(ServerSetup<T> serverSetup) {
        this.serverSetup = serverSetup;
        return this;
    }

    /**
     * Sets the console log name. The mode is prepended to the name when starting the server.
     * The default name is console.log
     *
     * @param  consoleLogName
     * @return
     */
    public SharedClassTestRule setConsoleLogName(String consoleLogName) {
        this.consoleLogName = consoleLogName;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description desc) {
        assertNotNull("Must set ServerSetup", serverSetup);
        return new SharedClassTestStatement(base);
    }

    private void deleteSharedClassCache() throws Exception {
        RemoteFile classCache = null;
        try {
            classCache = server.getFileFromLibertyInstallRoot("usr/servers/.classCache");
        } catch (Exception e) {
            // didn't find an existing cache; just return
            Log.info(FATSuite.class, "deleteSharedClassCache", "No .classCache found to delete.");
            return;
        }
        Log.info(FATSuite.class, "deleteSharedClassCache", "deleting " + classCache.getAbsolutePath());
        classCache.delete();
    }

    void checkSharedClassTrace(LibertyServer server, TestMethod testMethod, String expectedTraceMsg) throws Exception {
        Iterator<String> traceLines = server.findStringsInLogsAndTrace(".*").iterator();
        while (traceLines.hasNext()) {
            String line = traceLines.next();
            if (line.contains(expectedTraceMsg)) {
                String storedClass = traceLines.next();
                if (storedClass.contains(testMethod.className())) {
                    return;
                }
            }
        }
        fail("Did not find the expected trace message '" + expectedTraceMsg + "' for class: " + testMethod.className());
    }

    public void runSharedClassTest(String testServletPath, String testMethodName) throws Exception {
        int underscore = testMethodName.indexOf('_');
        if (underscore >= 0) {
            testMethodName = testMethodName.substring(0, underscore);
        }
        TestMethod testMethod = TestMethod.valueOf(testMethodName);
        runTest(server, SHARED_CLASSES_WAR_NAME + testServletPath, testMethod.name());
        switch(serverMode) {
            case storeInCache:
                checkSharedClassTrace(server, testMethod, "Called shared class cache to store class");
                break;
            case loadFromCache:
                checkSharedClassTrace(server, testMethod, "Found class in shared class cache");
                break;
            case modifyAppClasses:
                checkSharedClassTrace(server, testMethod, "Called shared class cache to store class");
                break;
            default:
                fail("Unknown serverMode: " + serverMode);
        }
    }

    /**
     * Statement class - performs the before/after operations around a
     * call to the base Statement's evaulate() method (which runs the test).
     */
    private class SharedClassTestStatement extends Statement {

        /**
         * A reference to the Statement that this Statement wraps around.
         */
        private final Statement base;

        /**
         *
         * @param base The Statement that this Statement wraps around.
         */
        public SharedClassTestStatement(Statement base) {
            this.base = base;
        }

        /**
         * This method is called by the test runner in order to execute the tests.
         *
         */
        @Override
        public void evaluate() throws Throwable {
            // first run without autoExpand
            evaluate(ServerMode.storeInCache, false);
            evaluate(ServerMode.loadFromCache, false);
            evaluate(ServerMode.modifyAppClasses, false);

            // now run all tests again with autoExpand
            evaluate(ServerMode.storeInCache, true);
            evaluate(ServerMode.loadFromCache, true);
            evaluate(ServerMode.modifyAppClasses, true);
        }

        private void evaluate(ServerMode mode, boolean autoExpand) throws Throwable {
            log("evaluate", "Running evaluate for test mode: " + mode + " autoExpand: " + autoExpand);
            serverMode = mode;
            log("evaluate", "Server setup for mode: " + mode);
            server = serverSetup.call(mode);

            ServerConfiguration config = server.getServerConfiguration();
            config.getApplicationManager().setAutoExpand(autoExpand);
            server.updateServerConfiguration(config);

            if (mode == ServerMode.storeInCache) {
                deleteSharedClassCache();
            }
            final String nameExtension = "_" + mode + (autoExpand ? "_autoExpand" : "");
            server.setConsoleLogName(consoleLogName + nameExtension);
            try {
                log("evaluate", "Starting server for mode: " + mode);
                server.startServer(mode == ServerMode.storeInCache ? true : false);
                log("evaluate", "Running tests for mode: " + mode);
                FATRunner.setTestNameModifier((t) -> t + nameExtension);
                base.evaluate();
            } finally {
                FATRunner.setTestNameModifier(null);
                server.stopServer();
            }
        }
    }
}
