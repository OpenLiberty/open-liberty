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
//import com.ibm.wsspi.pmi.stat.SPIStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;

//import com.ibm.websphere.pmi.*;

/*
 * Implement JSR77's RangeStatistic interface.
 * Extended to provide time-weighted mean as well as current.
 *
 *  5/10/2004   Moved updateIntegral out of synchronized block for perfOptimization
 *              Added final keyword all increment & decrement methods
 */

public class RangeStatisticImpl extends StatisticImpl implements SPIRangeStatistic {
    private static final long serialVersionUID = -855214334683355657L;

    protected long highWaterMark = 0; //-1
    protected long lowWaterMark = 0; // -1
    protected long current = 0;
    protected double integral = 0;
    protected boolean initWaterMark = false;

    protected RangeStatisticImpl baseValue = null;

    public RangeStatisticImpl(int dataId) {
        super(dataId);
    }

    public RangeStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public RangeStatisticImpl(int id, long lowWaterMark, long highWaterMark,
                              long current, double integral,
                              long startTime, long lastSampleTime) {
        super(id, null, null, null, startTime, lastSampleTime);
        this.lowWaterMark = lowWaterMark;
        this.highWaterMark = highWaterMark;
        this.current = current;
        this.integral = integral;
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_RANGE;
    }

    public WSStatistic copy() {
        RangeStatisticImpl copy = new RangeStatisticImpl(id, lowWaterMark,
                        highWaterMark, current, integral, startTime, lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    public long getLowWaterMark() {
        return lowWaterMark;
    }

    public long getHighWaterMark() {
        return highWaterMark;
    }

    public long getCurrent() {
        return current;
    }

    public double getIntegral() {
        return integral;
    }

    public double getMean() {
        long timeDiff = lastSampleTime - startTime;
        if (timeDiff > 0)
            return (integral * 1.0) / timeDiff;
        else
            return 0;
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean resetAll) {
        if (resetAll)
            super.reset();
        integral = 0;
        lowWaterMark = 0; //-1
        highWaterMark = 0; // -1
        initWaterMark = false;
        // do not reset current because current will be there lifetime
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void setWaterMark(long val) {
        setWaterMark(System.currentTimeMillis(), val);
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void setWaterMark(long curTime, long val) {
        lastSampleTime = curTime;

        if (!initWaterMark) {
            lowWaterMark = highWaterMark = val;
            initWaterMark = true;
        } else {
            if (val < lowWaterMark)
                lowWaterMark = val;

            if (val > highWaterMark)
                highWaterMark = val;
        }
        /*
         * if(val < lowWaterMark || lowWaterMark < 0)
         * lowWaterMark = val;
         * 
         * if(val > highWaterMark)
         * highWaterMark = val;
         */
        current = val;
    }

    /**
     * Internal final method for perfOptimization
     * 
     */
    final private void updateWaterMark() {
        if (initWaterMark) {
            if (current < lowWaterMark)
                lowWaterMark = current;

            if (current > highWaterMark)
                highWaterMark = current;
        } else {
            lowWaterMark = highWaterMark = current;
            initWaterMark = true;
        }
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void set(long lowWaterMark, long highWaterMark, long current, double integral,
                    long startTime, long lastSampleTime) {
        this.current = current;
        this.integral = integral;
        this.lowWaterMark = lowWaterMark;
        this.highWaterMark = highWaterMark;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     * (no need to synchronize because val is calculated elsewhere)
     */
    public void set(long val) // V5: add()
    {
        if (current == val) {
            lastSampleTime = System.currentTimeMillis();
            return;
        }
        if (enabled) {
            long curTime = updateIntegral();
            setWaterMark(curTime, val);
        } else {
            current = val;
        }
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void set(long curTime, long val) // V5: add()
    {
        if (current == val) {
            lastSampleTime = curTime;
            return;
        }
        if (enabled) {
            updateIntegral(curTime);
            setWaterMark(curTime, val);
        } else {
            current = val;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment() {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral();
            synchronized (this) {
                ++current;
            }
            updateWaterMark();
        } else {
            ++current;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment(long incVal) {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral();
            synchronized (this) {
                current += incVal;
            }
            updateWaterMark();
        } else {
            current += incVal;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void increment(long curTime, long incVal) {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral(curTime);
            synchronized (this) {
                current += incVal;
            }
            updateWaterMark();
        } else {
            current += incVal;
        }
    }

    // the caller of this method should be in synchronization block
    final public void incrementWithoutSync(long curTime, long val) {
        if (enabled) {
            lastSampleTime = updateIntegral(curTime);
            current += val;
            updateWaterMark();
        } else {
            current += val;
        }
    }

    // synchronize ONLY if "synchronized update" flag is enabled
    final public void incrementWithSyncFlag(long curTime, long val) {
        if (enabled) {
            lastSampleTime = updateIntegral(curTime);
            if (sync) // synchronizedUpdate flag
            {
                synchronized (this) {
                    current += val;
                }
            } else {
                current += val;
            }
            updateWaterMark();
        } else {
            current += val;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void decrement() {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral();
            synchronized (this) {
                --current;
            }
            updateWaterMark();
        } else {
            current--;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void decrement(long decVal) {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral();
            synchronized (this) {
                current -= decVal;
            }
            updateWaterMark();
        } else {
            current -= decVal;
        }
    }

    /*
     * Synchronizable: counter is "updated" with the input value
     */
    final public void decrement(long curTime, long decVal) {
        if (enabled) { // synchronize the update of lastValue
            lastSampleTime = updateIntegral(curTime);
            synchronized (this) {
                current -= decVal;
            }
            updateWaterMark();
        } else {
            current -= decVal;
        }
    }

    final public void decrementWithoutSync(long curTime, long val) {
        if (enabled) {
            lastSampleTime = updateIntegral(curTime);
            current -= val;
            updateWaterMark();
        } else {
            current -= val;
        }
    }

    // synchronize ONLY if "synchronized update" flag is enabled
    final public void decrementWithSyncFlag(long curTime, long val) {
        if (enabled) {
            lastSampleTime = updateIntegral(curTime);
            if (sync) {
                synchronized (this) {
                    current -= val;
                }
            } else {
                current -= val;
                // to eliminate negative numbers
                if (current < 0)
                    current = 0;
            }
            updateWaterMark();
        } else {
            current -= val;
        }
    }

    /**
     * Server side method in order to calculate the time-weighted mean.
     */

    //public long myupdate()
    final public long updateIntegral() {
        long curTime = System.currentTimeMillis();
        integral += (curTime - lastSampleTime) * current;
        return curTime;
    }

    //public long myupdate(long curTime)
    final public long updateIntegral(long curTime) {
        integral += (curTime - lastSampleTime) * current;
        return curTime;
    }

    /*
     * Non-Synchronizable: counter is "replaced" with the input value. Caller should synchronize.
     */
    public void setLastValue(long val) {
        this.current = val;
    }

    public void combine(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        RangeStatisticImpl other = (RangeStatisticImpl) otherStat;
        // combine current
        current += other.current;

        // combine integral
        // Note: since the startTime and lastSampleTime may not
        //       be exactly same, the combine is an approximate value.
        // WARNING: Our best bet is curTime will be almost same when
        //          iterating each member.Otherwise, the weight of earlier
        //          combined member may be less than it should be.
        //          However, this minor inaccuracy should be okay.

        // Bring the otherStat integral to current time
        long curTime = other.updateIntegral();
        other.setLastSampleTime(curTime);

        if (this.lastSampleTime - this.startTime == 0) {
            this.updateIntegral(curTime);
            this.setLastSampleTime(curTime);
        }

        // avg2
        // Calcuate otherStat average
        double otherAvg = (other.getIntegral() * 1.0) / (curTime - other.getStartTime());

        // iTotal = i1 + (i2/t2)*t1
        // avgTotal = (i1 + (i2/t2)*t1) / t1 == i1/t1 + i2/t2 == avg1 + avg2
        // Calcuate otherStat integral for the thisStat time
        integral += (long) (otherAvg * (this.lastSampleTime - this.startTime));

        // Don't update lastSampleTime since the combine is based on this stat time
        //lastSampleTime = curTime;

        // Combining WaterMarks
        if (other.lowWaterMark < this.lowWaterMark)
            this.lowWaterMark = other.lowWaterMark;

        if (other.highWaterMark > this.highWaterMark)
            this.highWaterMark = other.highWaterMark;
    }

    public void update(WSStatistic otherStat) {
        if (!validate(otherStat))
            return;

        RangeStatisticImpl other = (RangeStatisticImpl) otherStat;
        if (baseValue == null) {
            this.integral = other.integral;
            this.startTime = other.startTime;
        } else {
            this.integral = other.integral - baseValue.integral;
            this.startTime = baseValue.lastSampleTime;
        }

        this.current = other.current;
        this.lowWaterMark = other.lowWaterMark;
        this.highWaterMark = other.highWaterMark;
        this.lastSampleTime = other.lastSampleTime;
    }

    public WSStatistic delta(WSStatistic otherStat) {
        if (!validate(otherStat))
            return null;

        RangeStatisticImpl other = (RangeStatisticImpl) otherStat;
        RangeStatisticImpl newData = new RangeStatisticImpl(id);
        newData.current = current - other.current;
        newData.integral = integral - other.integral;
        newData.startTime = other.lastSampleTime;
        newData.lastSampleTime = lastSampleTime;

        return newData;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        if (!validate(otherStat)) {
            return null;
        }

        RangeStatisticImpl other = (RangeStatisticImpl) otherStat;
        RangeStatisticImpl newData = new RangeStatisticImpl(id);

        // calculating rate per second
        long timeDiff = (lastSampleTime - other.lastSampleTime) / 1000;

        if (timeDiff == 0) {
            return null;
        }

        // rate of change
        newData.current = (current - other.current) / timeDiff;
        newData.integral = (integral - other.integral) / timeDiff;

        newData.startTime = startTime;
        newData.lastSampleTime = lastSampleTime;
        newData.lowWaterMark = lowWaterMark;
        newData.highWaterMark = highWaterMark;

        return newData;
    }

    public void resetOnClient(WSStatistic other) {
        if (other == null) {
            // use itself as baseValue
            if (baseValue == null)
                baseValue = new RangeStatisticImpl(id);
            baseValue.set(lowWaterMark, highWaterMark, current, integral, startTime, lastSampleTime);
            update(baseValue);
        } else if (validate(other)) {
            baseValue = (RangeStatisticImpl) other;
            update(baseValue);
        }
    }

    private boolean validate(WSStatistic other) {
        if (other == null)
            return false;
        if (!(other instanceof RangeStatisticImpl)) {
            // System.out.println("WARNING: wrong data type: must be CountStatistic");
            return false;
        }
        if (other.getId() != id) {
            // System.out.println("WARNING: wrong data Id: expect dataId=" + id
            //                    + ", got dataId=" + other.getId());
            return false;
        }
        return true;
    }

    public void cleanup() {
        current = 0;
        integral = 0;
        lastSampleTime = System.currentTimeMillis();
        lowWaterMark = 0; //-1
        highWaterMark = 0; //-1

        initWaterMark = false;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer(super.toString(""));
        str.append(", type=").append("RangeStatistic");
        str.append(", lowWaterMark=");
        str.append(lowWaterMark);
        str.append(", highWaterMark=");
        str.append(highWaterMark);
        str.append(", current=");
        str.append(current);
        str.append(", integral=");
        str.append(integral);
        return str.toString();
    }

    public String toStringforBRS(String indent) {
        StringBuffer str = new StringBuffer(super.toString(""));
        str.append(", type=").append("BoundedRangeStatistic");
        str.append(", lowWaterMark=");
        str.append(lowWaterMark);
        str.append(", highWaterMark=");
        str.append(highWaterMark);
        str.append(", current=");
        str.append(current);
        str.append(", integral=");
        str.append(integral);
        return str.toString();
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<RS id=\"");
        res.append(id);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" lWM=\"");
        res.append(lowWaterMark);
        res.append("\" hWM=\"");
        res.append(highWaterMark);
        res.append("\" cur=\"");
        res.append(current);
        res.append("\" int=\"");
        res.append(integral);
        res.append("\"></RS>");

        return res.toString();
    }
}
