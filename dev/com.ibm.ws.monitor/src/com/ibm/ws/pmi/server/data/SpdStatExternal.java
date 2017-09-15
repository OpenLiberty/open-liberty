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
import com.ibm.websphere.pmi.server.SpdStat;
import com.ibm.websphere.pmi.server.SpdStatExternalValue;
import com.ibm.ws.pmi.stat.StatisticImpl;
import com.ibm.ws.pmi.stat.TimeStatisticImpl;

public class SpdStatExternal extends SpdDataImpl
                                 implements SpdStat {
    private static final long serialVersionUID = 2178747483919550839L;
    SpdStatExternalValue proxy;
    TimeStatisticImpl stat = null;

    public SpdStatExternal(PmiModuleConfig moduleConfig, String name,
                           SpdStatExternalValue proxy) {
        super(moduleConfig, name);
        this.proxy = proxy;
        stat = new TimeStatisticImpl(dataId);
    }

    public SpdStatExternal(int dataId, SpdStatExternalValue proxy) {
        super(dataId);
        this.proxy = proxy;
        stat = new TimeStatisticImpl(dataId);
    }

    // null methods in order to implement SpdStat
    public void add(double val) {}

    public void setDataInfo(PmiModuleConfig moduleConfig) {
        stat.setDataInfo(moduleConfig);
    }

    public StatisticImpl getStatistic() {
        if (enabled)
            return (StatisticImpl) proxy.getStatValue();
        else
            return null;
    }

    public void reset(boolean resetAll) {
        return;
    }

    public void combine(SpdStat other) {
        System.out.println("[PMI.SpdStatExternal] combine(). shouldn't be here");

        /*
         * if (other == null) return;
         * if (enabled)
         * stat.combine((TimeStatisticImpl)other.getStatistic());
         */
    }

    public boolean isExternal() {
        return true;
    }

    public void updateExternal() {
        proxy.updateStatistic();
    }
}
