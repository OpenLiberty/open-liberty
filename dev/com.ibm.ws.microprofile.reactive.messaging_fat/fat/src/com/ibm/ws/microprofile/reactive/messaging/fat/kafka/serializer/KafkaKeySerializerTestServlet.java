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

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

/**
 * Test that the kafka connector correctly passes on the configured custom serializers and deserializers
 */
@WebServlet("/kafkaKeySerializerTest")
public class KafkaKeySerializerTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST_GROUPID = "test-group";

    private static final MyData KEY1 = new MyData("key1a", "key1b");
    private static final MyData VALUE1 = new MyData("value1a", "value1b");
    private static final MyData KEY2 = new MyData("key2a", "key2b");
    private static final MyData VALUE2 = new MyData("value2a", "value2b");

    @Test
    public void testMyDataKey() throws Exception {

        KafkaReader<MyData, MyData> reader = extReaderFor(MyDataMessagingBean2.OUT_CHANNEL);
        KafkaWriter<MyData, MyData> writer = extWriterFor(MyDataMessagingBean2.IN_CHANNEL);

        try {
            writer.sendMessage(KEY1, VALUE1);
            writer.sendMessage(KEY2, VALUE2);

            List<ConsumerRecord<MyData, MyData>> records = reader.assertReadRecords(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            //since MP Reactive Messaging does not handle keys, they will get passed through as null but to prove
            //that the key serializer and deserializer is properly configured, they have special case code for null
            ConsumerRecord<MyData, MyData> r1 = records.get(0);
            assertEquals(MyData.NULL, r1.key());
            assertEquals(VALUE1.reverse(), r1.value());

            ConsumerRecord<MyData, MyData> r2 = records.get(1);
            assertEquals(MyData.NULL, r2.key());
            assertEquals(VALUE2.reverse(), r2.value());

        } finally {
            try {
                reader.close();
            } finally {
                writer.close();
            }
        }
    }

    @Test
    public void testReverseMyData() {
        MyData expected = new MyData("a1yek", "b1yek");

        MyData reversed1 = MyData.reverse(KEY1);
        assertEquals(expected, reversed1);

        MyData reversed2 = KEY1.reverse();
        assertEquals(expected, reversed2);
    }

    /**
     * Obtain a ExtendedKafkaReader for the given topic name
     * <p>
     * The returned reader expects MyData keys and messages and uses the {@value #TEST_GROUPID} consumer group
     *
     * @param topicName the topic to read from
     * @return the reader
     */
    public KafkaReader<MyData, MyData> extReaderFor(String topicName) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<MyData, MyData> kafkaConsumer = new KafkaConsumer<>(consumerConfig, new MyDataDeserializer(), new MyDataDeserializer());
        KafkaReader<MyData, MyData> reader = new KafkaReader<MyData, MyData>(kafkaConsumer, topicName);
        return reader;
    }

    /**
     * Obtain a ExtendedKafkaReader for the given topic name
     * <p>
     * The returned writer writes MyData keys and messages.
     *
     * @param topicName the topic to write to
     * @return the writer
     */
    public KafkaWriter<MyData, MyData> extWriterFor(String topicName) {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());

        KafkaProducer<MyData, MyData> kafkaProducer = new KafkaProducer<>(producerConfig, new MyDataSerializer(), new MyDataSerializer());
        KafkaWriter<MyData, MyData> writer = new KafkaWriter<MyData, MyData>(kafkaProducer, topicName);
        return writer;
    }

}
