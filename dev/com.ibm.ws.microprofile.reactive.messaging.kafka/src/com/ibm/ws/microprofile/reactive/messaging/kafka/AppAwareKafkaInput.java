/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;

public class AppAwareKafkaInput<K, V> extends KafkaInput<K, V> {

    /**
     * The name of the application that created this KafkaInput, captured at construction time.
     * May be {@code null} if the application name could not be determined.
     */
    private final String applicationName;

    /**
     * Cached flag set to {@code true} once the application-started latch has been observed to have
     * counted down. Once {@code true}, subsequent calls to {@link #executePollActions} skip the
     * latch await entirely for performance.
     */
    private volatile boolean applicationStarted = false;

    /**
     * Latch obtained from {@link KafkaApplicationStateListener} for the owning application.
     * Counted down to zero once the application has fully started.
     * {@link #executePollActions} awaits this latch before entering its poll loop so
     * that messages are never dispatched to application code before the application
     * is fully started.
     */
    private final CountDownLatch applicationStartedLatch;

    private final static ServiceCaller<KafkaApplicationStateListener> APP_STATE_LISTENER_CALLER = new ServiceCaller<>(KafkaInput.class,
                                                                                                                      KafkaApplicationStateListener.class);

    public AppAwareKafkaInput(KafkaAdapterFactory kafkaAdapterFactory, PartitionTrackerFactory partitionTrackerFactory, KafkaConsumer<K, V> kafkaConsumer,
                              RMAsyncProvider asyncProvider, String topic, int unackedLimit, boolean fastAck) {
        super(kafkaAdapterFactory, partitionTrackerFactory, kafkaConsumer, asyncProvider, topic, unackedLimit, fastAck);

        this.applicationName = resolveApplicationName();

        if (applicationName == null) {
            throw new IllegalStateException("Could not determine the application name for this KafkaInput");
        }

        this.applicationStartedLatch = APP_STATE_LISTENER_CALLER.run(kasl -> kasl.getLatch(applicationName))
                                                                .orElseThrow(() -> new IllegalStateException("Could not acquire the latch KafkaInput with applicationName "
                                                                                                             + applicationName));

    }

    /**
     * Capture the application name at construction time.
     * <p>
     * Mirrors OSGiConfigUtils.getApplicationName (in io.openliberty.microprofile.config.internal.serverxml):
     * reads the thread-local {@link ComponentMetaData} first then falls back to asking the {@link CDIService}
     * for the current application context ID.
     *
     * @return the application name, or {@code null} if it cannot be determined
     */
    private static String resolveApplicationName() {
        if (!FrameworkState.isValid()) {
            return null;
        }
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            return cmd.getJ2EEName().getApplication();
        }

        //Fallback to try getting the name from CDI. This will work if CDI's startup routine is on the current thread stack.
        return ServiceCaller.runOnce(KafkaInput.class, CDIService.class,
                                     CDIService::getCurrentApplicationContextID)
                            .orElse(null);
    }

    @Override
    protected void countDownAppStartedLatch() {
        applicationStartedLatch.countDown();
    }

    @Override
    protected boolean isAppStarted() {
        return applicationStarted;
    }

    @Override
    @FFDCIgnore(InterruptedException.class)
    protected boolean waitForAppStart(CompletableFuture<PublisherBuilder<Message<V>>> result) {
        if (!applicationStarted) {
            try {
                applicationStartedLatch.await();
                applicationStarted = true;
            } catch (InterruptedException e) {
                result.completeExceptionally(e);
                return false;
            }
        }
        return true;
    }
}
