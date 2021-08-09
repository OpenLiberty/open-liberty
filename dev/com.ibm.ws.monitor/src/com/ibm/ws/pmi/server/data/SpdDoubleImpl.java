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
import com.ibm.websphere.pmi.server.SpdDouble;
import com.ibm.ws.pmi.stat.DoubleStatisticImpl;
import com.ibm.ws.pmi.stat.StatisticImpl;

/**
 * SpdDouble: contains a long numeric value.
 */
public class SpdDoubleImpl extends SpdDataImpl
                        implements SpdDouble {
    private static final long serialVersionUID = -8540479668841981802L;
    protected DoubleStatisticImpl stat = null;

    public SpdDoubleImpl(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        stat = new DoubleStatisticImpl(dataId);
    }

    public SpdDoubleImpl(int id) {
        super(id);
        stat = new DoubleStatisticImpl(dataId);
    }

    // set the value
    public void set(double val) {
        stat.setDouble(val);
    }

    // increment the value by 1
    public void increment() {
        stat.increment();
    }

    // increment the value by val
    public void increment(double val) {
        stat.increment(val);
    }

    // decrement the value by 1
    public void decrement() {
        stat.decrement();
    }

    // decrement the value by val
    public void decrement(double val) {
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
    public void combine(SpdDouble other) {
        if (other == null)
            return;
        if (stat.isEnabled() && other.isEnabled())
            stat.combine((DoubleStatisticImpl) other.getStatistic());
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
