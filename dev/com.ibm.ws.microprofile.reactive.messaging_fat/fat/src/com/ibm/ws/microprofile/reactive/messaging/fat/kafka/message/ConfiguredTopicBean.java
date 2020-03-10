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

import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class ConfiguredTopicBean {

    public static final String TEST_PREFIX = "configured-topic_";

    public static final String CHANNEL_IN = TEST_PREFIX + "channel-in";
    public static final String CHANNEL_OUT = TEST_PREFIX + "channel-out";
    public static final String PRODUCER_RECORD_KEY = TEST_PREFIX + "key";
    public static final String PRODUCER_RECORD_VALUE = TEST_PREFIX + "value";
    public static final String GROUP_ID = TEST_PREFIX + "app-group";
    public static final String HEADER_KEY_PREFIX = TEST_PREFIX + "headerKey_";
    public static final String HEADER_VALUE_PREFIX = TEST_PREFIX + "headerValue_";
    public static final int NUM_HEADERS = 5;

    public static final String CONFIGURED_TOPIC = TEST_PREFIX + "configured-topic";
    public static final String PRODUCER_RECORD_TOPIC = TEST_PREFIX + "producer-record-topic";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public ProducerRecord<String, String> reverseString(String in) throws UnsupportedEncodingException {

        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(PRODUCER_RECORD_TOPIC, null, PRODUCER_RECORD_KEY, PRODUCER_RECORD_VALUE);
        for (int i = 0; i < NUM_HEADERS; i++) {
            producerRecord.headers().add(HEADER_KEY_PREFIX + i, (HEADER_VALUE_PREFIX + i).getBytes("UTF-8"));
        }

        return producerRecord;
    }

}
