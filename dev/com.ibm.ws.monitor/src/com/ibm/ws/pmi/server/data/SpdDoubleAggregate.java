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
//import com.ibm.websphere.pmi.stat.*;
import com.ibm.ws.pmi.server.PmiUtil;

import com.ibm.ws.pmi.stat.*;

public class SpdDoubleAggregate extends SpdGroupBase {
    private static final long serialVersionUID = -2943019258580587421L;

    SpdDouble myValue = null;

    // Constructor
    public SpdDoubleAggregate(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        myValue = new SpdDoubleImpl(dataId);
    }

    public SpdDoubleAggregate(int dataId) {
        super(dataId);
        myValue = new SpdDoubleImpl(dataId);
    }

    // Check data type and call super.add to add data - synchronized in super
    public boolean add(SpdData data) {
        if (data == null)
            return false;
        if (data instanceof SpdDouble) {
            return super.add(data);
        } else {
            return false;
        }
    }

    // Check data type and call super to remove data - synchronized in super
    public boolean remove(SpdData data) {
        if (data == null)
            return false;
        if (data instanceof SpdDouble) {
            return super.remove(data);
        } else {
            return false;
        }
    }

    // reset - do nothing for aggregate data
    public void reset(boolean resetAll) {}

    // Return a wire level data using given time
    public StatisticImpl getStatistic() {
        StatisticImpl aggStat = getSpdDouble().getStatistic();
        aggStat.setLastSampleTime(PmiUtil.currentTime());
        return aggStat;
    }

    // Return a SpdDouble that combines all the members of the aggregate data
    // Recursive.
    protected SpdDouble getSpdDouble() {
        myValue.reset(false);
        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (member == null)
                continue;
            if (member instanceof SpdDouble) {
                myValue.combine((SpdDouble) member);
            } else { // SpdDoubleAggregate
                myValue.combine(((SpdDoubleAggregate) member).getSpdDouble());
            }
        }
        return myValue;
    }

    public void updateAggregate() {
        myValue.reset(false);
        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (member == null)
                continue;
            if (member instanceof SpdDouble) {
                myValue.combine((SpdDouble) member);
            } else { // SpdDoubleAggregate
                myValue.combine(((SpdDoubleAggregate) member).getSpdDouble());
            }
        }
    }
}
