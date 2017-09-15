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
import com.ibm.ws.pmi.stat.*;

public class SpdLoadAggregate extends SpdGroupBase {
    private static final long serialVersionUID = -4157752241423356179L;

    private SpdLoad myValue = null;

    // Constructor
    public SpdLoadAggregate(PmiModuleConfig moduleConfig, String name) {
        super(moduleConfig, name);
        myValue = new SpdLoadImpl(dataId);
    }

    public SpdLoadAggregate(int dataId) {
        super(dataId);
        myValue = new SpdLoadImpl(dataId);
    }

    public SpdLoadAggregate(int type, int dataId) {
        super(dataId);
        myValue = new SpdLoadImpl(type, dataId);
    }

    // Check data type and call super.add to add data - synchronized in super
    public boolean add(SpdData data) {
        if (data == null)
            return false;
        if (data instanceof SpdLoad) {
            return super.add(data);
        } else {
            return false;
        }
    }

    // Check data type and call super to remove data - synchronized in super
    public boolean remove(SpdData data) {
        if (data == null)
            return false;
        if (data instanceof SpdLoad) {
            return super.remove(data);
        } else {
            return false;
        }
    }

    // reset - do nothing for aggregate data
    public void reset(boolean resetAll) {
        myValue.reset(resetAll);
    }

    // Return a wire level data using given time
    public StatisticImpl getStatistic() {
        return getSpdLoad().getStatistic();
    }

    // Return a SpdLoad that combines all the members of the aggregate data
    // Recursive.
    private SpdLoad getSpdLoad() {
        myValue.cleanup(); // reset data but not time
        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (member == null)
                continue;
            if (member instanceof SpdLoad) {
                myValue.combine((SpdLoad) member);
            } else { // SpdLoadAggregate
                myValue.combine(((SpdLoadAggregate) member).getSpdLoad());
            }
        }
        return myValue;
    }

    public void updateAggregate() {
        myValue.cleanup(); // reset data but not time
        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (member == null)
                continue;
            if (member instanceof SpdLoad) {
                myValue.combine((SpdLoad) member);
            } else { // SpdLoadAggregate
                myValue.combine(((SpdLoadAggregate) member).getSpdLoad());
            }
        }
    }
}
