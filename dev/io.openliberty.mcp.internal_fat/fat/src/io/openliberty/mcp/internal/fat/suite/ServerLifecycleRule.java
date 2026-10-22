/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.suite;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.util.Set;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;

import componenttest.custom.junit.runner.CheckpointSupportFilter;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.FeatureFilter;
import componenttest.custom.junit.runner.JavaLevelFilter;
import componenttest.custom.junit.runner.SecurityFilter;
import componenttest.custom.junit.runner.SystemPropertyFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Manages the Liberty server lifecycle and application deployments where the
 * server is owned by a suite and therefore cannot use {@code FATRunner}.
 *
 * <p>Before running the suite, this rule runs the filters usually applied by
 * {@link FATRunner} and skips execution of the whole suite if any do not match.
 * The check for {@code fat.test.class.name} is modified to also check the names
 * of the suite's children.
 *
 * <p>The configured server is started before the MCP FAT suite runs and stopped
 * afterward, even if the suite fails.
 *
 * <p>When the server has the {@code ssl-1.0} feature installed, startup waits
 * for the SSL port to open.
 *
 * <p>When the server has an {@code appSecurity-*} feature installed, startup waits
 * for the LTPA configuration to become ready.
 *
 * <p>While the server is running, this class provides access to it and helpers
 * for deploying and removing configured applications and drop-in applications.
 *
 */
public class ServerLifecycleRule implements TestRule {

    private static final Filter[] testFiltersToApply = new Filter[] {
                                                                      new TestModeFilter(),
                                                                      new SuiteTestNameFilter(),
                                                                      new FeatureFilter(),
                                                                      new SystemPropertyFilter(),
                                                                      new JavaLevelFilter(),
                                                                      new CheckpointSupportFilter(),
                                                                      new SecurityFilter()
    };

    private static final Statement EMPTY_STATEMENT = new Statement() {
        @Override
        public void evaluate() throws Throwable {
            // Intentionally empty
        }
    };

    /**
     * The name of the server managed by this rule
     */
    private final String serverName;
    /**
     * The LibertyServer object for the server in use. Only present while the suite is running.
     */
    private LibertyServer server;
    /**
     * Whether the suite is currently running. Most methods check this and abort if the suite is not running.
     */
    private boolean isRunning;
    /**
     * The test class which owns this rule.
     */
    private Class<?> testClass;

    /**
     * Creates an instance using the named server config
     *
     * @param serverName the name of the server to use
     */
    public ServerLifecycleRule(String serverName) {
        super();
        this.serverName = serverName;
    }

    @Override
    public Statement apply(Statement testToRun, Description description) {
        // If any of the filters prevent us from running, return an empty
        // statement so that we do not start the server and our children do not run.
        for (Filter filter : testFiltersToApply) {
            if (!filter.shouldRun(description)) {
                return EMPTY_STATEMENT;
            }
        }

        testClass = description.getTestClass();

        // Otherwise we are running, execute before and after actions around running the suite
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    testToRun.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    /**
     * Actions run at the start of the suite
     *
     * @throws Throwable
     */
    protected void before() throws Throwable {
        isRunning = true;
        server = LibertyServerFactory.getLibertyServer(serverName, testClass);
        server.startServer();

        if (serverInstalledSsl()) {
            server.waitForDefaultHTTPEndpointSSLStart();
        }
        if (serverInstalledAppSecurity()) {
            server.waitForLTPAConfigReady();
        }
    }

    /**
     * Actions to run at the end of the suite
     */
    protected void after() throws Exception {
        isRunning = false;
        server.stopServer();
    }

    /**
     * Assert that the suite is currently running.
     * <p>
     * Most methods should call this before doing anything to catch any tests referencing a rule from the wrong suite.
     */
    private void assertRunning() {
        if (!isRunning) {
            throw new AssertionError("ServerLifecycleRule accessed outside suite. Check your test is referencing its parent suite correctly.");
        }
    }

    /**
     * Returns whether the server is using {@code ssl-1.0}.
     *
     * @return {@code true} if {@code ssl-1.0} was installed
     */
    private boolean serverInstalledSsl() throws Exception {
        return getInstalledFeatures().contains("ssl-1.0");
    }

    /**
     * Returns whether the server is using any of the {@code appSecurity-x.x} features.
     *
     * @return {@code true} if any {@code appSecurity} feature was installed
     */
    private boolean serverInstalledAppSecurity() throws Exception {
        return getInstalledFeatures().stream().anyMatch(name -> name.startsWith("appSecurity-"));
    }

    private Set<String> getInstalledFeatures() throws Exception {
        // We only expect one line. If there are multiple,
        // it suggests the server config has changed
        var featureInstallSets = server.getInstalledFeatures();
        if (featureInstallSets.size() == 0) {
            throw new Exception("Server did not log the installed features, probably didn't start correctly?");
        }
        return featureInstallSets.get(0);
    }

    /**
     * Get the actual liberty server object managed by this rule.
     *
     * @return the liberty server
     */
    public LibertyServer getServer() {
        assertRunning();
        return server;
    }

    /**
     * Deploys {@code war} to the server's {@code apps/} directory and adds a
     * matching {@code <application>} element to server.xml, letting the caller
     * configure it via {@code configurator}.
     *
     * @param war the archive to deploy; its name is used as the file name
     * @param configurator callback that receives the new {@link Application} element
     *     so the caller can set properties (e.g. {@code <mcp stateless="true"/>})
     * @throws RuntimeException if the app does not start
     * @see McpDeployHelper#deployWithConfiguration(LibertyServer, WebArchive, Consumer)
     */
    public void deployWithConfiguration(WebArchive war,
                                        Consumer<Application> configurator)
                    throws Exception {
        assertRunning();
        McpDeployHelper.deployWithConfiguration(server, war, configurator);
    }

    /**
     * Removes the {@code <application>} entry for {@code appName} from server.xml,
     * waits for the app to stop, then deletes the application file.
     *
     * @param appName the application name that was previously deployed via
     *     {@link #deployWithConfiguration(WebArchive, Consumer)}. May include a
     *     {@code .war} or {@code .ear} extension; {@code .war} is assumed if there is none.
     * @throws RuntimeException if the app does not stop
     * @see McpDeployHelper#undeployWithConfiguration(LibertyServer, String)
     */
    public void undeployWithConfiguration(String appName) throws Exception {
        assertRunning();
        McpDeployHelper.undeployWithConfiguration(server, appName);
    }

    /**
     * Undeploys an application from {@code apps}, deriving the app's name from
     * an archive. Use {@link #undeployWithConfiguration(String)}
     * to undeploy by name if you don't still have the archive.
     *
     * @param war the archive that was previously deployed via
     *     {@link #deployWithConfiguration(WebArchive, Consumer)}
     * @throws RuntimeException if the app does not stop
     * @see #undeployWithConfiguration(String)
     */
    public void undeployWithConfiguration(WebArchive war) throws Exception {
        assertRunning();
        McpDeployHelper.undeployWithConfiguration(server, war);
    }

    /**
     * Deploys {@code archive} to the server's {@code dropins/} directory.
     *
     * The method waits for the app to start before returning.
     *
     * @param archive the {@code .war} or {@code .ear} to deploy; its name is used as the file name
     * @throws Exception if the app does not start
     */
    public void deployDropinApp(Archive<?> archive) throws Exception {
        assertRunning();
        ShrinkHelper.exportDropinAppToServer(server, archive, SERVER_ONLY);
    }

    /**
     * Removes a dropin application from the server's {@code dropins/} directory and
     * deregisters it from framework validation.
     *
     * @param appName the application name. May include a {@code .war} or {@code .ear}
     *     extension; {@code .war} is assumed if there is none.
     * @throws RuntimeException if the app does not stop
     * @see McpDeployHelper#undeployDropinApp(LibertyServer, String)
     */
    public void undeployDropinApp(String appName) throws Exception {
        assertRunning();
        McpDeployHelper.undeployDropinApp(server, appName);
    }

}
