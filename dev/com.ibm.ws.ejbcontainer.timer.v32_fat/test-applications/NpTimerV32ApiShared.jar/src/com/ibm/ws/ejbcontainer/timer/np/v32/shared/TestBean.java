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

/**
 * Interface for testing session beans that do not implements a timeout method.
 * It contains methods to test getAllTimers() API.
 **/
public interface TestBean {

    /**
     * Returns the number of automatic timers that should exist for the module. <p>
     *
     * This method does not rely on getAllTimers(), since it is providing the
     * value to compare against the result of getAllTimers(). The number of
     * automatic timer is not static, since some variations may cancel
     * automatic timers. <p>
     */
    public int getAllExpectedAutomaticTimerCount();

    /**
     * Changes the expected number of automatic timers for the module by the
     * specified delta.
     */
    public int adjustExpectedAutomaticTimerCount(int delta);

    /**
     * Calls the getAllTimers() API and asserts that the returned
     * value equals the expected parameter.
     */
    public void verifyGetAllTimers(int expected);

    /**
     * Cancels all of the programmatically created EJB timers for the
     * module associated with this bean. Automatic timers are not
     * effected by this method call.
     */
    public void clearAllProgrammaticTimers();
}
