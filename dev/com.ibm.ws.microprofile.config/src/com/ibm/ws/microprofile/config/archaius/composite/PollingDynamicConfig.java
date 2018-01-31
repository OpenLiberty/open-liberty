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
//Based on com.netflix.archaius.config.PollingDynamicConfig and com.netflix.archaius.config.AbstractConfig

package com.ibm.ws.microprofile.config.archaius.composite;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class PollingDynamicConfig implements Closeable {

    /**  */
    private static final TraceComponent tc = Tr.register(PollingDynamicConfig.class);

    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();

    private volatile Map<String, String> current = new HashMap<String, String>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong updateCounter = new AtomicLong();
    private final AtomicLong errorCounter = new AtomicLong();
    private Future<?> future;

    private final ScheduledExecutorService executor;
    private final long interval;
    private final TimeUnit units;
    private boolean localExecutor = false;

    private final String id;

    private final ConfigSource source;

    /**
     * Constructor
     * A refresh interval >1 will be set to have a minimum value of
     * {@value #ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL}
     *
     * @param source
     * @param executor
     */
    public PollingDynamicConfig(ConfigSource source, ScheduledExecutorService executor, long refreshInterval) {
        this.source = source;
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
     * Register a listener that will receive a call for each property that is added, removed
     * or updated. It is recommended that the callbacks be invoked only after a full refresh
     * of the properties to ensure they are in a consistent state.
     *
     * @param listener
     */
    public void addListener(ConfigListener listener) {
        listeners.add(listener);
    }

    protected void notifyConfigUpdated() {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated();
        }
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
                Tr.debug(tc, "start: Initial Update failed: {0}. Exception: {1}", this, e);
            }
            future = Futures.immediateFailure(e);
        }
        if (future == null && interval > 0) {
            future = executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "start: Scheduled Update starting: {0}", this);
                        }

                        update();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "start", "Scheduled Update completed: {0}", this);
                        }
                    } catch (Exception e) {
                        //                    LOG.warn("Failed to load properties", e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "start", "Scheduled Update failed: {0}. Exception: {1}", this, e);
                        }
                    }
                }
            }, interval, interval, units);
        }
        return future;
    }

    private Map<String, String> getToAdd() {
        Map<String, String> toAdd = new HashMap<>();
        Map<String, String> props = source.getProperties();
        if (props != null) {
            toAdd.putAll(props);
        }
        return toAdd;
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
                current = getToAdd();
                notifyConfigUpdated();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "update: Exception updating dynamic source: {0}. Exception: {1}", this, e);
                }

                errorCounter.incrementAndGet();

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
                if (!cancelled && TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "future.update.not.cancelled.CWMCG0016E", this);
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
     * @param key
     * @return True if the key is contained within this or any of it's child configurations
     */
    protected boolean containsKey(String key) {
        return current.containsKey(key);
    }

    /**
     * Return the raw, unconverted, String associated with a key.
     *
     * @param key
     */
    protected String getRawProperty(String key) {
        return current.get(key);
    }

    /**
     * @return Return an iterator to all property names owned by this config
     */
    protected Iterator<String> getKeys() {
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
