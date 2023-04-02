/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import javax.ejb.EJBLocalObject;

/**
 * Remote interface for a basic Stateless Session that does not
 * implement the TimedObject interface. It contains methods to test
 * TimerService access.
 **/
public interface StatelessObject extends EJBLocalObject {
    /**
     * Tests TimerService access from a Stateless Session bean that
     * does not implement the TimedObject interface. <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.create() fails with IllegalStateException
     * <li> TimerService.getTimers() returns an empty collection
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     **/
    public void testTimerService();
}
