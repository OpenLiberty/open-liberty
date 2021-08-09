/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;

import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.TimerNpRunnable;

/**
 * The interface between the core container and the services provided by
 * the EJBTimer runtime environment.
 */
public interface EJBTimerRuntime {

    /**
     * Creates the scheduler service implementation specific task handler
     * (Runnable) for non-persistent EJB timers. <p>
     * 
     * The returned task handler interacts with the runtime provided
     * scheduler service and executes the timeout method when the
     * timer expiration is reached. <p>
     * 
     * @param timer non-persistent EJB timer implementation
     * 
     * @return the runtime specific timer task handler.
     */
    TimerNpRunnable createNonPersistentTimerTaskHandler(TimerNpImpl timer);

    /**
     * Returns the PersistentExecutor that has been configured for persistent EJB timers,
     * or null if one has not been configured.
     * 
     * @throws IllegalStateException if the configured PersistentExecutor is not available.
     */
    ScheduledExecutorService getPersistentExecutor();

    /**
     * Returns the late timer warning threshold that has been configured for
     * EJB timers; default is 5 minutes and 0 disables the warning message.
     */
    long getLateTimerThreshold();

    /**
     * Returns the PersistentExecutorRef that has been configured for persistent EJB timers,
     * or null if one has not been configured.
     */
    ServiceReference<ScheduledExecutorService> getPersistentExecutorRef();
}
