/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import java.io.File;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.security.utils.SSLUtils;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorContainer;

/**
 * Test exporting traces to a Jaeger server with OpenTelemetry Collector
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JaegerSecureOtelCollectorTest extends JaegerBaseTest {

    public static final int OTLP_GRPC_PORT = 4317;
    private static File privateKeyFile;
    private static File certificateFile;
    private static boolean createdSSLStuff = false;

    public static Network network = Network.newNetwork();

    public static JaegerContainer jaegerContainer = new JaegerContainer()
                                                                         .withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"))
                                                                         .withNetwork(network)
                                                                         .withNetworkAliases("jaeger-all-in-one");

    public static OtelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(new File("lib/LibertyFATTestFiles/otel-collector-config-jaeger-secure.yaml"),
                                                                                             getCertificate(), getKey())
                                                                                                                        .withNetwork(network)
                                                                                                                        .withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class,
                                                                                                                                                               "otelCol"))
                                                                                                                        .withNetworkAliases("otel-collector-jaeger");

    public static RepeatTests repeat = FATSuite.allMPRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain
                                             .outerRule(network)
                                             .around(jaegerContainer)
                                             .around(otelCollectorContainer)
                                             .around(repeat);

    public static JaegerQueryClient client;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, otelCollectorContainer.getSecureOtlpGrpcUrl());
        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, "Test service");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_CERTIFICATE, certificateFile.getAbsolutePath());

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

    private static File getKey() {
        generateSSLStuff();
        return privateKeyFile;
    }

    private static File getCertificate() {
        generateSSLStuff();
        return certificateFile;
    }

    private synchronized static void generateSSLStuff() {
        if (createdSSLStuff) {
            return;
        }

        try {
            KeyPair generatedKeyPair = SSLUtils.generateKeyPair();

            String dockerIP = DockerClientFactory.instance().dockerHostIpAddress();
            String dnName = "O=Evil Inc Test Certificate, CN=" + dockerIP + ", L=Toronto,C=CA";
            List<String> genericNameList = new ArrayList<String>();
            genericNameList.add(dockerIP);

            Certificate certificateObject = SSLUtils.selfSign(generatedKeyPair, dnName, genericNameList);

            String pathToPrivateKey = server.getServerSharedPath() + "/private.key";
            privateKeyFile = new File(pathToPrivateKey);
            SSLUtils.exportPrivateKeyToFile(privateKeyFile, generatedKeyPair);

            String pathToCertificate = server.getServerSharedPath() + "/certificate.crt";
            certificateFile = new File(pathToCertificate);
            SSLUtils.exportCertificateToFile(certificateFile, certificateObject);
            createdSSLStuff = true;
        } catch (Exception e) { //If we get an exception let the test fail and show the developer what went wrong
            throw new RuntimeException("Exception doing SSLStuff", e);
        }
    }

}
