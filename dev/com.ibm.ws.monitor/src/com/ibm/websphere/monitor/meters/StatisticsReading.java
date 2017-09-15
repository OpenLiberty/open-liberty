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

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class StatisticsReading extends com.ibm.websphere.monitor.jmx.StatisticsReading {

    public StatisticsReading(long count, long min, long max, double total, double mean, double variance, double stddev, String unit) {
        super(System.currentTimeMillis(), count, min, max, total, mean, variance, stddev, unit);
    }

    @Override
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        StringBuilder sb = new StringBuilder();

        sb.append("count=").append(count);
        sb.append(" min=").append(minimumValue);
        sb.append(" max=").append(maximumValue);
        sb.append(" mean=").append(decimalFormat.format(mean));
        sb.append(" variance=").append(decimalFormat.format(variance));
        sb.append(" stddev=").append(decimalFormat.format(standardDeviation));
        sb.append(" total=").append(Math.round(total));

        return sb.toString();
    }
}
