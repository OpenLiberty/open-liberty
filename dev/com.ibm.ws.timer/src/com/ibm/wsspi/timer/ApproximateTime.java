/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.wsspi.timer;

/**
 * Approximate timer class that updates the equivalent of
 * System.currentTimeMillis() but at set intervals. This is
 * useful for callers that do not need to be exact.
 */
public interface ApproximateTime {
    /**
     * Get the time which is set according to the time interval.
     * 
     * @return time
     */
    long getApproxTime();
}
