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

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/MetricsTest")
public class MetricsTestServlet extends AbstractKafkaTestServlet {

    public static final String EMITTER_OUTGOING_CHANNEL = "MetricsEmitter";
    public static final String EMITTER_TOPIC = "EmitterTopic";
    public static final String OUTGOING_TOPIC = "OutgoingTopic";

    @Inject
    private MetricsDeliveryBean deliveryBean;

    @Inject
    private MetricsReceptionBeanForPayloads receptionBeanForPayloads;

    @Inject
    private MetricsReceptionBeanForMessages receptionBeanForMessages;

    @Inject
    @Channel(EMITTER_OUTGOING_CHANNEL)
    private Emitter<String> emitter;

    // called manually from MetricsTest
    public void emitterDeliverPayload() throws Exception {
        // deliver 5 payloads via an emitter
        for (int i = 1; i < 6; i++) {
            emitter.send("Message " + i);
        }

        receptionBeanForPayloads.assertReceivedMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
    }

    // called manually from MetricsTest
    public void deliverMessage() throws Exception {
        // deliver 5 messages via delivery and reception beans
        for (int i = 1; i < 6; i++) {
            deliveryBean.sendMessage("test");
        }

        receptionBeanForMessages.assertReceivedMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
    }
}
