/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
}
