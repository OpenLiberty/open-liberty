/*******************************************************************************
 * Copyright (c) 1997, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.stat;

import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.PmiDataInfo;

/**
 * WebSphere PMI Statistic interface.
 * 
 * @ibm-api
 */

public interface WSStatistic {
    /** Returns the name of this statistic. */
    public String getName();

    /** Returns the unit of measurement for this statistic. */
    public String getUnit();

    /** Returns the description of this statistic. */
    public String getDescription();

    /** Returns the time the first measurement was taken represented as a long. */
    public long getStartTime();

    /** Returns the time the most recent measurement was taken represented as a long. */
    public long getLastSampleTime();

    /**
     * Set textual information. If the text information like name, description, and unit are null then this
     * method can be used to bind the text information to the Stats. The text information
     * will be set by default.
     * 
     * @see com.ibm.websphere.pmi.stat.WSStatsHelper
     * 
     */
    public void setDataInfo(PmiModuleConfig config);

    /**
     * Set textual information. If the text information like name, description, and unit are null then this
     * method can be used to bind the text information to the Stats. The text information
     * will be set by default.
     * 
     * @see com.ibm.websphere.pmi.stat.WSStatsHelper
     * 
     */
    public void setDataInfo(PmiDataInfo info);

    /**
     * Returns the Statistic ID
     */
    public int getId();

    /**
     * Returns the statistic config information.
     */
    public PmiDataInfo getDataInfo();

    /**
     * Updates this statistic with the given value
     * 
     * @param newStatistic must have the same statistic ID and type
     */
    public void update(WSStatistic newStatistic);

    /**
     * Returns the difference between this statistic and the parameter otherStatistic
     * 
     * @param otherStatistic must have the same statistic ID and type
     * @return a Statistic object whose value is (this - otherStatistic)
     */
    public WSStatistic delta(WSStatistic otherStatistic);

    /**
     * Returns the aggregate the value of this statistic and parameter otherStatistic
     * 
     * @param otherStatistic must have the same statistic ID and type
     */
    public void combine(WSStatistic otherStatistic);

    /**
     * Resets the statistic with the parameter otherStatistic. When the parameter otherStatistic is null
     * the statistic will be reset to zero. Note that the reset happens only the client side and not in the server side.
     * 
     * @param otherStatistic must have the same statistic ID and type
     */
    public void resetOnClient(WSStatistic otherStatistic);

    /**
     * Returns a new copy of this statistic
     */
    public WSStatistic copy();

    /**
     * Returns the rate of change of this statistic with respect to the parameter otherStatistic.
     * 
     * @param otherStatistic must have the same statistic ID and type
     */
    public WSStatistic rateOfChange(WSStatistic otherStatistic);

    /** Return the XML representation of this statistic */
    public String toXML();

    /** Returns the String representation of this statistic */
    public String toString();
    //public String toString(String indent);
}
