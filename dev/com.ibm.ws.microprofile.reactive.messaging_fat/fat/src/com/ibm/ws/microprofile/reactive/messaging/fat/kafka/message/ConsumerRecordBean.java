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

import java.io.UnsupportedEncodingException;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class ConsumerRecordBean {

    public static final String CHANNEL_IN = "consumer-record-in";
    public static final String CHANNEL_OUT = "consumer-record-out";
    public static final String GROUP_ID = "consumer-record-app-group";

    public static final String TOPIC = CHANNEL_IN;
    public static final String KEY = "test_key";
    public static final String VALUE = "hello";
    //public static final int PARTITION = 9;
    public static final long TIMESTAMP = 100L;
    public static final int NUM_HEADERS = 5;
    public static final String HEADER_KEY_PREFIX = "headerKey";
    public static final String HEADER_VALUE_PREFIX = "headerKey";
    public static final String PASS = "PASS";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public Message<String> checkConsumerRecord(Message<String> in) throws UnsupportedEncodingException {

        @SuppressWarnings("unchecked")
        ConsumerRecord<String, String> consumerRecord = in.unwrap(ConsumerRecord.class);

        String incomingKey = consumerRecord.key();
        String incomingValue = consumerRecord.value();
        String incomingTopic = consumerRecord.topic();
        //int incomingPartition = consumerRecord.partition();
        long incomingTimestamp = consumerRecord.timestamp();
        Header[] incomingHeaders = consumerRecord.headers().toArray();

        if (!KEY.equals(incomingKey)) {
            return Message.of("Wrong ConsumerRecord Key. Expected: " + KEY + " - Actual: " + incomingKey);
        }

        if (!VALUE.equals(incomingValue)) {
            return Message.of("Wrong ConsumerRecord Value. Expected: " + VALUE + " - Actual: " + incomingValue);
        }

        if (!TOPIC.equals(incomingTopic)) {
            return Message.of("Wrong ConsumerRecord Topic. Expected: " + TOPIC + " - Actual: " + incomingTopic);
        }

//        if (PARTITION != incomingPartition) {
//            return Message.of("Wrong ConsumerRecord Partition. Expected: " + PARTITION + " - Actual: " + incomingPartition);
//        }

        if (TIMESTAMP != incomingTimestamp) {
            return Message.of("Wrong ConsumerRecord Timestamp. Expected: " + TIMESTAMP + " - Actual: " + incomingTimestamp);
        }

        if (NUM_HEADERS != incomingHeaders.length) {
            return Message.of("Wrong number of ConsumerRecord Headers. Expected: " + NUM_HEADERS + " - Actual: " + incomingHeaders.length);
        }

        for (int i = 0; i < NUM_HEADERS; i++) {
            Header header = incomingHeaders[i];
            String incomingHeaderKey = header.key();
            String expectedHeaderKey = HEADER_KEY_PREFIX + i;
            if (!expectedHeaderKey.equals(incomingHeaderKey)) {
                return Message.of("Wrong ConsumerRecord Header Key. Expected: " + expectedHeaderKey + " - Actual: " + incomingHeaderKey);
            }

            String incomingHeaderValue = new String(header.value(), "UTF-8");
            String expectedHeaderValue = HEADER_VALUE_PREFIX + i;
            if (!expectedHeaderKey.equals(incomingHeaderKey)) {
                return Message.of("Wrong ConsumerRecord Header Value. Expected: " + expectedHeaderValue + " - Actual: " + incomingHeaderValue);
            }
        }

        return Message.of(PASS);
    }

}
