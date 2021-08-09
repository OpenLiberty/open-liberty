/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.ffdc.FFDCConfigurator;

/**
 * This is a very simple service: it uses the ScheduledExecutorService registred
 * in the service registry to self-schedule a daily event (on or around midnight, local time)
 * to create a new exception summary log.
 */
public class FFDCJanitor implements Callable<Void> {
    private volatile ScheduledFuture<Void> future = null;
    private ScheduledExecutorService executorService;

    protected void activate() {}

    protected void deactivate(int reason) {
        future.cancel(false);
    }

    /**
     * Set the required service. Called before activate.
     * 
     * @param scheduler DS-injected ScheduledExecutorService -- required service
     */
    protected void setScheduler(ScheduledExecutorService scheduler) {
        executorService = scheduler;
        reschedule();
    }

    /**
     * Unset the required service. Called after deactivate.
     * 
     * @param scheduler DS-injected ScheduledExecutorService -- required service
     */
    protected void unsetScheduler(ScheduledExecutorService scheduler) {
        if (executorService == scheduler) {
            executorService = null;
        }
    }

    /**
     * Reschedule the task for midnight-ish the next day.
     */
    private void reschedule() {
        // set up a daily roll
        Calendar cal = Calendar.getInstance();
        long today = cal.getTimeInMillis();

        // adjust to somewhere after midnight of the next day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.add(Calendar.DATE, 1);
        long tomorrow = cal.getTimeInMillis();

        if (executorService != null) {
            future = executorService.schedule(this, tomorrow - today, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Trigger an FFDC log roll using the FFDCConfigurator.
     * Reschedule the task to trip again...
     */
    @Override
    public Void call() throws Exception {
        FFDCConfigurator.getDelegate().rollLogs();
        reschedule();
        return null;
    }

}
