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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Test bean to drive delivery of messages into reactive messaging
 * <p>
 * To use this class, subclass it and override {@link #getMessage()} to add an {@code @Outgoing} annotation.
 */
public class AbstractDeliveryBean {

    private final Queue<Message<String>> pendingMessages = new LinkedList<>();
    private final Queue<CompletableFuture<Message<String>>> incompleteFutures = new LinkedList<>();

    public AbstractDeliveryBean() {
        super();
    }

    public CompletionStage<Message<String>> getMessage() {
        synchronized (this) {
            if (pendingMessages.isEmpty()) {
                CompletableFuture<Message<String>> nextMessage = new CompletableFuture<>();
                incompleteFutures.add(nextMessage);
                System.out.println("Delivery bean returning incomplete CS");
                return nextMessage;
            } else {
                System.out.println("Delivery bean returning completed CS");
                return CompletableFuture.completedFuture(pendingMessages.poll());
            }
        }
    }

    /**
     * Send a message through the connector
     * <p>
     * This creates a {@link Message} wrapper and queues it to be sent into the reactive messaging system
     *
     * @param message the string to send in the message
     * @return a CompletableFuture which completes when the message is acknowledged
     */
    public CompletableFuture<Void> sendMessage(String message) {
        CompletableFuture<Void> ackCf = new CompletableFuture<>();
        Message<String> msg = Message.of(message, () -> {
            ackCf.complete(null);
            return CompletableFuture.completedFuture(null);
        });
        synchronized (this) {
            CompletableFuture<Message<String>> nextMessage = incompleteFutures.poll();
            if (nextMessage != null) {
                System.out.println("Delivery bean passing message to incomplete CS");
                nextMessage.complete(msg);
            } else {
                System.out.println("Delivery bean queuing message");
                pendingMessages.add(msg);
            }
        }
        return ackCf;
    }

}