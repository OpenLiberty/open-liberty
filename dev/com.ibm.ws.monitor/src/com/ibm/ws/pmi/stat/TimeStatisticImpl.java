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

import com.ibm.websphere.pmi.stat.WSStatistic;

import com.ibm.websphere.pmi.stat.WSTimeStatistic;
import com.ibm.wsspi.pmi.stat.SPITimeStatistic;

/**
 * TimeStatisticImpl: implements JSR77's TimeStatistic and extends StatisticImpl.
 * 
 * <p>
 * Note: PMI provides more performance data than JSR77 specification.
 * A TimeStatistic data in PMI may keep tracking a non-time related metric.
 * For example, the data tracking the average
 * read/write size in session manager is of TimeStatistic type but not a time-related metric.
 * PMI does not invent a new data type since TimeStatistic perfectly match the requirement
 * except that the name itself might be misleading. However, the unit for the data should
 * clarify any confusion.
 */
public class TimeStatisticImpl extends AverageStatisticImpl implements SPITimeStatistic, WSTimeStatistic {

    private static final long serialVersionUID = 3480487189513289050L;

    public TimeStatisticImpl(int dataId) {
        super(dataId);
    }

    // called from jni code. DataId will be set later.
    // Seems that we can only pass int to int from jni to java code.
    public TimeStatisticImpl(int count, int min, int max, int total,
                             int sumOfSquares, int startTime, int lastSampleTime) {
        super(0);
        set(count, min, max, total, sumOfSquares, startTime, lastSampleTime);
    }

    public TimeStatisticImpl(int dataId, long count, long min, long max, long total,
                             double sumOfSquares, long startTime, long lastSampleTime) {
        super(dataId);
        set(count, min, max, total, sumOfSquares, startTime, lastSampleTime);
    }

    public TimeStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_TIME;
    }

    /**
     * Can invoke super(), but for casting purposes, reimplement.
     * 
     * @see com.ibm.websphere.pmi.stat.WSStatistic#copy()
     */
    public WSStatistic copy() {
        TimeStatisticImpl copy = new TimeStatisticImpl(id, count, min, max, total,
                        sumOfSquares, startTime, lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    public long getTotalTime() {
        return total;
    }

    public long getMinTime() {
        return min;
    }

    public long getMaxTime() {
        return max;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer(super.toString(""));
        str.append(", type=").append("TimeStatistic");
        str.append(", avg=").append(getMean());
        str.append(", min=").append(min);
        str.append(", max=").append(max);
        str.append(", total=").append(total);
        str.append(", count=").append(count);
        str.append(", sumSq=").append(sumOfSquares);

        return str.toString();
    }

    public String toXML() {
        // JPM [2004/06/18] fixed defect 210440
        //return super.toXML();
        StringBuffer res = new StringBuffer();
        res.append("<TS");
        res.append(" id=\"");
        res.append(id);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" ct=\"");
        res.append(count);
        res.append("\" max=\"");
        res.append(max);
        res.append("\" min=\"");
        res.append(min);
        res.append("\" sOS=\"");
        res.append(sumOfSquares);
        res.append("\" tot=\"");
        res.append(total);
        res.append("\">\n");
        res.append("</TS>");

        return res.toString();
    }

    private boolean validate(WSStatistic other) {
        if (other == null)
            return false;
        if (!(other instanceof TimeStatisticImpl)) {
            // System.out.println("WARNING: wrong data type: must be TimeStatisticImpl");
            return false;
        }
        if (other.getId() != id) {
            // System.out.println("WARNING: wrong data Id: expect dataId=" + id
            //                    + ", got dataId=" + other.getId());
            return false;
        }
        return true;
    }

    /**
     * Can just invoke super(), but for casting purposes, reimplement.
     * 
     * @see com.ibm.websphere.pmi.stat.WSStatistic#rateOfChange(com.ibm.websphere.pmi.stat.WSStatistic)
     */
    public WSStatistic rateOfChange(WSStatistic otherStat) {
        if (!validate(otherStat)) {
            return null;
        }

        TimeStatisticImpl other = (TimeStatisticImpl) otherStat;
        TimeStatisticImpl newData = new TimeStatisticImpl(id);
        long timeDiff = lastSampleTime - other.lastSampleTime;

        if (timeDiff == 0) {
            return null;
        }

        // rate of change
        newData.count = (count - other.getCount()) / timeDiff;
        newData.sumOfSquares = (sumOfSquares - other.sumOfSquares) / timeDiff;
        newData.total = (total - other.getTotal()) / timeDiff;

        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;
        newData.max = max;
        newData.min = min;

        return newData;
    }

    public WSStatistic delta(WSStatistic otherStat) {
        if (!validate(otherStat))
            return null;

        TimeStatisticImpl other = (TimeStatisticImpl) otherStat;
        TimeStatisticImpl newData = new TimeStatisticImpl(id);
        newData.min = min;
        newData.max = max;
        newData.count = count - other.getCount();
        newData.total = total - other.getTotal();
        newData.sumOfSquares = sumOfSquares - other.sumOfSquares;
        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }
}
