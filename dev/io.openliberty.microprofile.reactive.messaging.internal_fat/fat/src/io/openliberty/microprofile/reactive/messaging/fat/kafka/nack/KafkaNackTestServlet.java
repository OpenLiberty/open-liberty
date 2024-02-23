/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.nack;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/NackTest")
public class KafkaNackTestServlet extends FATServlet {

    @Inject
    private KafkaNackTestDeliveryBean deliveryBean;

    @Inject
    private KafkaNackReceptionBean receptionBean;

    @Inject
    private KafkaTestClient kafkaTestClient;

    /**
     * Check that if a message sent to Kafka is not sent, it is nacked
     */
    @Test
    public void testUndeliveredMessageIsNacked() {
        CompletableFuture<Void> cf = deliveryBean.sendMessage("test");
        try {
            cf.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
            // Assert something about the exception?
        } catch (Exception e) {
            throw new AssertionError("Undelivered message was not nacked", e);
        }
    }

    // Test is driven manually by test class
    public void testIncomingMessageCanBeNacked() throws Exception {

        // Send messages to the topic
        try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(KafkaNackReceptionBean.CHANNEL_IN)) {
            for (int i = 1; i <= 5; i++) {
                writer.sendMessage("test message " + i);
            }
        }

        // Wait for them to arrive
        List<Message<String>> messages = receptionBean.assertReceivedMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        // Nack some of the received messages
        messages.get(0).ack();
        messages.get(1).ack();
        messages.get(2).nack(new KafkaNackTestException("Test exception 1"));
        messages.get(3).nack(new KafkaNackTestException("Test exception 2"));
        messages.get(4).ack();

        // Further asserts in test class
    }
}
