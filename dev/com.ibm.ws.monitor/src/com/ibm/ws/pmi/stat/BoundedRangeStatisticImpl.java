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
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;

//import com.ibm.websphere.pmi.*;

//  Note: Java does not allow extends multiple superclasses. So we have implement 
//        one interface and extends a class.

/**
 * Implement JSR77's BoundedRangeStatistic.
 */
public class BoundedRangeStatisticImpl extends RangeStatisticImpl implements SPIBoundedRangeStatistic {
    private static final long serialVersionUID = -6143293937412368962L;

    protected long upperBound = 0; //-1;
    protected long lowerBound = 0; //-1;

    public BoundedRangeStatisticImpl(int dataId) {
        super(dataId);
    }

    public BoundedRangeStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    // constructor 
    public BoundedRangeStatisticImpl(int id, long lowerBound, long upperBound,
                                     long lowWaterMark, long highWaterMark,
                                     long current, double integral,
                                     long startTime, long lastSampleTime) {
        super(id, lowWaterMark, highWaterMark, current, integral, startTime, lastSampleTime);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_BOUNDEDRANGE;
    }

    public WSStatistic copy() {
        BoundedRangeStatisticImpl copy = new BoundedRangeStatisticImpl(id,
                        lowerBound, upperBound, lowWaterMark, highWaterMark, current,
                        integral, startTime, lastSampleTime);
        copy.baseValue = this.baseValue;

        return copy;
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean resetAll) {
        super.reset(resetAll);
    }

    public void set(long lowerBound, long upperBound,
                    long lowWaterMark, long highWaterMark,
                    long current, double integral,
                    long startTime, long lastSampleTime) {
        super.set(lowWaterMark, highWaterMark, current, integral, startTime, lastSampleTime);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public void set(long lowerBound, long upperBound, long startTime, long lastSampleTime) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
    }

    public void cleanup() {
        // set every member to its initialization value
        super.cleanup();
        lowerBound = 0;
        upperBound = 0;
    }

    public void setLowerBound(long val) {
        this.lowerBound = val;

        // the current impl is to set lowWaterMark = lowBound
        // since the resource level always starts at lowBound
        super.lowWaterMark = super.highWaterMark = val;
        super.initWaterMark = true;
    }

    public void setUpperBound(long val) {
        this.upperBound = val;
    }

    // getters
    public long getLowerBound() {
        return lowerBound;
    }

    // combine the value of this data and other data
    public long getUpperBound() {
        return upperBound;
    }

    public void combine(BoundedRangeStatisticImpl other) {
        if (other == null)
            return;

        super.combine((RangeStatisticImpl) other);

        // TODO: how to combine lowerBound and upperBound? - not set them?

    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer(super.toStringforBRS(""));
        str.append(", lowerBound=").append(lowerBound);
        str.append(", upperBound=").append(upperBound);
        return str.toString();
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<BRS");
        res.append(" id=\"");
        res.append(id);
        res.append("\" lWM=\"");
        res.append(lowWaterMark);
        res.append("\" hWM=\"");
        res.append(highWaterMark);
        res.append("\" cur=\"");
        res.append(current);
        res.append("\" int=\"");
        res.append(integral);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" lB=\"");
        res.append(lowerBound);
        res.append("\" uB=\"");
        res.append(upperBound);
        res.append("\">\n");
        res.append("</BRS>");

        return res.toString();
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        // operation does not apply to boundary stats
        return null;
    }
}
