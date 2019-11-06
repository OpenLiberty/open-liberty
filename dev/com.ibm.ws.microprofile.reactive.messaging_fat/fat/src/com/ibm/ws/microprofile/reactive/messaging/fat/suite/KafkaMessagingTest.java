/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class KafkaMessagingTest {

    private static final String APP_NAME = "basicKafkaTest";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    private static KafkaConsumer<String, String> kafkaConsumer;
    private static KafkaProducer<String, String> kafkaProducer;

    @BeforeClass
    public static void setup() throws Exception {
        PropertiesAsset config = new PropertiesAsset()
                        .include(simpleOutgoingChannel(PlaintextTests.kafkaContainer, "test-out"))
                        .include(simpleIncomingChannel(PlaintextTests.kafkaContainer, "test-in", "test-consumer"));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties")
                        .addAsManifestResource(KafkaUtils.kafkaPermissions(), "permissions.xml")
                        .addAsLibraries(kafkaClientLibs());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @BeforeClass
    public static void initializeKafkaClients() {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "testClient");
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaConsumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new StringDeserializer());

        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        kafkaProducer = new KafkaProducer<>(producerConfig, new StringSerializer(), new StringSerializer());
    }

    @Test
    public void testBasic() throws InterruptedException, ExecutionException, TimeoutException {

        kafkaConsumer.subscribe(Collections.singleton("test-out"));

        ProducerRecord<String, String> testRecord = new ProducerRecord<String, String>("test-in", "abc");
        kafkaProducer.send(testRecord).get(30, TimeUnit.SECONDS);
        ProducerRecord<String, String> testRecord2 = new ProducerRecord<String, String>("test-in", "xyz");
        kafkaProducer.send(testRecord2).get(30, TimeUnit.SECONDS);

        List<ConsumerRecord<String, String>> records = pollForRecords(kafkaConsumer, 2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        Collection<String> values = records.stream()
                        .map(r -> r.value())
                        .collect(Collectors.toList());

        assertThat(values, contains("cba", "zyx"));
    }

    /**
     * Poll Kafka until the desired number of records is received
     *
     * @param <K>      Key type of kafka record
     * @param <V>      Value type of kafka record
     * @param consumer the kafka consumer
     * @param count    the number of records expected
     * @param timeout  the amount of time to wait for the expected number of records to be received
     * @return the list of records received
     */
    private static <K, V> List<ConsumerRecord<K, V>> pollForRecords(KafkaConsumer<K, V> consumer, int count, Duration timeout) {
        ArrayList<ConsumerRecord<K, V>> result = new ArrayList<>();
        Duration remaining = timeout;
        long startTime = System.nanoTime();
        while (!remaining.isNegative() && result.size() < count) {
            for (ConsumerRecord<K, V> record : consumer.poll(remaining)) {
                result.add(record);
            }
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            remaining = timeout.minus(elapsed);
        }
        assertThat("Wrong number of records fetched from kafka", result, hasSize(count));
        return result;
    }

    @AfterClass
    public static void teardownConsumer() {
        kafkaConsumer.close();
    }

    @AfterClass
    public static void testdownProducer() {
        kafkaProducer.close();
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        server.stopServer();
    }

}
