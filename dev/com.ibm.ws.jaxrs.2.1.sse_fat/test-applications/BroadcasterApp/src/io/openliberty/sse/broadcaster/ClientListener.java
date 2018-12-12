/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.sse.broadcaster;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

/**
 *
 */
public class ClientListener implements Runnable, Closeable {
    private final static Logger _log = Logger.getLogger(ClientListener.class.getName());
    private final static long TEST_WAIT_TIME = 60;
    private final static AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final int id = ID_GENERATOR.getAndIncrement();
    private final WebTarget target;
    private final AtomicReference<CountDownLatch> sharedLatch = new AtomicReference<>();
    private CountDownLatch privateLatch;
    private final List<String> receivedEvents = new ArrayList<>();
    private final Holder<SseEventSource> holder = new Holder<SseEventSource>();

    ClientListener(WebTarget target, CountDownLatch latch) {
        this.target = target;
        this.sharedLatch.set(latch);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        privateLatch = new CountDownLatch(1);

        try (SseEventSource source = SseEventSource.target(target).build()) {
            holder.value = source;
            source.register(event -> {
                _log.info("listener id " + id + " received event " + event);
                String msg = event.readData();
                receivedEvents.add(msg);                
                sharedLatch.get().countDown(); 
                //_log.info("id " + id + " received event " + msg);
            });
            source.open();
            try {
                if (!privateLatch.await(TEST_WAIT_TIME, TimeUnit.SECONDS)) {
                    _log.warning("run (" + id + ") TIMED OUT!");
                    receivedEvents.add("TIMED OUT!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    void setLatch(CountDownLatch latch) {
        this.sharedLatch.set(latch);
    }

    List<String> getReceivedEvents() {
        return receivedEvents;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        privateLatch.countDown();
        holder.value.close();
    }
    
    private class Holder<T> {
        public volatile T value;
    }
}
