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
package com.ibm.websphere.ejbcontainer.test.osgi.pmi;

import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal.AverageStatistic;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class TimeStatistic extends AverageStatistic {
    private final AtomicLong totalTime = new AtomicLong();

    @Override
    public String toString() {
        return super.toString() + "[count=" + getCount() + ", totalTime=" + totalTime + ']';
    }

    @Override
    public void add(long value) {
        super.add(value);
        totalTime.addAndGet(value);
    }

    public long getTotalTime() {
        return totalTime.get();
    }
}
