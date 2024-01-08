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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.List;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/MultiAppMetricsTestServletOne")
@ApplicationScoped
public class MultiAppMetricsTestServletOne extends AbstractKafkaTestServlet {

    public static final String INCOMING_TOPIC_NAME = "AppOneTopicOne";
    public static final String OUTGOING_TOPIC_NAME = "AppOneTopicTwo";

    @Inject
    private KafkaTestClient kafkaTestClient;

    // called manually from MultiAppMetricsTest
    public void stringToUpperCase() throws Exception {
        try (KafkaReader<String, String> reader = kafkaTestClient.readerFor(OUTGOING_TOPIC_NAME);
                        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(INCOMING_TOPIC_NAME)) {

            writer.sendMessage("def");
            writer.sendMessage("uvw");

            List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

            assertThat(msgs, contains("DEF", "UVW"));
        }
    }
}
