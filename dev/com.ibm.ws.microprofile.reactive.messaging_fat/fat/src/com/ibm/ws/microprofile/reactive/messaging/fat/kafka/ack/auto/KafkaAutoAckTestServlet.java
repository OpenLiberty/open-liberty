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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.ack.auto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;

@WebServlet("/kafkaAutoAckTest")
public class KafkaAutoAckTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * The kafka consumner group configured in the app config to be used by the connector
     * <p>
     * Must be referenced when checking the offset committed by the connector
     */
    public static final String APP_GROUPID = "auto-ack-app-group";

    @Inject
    private KafkaTestClient kafkaTestClient;

    @Inject
    private KafkaAutoAckReceptionBean receptionBean;

    @Test
    public void testReceptionWithoutAck() throws InterruptedException {
        // Baseline the offset
        long offset = kafkaTestClient.getTopicOffset(KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID);

        // Send message directly
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(KafkaAutoAckReceptionBean.CHANNEL_IN);
        writer.sendMessage("test1");

        // Assert message received
        Message<String> message1 = receptionBean.assertReceivedMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT).get(0);
        assertThat(message1.getPayload(), is("test1"));

        // Assert that the partition offset is committed
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID);

        // Send another message
        writer.sendMessage("test2");

        // Assert message received
        Message<String> message2 = receptionBean.assertReceivedMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT).get(0);
        assertThat(message2.getPayload(), is("test2"));

        // Assert the partition offset is committed
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID);

        // Ack message 1 and assert the partition offset does not go backwards
        message1.ack();
        Thread.sleep(1000);
        assertThat(kafkaTestClient.getTopicOffset(KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID), is(offset + 2));
    }

    /**
     * Check that the unacked message limit is ignored when enable.auto.commit is true
     */
    @Test
    public void testUnackedLimitIgnored() throws InterruptedException {
        // Baseline the offset
        long offset = kafkaTestClient.getTopicOffset(KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID);

        // Send 30 messages
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(KafkaAutoAckReceptionBean.CHANNEL_IN);
        for (int i = 0; i < 30; i++) {
            writer.sendMessage("test-" + i);
        }

        // Assert 10 messages received
        receptionBean.assertReceivedMessages(30, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        // Assert that the partition offset is committed
        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 30, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT, KafkaAutoAckReceptionBean.CHANNEL_IN, APP_GROUPID);
    }

}
