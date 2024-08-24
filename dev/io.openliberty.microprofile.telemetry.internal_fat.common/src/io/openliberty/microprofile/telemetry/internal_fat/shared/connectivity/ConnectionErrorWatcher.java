/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.shared.connectivity;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Ignore test failures which occur due to connectivity errors between liberty and the system collecting spans.
 * <p>
 * Test failures of monitored tests will be ignored if there are errors in the server log similar to
 *
 * <pre>
 * WARN io.opentelemetry.exporter.internal.grpc.GrpcExporter - Failed to export spans.
 * </pre>
 *
 * <p>
 * However, if more than a certain percentage of monitored tests fail in this way, an error will be reported at the end of the suite.
 * <p>
 * In the suite class:
 *
 * <pre>
 * &#64;ClassRule
 * public static TestRule connectionErrorWatcher = new ConnectionErrorWatcher(0.05);
 * </pre>
 *
 * <p>
 * In the test class:
 *
 * <pre>
 * &#64;Server("MyServer")
 * public static LibertyServer server;
 *
 * &#64;Rule
 * public TestRule ignoreConnectionErrors = FATSuite.connectionErrorWatcher.ignoreConnectivityFailures(server);
 * </pre>
 */
public class ConnectionErrorWatcher implements TestRule {

    private final List<Failure> failures = new ArrayList<>();
    private double failureRatioThreshold;
    private int testsRun = 0;
    private boolean watcherActive = false;

    /**
     * Create a new connection error watcher.
     * <p>
     * The watcher is created with the default failure ratio threshold of 5%
     */
    public ConnectionErrorWatcher() {
        super();
        failureRatioThreshold = 0.05;
    }

    /**
     * Set the failure threshold ratio.
     *
     * If the number of failures divided by the number of tests run with the watcher is greater than the threshold, a failure will be created at the end of the test run.
     *
     * @param failureRatioThreshold the threshold ratio
     */
    public void withFailureRatioThreshold(double failureRatioThreshold) {
        this.failureRatioThreshold = failureRatioThreshold;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try {
                    watcherActive = true;
                    statement.evaluate();
                } finally {
                    watcherActive = false;
                }

                if (failures.size() > testsRun * failureRatioThreshold) {
                    StringBuilder error = new StringBuilder();
                    error.append(failures.size()).append("/").append(testsRun)
                                    .append(" tests failed with connectivity errors in the server logs.\n");
                    error.append("Test failures:\n");
                    failures.forEach(f -> {
                        error.append("  ").append(f.desc.getDisplayName()).append("\n");
                        error.append("    ").append(f.error.toString());
                    });
                    fail(error.toString());
                }
            }
        };
    }

    /**
     * Ignore test failures if the server reports connectivity errors.
     * <p>
     * The returned value must be assigned to a field and annotated with {@code @Rule} to ignore failures from that class.
     *
     * @param server the server whose logs should be checked for errors
     * @return a test rule which causes test failures to be logged but ignored if there are connectivity errors in the server log
     */
    public ConnectionErrorsRule ignoreConnectivityFailures(LibertyServer server) {
        if (!watcherActive) {
            throw new IllegalStateException("Connectivity failure ignorer requested when Watcher is not running a suite. ConnectionErrorWatcher should be annotated with @ClassRule on the FATSuite");
        }
        return new ConnectionErrorsRule(this, server);
    }

    /**
     * Record a test which failed but there were connectivity errors in the logs which may have caused the failure.
     *
     * @param description the test description
     * @param e the error that caused the test to fail
     */
    void addFailure(Description description, Throwable e) {
        if (!watcherActive) {
            throw new IllegalStateException("Test failure recorded when Watcher is not running a suite. ConnectionErrorWatcher should be annotated with @ClassRule on the FATSuite", e);
        }
        failures.add(new Failure(description, e));
    }

    /**
     * Record that a test monitored for connection errors has started
     */
    void recordTestStart() {
        if (!watcherActive) {
            throw new IllegalStateException("Watched test started when Watcher is not running a suite. ConnectionErrorWatcher should be annotated with @ClassRule on the FATSuite");
        }
        testsRun++;
    }

    private static class Failure {
        private final Description desc;
        private final Throwable error;

        public Failure(Description desc, Throwable error) {
            super();
            this.desc = desc;
            this.error = error;
        }
    }

    /**
     * A test rule to ignore failures if a server has connectivity errors.
     * <p>
     * Instances are created via {@link ConnectionErrorWatcher#ignoreConnectivityFailures(LibertyServer)}.
     */
    public class ConnectionErrorsRule implements TestRule {

        private LibertyServer server;
        private Set<String> initialConnectivityErrors = Collections.emptySet();
        private final Set<String> finalConnectivityErrors = new HashSet<>();

        private ConnectionErrorsRule(ConnectionErrorWatcher watcher, LibertyServer server) {
            this.server = server;
        }

        @Override
        public Statement apply(Statement statement, Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    if (server != null) {
                        initialConnectivityErrors = new HashSet<>(findConnectivityErrors(server));
                    }
                    recordTestStart();
                    try {
                        statement.evaluate();
                    } catch (AssertionError | Exception e) {
                        if (server != null) {
                            checkForConnectivityErrors();
                        }
                        if (hasNewConnectivityErrors()) {
                            Log.info(ConnectionErrorWatcher.class, "evaluate",
                                     "Test " + description.getDisplayName() + " failed but server logs show connectivity errors so the failure is suppressed.");
                            e.printStackTrace();
                            addFailure(description, e);
                            throw new AssumptionViolatedException("Test failed but server log shows connectivity errors");
                        }
                        throw e;
                    } finally {
                        initialConnectivityErrors = null;
                    }
                }
            };
        }

        /**
         * Set the server whose logs should be checked for connectivity errors.
         * <p>
         * Only needed for tests where the server isn't known before the test runs.
         *
         * @param server the server to check
         * @throws Exception
         */
        public void setServer(LibertyServer server) throws Exception {
            this.server = server;
            initialConnectivityErrors = new HashSet<>(findConnectivityErrors(server));
        }

        /**
         * Check the server log and record any connectivity errors.
         * <p>
         * This method is called automatically at test end, but may need to be called manually if the test cleans up the logs before it ends.
         *
         * @throws Exception
         */
        public void checkForConnectivityErrors() throws Exception {
            finalConnectivityErrors.addAll(findConnectivityErrors(server));
        }

        private boolean hasNewConnectivityErrors() {
            return finalConnectivityErrors.stream()
                            .anyMatch(e -> !initialConnectivityErrors.contains(e));
        }
    }

    private static List<String> findConnectivityErrors(LibertyServer server) throws Exception {
        return server.findStringsInLogs("GrpcExporter - Failed to export spans");
    }

}
