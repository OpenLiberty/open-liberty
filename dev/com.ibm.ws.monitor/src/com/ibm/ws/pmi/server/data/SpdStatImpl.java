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
import com.ibm.websphere.pmi.server.SpdStat;
import com.ibm.ws.pmi.stat.AverageStatisticImpl;
import com.ibm.ws.pmi.stat.StatisticImpl;
import com.ibm.ws.pmi.stat.TimeStatisticImpl;

public class SpdStatImpl extends SpdDataImpl
                        implements SpdStat {
    private static final long serialVersionUID = -109459667146296521L;
    //private TimeStatisticImpl stat = null;

    // will hold either Average or TimeStatistic
    private AverageStatisticImpl stat = null;

    // Constructor
    public SpdStatImpl(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        stat = new TimeStatisticImpl(dataId);
    }

    public SpdStatImpl(int dataId) {
        super(dataId);
        stat = new TimeStatisticImpl(dataId);
    }

    public SpdStatImpl(int type, int dataId) {
        super(dataId);
        if (type == PmiConstants.TYPE_AVGSTAT)
            stat = new AverageStatisticImpl(dataId);
        else
            stat = new TimeStatisticImpl(dataId);
    }

    // Add a value
    public void add(double val) {
        stat.add((long) val);
    }

    // reset the value and create time
    public void reset(boolean resetAll) {
        stat.reset(resetAll);
    }

    // return the value itself
    public StatisticImpl getStatistic() {
        return stat;
    }

    // combine the value of this data and other data
    public void combine(SpdStat other) {
        if (other == null)
            return;
        if (stat.isEnabled() && other.isEnabled())
            //stat.combine((TimeStatisticImpl)other.getStatistic());
            stat.combine(other.getStatistic());
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
