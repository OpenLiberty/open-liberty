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
package com.ibm.ws.ejbcontainer.timer.np.operations.ejb;

/**
 * Remote interface for a basic Stateless Session that does not
 * implement the TimedObject interface. It contains methods to test
 * TimerService access.
 **/
public interface StatelessLocal {
    /**
     * Tests TimerService access from a Stateless Session bean that
     * does not implement a timeout method. <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     **/
    public void testTimerService();
}
