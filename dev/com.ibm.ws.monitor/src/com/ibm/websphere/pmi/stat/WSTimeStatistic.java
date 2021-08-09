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

package com.ibm.websphere.pmi.stat;

/**
 * WebSphere Time statistic interface.
 * 
 * @ibm-api
 */

public interface WSTimeStatistic extends WSAverageStatistic {
    /** Returns the sum total of time taken to complete all the invocations since the beginning of the measurement. */
    public long getTotalTime();

    /** Returns the minimum time taken to complete one invocation since the beginning of the measurement. */
    public long getMinTime();

    /** Returns the maximum time taken to complete one invocation since the beginning of the measurement. */
    public long getMaxTime();
}
