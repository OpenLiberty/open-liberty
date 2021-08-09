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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

@WebServlet("/consumerRecordTest")
public class ConsumerRecordServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    @Test
    public void testConsumerRecord() throws UnsupportedEncodingException {
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(ConsumerRecordBean.CHANNEL_IN);

        // Server uses the timestamp for things like message expiry, so make it approximately accurate
        // This test relies on the following server config:
        // * message.timestamp.type = CreateTime (default)
        // * message.timestamp.difference.max.ms > 500 (default is MAX_LONG)
        long timestamp = System.currentTimeMillis() + 500L;

        ProducerRecord<String, String> record = new ProducerRecord<String, String>(ConsumerRecordBean.CHANNEL_IN, ConsumerRecordBean.PARTITION, timestamp, ConsumerRecordBean.KEY, ConsumerRecordBean.VALUE);

        for (int i = 0; i < ConsumerRecordBean.NUM_TEST_HEADERS; i++) {
            record.headers().add(ConsumerRecordBean.HEADER_KEY_PREFIX + i, (ConsumerRecordBean.HEADER_VALUE_PREFIX + i).getBytes("UTF-8"));
        }

        ByteBuffer timestampBuffer = ByteBuffer.allocate(Long.BYTES).putLong(timestamp);
        record.headers().add(ConsumerRecordBean.EXPECTED_TIMESTAMP_HEADER_KEY, timestampBuffer.array());
        writer.sendMessage(record, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(ConsumerRecordBean.CHANNEL_OUT);
        List<String> msgs = reader.assertReadMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        assertEquals(1, msgs.size());

        String msg = msgs.get(0);
        assertEquals(msg, ConsumerRecordBean.PASS, msg);
    }

}
