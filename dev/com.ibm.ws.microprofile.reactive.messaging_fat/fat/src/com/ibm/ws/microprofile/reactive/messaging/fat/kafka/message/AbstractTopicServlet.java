/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaWriter;

public class AbstractTopicServlet extends AbstractKafkaTestServlet {

    public void testTopic(String topicIn, String expectedTopicOut, String unexpectedTopicOut, String keyOut, String valueOut) {
        SimpleKafkaWriter<String> writer = kafkaTestClient.writerFor(topicIn);
        String value = "hello"; //this value doesn't matter
        writer.sendMessage(value);

        SimpleKafkaReader<String> reader = kafkaTestClient.readerFor(expectedTopicOut);
        List<ConsumerRecord<String, String>> records = reader.waitForRecords(1, Duration.ofSeconds(1), false);

        if (records.size() != 1) {
            reader = kafkaTestClient.readerFor(unexpectedTopicOut);
            records = reader.waitForRecords(1, Duration.ofSeconds(1), false);

            if (records.size() == 1) {
                fail("Message was sent to wrong topic: " + unexpectedTopicOut);
            } else {
                fail("Message not found on any topic");
            }
        }

        ConsumerRecord<String, String> record = records.get(0);
        assertEquals(keyOut, record.key());
        assertEquals(valueOut, record.value());
        assertEquals(expectedTopicOut, record.topic());

        reader = kafkaTestClient.readerFor(unexpectedTopicOut);
        records = reader.waitForRecords(1, Duration.ofSeconds(1), false);

        assertEquals("Message may have been sent to more than one topic???", 0, records.size());
    }

}
