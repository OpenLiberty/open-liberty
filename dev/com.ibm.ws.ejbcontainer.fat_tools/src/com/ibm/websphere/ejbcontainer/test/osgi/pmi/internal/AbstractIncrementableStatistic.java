/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal;

import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public abstract class AbstractIncrementableStatistic {
    protected final AtomicLong value = new AtomicLong();

    @Override
    public String toString() {
        return super.toString() + '[' + value + ']';
    }

    public void increment() {
        value.incrementAndGet();
    }

    public void decrement() {
        value.decrementAndGet();
    }
}
