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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

public class TimerHelper {

    private final static String CLASSNAME = TimerHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Creates a new Timer from the passed-in TimerService using the passed-in
     * arguments.
     *
     * @param ts - The TimerService from the EJB
     * @param duration - How long (in ms) to wait before timing out
     * @param expiration - The date/time to time out (note that if this is set, duration must be null and vice versa)
     * @param info - The info to put into the timer
     * @param persistent - Should this timer be persisted?
     * @param intervalDuration - How often (in ms) the timer should time out (if null, the returned timer will be a single action timer)
     * @throws IllegalArgumentException if ts is null, if both duration and expiration are null, or if both duration and expiration are non-null
     * @return a new Timer object
     */
    public static Timer createTimer(TimerService ts, Long duration, Date expiration, Serializable info, Boolean persistent, Long intervalDuration) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "createTimer", new Object[] {
                                                                       ts, duration, expiration, info, persistent, intervalDuration
            });
        }

        if (ts == null) {
            throw new IllegalArgumentException("TimerService must not be null");
        }
        if (duration != null && expiration != null) {
            throw new IllegalArgumentException("Cannot specify both a duration (Long) and expiration (Date) - must specify exactly one.");
        }
        if (duration == null && expiration == null) {
            throw new IllegalArgumentException("Must specify either a duration (Long) or an expiration (Date) - must specify exactly one.");
        }

        Timer timer = null;
        TimerConfig timerConfig = null;
        if (!persistent) {
            timerConfig = new TimerConfig();
            timerConfig.setInfo(info);
            timerConfig.setPersistent(persistent);
        }

        if (timerConfig == null) {
            //persistent timer
            if (duration != null) {
                if (intervalDuration != null) {
                    timer = ts.createTimer(duration, intervalDuration, info);
                } else {
                    timer = ts.createTimer(duration, info);
                }
            } else {
                if (intervalDuration != null) {
                    timer = ts.createTimer(expiration, intervalDuration, info);
                } else {
                    timer = ts.createTimer(expiration, info);
                }
            }
        } else {
            //non-persistent timer
            if (duration != null) {
                if (intervalDuration != null) {
                    timer = ts.createIntervalTimer(duration, intervalDuration, timerConfig);
                } else {
                    timer = ts.createSingleActionTimer(duration, timerConfig);
                }
            } else {
                if (intervalDuration != null) {
                    timer = ts.createIntervalTimer(expiration, intervalDuration, timerConfig);
                } else {
                    timer = ts.createSingleActionTimer(expiration, timerConfig);
                }
            }
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "createTimer", timer);
        }

        return timer;
    }

    static Timer getTimerWithMatchingInfo(TimerService ts, Serializable info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "getTimerWithMatchingInfo", new Object[] { ts, info });
        }

        Timer timer = null;
        Collection<Timer> allTimers = ts.getTimers();
        for (Timer t : allTimers) {
            if ((info == null && t.getInfo() == null) || (info != null && info.equals(t.getInfo()))) {
                timer = t;
                break;
            }
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "getTimerWithMatchingInfo", timer);
        }

        return timer;
    }

}
