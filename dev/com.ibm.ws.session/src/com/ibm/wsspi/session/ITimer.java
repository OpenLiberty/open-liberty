/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.wsspi.session;

/**
 * Interface to assist with time based events
 */
public interface ITimer {

    /**
     * Starts the timer for this store and sets the interval for which it will
     * perform some event
     * 
     * @param store
     * @param interval
     */
    public void start(IStore store, int interval);

    /**
     * Stops the timer.
     */
    public void stop();

}
