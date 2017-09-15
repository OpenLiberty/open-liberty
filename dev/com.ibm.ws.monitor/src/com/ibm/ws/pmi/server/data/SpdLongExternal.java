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

public class SpdLongExternal extends SpdDataImpl
                             implements SpdLong {
    private static final long serialVersionUID = 6891597259892963600L;
    protected SpdLongExternalValue proxy;
    protected CountStatisticImpl stat;

    public SpdLongExternal(PmiModuleConfig moduleConfig, String name,
                           SpdLongExternalValue proxy) {
        super(moduleConfig, name);
        this.proxy = proxy;
        stat = new CountStatisticImpl(dataId);
    }

    public SpdLongExternal(int dataId, SpdLongExternalValue proxy) {
        super(dataId);
        this.proxy = proxy;
        stat = new CountStatisticImpl(dataId);
    }

    // null methods in order to implement SpdLong
    public void set(long val) {}

    public void increment() {}

    public void increment(long val) {}

    public void decrement() {}

    public void decrement(long val) {}

    public void reset(boolean resetAll) {
        return;
    }

    public void setDataInfo(PmiModuleConfig moduleConfig) {
        stat.setDataInfo(moduleConfig);
    }

    // return a wire level data using given time as snapshotTime
    public StatisticImpl getStatistic() {
        if (enabled) {
            return (StatisticImpl) proxy.getLongValue();
            //stat.setCount(proxy.getLongValue());
            //return stat;
        } else {
            return null;
        }
    }

    public void combine(SpdLong other) {
        System.out.println("[PMI.SpdLongExternal] combine(). shouldn't be here");
        /*
         * if (other == null) return;
         * if (enabled)
         * stat.combine((CountStatisticImpl)other.getStatistic());
         */
    }

    public boolean isExternal() {
        return true;
    }

    public void updateExternal() {
        proxy.updateStatistic();
    }
}
