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

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.ConfigStartException;

public class PollingDynamicConfig implements Closeable {

    /**  */
    private static final TraceComponent tc = Tr.register(PollingDynamicConfig.class);

    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();

    private volatile Map<String, String> current = new HashMap<String, String>();
    private final AtomicBoolean busy = new AtomicBoolean();
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
    @FFDCIgnore({ ConfigStartException.class })
    private Future<?> start() {
        Future<?> future = null;
        boolean startUpFailure = false;
        try {
            update();
        } catch (ConfigStartException cse) {
            //Swallow the exception, don't FFDC
            //At the moment this exception means that we could not properly query the config source
            //It was introduced as a quick fix for issue #3997 but we might reconsider the design at some point
            startUpFailure = true;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "start: Initial Update failed: {0}. Exception: {1}", this, e);
            }
            future = Futures.immediateFailure(e);
        }

        //if there was an initial startup failure, don't start the polling thread
        if (!startUpFailure && future == null && interval > 0) {
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

    /**
     * Go out and poll for updated values via callable.call()
     *
     * @throws Exception
     */
    @FFDCIgnore({ ConfigStartException.class })
    private void update() throws Exception {
        // OK to ignore calls to update() if already busy updating
        if (busy.compareAndSet(false, true)) {
            try {
                Map<String, String> updated = new HashMap<>();
                Map<String, String> props = source.getProperties();
                if (props != null) {
                    updated.putAll(props);
                }
                if (!updated.equals(current)) {
                    current = updated;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "update: Contents of ConfigSource {0} has changed.", this);
                    }

                    notifyConfigUpdated();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "update: Contents of ConfigSource {0} has NOT changed.", this);
                    }
                }
            } catch (ConfigStartException cse) {
                //Just Re-throw the ConfigStartException, don't FFDC
                //At the moment this exception means that we could not properly query the config source
                //It was introduced as a quick fix for issue #3997 but we might reconsider the design at some point
                throw cse;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "update: Exception updating dynamic source: {0}. Exception: {1}", this, e);
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
                if (!(future.isDone() || future.isCancelled())) {
                    boolean cancelled = future.cancel(true);
                    if (!cancelled) {
                        // On shutdown these threads are getting closed down from elsewhere
                        if (future.isDone() || future.isCancelled()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "PollingDynamicConfig lost race in future cancel: {0}", this);
                            }
                            // Something 'odd' happened
                        } else {
                            if (tc.isWarningEnabled()) {
                                Tr.warning(tc, "future.update.not.cancelled.CWMCG0016E", this);
                            }
                        }
                    }
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
    @Trivial
    protected boolean containsKey(String key) {
        return current.containsKey(key);
    }

    /**
     * Return the raw, unconverted, String associated with a key.
     *
     * @param key
     */
    @Trivial
    protected String getRawProperty(String key) {
        String rawValue = source.getValue(key);
        if (rawValue != null) {
            current.put(key, rawValue);
        }
        return rawValue;
    }

    /**
     * @return Return an iterator to all property names owned by this config
     */
    @Trivial
    protected Iterator<String> getKeys() {
        return current.keySet().iterator();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String toString() {
        return getSourceID();
    }

    /**
     * @return the source id
     */
    @Trivial
    public String getSourceID() {
        return this.id;
    }

}
