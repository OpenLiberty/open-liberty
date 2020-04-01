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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Test bean to monitor reception of messages from reactive messaging
 * <p>
 * To use this class, subclass it and override {@link #recieveMessage(Message)} to add {@code @Incoming} and {@code @Acknowledgement} annotations.
 */
public class AbstractReceptionBean<T> {

    private final Queue<Message<T>> receivedMessages = new LinkedList<>();

    public CompletionStage<Void> recieveMessage(Message<T> msg) {
        synchronized (this) {
            receivedMessages.add(msg);
            this.notifyAll();
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<Message<T>> assertReceivedMessages(int count, Duration timeout) throws InterruptedException {
        ArrayList<Message<T>> result = new ArrayList<>();
        Duration remaining = timeout;
        long startTime = System.nanoTime();
        while (!remaining.isNegative() && result.size() < count) {
            synchronized (this) {
                while (!receivedMessages.isEmpty() && result.size() < count) {
                    result.add(receivedMessages.poll());
                }
                if (result.size() < count) {
                    this.wait(remaining.toMillis());
                }
            }
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            remaining = timeout.minus(elapsed);
        }
        assertThat("Wrong number of records fetched from kafka", result, hasSize(count));
        return result;
    }

    /**
     * Return any messages which have been received by the bean, without waiting
     *
     * @return a list of messages, may be empty
     */
    public List<Message<T>> getReceivedMessages() {
        ArrayList<Message<T>> result = new ArrayList<>();
        synchronized (this) {
            while (!receivedMessages.isEmpty()) {
                result.add(receivedMessages.poll());
            }
        }
        return result;
    }

}