/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ContextTestServlet")
public class KafkaDefaultContextTestServlet extends FATServlet {

    @Inject
    private KafkaTestClient client;

    @Resource(lookup = "java:app/AppName")
    private String appName;

    @Test
    public void testContextPropagation() throws Exception {
        try (KafkaWriter<String, String> testWriter = client.writerFor(KafkaDefaultContextTestMessageBean.INPUT_CHANNEL);
                        KafkaReader<String, String> testReader = client.readerFor(KafkaDefaultContextTestMessageBean.OUTPUT_CHANNEL)) {

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Send two messages
            testWriter.sendMessage("abc");
            testWriter.sendMessage("def");

            // If application context is propagated correctly, each message should have the app name appended
            List<String> received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("abc-" + appName,
                                          "def-" + appName));

            // Send two more messages
            // Kafka connector should need to poll in the background between these two sets of messages
            // which would start another async task
            testWriter.sendMessage("xyz");
            testWriter.sendMessage("uvw");

            received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("xyz-" + appName,
                                          "uvw-" + appName));
        }
    }
}
