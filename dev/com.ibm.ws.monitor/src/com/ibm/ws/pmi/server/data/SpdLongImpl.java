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

import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.server.SpdLong;
import com.ibm.ws.pmi.stat.CountStatisticImpl;
import com.ibm.ws.pmi.stat.StatisticImpl;

/**
 * SpdLong: contains a long numeric value.
 */
public class SpdLongImpl extends SpdDataImpl
                        implements SpdLong {
    private static final long serialVersionUID = 6081293933019198051L;
    protected CountStatisticImpl stat = null;

    public SpdLongImpl(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        stat = new CountStatisticImpl(dataId);
    }

    public SpdLongImpl(int id) {
        super(id);
        stat = new CountStatisticImpl(dataId);
    }

    // set the value
    public void set(long val) {
        stat.setCount(val);
    }

    // increment the value by 1
    public void increment() {
        stat.increment();
    }

    // increment the value by val
    public void increment(long val) {
        stat.increment(val);
    }

    // decrement the value by 1
    public void decrement() {
        stat.decrement();
    }

    // decrement the value by val
    public void decrement(long val) {
        stat.decrement(val);
    }

    // reset the value and create time
    public void reset(boolean resetAll) {
        stat.reset(resetAll);
    }

    public StatisticImpl getStatistic() {
        return stat;
    }

    // combine the value of this data and other data
    public void combine(SpdLong other) {
        if (other == null)
            return;
        if (stat.isEnabled() && other.isEnabled())
            stat.combine((CountStatisticImpl) other.getStatistic());
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
