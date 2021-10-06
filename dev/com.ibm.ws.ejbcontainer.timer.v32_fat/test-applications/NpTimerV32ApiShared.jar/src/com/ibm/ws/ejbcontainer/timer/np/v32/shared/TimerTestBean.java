/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.v32.shared;

import java.util.concurrent.CountDownLatch;

/**
 * Interface for testing session beans that implements a timeout method.
 * It contains methods to test get getAllTimers() API.
 **/
public interface TimerTestBean extends TestBean {

    public static final long DEFAULT_EXPIRATION = 30 * 60 * 1000;
    public static final long SINGLE_ACTION_EXPIRATION = 3000;

    /**
     * Creates the requested number of single action timers using
     * an info object that contains the info text provided.
     *
     * The created timers are never expected to timeout; a very
     * large expiration is used, {@link #DEFAULT_EXPIRATION}.
     */
    public void createTimers(int create, String info);

    /**
     * Creates a single action timer that will expire quickly and
     * returns a CountDownLatch that may be used to wait for confirmation
     * that the timer has timed out.
     */
    public CountDownLatch createSingleActionTimer(String info);

    /**
     * Creates an interval timer that will expire quickly, and then
     * run a second time soon after. The timer will cancel itself
     * the second time it times out.
     *
     * Three CountDownLatches will be returned, the first may be used
     * to wait for the first timeout, the second should be used to signal
     * when it is acceptable to run the timer again, and the third may be
     * used to wait for the second and final timeout.
     */
    public CountDownLatch[] createIntervalTimer(String info);

    /**
     * Cancels one automatic timer and one programmatic timer associated
     * with this bean. This method will not effect the expected automatic
     * timer count for the module until the transaction associated with
     * the caller of this method commits.
     */
    public void cancelTwoTimers();
}
