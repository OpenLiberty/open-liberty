/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.stat;

import com.ibm.websphere.pmi.stat.WSStatistic;

/**
 * WebSphere interface to instrument a statistic.
 * 
 * @ibm-spi
 */

public interface SPIStatistic extends WSStatistic {
    /**
     * Resets the statistic to zero. Typically, this method is not called by the application.
     */
    public void reset();

    /**
     * Set last sample time
     */
    public void setLastSampleTime(long lastSampleTime);

    /**
     * Set start time
     */
    public void setStartTime(long startTime);

    /** Returns true if monitoring for this statitic is enabled */
    public boolean isEnabled();
}
