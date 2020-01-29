/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.delivery;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

/**
 * Test that the kafka connector acknowledges messages and commits partition offsets correctly
 */
@WebServlet("/kafkaAcknowledgementTest")
public class KafkaAcknowledgementTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    /**
     * The kafka consumner group configured in the app config to be used by the connector
     * <p>
     * Must be referenced when checking the offset committed by the connector
     */
    public static final String APP_GROUPID = "acknowledgement-app-group";

    @Inject
    private KafkaDeliveryBean deliveryBean;

    @Inject
    private KafkaReceptionBean receptionBean;

    @Test
    public void testDeliveryAndAcknowledgement() {
        // Send a message
        CompletableFuture<Void> acknowledged = deliveryBean.sendMessage("test1");

        // Assert that message appears on topic
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(KafkaDeliveryBean.CHANNEL_NAME);
        List<String> messages = reader.assertReadMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        assertThat(messages, contains("test1"));

        // Assert that message has been acknowledged
        assertThat(acknowledged.isDone(), is(true));
    }

    @Test
    public void testReceptionAndAcknowledgement() throws InterruptedException {
        // Baseline the offset
        long offset = kafkaTestClient.getTopicOffset(KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);

        // Send message directly
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(KafkaReceptionBean.CHANNEL_NAME);
        writer.sendMessage("test1");

        // Assert that message is received
        List<Message<String>> messages = receptionBean.assertReceivedMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        Message<String> message = messages.get(0);
        assertThat(message.getPayload(), is("test1"));

        // Assert that partition offset does not get committed
        long currentOffset = kafkaTestClient.getTopicOffset(KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);
        assertEquals(offset, currentOffset);

        // Acknowledge received message
        message.ack();

        // Assert that partition offset does get committed
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);
    }

    @Test
    public void testOutOfOrderAcknowledgement() throws InterruptedException {
        long offset = kafkaTestClient.getTopicOffset(KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);
        // Send three messages directly
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(KafkaReceptionBean.CHANNEL_NAME);
        writer.sendMessage("test1");
        writer.sendMessage("test2");
        writer.sendMessage("test3");

        // Assert messages are received
        List<Message<String>> messages = receptionBean.assertReceivedMessages(3, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        List<String> payloads = messages.stream().map(Message::getPayload).collect(toList());
        assertThat(payloads, contains("test1", "test2", "test3"));

        // Acknowledge messages 1 and 3
        messages.get(0).ack();
        messages.get(2).ack();

        // Assert that partition offset gets committed to 1
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);

        // Acknowledge message 2
        messages.get(1).ack();

        // Assert that partition offset gets committed to 3
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 3, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaReceptionBean.CHANNEL_NAME, APP_GROUPID);
    }
}
