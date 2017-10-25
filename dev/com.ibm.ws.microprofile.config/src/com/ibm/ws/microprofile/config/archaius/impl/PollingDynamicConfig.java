/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Based on com.netflix.archaius.config.PollingDynamicConfig

package com.ibm.ws.microprofile.config.archaius.impl;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.netflix.archaius.config.AbstractConfig;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.util.Futures;

public class PollingDynamicConfig extends AbstractConfig implements Closeable {

    /**  */
    private static final TraceComponent tc = Tr.register(PollingDynamicConfig.class);

    private volatile Map<String, String> current = new HashMap<String, String>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong updateCounter = new AtomicLong();
    private final AtomicLong errorCounter = new AtomicLong();
    private Future<?> future;

    private final ScheduledExecutorService executor;
    private final long interval;
    private final TimeUnit units;
    private boolean localExecutor = false;

    private final ConfigSourceCallable callable;

    private final String id;

    /**
     * Constructor
     * A refresh interval >1 will be set to have a minimum value of
     * {@value #ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL}
     *
     * @param source
     * @param executor
     */
    public PollingDynamicConfig(ConfigSource source, ScheduledExecutorService executor, long refreshInterval) {
        this.callable = new ConfigSourceCallable(source);
        this.id = source.getName();

        this.interval = refreshInterval;
        this.units = TimeUnit.MILLISECONDS;

        if (executor == null) {
            this.executor = Executors.newScheduledThreadPool(1);
            this.localExecutor = true;
        } else {
            this.executor = executor;
        }

        this.future = start();
    }

    /**
     * Start Polling
     *
     * @return a Future<?> executor.scheduleWithFixedDelay on update()
     */
    private Future<?> start() {
        Future<?> future = null;
        try {
            update();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "execute", "Initial Update failed: " + this, e);
            }
            future = Futures.immediateFailure(e);
        }
        if (future == null && interval > 0) {
            future = executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "execute", "Scheduled Update starting: " + this);
                        }

                        update();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "execute", "Scheduled Update completed: " + this);
                        }
                    } catch (Exception e) {
                        //                    LOG.warn("Failed to load properties", e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "execute", "Scheduled Update failed: " + this, e);
                        }
                    }
                }
            }, interval, interval, units);
        }
        return future;
    }

    /**
     * Go out and poll for updated values via callable.call()
     *
     * @throws Exception
     */
    private void update() throws Exception {
        // OK to ignore calls to update() if already busy updating
        if (busy.compareAndSet(false, true)) {
            updateCounter.incrementAndGet();
            try {
                PollingResponse response = callable.call();
                if (response.hasData()) {
                    current = response.getToAdd();
                    notifyConfigUpdated(this);
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "update", "Exception updating dynamic source: " + e);
                }

                errorCounter.incrementAndGet();
                try {
                    notifyError(e, this);
                } catch (Exception e2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "update", "Exception notifying listener: " + e2);
                    }
                }
                throw e;
            } finally {
                busy.set(false);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        try {
            if (future != null) {
                boolean cancelled = future.cancel(true);
                if (!cancelled) {
                    throw new ConfigException(Tr.formatMessage(tc, "future.update.not.cancelled.CWMCG0013E"));
                }
                future = null;
            }
        } finally {
            if (this.localExecutor) {
                this.executor.shutdown();
            }
        }
    }

    /**
     * @return updateCounter.value
     */
    public long getUpdateCounter() {
        return updateCounter.get();
    }

    /**
     * @return errorCounter.value
     */
    public long getErrorCounter() {
        return errorCounter.get();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(String key) {
        return current.containsKey(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return current.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public Object getRawProperty(String key) {
        return current.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getKeys() {
        return current.keySet().iterator();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.id;
    }

    /**
     * @return the source id
     */
    public String getSourceID() {
        return this.id;
    }

}
