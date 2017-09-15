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

package com.ibm.ws.pmi.stat;

import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.wsspi.pmi.stat.SPIDoubleStatistic;

/**
 * It is similar to CountStatisticImpl but contains a double member.
 */
public class DoubleStatisticImpl extends StatisticImpl implements SPIDoubleStatistic {
    private static final long serialVersionUID = -8967426759755629774L;

    protected double count = 0;
    protected DoubleStatisticImpl baseValue = null;

    public DoubleStatisticImpl(int dataId) {
        super(dataId);
    }

    public DoubleStatisticImpl(int dataId, double count, long startTime, long lastSampleTime) {
        super(dataId, null, null, null, startTime, lastSampleTime);
        this.count = count;
    }

    public DoubleStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_DOUBLE;
    }

    public WSStatistic copy() {
        DoubleStatisticImpl copy = new DoubleStatisticImpl(id, count, startTime,
                        lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean resetAll) {
        if (resetAll)
            super.reset();
        count = 0;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void set(double count, long startTime, long lastSampleTime) {
        this.count = count;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
    }

    // getters
    public double getDouble() {
        if (baseValue == null)
            return count;
        else
            return count - baseValue.getDouble();
    }

    // combine the value of this data and other data
    public void combine(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        DoubleStatisticImpl other = (DoubleStatisticImpl) otherStat;
        count += other.count;
        //TODO: how to update lastSampleTime???
        //if (other.lastSampleTime > lastSampleTime)
        //  lastSampleTime = other.lastSampleTime;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     * (No need to synchronize since the value is calculated elsewhere)
     */
    public void setDouble(double value) {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        count = value;
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    public void increment() {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        if (!sync) {
            count++;
        } else {
            synchronized (this) {
                count++;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    public void increment(double val) {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        if (!sync) {
            count += val;
        } else {
            synchronized (this) {
                count += val;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    public void decrement() {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        if (!sync) {
            count--;
        } else {
            synchronized (this) {
                count--;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    public void decrement(double val) {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        if (!sync) {
            count -= val;
        } else {
            synchronized (this) {
                count -= val;
            }
        }
    }

    public String toString() {
        return this.toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer(super.toString(""));
        str.append(", type=").append("DoubleStatistic");
        str.append(", count=").append(count);

        return str.toString();
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<DS");
        res.append(" id=\"");
        res.append(id);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" ct=\"");
        res.append(count);
        res.append("\">\n");
        res.append("</DS>");

        return res.toString();
    }

    public void update(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        DoubleStatisticImpl other = (DoubleStatisticImpl) otherStat;
        if (baseValue == null) {
            this.count = other.getDouble();
            this.startTime = other.getStartTime();
        } else {
            this.count = other.count - baseValue.count;
            this.startTime = baseValue.lastSampleTime;
        }
        this.lastSampleTime = other.lastSampleTime;
    }

    public WSStatistic delta(WSStatistic otherStat) {
        if (!validate(otherStat))
            return null;

        DoubleStatisticImpl other = (DoubleStatisticImpl) otherStat;
        DoubleStatisticImpl newData = new DoubleStatisticImpl(id);
        newData.count = count - other.getDouble();
        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        if (!validate(otherStat)) {
            return null;
        }

        DoubleStatisticImpl other = (DoubleStatisticImpl) otherStat;
        DoubleStatisticImpl newData = new DoubleStatisticImpl(id);

        // calculating rate per second
        long timeDiff = (lastSampleTime - other.lastSampleTime) / 1000;

        if (timeDiff == 0) {
            return null;
        }

        // rate of change
        newData.count = (count - other.count) / timeDiff;

        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }

    public void resetOnClient(WSStatistic other) {
        if (other == null) {
            // use itself as baseValue
            if (baseValue == null)
                baseValue = new DoubleStatisticImpl(id);
            baseValue.set(count, startTime, lastSampleTime);
            update(baseValue);
        } else if (validate(other)) {
            baseValue = (DoubleStatisticImpl) other;
            update(baseValue);
        }
    }

    private boolean validate(WSStatistic other) {
        if (other == null)
            return false;
        if (!(other instanceof DoubleStatisticImpl)) {
            // System.out.println("WARNING: wrong data type: must be DoubleStatisticImpl");
            return false;
        }
        if (other.getId() != id) {
            // System.out.println("WARNING: wrong data Id: expect dataId=" + id
            //                    + ", got dataId=" + other.getId());
            return false;
        }
        return true;
    }
}
