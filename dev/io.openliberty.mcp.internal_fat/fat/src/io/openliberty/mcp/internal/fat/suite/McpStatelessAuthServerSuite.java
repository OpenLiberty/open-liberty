/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.mcp.internal.fat.security.AdminsRoleAllowedTestsStateless;
import io.openliberty.mcp.internal.fat.security.DenyAllTestsStateless;
import io.openliberty.mcp.internal.fat.security.NoClassAnnotationTestsStateless;
import io.openliberty.mcp.internal.fat.security.PermitAllTestsStateless;

/**
 * Suite that owns the lifecycle of the {@code mcp-stateless-server-auth} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpStatelessAuthServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 *
 * <p>Test classes should use {@link #deployWithConfiguration} and
 * {@link #undeployWithConfiguration} to deploy WARs that require an
 * {@code <application>} entry in server.xml. These helpers enforce the correct
 * ordering (WAR written before config updated; config removed before WAR deleted)
 * so that {@code CWWKZ0014W} and {@code CWWKZ0059E} are never emitted.
 */
@RunWith(Suite.class)
@SuiteClasses({
                PermitAllTestsStateless.class,
                DenyAllTestsStateless.class,
                NoClassAnnotationTestsStateless.class,
                AdminsRoleAllowedTestsStateless.class,
})
public class McpStatelessAuthServerSuite {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("mcp-stateless-server-auth");

    @ClassRule
    public static ExternalResource serverLifecycle = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            server.startServer();
            server.waitForLTPAConfigReady();
        }

        @Override
        protected void after() {
            try {
                server.stopServer();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Deploys {@code war} to the server's {@code apps/} directory and adds a
     * matching {@code <application>} element to server.xml, letting the caller
     * configure it via {@code configurator}.
     *
     * <p>The WAR is written to disk <em>before</em> the server configuration is
     * updated, which prevents Liberty from emitting {@code CWWKZ0014W} (app
     * declared in server.xml before the WAR file exists).
     *
     * @param war the archive to deploy; its name is used as the file name
     * @param configurator callback that receives the new {@link Application} element
     *     so the caller can set properties (e.g. {@code <mcp stateless="true"/>})
     */
    public static void deployWithConfiguration(WebArchive war,
                                               Consumer<Application> configurator)
                    throws Exception {
        // Write WAR to disk first — file must exist before config references it.
        // DISABLE_VALIDATION skips ShrinkHelper's own app-started check; the
        // <application> entry does not exist yet so Liberty won't start the app
        // until step 2. The caller's waitForStringInLogUsingMark handles readiness.
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY, DISABLE_VALIDATION);

        // Add <application> entry config updated only after WAR is on disk
        ServerConfiguration config = server.getServerConfiguration();
        Application app = new Application();
        app.setLocation(war.getName());
        configurator.accept(app);
        config.getApplications().add(app);
        server.updateServerConfiguration(config);
    }

    /**
     * Removes the {@code <application>} entry for {@code war} from server.xml,
     * waits for the app to stop, then deletes the WAR file.
     *
     * <p>The config is removed <em>before</em> the file is deleted, which prevents
     * Liberty from emitting {@code CWWKZ0059E} (app still configured when WAR
     * is removed from disk).
     *
     * @param war the archive that was previously deployed via
     *     {@link #deployWithConfiguration}
     */
    public static void undeployWithConfiguration(WebArchive war) throws Exception {
        String appName = war.getName().replace(".war", "");

        // Now remove from server.xml first( triggers graceful app stop)
        ServerConfiguration config = server.getServerConfiguration();
        config.getApplications().removeBy("location", war.getName());
        server.updateServerConfiguration(config);

        //  Wait for app to stop before touching the file
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + appName);

        // Delete WAR only after app has fully stopped
        server.deleteFileFromLibertyServerRoot("apps/" + war.getName());
        server.removeInstalledAppForValidation(appName);
    }
}
