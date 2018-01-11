/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer.test.osgi.pmi;

import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal.AverageStatistic;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class TimeStatistic extends AverageStatistic {
    private final AtomicLong totalTime = new AtomicLong();

    @Override
    public String toString() {
        return super.toString() + "[count=" + getCount() + ", totalTime=" + totalTime + ']';
    }

    @Override
    public void add(long value) {
        super.add(value);
        totalTime.addAndGet(value);
    }

    public long getTotalTime() {
        return totalTime.get();
    }
}
