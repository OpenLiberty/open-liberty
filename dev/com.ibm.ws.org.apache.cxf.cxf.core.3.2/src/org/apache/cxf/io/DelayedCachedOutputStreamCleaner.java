/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;

public final class DelayedCachedOutputStreamCleaner implements CachedOutputStreamCleaner, BusLifeCycleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(DelayedCachedOutputStreamCleaner.class);
    private static final long MIN_DELAY = 2000; /* 2 seconds */
    private static final DelayedCleaner NOOP_CLEANER = new DelayedCleaner() {
        // NOOP
    };

    private DelayedCleaner cleaner = NOOP_CLEANER;
    private boolean cleanupOnShutdown = true;

    private interface DelayedCleaner extends CachedOutputStreamCleaner, Closeable {
        @Override
        default void register(Closeable closeable) {
        }
        
        @Override
        default void unregister(Closeable closeable) {
        }
        
        @Override
        default void close() {
        }
        
        @Override
        default void clean() {
        }

        @Override
        default int size() {
            return 0;
        }

        default void forceClean() {
        }
    }

    private static final class DelayedCleanerImpl implements DelayedCleaner {
        private final long delay; /* default is 30 minutes, in milliseconds */
        private final DelayQueue<DelayedCloseable> queue = new DelayQueue<>();
        private final Timer timer;
        
        DelayedCleanerImpl(final long delay) {
            this.delay = delay;
            this.timer = new Timer("DelayedCachedOutputStreamCleaner", true);
            this.timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    clean();
                }
            }, 0, Math.max(MIN_DELAY, delay >> 1));
        }

        @Override
        public void register(Closeable closeable) {
            queue.put(new DelayedCloseable(closeable, delay));
        }

        @Override
        public void unregister(Closeable closeable) {
            queue.remove(new DelayedCloseable(closeable, delay));
        }

        @Override
        public void clean() {
            final Collection<DelayedCloseable> closeables = new ArrayList<>();
            queue.drainTo(closeables);
            clean(closeables);
        }
        
        @Override
        public void forceClean() {
            clean(queue);
        }
        
        @Override
        public void close()  {
            timer.cancel();
            queue.clear();
        }

        @Override
        public int size() {
            return queue.size();
        }

        private void clean(Collection<DelayedCloseable> closeables) {
            final Iterator<DelayedCloseable> iterator = closeables.iterator();
            while (iterator.hasNext()) {
                final DelayedCloseable next = iterator.next();
                try {
                    iterator.remove();
                    LOG.warning("Unclosed (leaked?) stream detected: " + next.closeable.hashCode());
                    next.closeable.close();
                } catch (final IOException | RuntimeException ex) {
                    LOG.warning("Unable to close (leaked?) stream: " + ex.getMessage());
                }
            }
        }
    }

    private static final class DelayedCloseable implements Delayed {
        private final Closeable closeable;
        private final long expireAt;
        
        DelayedCloseable(final Closeable closeable, final long delay) {
            this.closeable = closeable;
            this.expireAt = System.nanoTime() + delay;
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(expireAt, ((DelayedCloseable) o).expireAt);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expireAt - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int hashCode() {
            return Objects.hash(closeable);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            
            if (obj == null) {
                return false;
            }
            
            if (getClass() != obj.getClass()) {
                return false;
            }
            
            final DelayedCloseable other = (DelayedCloseable) obj;
            return Objects.equals(closeable, other.closeable);
        }
    }

    @Resource
    public void setBus(Bus bus) {
        Number delayValue = null;
        BusLifeCycleManager busLifeCycleManager = null;
        Boolean cleanupOnShutdownValue = null;

        if (bus != null) {
            delayValue = (Number) bus.getProperty(CachedConstants.CLEANER_DELAY_BUS_PROP);
            cleanupOnShutdownValue = (Boolean) bus.getProperty(CachedConstants.CLEANER_CLEAN_ON_SHUTDOWN_BUS_PROP);
            busLifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        }

        if (cleaner != null) {
            cleaner.close();
        }

        if (cleanupOnShutdownValue != null) {
            cleanupOnShutdown = cleanupOnShutdownValue;
        } else {
            cleanupOnShutdown = true;
        }

        if (delayValue == null) {
            // Default delay is set to 30 mins
            cleaner = new DelayedCleanerImpl(TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES));
        } else {
            final long value = delayValue.longValue();
            if (value > 0 && value >= MIN_DELAY) {
                cleaner = new DelayedCleanerImpl(value); /* already in milliseconds */
            } else {
                cleaner = NOOP_CLEANER;
                if (value != 0) {
                    throw new IllegalArgumentException("The value of " + CachedConstants.CLEANER_DELAY_BUS_PROP 
                        + " property is invalid: " + value + " (should be >= " + MIN_DELAY + ", 0 to deactivate)");
                }
            }
        }

        if (busLifeCycleManager != null) {
            busLifeCycleManager.registerLifeCycleListener(this);
        }
    }

    @Override
    public void register(Closeable closeable) {
        cleaner.register(closeable);
    }

    @Override
    public void unregister(Closeable closeable) {
        cleaner.unregister(closeable);
    }

    @Override
    public int size() {
        return cleaner.size();
    }

    @Override
    public void clean() {
        cleaner.clean();
    }
    
    @Override
    public void initComplete() {
    }
    
    @Override
    public void postShutdown() {
        // If cleanup on shutdown is asked, force cleaning all cached output streams
        if (cleanupOnShutdown) {
            forceClean();
            cleaner.close();
        }
    }
    
    @Override
    public void preShutdown() {
        // If cleanup on shutdown is asked, defer closing till postShutdown hook
        if (!cleanupOnShutdown) {
            cleaner.close();
        }
    }

    public void forceClean() {
        cleaner.forceClean();
    }
}