/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.meters;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A counter is used to track and record metrics related to counted items
 * such as processed requests, bytes read, or bytes written. Counted data
 * <em>must</em> be monotonically increasing. Counters cannot appear to
 * move backwards except on reset or wrap.
 * <p>
 * This implementation attempts to avoid cache hot spots by associating a
 * counter per thread and then aggregating them when queried.
 */
@Trivial
public final class Counter extends com.ibm.websphere.monitor.jmx.Counter implements CounterMXBean {

    /**
     * Simple boxing type that is used to hold the value of a thread specific
     * counter value. This object will be strongly referenced by instances of
     * the {@link ValueReference} and {@link Value} classes. When an instance
     * of the {@linkplain Value} class is eligible for garbage collection, the {@linkplain ValueReference} will still be accessible and used to get the
     * final value of the counter.
     */
    private final static class LongValue {
        long value;
    }

    /**
     * A subclass of a {@link WeakReference} that we'll use to detect when a {@link Value} instance is no longer reachable. This reference holds a
     * strong reference to the boxed {@link LongValue} updated by {@link Value} so we can continue to access the final value of the counter after the
     * thread termination.
     */
    private final class ValueReference extends WeakReference<Value> {
        final LongValue valueObject;

        ValueReference(Value value) {
            super(value, valueReferenceQueue);
            valueObject = value.longValue;
        }

        long getValue() {
            return valueObject.value;
        }
    };

    /**
     * A boxing type that encapsulates a long and is referenced by a {@code ThreadLocal}. By having the thread local reference this type
     * instead of a {@code Long}, we avoid the overhead of constantly
     * reallocating a new instance of Long on every increment.
     */
    private final static class Value {
        LongValue longValue = new LongValue();
    }

    /**
     * Sum of all counters that were associated with terminated threads.
     */
    private final AtomicLong terminatedThreadOffset = new AtomicLong();

    /**
     * Set of all {@link ValueReference}s. This is used to aggregate the
     * thread specific counters.
     */
    private final Set<ValueReference> allReferences = Collections.synchronizedSet(new HashSet<ValueReference>());

    /**
     * Reference queue to track {@code Value} instances that are no longer
     * reachable through a {@link ThreadLocal}.
     */
    private final ReferenceQueue<Value> valueReferenceQueue = new ReferenceQueue<Value>();

    /**
     * Container to hold thread specific counters.
     */
    private final ThreadLocal<Value> threadValue = new ThreadLocal<Value>() {
        @Override
        public Value initialValue() {
            Value v = new Value();
            allReferences.add(new ValueReference(v));
            cleanup();
            return v;
        }
    };

    /**
     * Create a new counter.
     */
    public Counter() {
        super();
    }

    @Override
    public synchronized long getCurrentValue() {
        long currentValue = terminatedThreadOffset.get();
        synchronized (allReferences) {
            for (ValueReference vr : allReferences) {
                currentValue += vr.getValue();
                if (currentValue < 0) {
                    currentValue += Long.MAX_VALUE + 1;
                }
            }
        }
        cleanup();
        return currentValue;
    }

    public long getDifference(long oldValue) {
        long difference = getCurrentValue() - oldValue;
        if (difference < 0) {
            difference += Long.MAX_VALUE + 1;
        }
        cleanup();
        return difference;
    }

    public void incrementBy(long increment) {
        if (increment < 0) {
            throw new IllegalArgumentException("Counters must be monotonically increasing");
        }
        threadValue.get().longValue.value += increment;
    }

    private void cleanup() {
        ValueReference vr = null;
        while ((vr = (ValueReference) valueReferenceQueue.poll()) != null) {
            terminatedThreadOffset.addAndGet(vr.getValue());
            allReferences.remove(vr);
        }
    }

    @Override
    public CounterReading getReading() {
        return new CounterReading(getCurrentValue(), getUnit());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("current value = ").append(getCurrentValue());
        return sb.toString();
    }
}
