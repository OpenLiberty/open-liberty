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

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

@WebServlet("/useConfiguredTopicTest")
public class UseConfiguredTopicServlet extends AbstractKafkaTestServlet {

    @Test
    public void testConfiguredTopic() throws UnsupportedEncodingException {
        String topicIn = ConfiguredTopicBean.CHANNEL_IN;
        String expectedTopicOut = ConfiguredTopicBean.CONFIGURED_TOPIC;
        String unexpectedTopicOut = ConfiguredTopicBean.PRODUCER_RECORD_TOPIC;
        String keyOut = ConfiguredTopicBean.PRODUCER_RECORD_KEY;
        String valueOut = ConfiguredTopicBean.PRODUCER_RECORD_VALUE;

        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(topicIn);
        String value = "hello"; //this value doesn't matter
        writer.sendMessage(value);

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(expectedTopicOut);
        List<ConsumerRecord<String, String>> expectedRecords = reader.readRecords(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        reader = kafkaTestClient.readerFor(unexpectedTopicOut);
        List<ConsumerRecord<String, String>> unexpectedRecords = reader.readRecords(1, KafkaTestConstants.EXPECTED_FAILURE_KAFKA_TIMEOUT);

        if (expectedRecords.size() == 1) {
            if (unexpectedRecords.size() != 0) {
                fail("Message may have been sent to more than one topic???");
            } else {
                ConsumerRecord<String, String> record = expectedRecords.get(0);
                assertEquals(keyOut, record.key());
                assertEquals(valueOut, record.value());
                assertEquals(expectedTopicOut, record.topic());

                Header[] headers = record.headers().toArray();
                assertEquals(ConfiguredTopicBean.NUM_HEADERS, headers.length);
                for (int i = 0; i < ConfiguredTopicBean.NUM_HEADERS; i++) {
                    Header header = headers[i];
                    String headerKey = header.key();
                    String headerValue = new String(header.value(), "UTF-8");
                    assertEquals(ConfiguredTopicBean.HEADER_KEY_PREFIX + i, headerKey);
                    assertEquals(ConfiguredTopicBean.HEADER_VALUE_PREFIX + i, headerValue);
                }
            }
        } else {
            if (unexpectedRecords.size() == 0) {
                fail("Message not found on any topic");
            } else {
                fail("Message was sent to wrong topic: " + unexpectedTopicOut);
            }
        }
    }

}
