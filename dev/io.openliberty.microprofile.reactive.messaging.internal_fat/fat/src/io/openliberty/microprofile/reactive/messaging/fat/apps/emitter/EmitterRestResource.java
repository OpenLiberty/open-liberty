/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.reactive.messaging.fat.apps.emitter;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;

import jakarta.ws.rs.Path;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/")
public class EmitterRestResource {

    public static final String PAYLOAD_CHANNEL_NAME = "restful-emitter-payload";
    public static final String MESSAGE_CHANNEL_NAME = "restful-emitter-message";

    @Inject
    @Channel(PAYLOAD_CHANNEL_NAME)
    private Emitter<String> payloadEmitter;

    @Inject
    @Channel(MESSAGE_CHANNEL_NAME)
    private Emitter<String> messageEmitter;

    @POST
    @Path("/payload")
    public CompletionStage<Void> emitPayload(String payload){
        CompletionStage<Void> cs = payloadEmitter.send(payload);
        return cs;
    }

    @POST
    @Path("/message")
    public CompletionStage<Void> emitMessage(String payload) {
        CompletableFuture<Void> ackCf = new CompletableFuture<>();
        messageEmitter.send(Message.of(payload,
                () -> {
                    ackCf.complete(null);
                    return CompletableFuture.completedFuture(null);
                },
                t -> {
                    ackCf.completeExceptionally(t);
                    return CompletableFuture.completedFuture(null);
                }));
        return ackCf;
    }

}
