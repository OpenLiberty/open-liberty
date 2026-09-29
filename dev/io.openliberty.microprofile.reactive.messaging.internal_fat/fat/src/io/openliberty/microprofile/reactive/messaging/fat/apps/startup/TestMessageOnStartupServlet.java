/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.startup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/TestMessageOnStartup")
public class TestMessageOnStartupServlet extends FATServlet {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    // The five messages sent to Kafka before the server started (see TestMessageOnStartupTest.setup())
    private static final int EXPECTED_MESSAGE_COUNT = 5;

    @Inject
    private TestMessageOnStartupKafkaConsumer consumer;

    @Test
    public void testPreloadedMessagesWorkAfterStartup() throws Exception {

        List<Message<String>> received = consumer.assertReceivedMessages(EXPECTED_MESSAGE_COUNT, TIMEOUT);
        List<String> payloads = received.stream().map(Message::getPayload).collect(Collectors.toList());
        assertThat(payloads, containsInAnyOrder(
                                                "test message 1",
                                                "test message 2",
                                                "test message 3",
                                                "test message 4",
                                                "test message 5"));
    }

}
