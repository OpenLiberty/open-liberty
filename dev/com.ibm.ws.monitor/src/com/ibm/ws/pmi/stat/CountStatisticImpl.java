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
import com.ibm.wsspi.pmi.stat.SPICountStatistic;

/**
 * Implement JSR77's CountStatistic interface.
 */
public class CountStatisticImpl extends StatisticImpl implements SPICountStatistic {
    private static final long serialVersionUID = 6335644998767409978L;

    protected long count = 0;
    protected CountStatisticImpl baseValue = null;

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_COUNT;
    }

    public CountStatisticImpl(int dataId) {
        super(dataId);
    }

    public CountStatisticImpl(int dataId, long count, long startTime, long lastSampleTime) {
        super(dataId, null, null, null, startTime, lastSampleTime);
        this.count = count;
    }

    public CountStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public WSStatistic copy() {
        CountStatisticImpl copy = new CountStatisticImpl(id, count, startTime,
                        lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    // JSR77 method
    public long getCount() {
        return count;
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean resetAll) {
        if (resetAll) {
            // reset startTime and lastSampleTime in super class
            super.reset();
        }

        count = 0;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void set(long count, long startTime, long lastSampleTime) {
        this.count = count;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
    }

    // combine the value of this data and other data
    public void combine(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        CountStatisticImpl other = (CountStatisticImpl) otherStat;
        count += other.count;

        // Compare lastSampleTime
        if (other.lastSampleTime > lastSampleTime)
            lastSampleTime = other.lastSampleTime;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     * (no need to synchronize since the value is calculated elsewhere)
     */
    public void setCount(long value) {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        count = value;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void setCount(long value, long lastSampleTime) {
        if (!enabled)
            return;

        this.lastSampleTime = lastSampleTime;
        count = value;
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment(long time, long value) {
        if (!enabled)
            return;

        lastSampleTime = time;
        if (!sync) {
            count += value;
        } else {
            synchronized (this) {
                count += value;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void decrement(long time, long value) {
        if (!enabled)
            return;

        lastSampleTime = time;
        if (!sync) {
            count -= value;
        } else {
            synchronized (this) {
                count -= value;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment() {
        if (!enabled)
            return;

        lastSampleTime = System.currentTimeMillis();
        if (!sync) {
            ++count;
        } else {
            synchronized (this) {
                ++count;
            }
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment(long val) {
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
    final public void decrement() {
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
    final public void decrement(long val) {
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
        str.append(", type=").append("CountStatistic");
        str.append(", count=").append(count);

        return str.toString();
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<CS");
        res.append(" id=\"");
        res.append(id);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" ct=\"");
        res.append(count);
        res.append("\">\n");
        res.append("</CS>");

        return res.toString();
    }

    public void update(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        CountStatisticImpl other = (CountStatisticImpl) otherStat;
        if (baseValue == null) {
            this.count = other.getCount();
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

        CountStatisticImpl other = (CountStatisticImpl) otherStat;
        CountStatisticImpl newData = new CountStatisticImpl(id);
        newData.count = count - other.getCount();
        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        if (!validate(otherStat)) {
            return null;
        }

        CountStatisticImpl other = (CountStatisticImpl) otherStat;
        CountStatisticImpl newData = new CountStatisticImpl(id);

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
                baseValue = new CountStatisticImpl(id);
            baseValue.set(count, startTime, lastSampleTime);
            update(baseValue);
        } else if (validate(other)) {
            baseValue = (CountStatisticImpl) other;
            update(baseValue);
        }
    }

    private boolean validate(WSStatistic other) {
        if (other == null)
            return false;
        if (!(other instanceof CountStatisticImpl)) {
            // System.out.println("WARNING: wrong data type: must be CountStatisticImpl");
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
