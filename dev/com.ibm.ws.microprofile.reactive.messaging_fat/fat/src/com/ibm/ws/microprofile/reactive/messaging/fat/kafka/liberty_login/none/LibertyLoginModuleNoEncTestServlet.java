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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.liberty_login.none;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaWriter;

import componenttest.app.FATServlet;

@WebServlet("/KafkaLoginModuleNoEncTestServlet")
@ApplicationScoped
public class LibertyLoginModuleNoEncTestServlet extends FATServlet {

    public static final String TEST_USER_PROPERTY = "kafka.test.user";
    public static final String TEST_SECRET_PROPERTY = "kafka.test.secret";
    public static final Duration TIMEOUT = Duration.ofSeconds(30);

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
    public void testLoginModuleNoEnc() {
        SimpleKafkaReader<String> reader = kafkaTestClient.readerFor(NoEncTestBean.CHANNEL_OUT);
        SimpleKafkaWriter<String> writer = kafkaTestClient.writerFor(NoEncTestBean.CHANNEL_IN);

        writer.sendMessage("abc");
        writer.sendMessage("xyz");

        List<String> msgs = reader.waitForMessages(2, TIMEOUT);

        assertThat(msgs, contains("none-abc", "none-xyz"));
    }

}
