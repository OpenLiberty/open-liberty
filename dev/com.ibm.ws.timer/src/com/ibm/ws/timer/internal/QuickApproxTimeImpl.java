/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.timer.internal;

import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.timer.ApproximateTime;

/**
 *
 */
@Component(service = ApproximateTime.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class QuickApproxTimeImpl implements ApproximateTime {

    // Testing has shown that currentTimeMillis is now not such a performance drag, as it was years ago.
    // For 2Q 2015 we will go straight to currentTimeMillis.
    // In 3Q 2015, assuming no issues with this fix, we can get rid of the Approx Timer service altogether
    // This class should now never get called, but I will also no-op out everything except currentTimeMillis just be safe for 2Q 2015

//    /** Required ScheduledExecutor service */
//    private ScheduledExecutorService execSvc = null;

    /** Approximate time maintained by scheduled future */
    public static volatile long currentApproxTime = 0;

//    /** Scheduled task that pops every second to maintain current approximate time */
//    private ScheduledFuture<?> timeKeeper = null;

//    public final static AtomicReference<QuickApproxTimeImpl> instance = new AtomicReference<QuickApproxTimeImpl>();

//    boolean doCurrentTimeMillis = true;
//    long lastSecond = 0;
//    static int unsyncHitCounter = 0;
//    static int timesIdle = 0;
//    static final int timesIdleThreshold = 5;
//    static int unsyncRestartHitCounter = 0;
//    static final int restartHitCounterThreshold = 10;
//    static final Object syncTimerStartObject = new Object() {};

    /**
     * Activate this component.
     * 
     * @param context
     */
    protected void activate() {

//        instance.set(this);
//        doCurrentTimeMillis = false;
    }

//    private void resetTime() {
//        doCurrentTimeMillis = true;
//    }

    /**
     * Deactivate this component.
     * 
     * @param context
     */
    protected void deactivate() {

//        instance.compareAndSet(this, null);
    }

    /**
     * Set the executor service reference.
     * This is a required reference: will be called before activation.
     * 
     * @param ref new scheduled executor service instance/provider
     */
    @Reference(service = ScheduledExecutorService.class)
    protected void setScheduledExecutor(ScheduledExecutorService ref) {
//        this.execSvc = ref;
    }

    /**
     * Remove the reference to the executor service.
     * This is a required reference, will be called after deactivate.
     * 
     * @param ref scheduled executor service instance/provider to remove
     */
    protected void unsetScheduledExecutor(ScheduledExecutorService ref) {
//        if (ref == this.execSvc) {
//
//            synchronized (syncTimerStartObject) {
//                // revert to direct call to System.currentTimeMillis
//                resetTime();
//
//                if (timeKeeper != null) {
//                    timeKeeper.cancel(false);
//                    timeKeeper = null;
//                }
//            }
//
//            this.execSvc = null;
//        }
    }

    @Trivial
    @Override
    public long getApproxTime() {

        // should only be true after a reset, or before the service is activated, or on Z
//        if (doCurrentTimeMillis) {
        return System.currentTimeMillis();
//        }

//        unsyncHitCounter++;
//        // can't do "if/while" comparisons using currentApproxTime, since it could change after the "if"
//        long myTime = currentApproxTime;
//
//        while (myTime == 0) {
//            // timer thread isn't running, so get the current time and see if we should start the timer thread 
//            myTime = System.currentTimeMillis();
//
//            // truncate the time to the nearest second by dividing the msec time by 1024            
//            // quick way to divide by 1024 with remainder discarded is to shift it right ten times.
//            long thisSecond = myTime >>> 10;
//
//            if (lastSecond == thisSecond) {
//                unsyncRestartHitCounter++; // increment count of how many times we hit in the same second
//                // don't restart the timer thread until we get the threshold level of activity using the timer 
//                if (unsyncRestartHitCounter < restartHitCounterThreshold) {
//                    break;
//                }
//            } else {
//                lastSecond = thisSecond; // reset the last second we got a hit
//                unsyncRestartHitCounter = 0;
//                break;
//            }
//
//            // timer thread went away and we have seen enough activity to restart it
//            synchronized (syncTimerStartObject) {
//                // make sure no one beat us to it
//                if (currentApproxTime == 0) {
//                    unsyncRestartHitCounter = 0;
//                    // timer thread went away, so restart it
//                    // Invoke the approximate time runnable every second.
//                    timeKeeper = execSvc.scheduleAtFixedRate(approximateTime, 1L, 1L, TimeUnit.SECONDS);
//
//                    currentApproxTime = System.currentTimeMillis();
//                    unsyncHitCounter++;
//                }
//
//                myTime = currentApproxTime;
//            }
//        }
//
//        return myTime;
    }

//    // Scheduled executor will invoke this runnable at a fixed interval: 
//    // update the current approximate time when the runnable is called.
//    final Runnable approximateTime = new Runnable() {
//        @Trivial
//        @Override
//        public void run() {
//
//            currentApproxTime = System.currentTimeMillis();
//
//            if (unsyncHitCounter == 0) {
//                // no one wanted the time while we were sleeping
//                timesIdle++;
//                if (timesIdle >= timesIdleThreshold) {
//                    // no one has gotten the time in a while, so exit time keeping, so we can really be idle
//                    // let other threads know this thread is not keeping the time anymore
//                    synchronized (syncTimerStartObject) {
//                        currentApproxTime = 0;
//                        timesIdle = 0;
//                        if (timeKeeper != null) {
//                            timeKeeper.cancel(false);
//                            timeKeeper = null;
//                        }
//                    }
//                }
//            } else {
//                // someone ask for the time, so reset counters
//                unsyncHitCounter = 0;
//                timesIdle = 0;
//            }
//        }
//    };

}
