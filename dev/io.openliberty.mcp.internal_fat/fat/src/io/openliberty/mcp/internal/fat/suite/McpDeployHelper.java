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
 * Liberty server without emitting {@code CWWKZ0014W} or {@code CWWKZ0059E}.
 *
 * <h3>Why the ordering matters</h3>
 * <ul>
 * <li>{@code CWWKZ0014W} — Liberty tries to start an app whose WAR is not yet
 * on disk. Fix: write the WAR <em>before</em> adding the
 * {@code <application>} entry to server.xml.</li>
 * <li>{@code CWWKZ0059E} — Liberty tries to stop an app but the WAR has
 * already been deleted. Fix: remove the {@code <application>} entry and
 * wait for the app to stop <em>before</em> deleting the WAR.</li>
 * </ul>
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
 * server.waitForStringInLogUsingMark("CWWKZ0001I:.*myApp");
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
     * updated, which prevents Liberty from emitting {@code CWWKZ0014W} (app
     * declared in server.xml before the WAR file exists).
     *
     * <p>After updating the configuration, the app is registered with
     * {@code server.addInstalledAppForValidation} so that framework shutdown
     * validation knows about it, even though {@code DISABLE_VALIDATION} was
     * used during the export.
     *
     * @param server the Liberty server to deploy to
     * @param war the archive to deploy; its name is used as the file name
     * @param configurator callback that receives the new {@link Application} element
     *     so the caller can set properties (e.g. {@code <mcp stateless="true"/>})
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
     * <p>A mark-based {@code waitForStringInLogUsingMark("CWWKZ0009I:.*appName")} is used
     * instead of relying solely on {@link LibertyServer#removeInstalledAppForValidation},
     * because that method scans from the start of the log and can be confused by earlier
     * {@code CWWKZ0001I} start messages on a long-running shared server.
     *
     * @param server the Liberty server to undeploy from
     * @param appName the application name (without {@code .war} suffix) that was
     *     previously deployed via {@link #deployWithConfiguration}
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

        // 2. Wait for the app to stop using a mark-based search so that earlier
        //    CWWKZ0001I messages from this same server run don't confuse
        //    waitForAppState (which scans from the start of the log).
        //    The caller must have called server.setMarkToEndOfLog() before teardown.
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + appName);

        // 3. Deregister from the framework. removeInstalledAppForValidation will
        //    re-check state via waitForAppState; since CWWKZ0009I is now the last
        //    matching message it will return immediately.
        server.removeInstalledAppForValidation(appName);

        // 4. Delete WAR only after the app has fully stopped.
        server.deleteFileFromLibertyServerRoot("apps/" + appName + ".war");
    }

    /**
     * Convenience overload that derives the app name from the archive.
     * Prefer passing the app name string directly where the archive does not
     * need to be kept around solely for teardown.
     *
     * @param server the Liberty server to undeploy from
     * @param war the archive that was previously deployed via
     *     {@link #deployWithConfiguration(LibertyServer, WebArchive, Consumer)}
     * @see #undeployWithConfiguration(LibertyServer, String)
     */
    public static void undeployWithConfiguration(LibertyServer server,
                                                 WebArchive war)
                    throws Exception {
        undeployWithConfiguration(server, war.getName().replace(".war", ""));
    }

    /**
     * Removes a dropin WAR from the server's {@code dropins/} directory and
     * deregisters it from framework validation.
     *
     * <p>The WAR file is deleted first — Liberty detects the removal, stops the
     * app, and logs {@code CWWKZ0009I}. {@link LibertyServer#removeInstalledAppForValidation}
     * already waits for that message internally, so no separate
     * {@code waitForStringInLogUsingMark} call is needed here.
     *
     * <p>The caller must have called {@code server.setMarkToEndOfLog()} before
     * teardown so that the internal wait does not match an earlier
     * {@code CWWKZ0001I} start message from the same server run.
     *
     * @param server the Liberty server to undeploy from
     * @param appName the application name (without {@code .war} suffix)
     */
    public static void undeployDropinApp(LibertyServer server,
                                         String appName)
                    throws Exception {
        server.deleteFileFromLibertyServerRoot("dropins/" + appName + ".war");
        server.removeInstalledAppForValidation(appName);
    }
}
