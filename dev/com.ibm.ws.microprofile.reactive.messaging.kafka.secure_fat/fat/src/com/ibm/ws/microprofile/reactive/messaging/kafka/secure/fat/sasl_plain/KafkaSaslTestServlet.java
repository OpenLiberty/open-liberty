/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.sasl_plain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;

@WebServlet("/KafkaTlsTestServlet")
@ApplicationScoped
public class KafkaSaslTestServlet extends FATServlet {

    @Inject
    private KafkaTestClient kafkaTestClient;

    private static final long serialVersionUID = 1L;

    @Test
    public void testTls() {
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(BasicMessagingBean.CHANNEL_OUT);
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(BasicMessagingBean.CHANNEL_IN);

        writer.sendMessage("abc");
        writer.sendMessage("xyz");

        List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        assertThat(msgs, contains("cba", "zyx"));
    }

}
