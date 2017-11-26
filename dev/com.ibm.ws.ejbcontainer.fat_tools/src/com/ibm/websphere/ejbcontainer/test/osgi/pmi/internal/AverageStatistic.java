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
public abstract class AverageStatistic {
    private final AtomicLong count = new AtomicLong();

    public void add(long value) {
        count.incrementAndGet();
    }

    public long getCount() {
        return count.get();
    }
}
