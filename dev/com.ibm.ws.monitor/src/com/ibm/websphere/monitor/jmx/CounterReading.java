/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.jmx;

import java.beans.ConstructorProperties;

/**
 * Represents a snapshot of a {@link Counter}. A CounterReading holds the current value of the Counter at the time
 * it was obtained and will not change.
 * 
 * @ibm-api
 */
public class CounterReading {

    protected long timestamp;
    protected long count;
    protected String unit;

    /**
     * Constructor used during construction of proxy objects for MXBeans.
     */
    @ConstructorProperties({ "timestamp", "count", "unit" })
    public CounterReading(long timestamp, long count, String unit) {
        this.timestamp = timestamp;
        this.count = count;
        this.unit = unit;
    }

    /**
     * @return timestamp of the counter reading
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return counter value at time of snapshot
     */
    public long getCount() {
        return count;
    }

    /**
     * @return unit of measurement of the counter
     */
    public String getUnit() {
        return unit;
    }
}
