/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.server.data;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.server.SpdLoad;
import com.ibm.ws.pmi.stat.BoundedRangeStatisticImpl;
import com.ibm.ws.pmi.stat.RangeStatisticImpl;
import com.ibm.ws.pmi.stat.StatisticImpl;

public class SpdLoadImpl extends SpdDataImpl
                        implements SpdLoad {
    private static final long serialVersionUID = -1296441240799172895L;
    // may have either Range or BoundedRange
    public RangeStatisticImpl stat = null; // public so that it can be accessed in modules

    //public BoundedRangeStatisticImpl stat = null;  // public so that it can be accessed in modules

    // Constructor
    public SpdLoadImpl(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        stat = new BoundedRangeStatisticImpl(dataId);
    }

    public SpdLoadImpl(int dataId) {
        super(dataId);
        stat = new BoundedRangeStatisticImpl(dataId);
    }

    public SpdLoadImpl(int type, int dataId) {
        super(dataId);

        if (type == PmiConstants.TYPE_RANGE)
            stat = new RangeStatisticImpl(dataId);
        else
            stat = new BoundedRangeStatisticImpl(dataId);
    }

    // Add a value
    public void add(double val) {
        stat.set((long) val);
    }

    // increment the lastValue by incVal (default is 1)
    public void increment(double incVal) {
        stat.increment((long) incVal);
    }

    // increment the lastValue by incVal (default is 1)
    public void increment() {
        stat.increment();
    }

    // decrement the lastValue by incVal (default is 1)
    public void decrement(double incVal) {
        stat.decrement((long) incVal);
    }

    // decrement the lastValue by incVal (default is 1)
    public void decrement() {
        stat.decrement();
    }

    public void setConfig(long minSize, long maxSize) {
        if (stat instanceof BoundedRangeStatisticImpl) {
            ((BoundedRangeStatisticImpl) stat).setLowerBound(minSize);
            ((BoundedRangeStatisticImpl) stat).setUpperBound(maxSize);
        }
    }

    public void reset(boolean resetAll) {
        stat.reset(resetAll);
    }

    //  Even though a time was passed in for us to use we set
    //  the time associated with this value to the time the
    //  actual update was performed.  Load values are very sensitive
    //  to time.
    public StatisticImpl getStatistic() {

        if (enabled) {
            long curTime = stat.updateIntegral();
            stat.setLastSampleTime(curTime);
            return stat;
        } else {
            return stat;
        }

    }

    public long getCurrent() {
        return stat.getCurrent();
    }

    // Combine this data and other SpdLoad data
    public void combine(SpdLoad other) {
        if (other == null)
            return;
        if (stat.isEnabled() && other.isEnabled())
            //stat.combine((BoundedRangeStatisticImpl)other.getStatistic());
            stat.combine(other.getStatistic());
    }

    // a method for SpdLoadImpl only
    // clean up date only but do not reset createTime
    public void cleanup() {
        stat.cleanup();
    }

    // mark the data enabled and reset the value and createTime
    public void enable(int level) {
        super.enable(level);
        stat.enable(level);
    }

    // mark the data disabled
    public void disable() {
        super.disable();
        stat.disable();
    }

    // return if the data is enabled
    public boolean isEnabled() {
        return stat.isEnabled();
    }
}
