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

import com.ibm.websphere.pmi.*;
import com.ibm.websphere.pmi.server.*;
import com.ibm.ws.pmi.stat.*;

public class SpdLoadExternal extends SpdDataImpl
                                 implements SpdLoad {
    private static final long serialVersionUID = 7079768640124356041L;
    SpdLoadExternalValue proxy;
    StatisticImpl stat = null;

    public SpdLoadExternal(PmiModuleConfig moduleConfig, String name,
                           SpdLoadExternalValue proxy) {
        super(moduleConfig, name);
        this.proxy = proxy;
        stat = new BoundedRangeStatisticImpl(dataId);
    }

    public SpdLoadExternal(int dataId, SpdLoadExternalValue proxy) {
        super(dataId);
        this.proxy = proxy;
        stat = new BoundedRangeStatisticImpl(dataId);
    }

    public SpdLoadExternal(int dataId, int type, SpdLoadExternalValue proxy) {
        super(dataId);
        this.proxy = proxy;
        if (type == TYPE_RANGE)
            stat = new RangeStatisticImpl(dataId);
        else
            stat = new BoundedRangeStatisticImpl(dataId);
    }

    // null methods in order to implement SpdLoad
    public void add(double val) {}

    public void increment(double incVal) {}

    public void increment() {}

    public void decrement(double incVal) {}

    public void decrement() {}

    public void setConfig(long minSize, long maxSize) {}

    public void reset(boolean resetAll) {
        return;
    }

    public void setDataInfo(PmiModuleConfig moduleConfig) {
        stat.setDataInfo(moduleConfig);
    }

    // return a wire level data using given time as snapshotTime
    public StatisticImpl getStatistic() {
        if (enabled)
            return (StatisticImpl) proxy.getLoadValue();
        else
            return null;
    }

    public void combine(SpdLoad other) {
        //System.out.println ("[PMI.SpdLoadExternal] combine(). shouldn't be here");
        /*
         * if (other == null) return;
         * if (enabled)
         * stat.combine((BoundedRangeStatisticImpl)other.getStatistic());
         */
    }

    public void cleanup() {}

    public boolean isExternal() {
        return true;
    }

    public void updateExternal() {
        proxy.updateStatistic();
    }
}
