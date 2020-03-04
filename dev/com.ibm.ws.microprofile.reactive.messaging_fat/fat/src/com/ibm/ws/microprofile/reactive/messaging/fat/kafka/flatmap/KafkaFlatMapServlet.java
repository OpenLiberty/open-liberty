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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.flatmap;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

/**
 *
 */
@WebServlet("/flatMapTest")
public class KafkaFlatMapServlet extends AbstractKafkaTestServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    public static final String IN_TOPIC = "flat-map-in";
    public static final String OUT_TOPIC = "flat-map-out";
    public static final String APP_GROUPID = "KafkaFlatMapTest-group";

    @Test
    public void testFlatMap() throws InterruptedException {
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(IN_TOPIC);
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(OUT_TOPIC);

        long offset = kafkaTestClient.getTopicOffset(IN_TOPIC, APP_GROUPID);

        writer.sendMessage("abc");
        writer.sendMessage("abcd");
        writer.sendMessage("abcde");
        writer.sendMessage("abcdef");

        List<String> messages = reader.assertReadMessages(2, DEFAULT_KAFKA_TIMEOUT);
        assertThat(messages, contains("abc", "abcde"));

        kafkaTestClient.assertTopicOffsetAdvancesTo(offset + 4, DEFAULT_KAFKA_TIMEOUT, IN_TOPIC, APP_GROUPID);
    }
}
