/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 * Servlet Stats MXBean
 * 
 */
public interface ServletStatsMXBean extends com.ibm.websphere.webcontainer.ServletStatsMXBean {

    @Override
    public Counter getRequestCountDetails();

    @Override
    public StatisticsMeter getResponseTimeDetails();
}
