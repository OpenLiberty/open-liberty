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
// https://github.com/apache/cxf/blob/03a85a52f78e6eb5826e4b8bfc4992e0f4e0414b/core/src/main/java/org/apache/cxf/io/DelayedCachedOutputStreamCleaner.java
//backport  https://github.com/apache/cxf/pull/2684, https://github.com/apache/cxf/pull/2706
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.Resource; // Liberty Change
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;

public final class DelayedCachedOutputStreamCleaner implements CachedOutputStreamCleaner, BusLifeCycleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(DelayedCachedOutputStreamCleaner.class);
    private static final long MIN_DELAY = 2000; /* 2 seconds */
    private static final String DEFAULT_STRATEGY = "default";
    private static final String SINGLE_TIMER_STRATEGY = "single-timer";
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

    private static final class SingleTimerDelayedCleaner implements DelayedCleaner {
        private static volatile Timer timer;
        private static final Object TIMER_LOCK = new Object();

        private final long delay; /* default is 30 minutes, in milliseconds */
        private final DelayQueue<DelayedCloseable> queue = new DelayQueue<>();
        private final Object lock = new Object();
        private TimerTask timerTask;

        SingleTimerDelayedCleaner(final long delay) {
            this.delay = delay;
        }

        @Override
        public void register(final Closeable closeable) {
            TimerTask newTimerTask = null;

            synchronized (lock) {
                queue.put(new DelayedCloseable(closeable, delay));
                if (timerTask == null) {
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            clean();
                        }
                    };
                    newTimerTask = timerTask;
                }
            }

            if (newTimerTask != null) {
                Timer t;
                // CHECKSTYLE:OFF
                // May the Gods of Code Style forgive us these three inner assignments
                if ((t = timer) == null) {
                    synchronized (TIMER_LOCK) {
                        if ((t = timer) == null) {
                            t = timer = new Timer("DelayedCachedOutputStreamCleaner", true);
                        }
                    }
                }
                // CHECKSTYLE:ON
                t.scheduleAtFixedRate(newTimerTask, 0, Math.max(MIN_DELAY, delay >> 1));
            }
        }

        @Override
        public void unregister(final Closeable closeable) {
            TimerTask oldTimerTask = null;
            synchronized (lock) {
                queue.remove(new DelayedCloseable(closeable, delay));
                if (queue.isEmpty() && timerTask != null) {
                    oldTimerTask = timerTask;
                    timerTask = null;
                }
            }
            if (oldTimerTask != null) {
                oldTimerTask.cancel();
            }
        }

        @Override
        public void clean() {
            final Collection<DelayedCloseable> closeables = new ArrayList<>();
            TimerTask oldTimerTask = null;
            synchronized (lock) {
                queue.drainTo(closeables);
                if (queue.isEmpty() && timerTask != null) {
                    oldTimerTask = timerTask;
                    timerTask = null;
                }
            }
            if (oldTimerTask != null) {
                oldTimerTask.cancel();
            }
            clean(closeables);
        }

        @Override
        public void forceClean() {
            TimerTask oldTimerTask = null;
            synchronized (lock) {
                clean(queue);
                if (timerTask != null) {
                    oldTimerTask = timerTask;
                    timerTask = null;
                }
            }
            if (oldTimerTask != null) {
                oldTimerTask.cancel();
            }
        }

        @Override
        public void close()  {
            TimerTask oldTimerTask = null;
            synchronized (lock) {
                queue.clear();
                if (timerTask != null) {
                    oldTimerTask = timerTask;
                    timerTask = null;
                }
            }
            if (oldTimerTask != null) {
                oldTimerTask.cancel();
            }
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

    private static final class DefaultDelayedCleaner implements DelayedCleaner {
        private final long delay; /* default is 30 minutes, in milliseconds */
        private final DelayQueue<DelayedCloseable> queue = new DelayQueue<>();
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private volatile Timer timer;

        DefaultDelayedCleaner(final long delay) {
            this.delay = delay;
        }

        @Override
        public void register(Closeable closeable) {
            queue.put(new DelayedCloseable(closeable, delay));
            // Initialize timer lazily only when at least one closeable is registered
            if (initialized.compareAndSet(false, true)) {
                this.timer = new Timer("DelayedCachedOutputStreamCleaner", true);
                this.timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        clean();
                    }
                }, 0, Math.max(MIN_DELAY, delay >> 1));
            }
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
            final Timer t = timer;
            if (t != null) {
                t.cancel();
            }
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
            this.expireAt = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delay, TimeUnit.MILLISECONDS);
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
        Function<Long, DelayedCleaner> delayedCleanerStrategy = DefaultDelayedCleaner::new;

        if (bus != null) {
            delayValue = (Number) bus.getProperty(CachedConstants.CLEANER_DELAY_BUS_PROP);
            cleanupOnShutdownValue = (Boolean) bus.getProperty(CachedConstants.CLEANER_CLEAN_ON_SHUTDOWN_BUS_PROP);
            busLifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            final String strategy = (String) bus.getProperty(CachedConstants.CLEANER_STRATEGY_BUS_PROP);
            if (strategy == null || DEFAULT_STRATEGY.equalsIgnoreCase(strategy)) {
                delayedCleanerStrategy = DefaultDelayedCleaner::new;
            } else if (SINGLE_TIMER_STRATEGY.equalsIgnoreCase(strategy)) {
                delayedCleanerStrategy = SingleTimerDelayedCleaner::new;
            } else {
                throw new IllegalArgumentException("The value of " + CachedConstants.CLEANER_STRATEGY_BUS_PROP
                    + " property is invalid: " + strategy + " (should be " + DEFAULT_STRATEGY + " or "
                        + SINGLE_TIMER_STRATEGY);
            }
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
            cleaner = delayedCleanerStrategy.apply(TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES));
        } else {
            final long value = delayValue.longValue();
            if (value > 0 && value >= MIN_DELAY) {
                cleaner = delayedCleanerStrategy.apply(value); /* already in milliseconds */
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