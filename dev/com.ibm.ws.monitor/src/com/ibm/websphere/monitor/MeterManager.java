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

package com.ibm.websphere.monitor;

import java.util.Collection;

import com.ibm.websphere.monitor.meters.CounterReading;
import com.ibm.websphere.monitor.meters.GaugeReading;
import com.ibm.websphere.monitor.meters.StatisticsReading;

public interface MeterManager {

    public CounterReading getCounterReading(String counterName);

    public GaugeReading getGaugeReading(String gaugeName);

    public StatisticsReading getStatisticsReading(String statisticsMeterName);

    public MeterType getMeterType(String meterName);

    public Collection<String> getMeterNames();
}
