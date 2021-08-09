/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.ejb;

import javax.ejb.Local;
import javax.ejb.Timer;

@Local
public interface NPAutoTimerLocal {

    /**
     * Check the Principle name of the timeout thread against the expected
     * Principle name, which is set by the thread that creates the timer.
     *
     * @param timer
     */
    public void timeout(Timer timer);

    /**
     * Waits in a countDownLatch for the timer to timeout.
     *
     * @return
     */
    public boolean waitForAutomaticTimer();

    /**
     * Gets the Principle name from authenticate(). Only allows role1 which
     * corresponds to userA in the UserRegistry.
     *
     * @return
     */
    public String role1Only();
}
