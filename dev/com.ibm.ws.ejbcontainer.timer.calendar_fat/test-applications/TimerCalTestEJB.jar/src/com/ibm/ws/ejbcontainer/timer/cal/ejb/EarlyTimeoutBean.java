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
package com.ibm.ws.ejbcontainer.timer.cal.ejb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.Calendar;
import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class EarlyTimeoutBean {

    @Resource
    private TimerService ivTimerService;
    @Resource
    private SessionContext ivContext;

    private volatile String ivFired;

    public void test(boolean persistent) {
        ivFired = null;
        Timer timer = ivContext.getBusinessObject(EarlyTimeoutBean.class).createTimer(persistent);

        // createTimer already checked the scheduling; just cancel
        timer.cancel();

        // double check that the timer didn't fire immediately
        assertNull("timer fired", ivFired);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Timer createTimer(boolean persistent) {
        Calendar beforeCreate = Calendar.getInstance();
        int second = beforeCreate.get(Calendar.SECOND);
        ScheduleExpression schedule = new ScheduleExpression().second(second).minute("*").hour("*");
        String info = "T1:" + beforeCreate.getTimeInMillis();
        Timer timer = ivTimerService.createCalendarTimer(schedule, new TimerConfig(info, persistent));
        Calendar afterCreate = Calendar.getInstance();

        // Verify the timer is scheduled to go off about a minute from now;
        // taking into account that the timer may have been created at exactly
        // a second boundary (i.e. milliseconds=0) or that the system may
        // be very slow and thus the timer was actually created much later
        // than the time captured before the create call.

        // The earliest time that the timer may expire is immediately,
        // but only if the timer was created at exactly a second boundary
        // (i.e. milliseconds = 0), otherwise it can expire no earlier
        // than one minute from now.
        long minAcceptableTime, maxAcceptableTime;
        long nextTimeout = timer.getNextTimeout().getTime();
        if (beforeCreate.get(Calendar.MILLISECOND) == 0
            || beforeCreate.get(Calendar.SECOND) < afterCreate.get(Calendar.SECOND)) {
            // if beforeCreate == 0 milliseconds or timer create has rolled over to new second
            // Timer could have been created at exactly 0ms and might fire immediately,
            // but still could have been created not at 0ms and fire up to 1 minute later.
            // This would also catch the case where server created timer too slowly

            minAcceptableTime = beforeCreate.getTimeInMillis();
            afterCreate.set(Calendar.MILLISECOND, 0); // round off milliseconds
            maxAcceptableTime = afterCreate.getTimeInMillis() + 60 * 1000;

            assertTrue("Next timeout not at least the time created: min=" + minAcceptableTime + ", actual=" + nextTimeout,
                       nextTimeout >= minAcceptableTime);
            assertTrue("Next timeout greater than 1 minute past create time: max=" + maxAcceptableTime + ", actual=" + nextTimeout,
                       nextTimeout <= maxAcceptableTime);
        } else {
            // What we are actually trying to test and will happen most of the time:
            // timeout should go off exactly 1 minute from now
            beforeCreate.set(Calendar.MILLISECOND, 0); // round off milliseconds
            minAcceptableTime = beforeCreate.getTimeInMillis() + 60 * 1000;

            assertEquals("Next timeout not 1 minute from scheduled time:", minAcceptableTime, nextTimeout);
        }

        // Since the current transaction that created the timer has not committed,
        // the timer could not have run yet, but double check that as well anyway.
        assertNull("Timer fired unexpectedly", ivFired);

        return timer;
    }

    @Timeout
    private void timeout(Timer timer) {
        ivFired = timer + ": " + timer.getInfo() + ", T2: " + System.currentTimeMillis();
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ivTimerService.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }
}
