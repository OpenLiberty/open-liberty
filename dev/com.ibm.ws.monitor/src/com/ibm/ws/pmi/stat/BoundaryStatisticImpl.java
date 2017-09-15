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
import com.ibm.wsspi.pmi.stat.SPIBoundaryStatistic;

public class BoundaryStatisticImpl extends StatisticImpl implements SPIBoundaryStatistic {
    private static final long serialVersionUID = -540939538865302765L;

    protected long upperBound = 0; //-1;
    protected long lowerBound = 0; //-1;

    public BoundaryStatisticImpl(int dataId) {
        super(dataId);
    }

    public BoundaryStatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        super(id, name, unit, description, startTime, lastSampleTime);
    }

    public BoundaryStatisticImpl(int id, long startTime, long lastSampleTime, long upperBound, long lowerBound) {
        super(id);
        set(lowerBound, upperBound, startTime, lastSampleTime);
    }

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_BOUNDARY;
    }

    public WSStatistic copy() {
        BoundaryStatisticImpl copy = new BoundaryStatisticImpl(id, startTime,
                        lastSampleTime, upperBound, lowerBound);

        return copy;
    }

    public void set(long lowerBound, long upperBound, long startTime, long lastSampleTime) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;
    }

    // set the value
    public void setLowerBound(long val) {
        // not updating the lastSampleTime
        this.lowerBound = val;
    }

    public void setUpperBound(long val) {
        // not updating the lastSampleTime
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

    public void combine(WSStatistic other) {
        // Do nothing
    }

    public void update(WSStatistic data) {}

    public WSStatistic delta(WSStatistic data) {
        return null;
    }

    public WSStatistic rateOfChange(WSStatistic otherStat) {
        // operation does not apply to boundary stats
        return null;
    }

    public void resetOnClient(WSStatistic data) {}

    public void reset(boolean resetAll) {
        if (resetAll) {
            super.reset();
        }

    }

    public String toXML(String indent) {
        StringBuffer res = new StringBuffer();
        res.append(indent);
        res.append("<BS");
        res.append(" id=\"");
        res.append(id);
        res.append("\" sT=\"");
        res.append(startTime);
        res.append("\" lST=\"");
        res.append(lastSampleTime);
        res.append("\" lB=\"");
        res.append(lowerBound);
        res.append("\" uB=\"");
        res.append(upperBound);
        res.append("\">\n");
        res.append(indent);
        res.append("</BS>");

        return res.toString();
    }
}