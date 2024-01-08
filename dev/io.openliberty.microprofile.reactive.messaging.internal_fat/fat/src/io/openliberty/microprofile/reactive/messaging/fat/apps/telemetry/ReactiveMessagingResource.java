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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@ApplicationScoped
public class ReactiveMessagingResource {

    public static final String EMITTER_CHANNEL = "emitter-channel";

    @Inject
    @Channel(EMITTER_CHANNEL)
    private Emitter<String> emitter;

    @Inject
    private RmTelemetryProcessingBean processingBean;

    @Inject
    private RmTelemetryReceptionBean receptionBean;

    @POST
    @Path("/emitMessage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void emitMessage(String payload) throws Exception {
        // payload is sent to Kafka and returned in upper case via two beans
        emitter.send(payload);
    }

    @GET
    @Path("/receiveMessages")
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public String receiveMessages() throws Exception {
        List<Message<String>> messages = receptionBean.assertReceivedMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
        List<String> response = new ArrayList<String>(messages.size());
        for (Message<String> message : messages) {
            response.add(message.getPayload());
        }
        return response.toString();
    }
}
