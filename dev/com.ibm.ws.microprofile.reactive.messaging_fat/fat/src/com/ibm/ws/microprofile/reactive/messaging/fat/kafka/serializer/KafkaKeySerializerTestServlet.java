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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.ExtendedKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.ExtendedKafkaWriter;

/**
 * Test that the kafka connector acknowledges messages and commits partition offsets correctly
 */
@WebServlet("/kafkaKeySerializerTest")
public class KafkaKeySerializerTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST_GROUPID = "test-group";

    @Test
    public void testMyDataKey() throws Exception {

        ExtendedKafkaReader<MyData, MyData> reader = extReaderFor(MyDataMessagingBean.OUT_CHANNEL);
        ExtendedKafkaWriter<MyData, MyData> writer = extWriterFor(MyDataMessagingBean.IN_CHANNEL);

        try {
            writer.sendMessage(new MyData("abc", "123"), new MyData("abc", "123"));
            writer.sendMessage(new MyData("xyz", "456"), new MyData("xyz", "456"));

            List<ConsumerRecord<MyData, MyData>> records = reader.waitForRecords(2, Duration.ofSeconds(5));
            for (ConsumerRecord<MyData, MyData> r : records) {
                //since MP Reactive Messaging does not handle keys, they will get passed through as null but to prove
                //that the key serializer and deserializer is properly configured, they have special case code for null
                assertEquals(MyData.NULL, r.key());
            }

        } finally {
            try {
                reader.close();
            } finally {
                writer.close();
            }
        }
    }

    /**
     * Obtain a SimpleKafkaReader for the given topic name
     * <p>
     * The returned reader expects String messages and uses the {@value #TEST_GROUPID} consumer group
     *
     * @param topicName the topic to read from
     * @return the reader
     */
    public ExtendedKafkaReader<MyData, MyData> extReaderFor(String topicName) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<MyData, MyData> kafkaConsumer = new KafkaConsumer<>(consumerConfig, new MyDataDeserializer(), new MyDataDeserializer());
        ExtendedKafkaReader<MyData, MyData> reader = new ExtendedKafkaReader<MyData, MyData>(kafkaConsumer, topicName);
        return reader;
    }

    /**
     * Obtain a SimpleKafkaWriter for the given topic name
     * <p>
     * The returned writer writes String messages.
     *
     * @param topicName the topic to write to
     * @return the writer
     */
    public ExtendedKafkaWriter<MyData, MyData> extWriterFor(String topicName) {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());

        KafkaProducer<MyData, MyData> kafkaProducer = new KafkaProducer<>(producerConfig, new MyDataSerializer(), new MyDataSerializer());
        ExtendedKafkaWriter<MyData, MyData> writer = new ExtendedKafkaWriter<MyData, MyData>(kafkaProducer, topicName);
        return writer;
    }

}
