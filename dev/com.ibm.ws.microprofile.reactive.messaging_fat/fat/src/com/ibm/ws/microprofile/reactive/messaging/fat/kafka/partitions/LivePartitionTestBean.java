/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 *
 */
@ApplicationScoped
public class LivePartitionTestBean {

    public static final String CHANNEL_IN = "live-partition-test-in";
    public static final int WORK_TIME = 100;
    public static final int ACK_TIME = 1000;
    public static final int FINAL_MESSAGE_NUMBER = 9999;
    public static final int PARTITION_COUNT = 2;

    private final ArrayList<ReceivedMessage> messages = new ArrayList<>();
    private final CountDownLatch paritionsFinished = new CountDownLatch(PARTITION_COUNT);

    @Resource
    private ManagedScheduledExecutorService executor;

    @Incoming(CHANNEL_IN)
    @Acknowledgment(Strategy.MANUAL)
    public CompletionStage<Void> receive(Message<String> message) throws InterruptedException {

        Thread.sleep(WORK_TIME);

        ReceivedMessage status = new ReceivedMessage(message.getPayload());
        messages.add(status);
        System.out.println("Bean received message " + message.getPayload());

        executor.schedule(() -> {
            System.out.println("Bean acking message " + message.getPayload());
            message.ack().handle((r, t) -> {
                if (t == null) {
                    System.out.println("Bean successfully acked message " + message.getPayload());
                    status.ackStatus.set(AckStatus.ACK_SUCCESS);
                    if (status.number == FINAL_MESSAGE_NUMBER) {
                        paritionsFinished.countDown();
                    }
                } else {
                    System.out.println("Bean failed to ack message " + message.getPayload());
                    status.ackStatus.set(AckStatus.ACK_FAILED);
                }
                return null;
            });
        }, ACK_TIME, TimeUnit.MILLISECONDS);

        return CompletableFuture.completedFuture(null);
    }

    public void awaitFinish() throws InterruptedException {
        int timeout = 20 * PARTITION_COUNT;
        assertTrue("Test bean did not process all messages within " + timeout + " seconds", paritionsFinished.await(timeout, TimeUnit.SECONDS));
    }

    public List<ReceivedMessage> getMessages() {
        return messages;
    }

    public static class ReceivedMessage {
        public ReceivedMessage(String message) {
            String[] parts = message.split("-");
            partition = Integer.parseInt(parts[0]);
            number = Integer.parseInt(parts[1]);
        }

        int partition;
        int number;
        AtomicReference<AckStatus> ackStatus = new AtomicReference<>(AckStatus.ACK_PENDING);

        @Override
        public String toString() {
            return partition + "-" + number;
        }
    }

    public static enum AckStatus {
        ACK_PENDING,
        ACK_SUCCESS,
        ACK_FAILED,
    }

}
