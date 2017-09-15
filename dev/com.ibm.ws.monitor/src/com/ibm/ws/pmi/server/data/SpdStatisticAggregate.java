/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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

//import com.ibm.ws.pmi.server.PmiUtil;

public class SpdStatisticAggregate extends SpdGroupBase implements PmiConstants {
    private static final long serialVersionUID = -3407297120698502783L;
    protected StatisticImpl aggregateValue = null;
    protected int _type = TYPE_INVALID;

    public SpdStatisticAggregate(PmiDataInfo dataInfo) {
        super(dataInfo.getId());
        _type = dataInfo.getType();

        switch (_type) {
            case TYPE_LONG:
                aggregateValue = new CountStatisticImpl(dataId);
                break;

            case TYPE_DOUBLE:
                aggregateValue = new DoubleStatisticImpl(dataId);
                break;

            case TYPE_STAT:
                aggregateValue = new TimeStatisticImpl(dataId);
                break;

            case TYPE_AVGSTAT:
                aggregateValue = new AverageStatisticImpl(dataId);
                break;

            case TYPE_RANGE:
                aggregateValue = new RangeStatisticImpl(dataId);
                break;

            case TYPE_LOAD:
                aggregateValue = new BoundedRangeStatisticImpl(dataId);
                break;

            default:
                _type = TYPE_INVALID;
                System.out.println("[SpdStatisticAggregate] Invalid statistic type");
        }
    }

    // Check data type and call super.add to add data - synchronized in super
    public boolean add(SpdData data) {
        if (data == null)
            return false;
        else {
            return super.add(data);
        }
    }

    // Check data type and call super to remove data - synchronized in super
    public boolean remove(SpdData data) {
        if (data == null)
            return false;
        else {
            return super.remove(data);
        }
    }

    // Return a wire level data using given time
    public StatisticImpl getStatistic() {
        StatisticImpl aggStat = _getAggregate();
        aggStat.setLastSampleTime(System.currentTimeMillis());
        return aggStat;
    }

    private StatisticImpl _getAggregate() {
        // **FIXME: should be reset(false). add to statistic, statisticimpl
        // this requires the code cleanup and adding reset or cleanup method to statistic interface
        // if we add now it will be exposed so not the hard way now.
        switch (_type) {
            case TYPE_LONG:
                ((CountStatisticImpl) aggregateValue).reset(false);
                break;

            case TYPE_DOUBLE:
                ((DoubleStatisticImpl) aggregateValue).reset(false);
                break;

            case TYPE_STAT:
                ((TimeStatisticImpl) aggregateValue).reset(false);
                break;

            case TYPE_AVGSTAT:
                ((AverageStatisticImpl) aggregateValue).reset(false);
                break;

            case TYPE_LOAD:
                ((BoundedRangeStatisticImpl) aggregateValue).cleanup();
                break;

            case TYPE_RANGE:
                ((RangeStatisticImpl) aggregateValue).cleanup();
                break;

            default:
                _type = TYPE_INVALID;
                System.out.println("[SpdStatisticAggregate] Invalid statistic type");
        }

        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (member == null)
                continue;

            if (member instanceof SpdStatisticAggregate) {
                aggregateValue.combine(((SpdStatisticAggregate) member)._getAggregate());
            } else {
                aggregateValue.combine(((SpdData) member).getStatistic());
            }

        }
        return aggregateValue;
    }

    /*
     * PK80147: TPV IS INCORRECTLY DISPLAYING SIBUS SERVICE METRICS
     * SIB FileStore stats do not get aggregated at immediate parent level.
     * The aggregation at parent level is happening before the values for stats are populated in child.
     * To resolve this, an overloaded version of updateAggregate() has been added below
     * and a call to getStatistic() method is made, when the stats are requested.
     */
    public void updateAggregate() {
        getStatistic();
    }

    public void reset(boolean resetAll) {
        // ** Nothing to do **
    }
}
