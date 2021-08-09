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
import com.ibm.wsspi.pmi.stat.SPIAverageStatistic;

public class AverageStatisticImpl extends StatisticImpl implements SPIAverageStatistic {
    private static final long serialVersionUID = 532089977446362907L;

    protected long count = 0;
    protected long min = 0;
    protected long max = 0;
    protected long total = 0;
    protected double sumOfSquares = 0;
    protected AverageStatisticImpl baseValue = null;

    public AverageStatisticImpl(int dataId) {
        super(dataId);
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_AVERAGE;
    }

    // called from jni code. DataId will be set later.
    // Seems that we can only pass int to int from jni to java code.
    public AverageStatisticImpl(int count, int min, int max, int total,
                                double sumOfSquares, int startTime, int lastSampleTime) {
        super(0);
        set(count, min, max, total, sumOfSquares, startTime, lastSampleTime);
    }

    public AverageStatisticImpl(int dataId, long count, long min, long max, long total,
                                double sumOfSquares, long startTime, long lastSampleTime) {
        super(dataId);
        set(count, min, max, total, sumOfSquares, startTime, lastSampleTime);
    }

    public AverageStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public WSStatistic copy() {
        AverageStatisticImpl copy = new AverageStatisticImpl(id, count, min, max,
                        total, sumOfSquares, startTime, lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    // used for data returned from jni
    public void setDataId(int id) {
        this.id = id;
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    public void add(long val) {
        if (!enabled)
            return;

        add(System.currentTimeMillis(), val);
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void add(long curTime, long val) {
        if (!enabled)
            return;

        lastSampleTime = curTime;
        if (!sync) {
            if (val > max)
                max = val;

            if (count > 0) {
                if (val < min)
                    min = val;
            } else
                min = val; //set min=val for the first time

            /*
             * if (val < min || min < 0)
             * min = val;
             */
            count++;
            total += val;
            sumOfSquares += val * val;
        } else {
            // synchronized ONLY the count updates
            synchronized (this) {
                count++;
                total += val;
            }

            if (val > max)
                max = val;

            if (count > 0) {
                if (val < min)
                    min = val;
            } else
                min = val; //set min=val for the first time

            sumOfSquares += val * val;
        }
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     * (Set the value using given count, total, and sumOfSquares)
     */
    public void set(long count, long min, long max, long total, double sumOfSquares,
                    long startTime, long lastSampleTime) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.total = total;
        this.sumOfSquares = sumOfSquares;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
        // TODO: createTime
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean resetAll) {
        if (resetAll)
            super.reset();
        count = 0;
        total = 0;
        sumOfSquares = 0;
        min = 0; //-1
        max = 0; //-1
    }

    // getters
    // return count
    public long getCount() {
        return count;
    }

    // return total
    public long getTotal() {
        return total;
    }

    public double getMean() {
        if (count == 0)
            return 0;
        else
            return (total * 1.0) / count;
    }

    // return min
    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    // return sumOfSquares
    public double getSumOfSquares() {
        return sumOfSquares;
    }

    // Combine this StatData with other StatData
    public void combine(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        AverageStatisticImpl other = (AverageStatisticImpl) otherStat;
        boolean previousCountWasZero = (count == 0);
        count += other.count;
        total += other.total;
        sumOfSquares += other.sumOfSquares;

        // min is min of min, max is max of max
        // need to handle one of the Statistics not having any count so
        // min of 0 isn't reported erroneously
        if (other.count != 0 && (min > other.min || previousCountWasZero == true))
            min = other.min;
        if (max < other.max)
            max = other.max;

        // Compare lastSampleTime
        if (other.lastSampleTime > lastSampleTime)
            lastSampleTime = other.lastSampleTime;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer(super.toString(""));
        str.append(", type=").append("AverageStatistic");
        str.append(", avg=").append(getMean());
        str.append(", min=").append(min);
        str.append(", max=").append(max);
        str.append(", total=").append(total);
        str.append(", count=").append(count);
        str.append(", sumSq=").append(sumOfSquares);

        /*
         * ret.append("AverageStatisticImpl: ");
         * ret.append(super.toString(""));
         * ret.append(" min=");
         * ret.append(min);
         * ret.append(" max=");
         * ret.append(max);
         * ret.append(" count=");
         * ret.append(count);
         * ret.append(" total=");
         * ret.append(total);
         * ret.append(" sumOfSquares=");
         * ret.append(sumOfSquares);
         */
        return str.toString();
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<AS");
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
        res.append("</AS>");

        return res.toString();
    }

    public void update(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        AverageStatisticImpl other = (AverageStatisticImpl) otherStat;
        if (baseValue == null) {
            this.count = other.getCount();
            this.total = other.getTotal();
            this.sumOfSquares = other.sumOfSquares;
            this.startTime = other.getStartTime();
        } else {
            this.count = other.count - baseValue.count;
            this.total = other.total - baseValue.total;
            this.sumOfSquares = other.sumOfSquares - baseValue.sumOfSquares;

            this.startTime = baseValue.lastSampleTime;
        }
        this.min = other.getMin();
        this.max = other.getMax();
        this.lastSampleTime = other.getLastSampleTime();
    }

    public WSStatistic delta(WSStatistic otherStat) {
        if (!validate(otherStat))
            return null;

        AverageStatisticImpl other = (AverageStatisticImpl) otherStat;
        AverageStatisticImpl newData = new AverageStatisticImpl(id);
        newData.min = min;
        newData.max = max;
        newData.count = count - other.getCount();
        newData.total = total - other.getTotal();
        newData.sumOfSquares = sumOfSquares - other.sumOfSquares;
        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        if (!validate(otherStat)) {
            return null;
        }

        AverageStatisticImpl other = (AverageStatisticImpl) otherStat;
        AverageStatisticImpl newData = new AverageStatisticImpl(id);

        // calculating rate per second
        long timeDiff = (lastSampleTime - other.lastSampleTime) / 1000;

        if (timeDiff == 0) {
            return null;
        }

        // rate of change
        newData.count = (count - other.count) / timeDiff;
        newData.sumOfSquares = (sumOfSquares - other.sumOfSquares) / timeDiff;
        newData.total = (total - other.total) / timeDiff;

        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;
        newData.max = max;
        newData.min = min;

        return newData;
    }

    public void resetOnClient(WSStatistic other) {
        if (other == null) {
            // use itself as baseValue
            if (baseValue == null)
                baseValue = new AverageStatisticImpl(id);
            baseValue.set(count, min, max, total, sumOfSquares, startTime, lastSampleTime);
            update(baseValue);
        } else if (validate(other)) {
            baseValue = (AverageStatisticImpl) other;
            update(baseValue);
        }
    }

    private boolean validate(WSStatistic other) {
        if (other == null)
            return false;
        if (!(other instanceof AverageStatisticImpl)) {
            // System.out.println("WARNING: wrong data type: must be AverageStatisticImpl");
            return false;
        }
        if (other.getId() != id) {
            //System.out.println("WARNING: wrong data Id: expect dataId=" + id
            //                   + ", got dataId=" + other.getId());
            return false;
        }
        return true;
    }
}
