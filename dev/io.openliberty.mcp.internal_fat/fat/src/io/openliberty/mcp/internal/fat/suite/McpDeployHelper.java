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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 * Shared helpers for dynamically deploying and undeploying WARs on a running
 * Liberty server.
 *
 * <p>Does things in the right order to avoid creating warnings
 * {@code CWWKZ0014W} (config with no app) or {@code CWWKZ0059E}
 * (trying to stop an app that's been deleted).
 *
 * <p>Usage in a suite:
 *
 * <pre>
 * // deploy — store the archive so teardown can use the same instance,
 * //           or just pass the app name string to undeployWithConfiguration.
 * McpDeployHelper.deployWithConfiguration(server, war, app -> {
 *     Mcp mcp = new Mcp();
 *     mcp.setStateless("true");
 *     app.getMcpElements().add(mcp);
 * });
 *
 * // undeploy — preferred: pass the app name directly (no need to recreate the archive)
 * McpDeployHelper.undeployWithConfiguration(server, "myApp");
 * </pre>
 */

public final class McpDeployHelper {

    private McpDeployHelper() {
        // utility class — not instantiable
    }

    /**
     * Deploys {@code war} to the server's {@code apps/} directory and adds a
     * matching {@code <application>} element to server.xml, letting the caller
     * configure it via {@code configurator}.
     *
     * <p>The WAR is written to disk <em>before</em> the server configuration is
     * updated, and the method waits for the app to start before returning.
     *
     * @param server the Liberty server to deploy to
     * @param war the archive to deploy; its name is used as the file name
     * @param configurator callback that receives the new {@link Application} element
     *     so the caller can set properties (e.g. {@code <mcp stateless="true"/>})
     * @throws Exception if the app does not start
     */
    public static void deployWithConfiguration(LibertyServer server,
                                               WebArchive war,
                                               Consumer<Application> configurator)
                    throws Exception {
        String appName = war.getName().replace(".war", "");

        // 1. Write WAR to disk first — file must exist before config references it.
        //    DISABLE_VALIDATION skips ShrinkHelper's internal addInstalledAppForValidation
        //    call (and its app-started wait). The <application> entry does not exist yet,
        //    so Liberty won't start the app until step 2.
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY, DISABLE_VALIDATION);

        // 2. Add <application> entry — config updated only after WAR is on disk.
        ServerConfiguration config = server.getServerConfiguration();
        Application app = new Application();
        app.setLocation(war.getName());
        configurator.accept(app);
        config.getApplications().add(app);
        server.updateServerConfiguration(config);

        // 3. Register with the framework so shutdown validation tracks this app.
        //    We do this manually here because DISABLE_VALIDATION suppressed the
        //    automatic registration inside exportAppToServer.
        //    This waits for the app to start before returning.
        server.addInstalledAppForValidation(appName);
    }

    /**
     * Removes the {@code <application>} entry for {@code appName} from server.xml,
     * waits for the app to stop, then deletes the WAR file.
     *
     * <p>The config is removed <em>before</em> the file is deleted, which prevents
     * Liberty from emitting {@code CWWKZ0059E} (app still configured when WAR
     * is removed from disk).
     *
     * @param server the Liberty server to undeploy from
     * @param appName the application name (without {@code .war} suffix) that was
     *     previously deployed via {@link #deployWithConfiguration}
     * @throws Exception if the app does not stop
     */
    public static void undeployWithConfiguration(LibertyServer server,
                                                 String appName)
                    throws Exception {
        // 1. Remove from server.xml first — triggers graceful app stop.
        //    Loop until all entries are gone; a failed previous run may have left
        //    a duplicate <application> entry which removeBy (first-match only) misses.
        ServerConfiguration config = server.getServerConfiguration();
        while (config.getApplications().removeBy("location", appName + ".war") != null) {
            // keep removing until none remain
        }
        server.updateServerConfiguration(config);

        // 2. Deregister from the framework. removeInstalledAppForValidation will
        //    wait until the app is stopped.
        server.removeInstalledAppForValidation(appName);

        // 3. Delete WAR only after the app has fully stopped.
        server.deleteFileFromLibertyServerRoot("apps/" + appName + ".war");
    }

    /**
     * Undeploys an application from {@code apps}, deriving the app's name from
     * an archive. Use {@link #undeployWithConfiguration(LibertyServer, String)}
     * to undeploy by name if you don't still have the archive.
     *
     * @param server the Liberty server to undeploy from
     * @param war the archive that was previously deployed via
     *     {@link #deployWithConfiguration(LibertyServer, WebArchive, Consumer)}
     * @throws Exception if the app does not stop
     * @see #undeployWithConfiguration(LibertyServer, String)
     */
    public static void undeployWithConfiguration(LibertyServer server,
                                                 WebArchive war)
                    throws Exception {
        undeployWithConfiguration(server, war.getName().replace(".war", ""));
    }

    /**
     * Deploys {@code war} to the server's {@code dropins/} directory.
     *
     * The method waits for the app to start before returning.
     *
     * @param server the Liberty server to deploy to
     * @param war the archive to deploy; its name is used as the file name
     * @throws Exception if the app does not start
     */
    public static void deployDropinApp(LibertyServer server, WebArchive war) throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
    }

    /**
     * Removes a dropin WAR from the server's {@code dropins/} directory and
     * deregisters it from framework validation.
     *
     * <p>The WAR file is deleted first — Liberty detects the removal, stops the
     * app, and logs {@code CWWKZ0009I}. Then {@link LibertyServer#removeInstalledAppForValidation}
     * Is called to wait for the app to stop.
     *
     * @param server the Liberty server to undeploy from
     * @param appName the application name (without {@code .war} suffix)
     * @throws Exception if the app does not stop
     */
    public static void undeployDropinApp(LibertyServer server,
                                         String appName)
                    throws Exception {
        server.deleteFileFromLibertyServerRoot("dropins/" + appName + ".war");
        server.removeInstalledAppForValidation(appName);
    }
}
