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

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class ProducerRecordBean {

    public static final String TEST_PREFIX = "producer-record_";

    public static final String CHANNEL_IN = TEST_PREFIX + "channel-in";
    public static final String CHANNEL_OUT = TEST_PREFIX + "channel-out";
    public static final String PRODUCER_RECORD_KEY = TEST_PREFIX + "key";
    public static final String PRODUCER_RECORD_VALUE = TEST_PREFIX + "value";
    public static final String GROUP_ID = TEST_PREFIX + "app-group";

    public static final String CONFIGURED_TOPIC = TEST_PREFIX + "configured-topic";
    public static final String PRODUCER_RECORD_TOPIC = TEST_PREFIX + "producer-record-topic";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public ProducerRecord<String, String> reverseString(String in) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(PRODUCER_RECORD_TOPIC, PRODUCER_RECORD_KEY, PRODUCER_RECORD_VALUE);
        return producerRecord;
    }

}
