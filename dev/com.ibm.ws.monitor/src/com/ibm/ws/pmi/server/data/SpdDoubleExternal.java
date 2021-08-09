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

public class SpdDoubleExternal extends SpdDataImpl
                             implements SpdDouble {
    private static final long serialVersionUID = -7694571935974380747L;
    protected SpdDoubleExternalValue proxy;
    protected DoubleStatisticImpl stat;

    public SpdDoubleExternal(PmiModuleConfig moduleConfig, String name,
                             SpdDoubleExternalValue proxy) {
        super(moduleConfig, name);
        this.proxy = proxy;
        stat = new DoubleStatisticImpl(dataId);
    }

    public SpdDoubleExternal(int dataId, SpdDoubleExternalValue proxy) {
        super(dataId);
        this.proxy = proxy;
        stat = new DoubleStatisticImpl(dataId);
    }

    // null methods in order to implement SpdDouble
    public void set(double val) {}

    public void increment() {}

    public void increment(double val) {}

    public void decrement() {}

    public void decrement(double val) {}

    public void reset(boolean resetAll) {
        return;
    }

    public void setDataInfo(PmiModuleConfig moduleConfig) {
        stat.setDataInfo(moduleConfig);
    }

    // return a wire level data using given time as snapshotTime
    public StatisticImpl getStatistic() {
        if (enabled) {
            return (StatisticImpl) proxy.getDoubleValue();
            //stat.setDouble(proxy.getDoubleValue());
            //return stat;
        } else {
            return null;
        }
    }

    public void combine(SpdDouble other) {
        //System.out.println ("[PMI.SpdDoubleExternal] combine(). shouldn't be here");
        return;
        /*
         * if (other == null) return;
         * if (enabled)
         * stat.combine((DoubleStatisticImpl)other.getStatistic());
         */
    }

    public boolean isExternal() {
        return true;
    }

    public void updateExternal() {
        proxy.updateStatistic();
    }
}
