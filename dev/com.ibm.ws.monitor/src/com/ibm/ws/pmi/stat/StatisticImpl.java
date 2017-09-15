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

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiDataInfo;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * The <code> StatisticImpl </code> is an abstract class. It is the base class
 * for all the XXXStatisticImpl classes.
 */
public abstract class StatisticImpl implements SPIStatistic, java.io.Serializable {
    static final long serialVersionUID = 1358353157061734347L;
    protected int id;
    long startTime = 0;
    long lastSampleTime = 0;

    // booleans to do data management
    protected transient boolean enabled = true;
    transient boolean sync = false;
    //transient Object syncObj = new Object();

    // Note the following are transient members, it will only be set on client side.
    // It is always null on server side. 
    transient PmiDataInfo dataInfo = null; // used to retrieve the static info on client side

    protected static final int TYPE = 0;

    public int getStatisticType() {
        return WSStatTypes.STATISTIC_UNDEFINED;
    }

    // constructor
    public StatisticImpl(int id) {
        this.id = id;
        startTime = System.currentTimeMillis();
        lastSampleTime = startTime;
    }

    public StatisticImpl(int id, String name, String unit, String description, long startTime, long lastSampleTime) {
        this.id = id;
        this.startTime = startTime;
        this.lastSampleTime = lastSampleTime;

    }

    // ------------ JSR77 methods -------------
    public String getName() {
        if (dataInfo != null)
            return dataInfo.getName();
        else
            return null;
    }

    public String getUnit() {
        if (dataInfo != null)
            return dataInfo.getUnit();
        else
            return null;
    }

    public String getDescription() {
        if (dataInfo != null)
            return dataInfo.getDescription();
        else
            return null;
    }

    /**
     * @return the time when the data is started
     */
    public long getStartTime() {
        return startTime;
    }

    public long getLastSampleTime() {
        return lastSampleTime;
    }

    // ------------ JSR77 methods -------------

    /**
     * Server side API only: Set the data enabled and reset the value and createTime
     */
    public void enable(int level) {
        // 5.0 LEVEL_MAX or 6.0 fine-grained enable sync
        if (level >= PmiConstants.LEVEL_MAX) {
            sync = true;
        } else {

            sync = false;
        }

        if (!enabled) {
            enabled = true;
            reset();
        }
    }

    /**
     * Server side API only: Set the data disabled
     */
    public void disable() {
        enabled = false;
        sync = false;
    }

    /**
     * return if the data is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Reset the createTime
     */
    public void reset() {
        startTime = System.currentTimeMillis();
        lastSampleTime = startTime;
        // need to reset the value in subclass
    }

    // setters
    public void setDataInfo(PmiModuleConfig config) {
        if (config != null)
            dataInfo = config.getDataInfo(id);
    }

    public void setDataInfo(PmiDataInfo info) {
        dataInfo = info;
    }

    public void mSetDataInfo(PmiModuleConfig config) {
        if (config != null) {
            if (config.getDataInfo(id) != null) {
                dataInfo = config.getDataInfo(id);
            }
        }
    }

    public void setLastSampleTime(long lastSampleTime) {
        this.lastSampleTime = lastSampleTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getId() {
        return id;
    }

    public PmiDataInfo getDataInfo() {
        return dataInfo;
    }

    public String toXML() {
        StringBuffer res = new StringBuffer();
        res.append("<Statistic id=\"");
        res.append(id);
        res.append("\" name=\"");
        res.append(getName());
        res.append("\" description=\"");
        res.append(getDescription());
        res.append("\" unit=\"");
        res.append(getUnit());
        res.append("\" startTime=\"");
        res.append(startTime);
        res.append("\" lastSampleTime=\"");
        res.append(lastSampleTime);
        res.append("</Statistic>");

        return res.toString();
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    public String toString(String indent) {
        StringBuffer str = new StringBuffer();
        str.append("name=").append(getName());
        str.append(", ID=").append(id);
        str.append(", description=").append(getDescription());
        str.append(", unit=").append(getUnit());
        //str.append ("StartTime: ").append(startTime).append ("\n");

        /*
         * str.append("id=");
         * str.append(id);
         * str.append(" name=");
         * str.append(getName());
         * str.append(" description=");
         * str.append(getDescription());
         * str.append(" unit=");
         * str.append(getUnit());
         * str.append(" startTime=");
         * str.append(startTime);
         * //str.append(" lastSampleTime=");
         * //str.append(lastSampleTime);
         */
        return str.toString();
    }

    /**
     * Update itself with the new value in data.
     * 
     * @param data must have the same data ID and type
     */
    abstract public void update(WSStatistic data);

    /**
     * @param data must have the same data ID and type
     * @return an Statistic object whose value is the difference of (this - data)
     */
    abstract public WSStatistic delta(WSStatistic data);

    /**
     * @param wss must have the same data ID and type
     * @return Statistic object whose value is the rate of change
     */
    abstract public WSStatistic rateOfChange(WSStatistic wss);

    /**
     * @return Statistic that is a copy of this
     */
    abstract public WSStatistic copy();

    /**
     * Aggregate the value of parameter data to this data
     * 
     * @param data must have the same data ID and type
     */
    abstract public void combine(WSStatistic data);

    /**
     * Reset the data value to zero on client side. When using update method, the value
     * will always be the value since the last reset is called.
     * 
     * @param data must have the same data ID and type
     */
    abstract public void resetOnClient(WSStatistic data);

    abstract public void reset(boolean resetAll);
}
