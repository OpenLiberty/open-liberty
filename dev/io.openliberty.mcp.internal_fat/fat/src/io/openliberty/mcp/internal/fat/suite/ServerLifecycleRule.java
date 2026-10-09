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

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class ServerLifecycleRule implements TestRule {

    private String serverName;
    private LibertyServer server;
    private boolean isRunning;

    public ServerLifecycleRule(String serverName) {
        super();
        this.serverName = serverName;
    }

    @Override
    public Statement apply(Statement testToRun, Description description) {
        // TODO: check if _any_ of our child tests are configured to run before starting the server
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

    protected void before() throws Throwable {
        isRunning = true;
        server = LibertyServerFactory.getLibertyServer(serverName);
        server.startServer();
        if (serverInstalledSsl()) {
            server.waitForLTPAConfigReady();
        }
    }

    protected void after() {
        isRunning = false;
        try {
            server.stopServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertRunning() {
        if (!isRunning) {
            throw new AssertionError("ServerLifecycleRule accessed outside suite. Check your test is referencing its parent suite correctly.");
        }
    }

    private boolean serverInstalledSsl() throws Exception {
        // We only expect one line. If there are multiple,
        // it suggests the server config has changed
        var featureInstallSets = server.getInstalledFeatures();
        if (featureInstallSets.size() == 0) {
            throw new Exception("Server did not log the installed features, probably didn't start correctly?");
        } else if (featureInstallSets.size() > 1) {
            throw new Exception("Server logged more than one line with the installed features at startup.");
        }

        return featureInstallSets.get(0).contains("ssl-1.0");
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
     * waits for the app to stop, then deletes the WAR file.
     *
     * @param appName the application name (without {@code .war} suffix) that was
     *     previously deployed via {@link #deployWithConfiguration(WebArchive, Consumer)}
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
     * Deploys {@code war} to the server's {@code dropins/} directory.
     *
     * The method waits for the app to start before returning.
     *
     * @param war the archive to deploy; its name is used as the file name
     * @throws Exception if the app does not start
     */
    public void deployDropinApp(WebArchive war) throws Exception {
        assertRunning();
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
    }

    /**
     * Removes a dropin WAR from the server's {@code dropins/} directory and
     * deregisters it from framework validation.
     *
     * @param appName the application name (without {@code .war} suffix)
     * @throws RuntimeException if the app does not stop
     * @see McpDeployHelper#undeployDropinApp(LibertyServer, String)
     */
    public void undeployDropinApp(String appName) throws Exception {
        assertRunning();
        McpDeployHelper.undeployDropinApp(server, appName);
    }

}
