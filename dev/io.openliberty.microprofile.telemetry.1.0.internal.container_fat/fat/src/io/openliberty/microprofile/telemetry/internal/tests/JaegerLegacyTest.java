/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;

/**
 * Test exporting traces to a Jaeger server with the legacy Jaeger protocol
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JaegerLegacyTest extends JaegerBaseTest {

    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));
    public static RepeatTests repeat = FATSuite.allMPRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(repeat);

    public static JaegerQueryClient client;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "jaeger");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_JAEGER_ENDPOINT, jaegerContainer.getJaegerLegacyUrl());

        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, "Test service");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing

        // Construct the test application
        WebArchive jaegerTest = ShrinkWrap.create(WebArchive.class, "spanTest.war")
                                          .addPackage(TestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, jaegerTest, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Override
    protected JaegerQueryClient getJaegerClient() {
        return client;
    }

}
