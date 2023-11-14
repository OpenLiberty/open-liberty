/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/RmTelemetryTest")
public class ReactiveMessagingTelemetryTestServlet extends FATServlet {

    public static final String EMITTER_CHANNEL = "emitter-channel";

    @Inject
    @Channel(EMITTER_CHANNEL)
    private Emitter<String> emitter;

    @Inject
    private RmTelemetryProcessingBean processingBean;

    @Inject
    private RmTelemetryReceptionBean receptionBean;

    public void testEmitter() throws Exception {
        // payload is sent to Kafka and returned in upper case via two beans
        emitter.send("TestMessage");
        List<Message<String>> messages = receptionBean.assertReceivedMessages(1, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        Message<String> message = messages.get(0);
        assertThat(message.getPayload(), is("TESTMESSAGE"));
    }
}
