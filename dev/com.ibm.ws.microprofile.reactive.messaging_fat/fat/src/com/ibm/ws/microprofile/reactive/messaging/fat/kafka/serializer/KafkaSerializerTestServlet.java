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

import java.time.Duration;
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

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaWriter;

/**
 * Test that the kafka connector acknowledges messages and commits partition offsets correctly
 */
@WebServlet("/kafkaAcknowledgementTest")
public class KafkaSerializerTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST_GROUPID = "test-group";

    @Test
    public void testMyData() throws Exception {

        SimpleKafkaReader<MyData> reader = readerFor(MyDataMessagingBean.OUT_CHANNEL);
        SimpleKafkaWriter<MyData> writer = writerFor(MyDataMessagingBean.IN_CHANNEL);

        try {
            writer.sendMessage(new MyData("abc", "123"));
            writer.sendMessage(new MyData("xyz", "456"));

            List<MyData> msgs = reader.waitForMessages(2, Duration.ofSeconds(5));

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
     * Obtain a SimpleKafkaReader for the given topic name
     * <p>
     * The returned reader expects String messages and uses the {@value #TEST_GROUPID} consumer group
     *
     * @param topicName the topic to read from
     * @return the reader
     */
    public SimpleKafkaReader<MyData> readerFor(String topicName) {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUPID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, MyData> kafkaConsumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new MyDataDeserializer());
        SimpleKafkaReader<MyData> reader = new SimpleKafkaReader<MyData>(kafkaConsumer, topicName);
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
    public SimpleKafkaWriter<MyData> writerFor(String topicName) {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrap());

        KafkaProducer<String, MyData> kafkaProducer = new KafkaProducer<>(producerConfig, new StringSerializer(), new MyDataSerializer());
        SimpleKafkaWriter<MyData> writer = new SimpleKafkaWriter<MyData>(kafkaProducer, topicName);
        return writer;
    }

}
