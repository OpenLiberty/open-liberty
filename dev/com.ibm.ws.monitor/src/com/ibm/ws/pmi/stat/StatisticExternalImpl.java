/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.stat;

import com.ibm.websphere.pmi.*;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.wsspi.pmi.factory.StatisticActions;

public class StatisticExternalImpl extends StatisticImpl implements PmiConstants {
    private static final long serialVersionUID = 4818824132153915157L;
    protected StatisticActions proxy;
    protected StatisticImpl onReqStatistic;

    //private int _type;

    public StatisticExternalImpl(PmiDataInfo dataInfo, StatisticActions proxy) {
        super(dataInfo.getId());
        this.proxy = proxy;

        int type = dataInfo.getType();
        switch (type) {
            case TYPE_LONG:
                onReqStatistic = new CountStatisticImpl(id);
                break;

            case TYPE_DOUBLE:
                onReqStatistic = new DoubleStatisticImpl(id);
                break;

            case TYPE_STAT:
                onReqStatistic = new TimeStatisticImpl(id);
                break;

            case TYPE_LOAD:
                onReqStatistic = new BoundedRangeStatisticImpl(id);
                break;
            default:
                System.err.println("[SpdStatisticExternal] Invalid statistic type");
        }
    }

    public WSStatistic copy() {
        // TODO: Implement this function
        return null;
    }

    // return a wire level data using given time as snapshotTime
    public StatisticImpl getStatistic() {//System.out.println("Hi This is for UpdataStatistic");
        if (enabled) {
            proxy.updateStatisticOnRequest(id);

            // onReqStatistic will be updated by the component
            // the reference is kept in this class
            return onReqStatistic;
        } else
            return null;
    }

    public StatisticImpl getStatisticRef() {
        return onReqStatistic;
    }

    // dummy impl
    // these methods will not be called on this class
    // these methods will be called on the "aggregateValue"
    public void update(WSStatistic data) {}

    public WSStatistic delta(WSStatistic data) {
        return null;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        // TODO: Implement this function
        return null;
    }

    public void combine(WSStatistic data) {}

    public void resetOnClient(WSStatistic data) {}

    public void reset(boolean resetAll) {}

    public String toXML() {
        return null;
    }
}
