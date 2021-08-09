/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
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
//import com.ibm.ws.pmi.server.*;
//import com.ibm.websphere.pmi.stat.*;

import com.ibm.ws.pmi.stat.*;

// To reduce overhead of instrumentation, none of SpdData and its 
// subclasses takes care of synchronization. This should not affect the 
// overall trend of any data. But if it is really important, it is 
// caller's responsibility to synchronize.

/**
 * The abstract base class to implement SpdData.
 * When remove data, disable it but still keep the object.
 */
public abstract class SpdDataImpl implements SpdData, PmiConstants {
    // TODO: dataId is also copied to Statistic data, which is duplicate.
    //       Will remove one of them. They are consistent.
    final protected int dataId;
    protected boolean enabled = true;
    protected boolean sync = false;

    // Constructor used for all data in config file
    // The caller is responsible of checking if dataId is UNKNOWN_ID 
    public SpdDataImpl(PmiModuleConfig moduleConfig, String name) {
        if (moduleConfig != null)
            dataId = moduleConfig.getDataId(name);
        else
            dataId = UNKNOWN_ID;
    }

    // Constructor using a given data id
    public SpdDataImpl(int id) {
        dataId = id;
    }

    // return the data id
    public int getId() {
        return dataId;
    }

    // mark the data enabled and reset the value and createTime
    public void enable(int level) {
        if (level >= PmiConstants.LEVEL_HIGH)
            sync = true;
        else
            sync = false;

        if (!enabled) {
            enabled = true;
            reset();
        }
    }

    // mark the data disabled
    public void disable() {
        enabled = false;
    }

    // return if the data is enabled
    public boolean isEnabled() {
        return enabled;
    }

    // compare the dataId and return -1, 0, 1 for less, equal, and greater
    public int compareTo(SpdData other) {
        if (dataId < other.getId()) {
            return -1;
        } else if (dataId > other.getId()) {
            return 1;
        } else {
            return 0;
        }
    }

    public void reset() {
        reset(true);
    }

    public void setDataInfo(PmiModuleConfig moduleConfig) {
        StatisticImpl stat = getStatistic();
        if (stat != null)
            stat.setDataInfo(moduleConfig);
    }

    // reset the value and create time
    public abstract void reset(boolean resetAll);

    public abstract StatisticImpl getStatistic();

    public boolean isExternal() {
        return false;
    }

    public void updateExternal() {}

    public boolean isAggregate() {
        return false;
    }
}
