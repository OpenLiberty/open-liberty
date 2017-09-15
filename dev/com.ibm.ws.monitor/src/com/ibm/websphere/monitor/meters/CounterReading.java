/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.meters;

import java.util.Date;

public class CounterReading extends com.ibm.websphere.monitor.jmx.CounterReading {

    public CounterReading(long count, String unit) {
        super(System.currentTimeMillis(), count, unit);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("count = ").append(count).append(" ").append(unit);
        sb.append(" at ").append(new Date(timestamp).toString());
        return sb.toString();
    }
}
