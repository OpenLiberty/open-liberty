/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;

public class PoolManagerImpl
                extends PoolManager
                implements Runnable
{
    private static final TraceComponent tc = Tr.register(PoolManagerImpl.class, "EJBContainer", null);

    private final List<PoolImplBase> pools = new ArrayList<PoolImplBase>();
    private PoolImplBase[] poolArray = new PoolImplBase[10];

    /** Interval between drain sweeps, in milliseconds. **/
    private volatile long drainInterval = 30000;

    /**
     * Holds a reference to the Scheduled Future object
     */
    private ScheduledFuture<?> ivScheduledFuture;

    /**
     * Reference to the Scheduled Executor Service instance in use in this container.
     */
    private ScheduledExecutorService ivScheduledExecutorService;

    private boolean ivIsCanceled = false; //d583637
    private boolean ivIsRunning;

    @Override
    public void setDrainInterval(long di)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Setting drain interval to: " + di);

        drainInterval = di;
    }

    /**
     * Handle the scavenger alarm. Scan the list of pools and drain
     * all inactive ones.
     */
    public void run()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "run");

        int numPools;
        synchronized (this)
        {
            if (ivIsCanceled)
            {
                return;
            }

            ivIsRunning = true;

            numPools = pools.size();
            if (numPools > 0)
            {
                if (numPools > poolArray.length)
                {
                    poolArray = new PoolImplBase[numPools];
                }

                pools.toArray(poolArray);
            }
        }

        try
        {
            for (int i = 0; i < poolArray.length && poolArray[i] != null; ++i) // 147140
            {
                if (poolArray[i].inactive)
                {
                    poolArray[i].periodicDrain();
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "setting inactive: " + poolArray[i]);
                    poolArray[i].inactive = true;
                }

                // Allow pools to be GC'ed promptly after being removed.     PM54417
                poolArray[i] = null;
            }
        } finally
        {
            synchronized (this)
            {
                ivIsRunning = false;
                if (ivIsCanceled)
                {
                    notify();
                }
                else if (!pools.isEmpty())
                {
                    startAlarm();
                }
                else
                {
                    ivScheduledFuture = null;
                }
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.exit(tc, "run");
    } // run

    /**
     * Adds a pool to the list of pools managed by this pool manager. <p>
     * 
     * This method is NOT public, and is intended to be used within the
     * package to allow pools that have become inactive (and removed from
     * the list of managed pools) to become active and once again managed
     * by the pool manager. <p>
     * 
     * @param p pool to be added to the list of managed pools.
     **/
    // d376426
    synchronized void add(PoolImplBase p)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "add: " + p);

        if (!ivIsCanceled)
        {
            if (pools.isEmpty())
            {
                startAlarm();
            }

            pools.add(p);
        }
    } // add

    /**
     * Removes a pool from the list of pools managed by this pool manager. <p>
     * 
     * This method is NOT public, and is intended to be used within the
     * package to allow inactive pools to be removed for the list of managed
     * pools (so periodicDrain will not continue to be called), and also
     * to allow pools that are being destroyed to be removed. <p>
     * 
     * @param p pool to be removed from the list of managed pools.
     **/
    synchronized void remove(Pool p)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "remove: " + p);
        pools.remove(p);
        if (pools.isEmpty())
        {
            stopAlarm();
        }
    } // remove

    @Override
    public Pool createThreadSafePool(int minimum, int maximum)
    {
        PoolImplBase result = new PoolImplThreadSafe(minimum, maximum, null, null, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "createThreadSafePool: " + result);
        return result;
    } // createThreadSafePool

    @Override
    public Pool createThreadSafePool(int minimum, int maximum, EJBPMICollaborator beanPerf)
    {
        PoolImplBase result = new PoolImplThreadSafe(minimum, maximum, beanPerf, null, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "createThreadSafePool: " + result);
        return result;
    } // createThreadSafePool

    @Override
    public Pool create(int minimum, int maximum, EJBPMICollaborator beanPerf, PoolDiscardStrategy d)
    {
        PoolImplBase result = new PoolImplThreadSafe(minimum, maximum, beanPerf, d, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "create: " + result);
        return result;
    } // create

    private void startAlarm()
    {
        ivScheduledFuture = ivScheduledExecutorService.schedule(this, drainInterval, TimeUnit.MILLISECONDS);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "started alarm: " + ivScheduledFuture);
    }

    private void stopAlarm()
    {
        if (ivScheduledFuture != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "stopping alarm: " + ivScheduledFuture);
            ivScheduledFuture.cancel(false); // do not cancel the running thread if already running
            ivScheduledFuture = null;
        }
    }

    @Override
    public synchronized void cancel() //d583637
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "cancel");

        ivIsCanceled = true;
        stopAlarm();

        // F743-33394 - Wait until the alarm thread is done running.
        while (ivIsRunning)
        {
            try
            {
                wait();
            } catch (InterruptedException ex)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "cancel: interrupted", ex);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor)
    {
        ivScheduledExecutorService = scheduledExecutor;
    }
} // PoolManagerImpl
