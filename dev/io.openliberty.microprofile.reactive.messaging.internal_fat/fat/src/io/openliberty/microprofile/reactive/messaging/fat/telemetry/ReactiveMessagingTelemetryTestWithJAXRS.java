/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.telemetry;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaStopServer;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider.CONNECTION_PROPERTIES_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.ReactiveMessagingApplication;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.ReactiveMessagingResource;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.RmTelemetryProcessingBean;
import io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry.RmTelemetryReceptionBean;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

/**
 * Assert that Reactive Messaging works as expected with Telemetry enabled on the server.
 */
@RunWith(FATRunner.class)
public class ReactiveMessagingTelemetryTestWithJAXRS extends FATServletClient {

    private static final String APP_NAME = "ReactiveMessagingTelemetryApp";
    public static final String SERVER_NAME = "RxMessagingServerWithTelemetry";
    private static final String TOPIC_NAME = "JAXRSTopic";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30);

    @BeforeClass
    public static void setup() throws Exception {

        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty(AbstractKafkaTestServlet.KAFKA_BOOTSTRAP_PROPERTY, KafkaTests.kafkaContainer.getBootstrapServers())
                        .include(ConnectorProperties.simpleIncomingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           RmTelemetryProcessingBean.CHANNEL_IN, "ReactiveMessagingTelemetryApp", TOPIC_NAME))
                        .include(ConnectorProperties.simpleOutgoingChannel(KafkaTests.connectionProperties(), ConnectorProperties.DEFAULT_CONNECTOR_ID,
                                                                           ReactiveMessagingResource.EMITTER_CHANNEL, TOPIC_NAME))
                        .addProperty(CONNECTION_PROPERTIES_KEY, KafkaTestClientProvider.encodeProperties(KafkaTests.connectionProperties()));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addClasses(ReactiveMessagingApplication.class, ReactiveMessagingResource.class, RmTelemetryProcessingBean.class, RmTelemetryReceptionBean.class)
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addPackage(AbstractKafkaTestServlet.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"), "META-INF/microprofile-config.properties")
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(KafkaTests.getAdminClient());
        }

    }

    @Test
    public void invokeEmitterViaREST() throws Exception {
        sendRequest("jaxrstest");
        String messages = receiveMessages();
        assertThat(messages, is("[JAXRSTEST1, JAXRSTEST2, JAXRSTEST3, JAXRSTEST4, JAXRSTEST5]"));
    }

    private void sendRequest(String message) throws Exception {
        // payload is sent emitter via REST call
        URL url = HttpUtils.createURL(server, APP_NAME + "/emitMessage");
        for (int i = 1; i < 6; i++) {
            HttpURLConnection conn = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HttpUtils.HTTPRequestMethod.POST);
            OutputStream os = conn.getOutputStream();
            os.write((message + i).getBytes());
            os.flush();
            assertThat(conn.getResponseCode(), is(204));
            os.close();
            conn.disconnect();
        }
    }

    private String receiveMessages() throws Exception {
        // payload is sent emitter via REST call
        URL url = HttpUtils.createURL(server, APP_NAME + "/receiveMessages");
        HttpURLConnection conn = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HttpUtils.HTTPRequestMethod.GET);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = in.readLine();
        in.close();
        conn.disconnect();
        return response;
    }
}
