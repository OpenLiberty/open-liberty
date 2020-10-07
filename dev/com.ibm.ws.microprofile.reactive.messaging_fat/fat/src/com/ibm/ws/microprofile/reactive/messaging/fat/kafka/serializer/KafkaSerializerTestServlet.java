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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

/**
 * Test that the kafka connector acknowledges messages and commits partition offsets correctly
 */
@WebServlet("/kafkaSerializerTest")
public class KafkaSerializerTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST_GROUPID = "test-group";

    @Test
    public void testMyData() throws Exception {

        KafkaReader<String, MyData> reader = readerFor(MyDataMessagingBean.OUT_CHANNEL);
        KafkaWriter<String, MyData> writer = writerFor(MyDataMessagingBean.IN_CHANNEL);

        try {
            writer.sendMessage(new MyData("abc", "123"));
            writer.sendMessage(new MyData("xyz", "456"));

            List<MyData> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

            assertThat(msgs, contains(new MyData("cba", "321"), new MyData("zyx", "654")));
        } finally {
            try {
                reader.close();
            } finally {
                writer.close();
            }
        }
    }

    /**
     * Obtain a KafkaReader for the given topic name
     * <p>
     * The returned reader expects String messages and uses the {@value #TEST_GROUPID} consumer group
     *
     * @param topicName the topic to read from
     * @return the reader
     */
    public KafkaReader<String, MyData> readerFor(String topicName) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, MyData> kafkaConsumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new MyDataDeserializer());
        KafkaReader<String, MyData> reader = new KafkaReader<String, MyData>(kafkaConsumer, topicName);
        return reader;
    }

    /**
     * Obtain a KafkaWriter for the given topic name
     * <p>
     * The returned writer writes String messages.
     *
     * @param topicName the topic to write to
     * @return the writer
     */
    public KafkaWriter<String, MyData> writerFor(String topicName) {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());

        KafkaProducer<String, MyData> kafkaProducer = new KafkaProducer<>(producerConfig, new StringSerializer(), new MyDataSerializer());
        KafkaWriter<String, MyData> writer = new KafkaWriter<String, MyData>(kafkaProducer, topicName);
        return writer;
    }

}
