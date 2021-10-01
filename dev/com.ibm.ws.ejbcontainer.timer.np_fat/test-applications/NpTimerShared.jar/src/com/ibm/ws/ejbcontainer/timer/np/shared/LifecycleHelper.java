/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.shared;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.ejb.Timer;

public class LifecycleHelper {
    /**
     * Set by LifecycleHelper.timeout.
     */
    public static Map<Serializable, Date> svTimerFired = new ConcurrentHashMap<Serializable, Date>();

    /**
     * Set if LifecycleHelper.timeout is called while
     * LifecycleBean.postConstruct is executing.
     */
    public static Date svPostConstructTimerFired;

    /**
     * The result of TimerService.getTimers after creating a timer in
     * LifecycleBean.postConstruct.
     */
    public static Collection<Timer> svPostConstructTimers;

    /**
     * Signals when the timer created during @PostConstruct runs.
     */
    public static CountDownLatch svPostConstructTimerLatch = new CountDownLatch(1);

    /**
     * Signals when to complete the asynchronous creation of a timer.
     */
    public static CountDownLatch svAsyncCreateTimerLatch = new CountDownLatch(1);

    /**
     * The timer that was created during LifecycleBean.preDestroy.
     */
    public static Timer svPreDestroyTimerCreated;

    /**
     * The result of TimerService.getTimers after attempting to create a timer
     * in LifecycleBean.postConstruct.
     */
    public static Collection<Timer> svPreDestroyTimers;

}
