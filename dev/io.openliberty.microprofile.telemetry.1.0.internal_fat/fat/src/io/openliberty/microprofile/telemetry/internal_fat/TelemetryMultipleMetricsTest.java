/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.http.metrics.RestApplication;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TelemetryMultipleMetricsTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Metrics";
    public static final String APP_NAME = "TelemetryMetricsApp";
    public static final String APP_NAME_2 = APP_NAME + "Two";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry21andLatest20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(RestApplication.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties");
        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);

        WebArchive app2 = ShrinkWrap.create(WebArchive.class, APP_NAME_2 + ".war")
                        .addPackage(RestApplication.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties");
        ShrinkHelper.exportAppToServer(server, app2, SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void testHttpMetricsInTwoApps() throws Exception {
        server.setMarkToEndOfLog();
        new HttpRequest(server, "/" + APP_NAME + "/span")
                        .expectCode(200)
                        .run(String.class);

        new HttpRequest(server, "/" + APP_NAME_2 + "/span")
                        .expectCode(200)
                        .run(String.class);

        Thread.sleep(4000);

        String searchStringOne = "service.name=\"TelemetryMetricsApp\"";
        String searchStringTwo = "service.name=\"TelemetryMetricsAppTwo\"";

        Assert.assertNotNull("could not find " + searchStringOne + "in messages.log", server.waitForStringInLogUsingMark(searchStringOne));
        Assert.assertNotNull("could not find " + searchStringTwo + "in messages.log", server.waitForStringInLogUsingMark(searchStringTwo));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
