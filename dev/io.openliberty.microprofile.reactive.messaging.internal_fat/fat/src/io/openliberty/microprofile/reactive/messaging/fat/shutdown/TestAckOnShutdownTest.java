/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.shutdown;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.reactive.messaging.fat.apps.shutdown.TestAckOnShutdownServlet;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
public class TestAckOnShutdownTest extends FATServletClient {

    public static final String APP_NAME = "TestAckOnShutdown";
    public static final String SERVER_NAME = "TestAckOnShutdownServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static final RepeatTests r = ReactiveMessagingActions.reactive30Repeats(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties inputConfig = simpleIncomingChannel(KafkaTests.connectionProperties(),
                                                                APP_NAME,
                                                                APP_NAME);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(inputConfig);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addPackage(TestAckOnShutdownServlet.class.getPackage())
                        .addPackage(AbstractReceptionBean.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testAckOnShutdown() throws Exception {
        AtomicBoolean stopSending = new AtomicBoolean(false);
        AtomicReference<Exception> senderException = new AtomicReference<>(null);
        // Tracks the Kafka offset of the last message successfully sent to the broker
        AtomicLong lastSentOffset = new AtomicLong(-1);

        KafkaTestClient kafkaTestClient = new KafkaTestClient(KafkaTests.kafkaContainer.getBootstrapServers());

        Thread sender = new Thread(() -> {
            try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(APP_NAME)) {
                int count = 0;
                while (!stopSending.get()) {
                    RecordMetadata meta = writer.sendMessage("test message " + ++count);
                    lastSentOffset.set(meta.offset());
                    Thread.sleep(10); // 1/100th of a second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                senderException.set(e);
                stopSending.set(true);
            }
        });

        sender.setDaemon(true);
        sender.start();

        // Let messages flow for 3 seconds while the app is running
        TimeUnit.SECONDS.sleep(3);

        // Shut down the server while messages are still being sent
        server.stopServer();

        // Signal the sender thread to stop and wait for it to finish
        stopSending.set(true);
        sender.join(TimeUnit.SECONDS.toMillis(10));

        // Propagate any exception thrown on the sender thread to fail the test
        Exception e = senderException.get();
        if (e != null) {
            throw e;
        }

        // The committed offset must be lastSentOffset + 1 (Kafka's committed offset
        // points to the next message to fetch, so acking offset N commits N+1).
        // This verifies that every message delivered to the broker before shutdown
        // was acknowledged by the application.
        long expectedCommittedOffset = lastSentOffset.get() + 1;
        kafkaTestClient.assertTopicOffsetAdvancesTo(expectedCommittedOffset,
                                                    KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT,
                                                    APP_NAME,
                                                    APP_NAME);
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

}
