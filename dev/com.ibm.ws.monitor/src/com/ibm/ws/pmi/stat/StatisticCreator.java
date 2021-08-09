/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Oct 21, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ws.pmi.stat;

import com.ibm.wsspi.pmi.stat.SPIAverageStatistic;
import com.ibm.wsspi.pmi.stat.SPIBoundaryStatistic;
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIDoubleStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPITimeStatistic;

/**
 * @author joelm
 * 
 *         To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StatisticCreator {

    public static SPIAverageStatistic createAverageStatistic(int dataId) {
        return new AverageStatisticImpl(dataId);
    }

    public static SPIBoundaryStatistic createBoundaryStatistic(int dataId) {
        return new BoundaryStatisticImpl(dataId);
    }

    public static SPIBoundedRangeStatistic createBoundedRangeStatistic(int dataId) {
        return new BoundedRangeStatisticImpl(dataId);
    }

    public static SPICountStatistic createCountStatistic(int dataId) {
        return new CountStatisticImpl(dataId);
    }

    public static SPIDoubleStatistic createDoubleStatistic(int dataId) {
        return new DoubleStatisticImpl(dataId);
    }

    public static SPIRangeStatistic createRangeStatistic(int dataId) {
        return new RangeStatisticImpl(dataId);
    }

    public static SPITimeStatistic createTimeStatistic(int dataId) {
        return new TimeStatisticImpl(dataId);
    }
}
