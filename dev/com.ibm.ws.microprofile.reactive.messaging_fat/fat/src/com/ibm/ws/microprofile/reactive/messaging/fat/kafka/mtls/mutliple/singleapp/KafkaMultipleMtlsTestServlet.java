/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.singleapp;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.apps.MessagingBeanOne;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.mtls.mutliple.apps.MessagingBeanTwo;
import componenttest.app.FATServlet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@WebServlet("/KafkaMultipleMtlsTestServlet")
@ApplicationScoped
public class KafkaMultipleMtlsTestServlet extends FATServlet {

    @Inject
    private KafkaTestClient kafkaTestClient;

    private static final long serialVersionUID = 1L;

    public void testMtls() throws Exception {
        try(KafkaWriter<String, String> writer1 = kafkaTestClient.writerFor(MessagingBeanOne.CHANNEL_IN);
            KafkaWriter<String, String> writer2 = kafkaTestClient.writerFor(MessagingBeanTwo.CHANNEL_IN)){

            writer1.sendMessage("abc");
            writer1.sendMessage("xyz");

            writer2.sendMessage("def");
            writer2.sendMessage("uvw");

            // Due to the behavior of the readers, if the first reads from its topic,
            // it must be closed before the second can start to read from its topic due to some locking
            try (KafkaReader<String, String> reader1 = kafkaTestClient.readerFor(MessagingBeanOne.CHANNEL_OUT)){
                List<String> msgs1 = reader1.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
                assertThat(msgs1, contains("cba", "zyx"));
            }

            try (KafkaReader<String, String> reader2 = kafkaTestClient.readerFor(MessagingBeanTwo.CHANNEL_OUT)){
                List<String> msgs2 = reader2.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
                assertThat(msgs2, contains("DEF", "UVW"));
            }
        }
    }
}
