/*******************************************************************************
 * Copyright (c) 1998, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.util.FastHashtable;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * <code>StatefulBeanReaper</code> runs as an alarm that will cleanup stateful
 * session beans as their timeouts expire. <p>
 *
 * The class keeps a list of all the stateful bean ids and their timeout
 * values. Whenever a stateful bean is created, its bean id and the session
 * timeout are enlisted in this class. <p>
 *
 * The alarm runs periodically, for each element in the list of stateful
 * beans a check is performed to find out if the bean has expired while in
 * the passivated state. If the bean has expired it will be removed from the
 * passivation store. <p>
 *
 * Also, during {@link EJSContainer} shutdown/termination, this class is used
 * to remove all Stateful beans from the passivation store, regardless of whether
 * they have timed out or not. See {@link #finalSweep finalSweep()}. <p>
 *
 * <DL>
 * <DT><B>Known Users:</B>
 * <DD> {@link com.ibm.ejs.container.activator.Activator} - creates and starts a <code>StatefulBeanReaper</code> with the
 * default sweep interval.
 * </DL>
 *
 * @see EJSContainer
 * @see com.ibm.ejs.container.activator.Activator
 **/
public final class StatefulBeanReaper implements Runnable {
    private static final TraceComponent tc = Tr.register(StatefulBeanReaper.class,
                                                         "EJBCache",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.StatefulBeanReaper";

    //
    // Construction
    //

    /**
     * Constructs a <code>StatefulBeanReaper</code> object, which
     * cleans up stateful beans whose session timeout has expired. The
     * alarm runs at the default interval of once every minute. <p>
     *
     * @param a EJB Cache manager, which is responsible for removing Stateful
     *            Session beans from the cache.
     * @param numBuckets the number of hash table buckets to use for holding
     *            the Stateful Bean timeout information.
     * @param failoverCache is the SfFailoverCache used by this application server
     *            to hold replicated SFSB data.
     * @param scheduledExecutorService Configured Scheduled Executor Service
     *            for scheduling alarms
     **/
    public StatefulBeanReaper(Activator a, int numBuckets, SfFailoverCache failoverCache, ScheduledExecutorService scheduledExecutorService) //LIDB2018-1
    {
        this(a, numBuckets, DEFAULT_MIN_CLEANUP_INTERVAL, failoverCache, scheduledExecutorService); // LIDB2775-23.4, F73234
    }

    /**
     * Constructs a <code>StatefulBeanReaper</code> object, which
     * cleans up stateful beans whose session timeout has expired. The
     * alarm runs at the specified interval (does not run more frequently
     * than once every minute). <p>
     *
     * @param a EJB Cache manager, which is responsible for removing Stateful
     *            Session beans from the cache.
     * @param numBuckets the number of hash table buckets to use for holding
     *            the Stateful Bean timeout information.
     * @param cleanupInterval interval, in milliseconds, at which to look
     *            for session beans to clean up.
     * @param scheduledExecutorService Configured Scheduled Executor Service
     *            for scheduling alarms
     **/
    public StatefulBeanReaper(Activator a, int numBuckets, long cleanupInterval, SfFailoverCache failoverCache, ScheduledExecutorService scheduledExecutorService) //LIDB2018-1, F73234
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>");

        if (EJSPlatformHelper.isZOS() && cleanupInterval == 0) // LIDB2775-23.4
            this.ivSweepInterval = DEFAULT_CLEANUP_INTERVAL; // LIDB2775-23.4
        else if (cleanupInterval < DEFAULT_MIN_CLEANUP_INTERVAL) // LIDB2775-23.4
            this.ivSweepInterval = DEFAULT_MIN_CLEANUP_INTERVAL; // LIDB2775-23.4
        else
            this.ivSweepInterval = cleanupInterval;

        ivStatefulBeanList = new FastHashtable<BeanId, TimeoutElement>(numBuckets); // d112258

        ivActivator = a;

        ivSfFailoverCache = failoverCache; //LIDB2018-1

        ivScheduledExecutorService = scheduledExecutorService; // F73234

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : sweep = " + ivSweepInterval + " ms");
    }

    public void start() {
        // F743-33394 - Don't start an alarm until the first bean is added.
    }

    //
    // Alarm interface
    //

    /**
     * This method is called when an alarm scheduled with the
     * Scheduled Executor Service has expired. <p>
     *
     * Looks up a list of bean ids and their associated session timeouts,
     * looking for beans whose timeouts have expired. <p>
     **/
    // d91878
    @Override
    public void run() // F73234
    {
        synchronized (this) // d601399
        {
            if (ivIsCanceled) // d583637
            {
                //if this instance has been canceled, we do no more processing.
                // this also guarantees that a future alarm is not created.
                return;
            }

            ivIsRunning = true; // F743-33394
        }

        try {
            // Go through the list of beans and check to see if any of
            // them needs to be removed
            sweep();
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".alarm", "226", this);
            Tr.warning(tc, "UNEXPECTED_EXCEPTION_DURING_STATEFUL_BEAN_CLEANUP_CNTR0015W",
                       new Object[] { this, e }); //p111002.5
        } finally {
            synchronized (this) {
                ivIsRunning = false; // F743-33394

                if (ivIsCanceled) {
                    // The reaper was canceled while we were sweeping.  Notify
                    // the canceling thread that we're done.
                    notify();
                } else if (numObjects != 0) {
                    startAlarm(); // F743-33394
                } else {
                    ivScheduledFuture = null; // F73234
                }
            }
        }
    }

    //
    // Internal Methods
    //

    /**
     * Go through the list of bean ids and cleanup beans which have timed out.
     */
    public void sweep() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "Sweep : Stateful Beans = " + ivStatefulBeanList.size());

        for (Enumeration<TimeoutElement> e = ivStatefulBeanList.elements(); e.hasMoreElements();) {
            TimeoutElement elt = e.nextElement();

            // If the bean has timed out, regardless of whether it has been
            // passivated, or is still in the cache ('active'), go ahead and try
            // to delete it.                                                d112258
            if (elt.isTimedOut()) // F61004.5
            {
                deleteBean(elt.beanId);
            }
        }

        // Sweep failover cache if SFSB failover is enabled.
        if (ivSfFailoverCache != null) //LIDB2018-1
        {
            ivSfFailoverCache.sweep(); //LIDB2018-1
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "Sweep : Stateful Beans = " + ivStatefulBeanList.size());
    }

    /**
     * This method is invoked just before container termination to clean
     * up stateful beans which have been passivated.
     */
    public void finalSweep(StatefulPassivator passivator) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "finalSweep : Stateful Beans = " + ivStatefulBeanList.size());

        for (Enumeration<TimeoutElement> e = ivStatefulBeanList.elements(); e.hasMoreElements();) {
            TimeoutElement elt = e.nextElement();

            if (elt.passivated) {
                try {
                    // If the bean hasn't already been removed (possibly by the
                    // regular sweep(), then go ahead and remove the file. d129562
                    if (remove(elt.beanId)) {
                        passivator.remove(elt.beanId, false); //LIDB2018-1
                    }

                } catch (RemoteException ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".finalSweep",
                                                "298", this);
                    Tr.warning(tc, "REMOVE_FROM_PASSIVATION_STORE_FAILED_CNTR0016W", new Object[] { elt.beanId, ex }); //p111002.3
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "finalSweep : Stateful Beans = " + ivStatefulBeanList.size());
    }

    /**
     * Gets the timeout element for the specified bean id. Note that a null
     * return value does not mean that the bean does not exist.
     *
     * @param beanId the bean id
     * @return the timeout element, or null if the reaper does not have a
     *         timeout element for this bean
     */
    // F61004.5
    public TimeoutElement getTimeoutElement(BeanId beanId) {
        return ivStatefulBeanList.get(beanId);
    }

    /**
     * Check a particular bean to see if it still exists and has timed out. <p>
     *
     * Returns true if the bean exists (is registered with the reaper) and has
     * timed out; otherwise, returns false.
     */
    // d112258
    public boolean beanExistsAndTimedOut(TimeoutElement elt, BeanId beanId) {
        // If the bean does not exist in the Reaper's list, return false.  This
        // may occur if remove() has been called on the bean, while the reaper
        // is attempting to time it out.
        if (elt == null) {
            if (ivSfFailoverCache != null) // LIDB2018-1
            {
                return ivSfFailoverCache.beanExistsAndTimedOut(beanId);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Session bean not in Reaper: Timeout = false");
            }

            return false;
        }

        return elt.isTimedOut(); // F61004.5
    }

    /**
     * Returns true if either the SFSB does not exist in the
     * reaper list (which includes the SfFailoverCache local to this
     * application server process) or the SFSB has timed out.
     *
     * @param beanId is the BeanId for the SFSB.
     *
     * @return see description of method.
     */
    //LIDB2018-1 renamed old beanTimedOut method and clarified description.
    public boolean beanDoesNotExistOrHasTimedOut(TimeoutElement elt, BeanId beanId) {
        if (elt == null) {
            // Not in the reaper list, but it might be in local
            // failover cache if not in reaper list.  So check it if
            // there is a local SfFailoverCache object (e.g. when SFSB
            // failover is enabled to use failover cache).
            if (ivSfFailoverCache != null) {
                // Not in reaper list, but SFSB failover enabled.
                // Have local SfFailoverCache determine if bean does not exist
                // or has timed out.
                return ivSfFailoverCache.beanDoesNotExistOrHasTimedOut(beanId);
            }

            // Return true since bean not found in reaper list
            // and SFSB failover is not enabled so not in failover
            // cache list.
            return true;
        }

        // SFSB found in reaper list, so return whether it
        // has timed out or not.
        return elt.isTimedOut(); // F61004.5
    }

    private void deleteBean(BeanId beanId) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "deleteBean " + beanId);

        synchronized (this) {
            numDeletes++;
        }

        try {
            ivActivator.timeoutBean(beanId);
        } catch (Exception e) {

            FFDCFilter.processException(e, CLASS_NAME + ".deleteBean", "367", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to timeout session bean");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "deleteBean");
    }

    /**
     * Add a new bean to the list of beans to be checked for timeouts
     */
    public void add(StatefulBeanO beanO) {
        BeanId id = beanO.beanId;
        TimeoutElement elt = beanO.ivTimeoutElement;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "add " + beanO.beanId + ", " + elt.timeout);

        // LIDB2775-23.4 Begins
        Object obj = ivStatefulBeanList.put(id, elt);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, (obj != null) ? "Stateful bean information replaced" : "Stateful bean information added");
        // LIDB2775-23.4 Ends

        synchronized (this) {
            if (numObjects == 0 && !ivIsCanceled) // F743-33394
            {
                startAlarm();
            }

            numObjects++;
            numAdds++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "add");
    }

    /**
     * Remove a bean from the list and return true if the bean was in the
     * list and removed successfully; otherwise return false.
     */
    public boolean remove(BeanId id) // d129562
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "remove (" + id + ")");

        TimeoutElement elt = null;

        elt = ivStatefulBeanList.remove(id);

        synchronized (this) {
            if (elt != null) {
                numObjects--;
                numRemoves++;

                if (numObjects == 0) { // F743-33394
                    stopAlarm();
                }
            } else {
                numNullRemoves++;
            }
        }

        boolean result = elt != null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "remove (" + result + ")");
        return result; // d129562
    }

    /**
     * Get a list of all the passivated beans which belong to a certain home.
     *
     * @param homeName is the Java EE name of the home that bean ids are to be
     *            returned for.
     **/
    // d103404.1
    public synchronized Iterator<BeanId> getPassivatedStatefulBeanIds(J2EEName homeName) {
        ArrayList<BeanId> beanList = new ArrayList<BeanId>();

        for (Enumeration<TimeoutElement> e = ivStatefulBeanList.elements(); e.hasMoreElements();) {
            TimeoutElement elt = e.nextElement();

            if (homeName.equals(elt.beanId.getJ2EEName())
                && (elt.passivated))
                beanList.add(elt.beanId);
        }
        return (beanList.iterator());
    }

    // LIDB2775-23.4 Begins
    public long getBeanTimeoutTime(BeanId beanId) {
        TimeoutElement elt = ivStatefulBeanList.get(beanId);
        long timeoutTime = 0;
        if (elt != null) {
            if (elt.timeout != 0) {
                timeoutTime = elt.lastAccessTime + elt.timeout;
                if (timeoutTime < 0) { // F743-6605.1
                    timeoutTime = Long.MAX_VALUE; // F743-6605.1
                }
            }
        }
        return timeoutTime;
    }

    // LIDB2775-23.4 Ends

    /**
     * Dump the internal state of the cache
     */
    public void dump() {

        if (dumped) {
            return;
        }

        try {
            Tr.dump(tc, "-- StatefulBeanReaper Dump -- ", this);
            synchronized (this) {
                Tr.dump(tc, "Number of objects:      " + this.numObjects);
                Tr.dump(tc, "Number of adds:         " + this.numAdds);
                Tr.dump(tc, "Number of removes:      " + this.numRemoves);
                Tr.dump(tc, "Number of null removes: " + this.numNullRemoves);
                Tr.dump(tc, "Number of deletes:      " + this.numDeletes);
            }
        } finally {
            dumped = true;
        }
    } // dump

    /**
     * Reset dumped state of this cache.
     */
    public void resetDump() {

        dumped = false;
        /*
         * synchronized (this) {
         * int numObjects = 0;
         * int numAdds = 0;
         * int numRemoves = 0;
         * int numNullRemoves = 0;
         * int numDeletes = 0;
         * }
         */

    } // resetDump

    private void startAlarm() // F743-33394, F73234
    {
        ivScheduledFuture = ivScheduledExecutorService.schedule(this, ivSweepInterval, TimeUnit.MILLISECONDS);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "started alarm: " + ivScheduledFuture);
    }

    private void stopAlarm() // F743-33394, F73234
    {
        if (ivScheduledFuture != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "stopping alarm: " + ivScheduledFuture);
            ivScheduledFuture.cancel(false);
            ivScheduledFuture = null;
        }
    }

    /**
     * Cancel this alarm
     */
    public synchronized void cancel() // d583637
    {
        ivIsCanceled = true;
        stopAlarm();

        // F743-33394 - Wait for the sweep to finish.
        while (ivIsRunning) {
            try {
                wait();
            } catch (InterruptedException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "interrupted", ex);
                Thread.currentThread().interrupt();
            }
        }

        ivActivator = null;
    }

    /**
     * Clean up interval for the alarm.
     */
    private long ivSweepInterval;

    /**
     * List of stateful beans and their associated timeouts.
     */
    private final FastHashtable<BeanId, TimeoutElement> ivStatefulBeanList; // d112258

    /**
     * Default cleanup interval: 60,000 ms (1 minute).
     */
    private static final long DEFAULT_MIN_CLEANUP_INTERVAL = 60000;// LIDB2775-23.4

    /**
     * Default cleanup interval: 4,200,000 ms (70 minute).
     */
    private static final long DEFAULT_CLEANUP_INTERVAL = 4200000; // LIDB2775-23.4

    /**
     * Holds a reference to the activator object (EJB Cache manager).
     */
    private Activator ivActivator;

    /**
     * Failover cache if SFSB failover is enabled on this application server.
     */
    private final SfFailoverCache ivSfFailoverCache; //LIDB2018-1

    /**
     * True if this alarm is being canceled.
     */
    private boolean ivIsCanceled; // d583637

    /**
     * True if the alarm is running.
     */
    private boolean ivIsRunning; // F743-33394

    /**
     * Reference to the Scheduled Executor Service instance in use in this container.
     **/
    private final ScheduledExecutorService ivScheduledExecutorService; // F73234

    /**
     * Holds a reference to the Scheduled Future object
     **/
    private ScheduledFuture<?> ivScheduledFuture; // F73234

    //
    // Statistics for dump.
    //
    protected boolean dumped = false;
    protected int numObjects = 0;
    protected int numAdds = 0;
    protected int numRemoves = 0;
    protected int numNullRemoves = 0;
    protected int numDeletes = 0;
}
