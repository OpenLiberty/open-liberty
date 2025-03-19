/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.liberty_login.xor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;

@WebServlet("/KafkaLoginModuleXorTestServlet")
@ApplicationScoped
public class LibertyLoginModuleXorTestServlet extends FATServlet {

    public static final String TEST_USER_PROPERTY = "kafka.test.user";
    public static final String TEST_SECRET_PROPERTY = "kafka.test.secret";

    @Inject
    @ConfigProperty(name = TEST_USER_PROPERTY)
    private String testUser;

    @Inject
    @ConfigProperty(name = TEST_SECRET_PROPERTY)
    private String testSecret;

    @Inject
    private KafkaTestClient kafkaTestClient;

    private static final long serialVersionUID = 1L;

    @Test
    public void testLoginModuleXor() {
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(XorTestBean.CHANNEL_OUT);
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(XorTestBean.CHANNEL_IN);

        writer.sendMessage("abc");
        writer.sendMessage("xyz");

        List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        assertThat(msgs, contains("xor-abc", "xor-xyz"));
    }

}
