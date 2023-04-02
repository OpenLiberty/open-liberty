/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 * RESTful Stats MXBean
 * 
 */
public interface RestStatsMXBean extends com.ibm.websphere.jaxrs.monitor.RestStatsMXBean {

    @Override
    public Counter getRequestCountDetails();

    @Override
    public StatisticsMeter getResponseTimeDetails();
    
    @Override
    public long getMinuteLatestMinimumDuration();
    
    @Override
    public long getMinuteLatestMaximumDuration();
    
    @Override
    public long getMinuteLatest();
    
    @Override
    public long getMinutePreviousMinimumDuration();
    
    @Override
    public long getMinutePreviousMaximumDuration();
    
    @Override
    public long getMinutePrevious();
}
