/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.metrics;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/MetricsTest")
public class MetricsTestServlet extends AbstractKafkaTestServlet {

    @Inject
    MetricsReceptionBean emitterReception;

    @Inject
    MetricsDeliveryBean deliveryBean;

    @Inject
    @Channel(EMITTER_OUTGOING_CHANNEL)
    public Emitter<String> emitter;

    public static final String EMITTER_OUTGOING_CHANNEL = "MetricsEmitter";
    public static final String EMITTER_TOPIC = "EmitterTopic";
    public static final String OUTGOING_TOPIC = "OutgoingTopic";

    public void emitterDeliverPayload() {
        // deliver 5 payloads via an emitter
        for (int i = 1; i < 6; i++) {
            emitter.send("Message " + i);
        }

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(EMITTER_TOPIC);
        reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
    }

    public void deliverMessage() {
        // deliver 5 messages via delivery and reception beans
        for (int i = 1; i < 6; i++) {
            deliveryBean.sendMessage("test");
        }
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(OUTGOING_TOPIC);
        reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
    }
}
