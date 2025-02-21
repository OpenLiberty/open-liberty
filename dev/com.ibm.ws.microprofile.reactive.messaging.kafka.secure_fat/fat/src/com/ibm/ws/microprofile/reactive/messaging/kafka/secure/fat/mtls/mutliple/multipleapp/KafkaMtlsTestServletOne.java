/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.multipleapp;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.mtls.mutliple.apps.MessagingBeanOne;
import componenttest.app.FATServlet;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@WebServlet("/KafkaMtlsServletOne")
@ApplicationScoped
public class KafkaMtlsTestServletOne extends FATServlet {

    @Inject
    private KafkaTestClient kafkaTestClient;

    private static final long serialVersionUID = 1L;

    /**
     * Manually invoked from the test class
     */
    public void putMessages() throws Exception {
        try(KafkaReader<String, String> reader = kafkaTestClient.readerFor(MessagingBeanOne.CHANNEL_OUT);
            KafkaWriter<String, String> writer = kafkaTestClient.writerFor(MessagingBeanOne.CHANNEL_IN)){

            writer.sendMessage("abc");
            writer.sendMessage("xyz");

            List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

            assertThat(msgs, contains("cba", "zyx"));
        }
    }
}
