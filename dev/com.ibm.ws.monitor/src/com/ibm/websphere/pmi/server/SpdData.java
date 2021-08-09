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

package com.ibm.websphere.pmi.server;

import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.ws.pmi.stat.*;

public interface SpdData extends java.io.Serializable {

    // return the data id
    public int getId();

    // mark the data enabled and reset the value and createTime
    public void enable(int level);

    // mark the data disabled
    public void disable();

    // return if the data is enabled
    public boolean isEnabled();

    // reset the value and create time
    public void reset();

    public void reset(boolean resetAll);

    public void setDataInfo(PmiModuleConfig moduleConfig);

    // return a wire level data
    public StatisticImpl getStatistic();

    // compare the dataId and return -1, 0, 1 for less, equal, and greater
    public int compareTo(SpdData other);

    public boolean isExternal();

    public void updateExternal();

    public boolean isAggregate();
}
