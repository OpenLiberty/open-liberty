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
package com.ibm.ws.sip.container.timer;

/**
 * An interface for a general timer service in the SIP container
 * 
 * @author Nitzan Nissim
 */
public interface BaseTimerService {

    /**
     * Scheduling a future task
     * 
     * @param timer Related BaseTimer instance
     * @param isPersistent Timer is persistent in an high availability setting
     * @param delay Milliseconds before task execution
     * @param period time in milliseconds between successive timer expirations
     * @param fixedDelay if true, the repeating timer is scheduled in
     *            a fixed-delay mode, otherwise in a fixed-rate mode
     */
    public void schedule(BaseTimer timer, boolean isPersistent, long delay, long period, boolean fixedDelay);

    /**
     * Scheduling a future task
     * 
     * @param timer Related BaseTimer instance
     * @param isPersistent Timer is persistent in an high availability setting
     * @param delay Milliseconds before task execution
     */
    public void schedule(BaseTimer timer, boolean isPersistent, long delay);
}
