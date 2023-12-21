/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.reactive.messaging.fat.kafka.emitter.basic;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@WebServlet("/KafkaEmitterMessageTestServlet")
public class KafkaEmitterTestServlet extends AbstractKafkaTestServlet {

    public static final String CHANNEL_NAME = "emitter-test-outgoing-message";
    public static final String CHANNEL_NAME2 = "emitter-test-outgoing-payload";

    @Inject
    @Channel(CHANNEL_NAME)
    private Emitter<String> emitter;

    @Inject
    @Channel(CHANNEL_NAME2)
    private Emitter<String> emitter2;

    @Test
    public void testEmitterMessage() {
        for (int i = 1; i <= 5; i++) {
            emitter.send(Message.of("message" + i));
        }

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(CHANNEL_NAME);
        List<String> messages = reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        assertThat(messages, contains("message1", "message2", "message3", "message4", "message5"));
    }

    @Test
    public void testEmitterPayload() {
        for (int i = 1; i <= 5; i++) {
            emitter.send("payload" + i);
        }

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(CHANNEL_NAME);
        List<String> messages = reader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        assertThat(messages, contains("payload1", "payload2", "payload3", "payload4", "payload5"));
    }

}
