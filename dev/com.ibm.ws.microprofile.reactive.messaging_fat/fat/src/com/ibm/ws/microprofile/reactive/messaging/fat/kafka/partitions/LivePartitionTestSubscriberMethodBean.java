/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.LivePartitionTestBean.ReceivedMessage;

@ApplicationScoped
public class LivePartitionTestSubscriberMethodBean {

    public static final String CHANNEL_IN = "live-partition-test-subscriber-method-in";
    public static final int WORK_TIME = 50;
    public static final int PARTITION_COUNT = 2;

    private final ArrayList<ReceivedMessage> messages = new ArrayList<>();
    private final CountDownLatch partitionsFinished = new CountDownLatch(PARTITION_COUNT);

    @Incoming(CHANNEL_IN)
    public void receive(String message) throws InterruptedException {
        System.out.println("SubscriberMethodBean received message: " + message);
        Thread.sleep(WORK_TIME);
        ReceivedMessage status = new ReceivedMessage(message);
        messages.add(status);
        if (status.number == LivePartitionTestServlet.FINAL_MESSAGE_NUMBER) {
            partitionsFinished.countDown();
        }
    }

    public void awaitFinish() throws InterruptedException {
        int timeout = 20 * PARTITION_COUNT;
        assertTrue("Test bean did not process all messages within " + timeout + " seconds", partitionsFinished.await(timeout, TimeUnit.SECONDS));
    }

    public List<ReceivedMessage> getMessages() {
        return messages;
    }

}
