/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.ejs.container.ContainerProperties;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * <code>BackgroundLruEvictionStrategy</code> implements a basic LRU
 * mechanism for evicting objects from a {@link Cache}. A background
 * thread or alarm periodically wakes up and walks objects in the cache,
 * checking the LRU flag. Objects which have not been accessed since the
 * previous sweep may be immediately evicted. The interval between cache
 * walks is configurable at the time <code>BackgroundLruEvictionStrategy</code>
 * is constructed. <p>
 * 
 * For improved performance, the objects in the cache are not walked on
 * every sweep, only when the cache preferred maximum has been reached.
 * And once enough objects have been evicted to get the cache 1% below
 * the cache preferred maximum, the sweep will be discontinued.
 * This implementation relies on the {@link Cache} to set the LRU flag
 * appropriately (sweep count when the object was last accessed). <p>
 * 
 * A list of potential victims to evict is built dynamically based on the
 * LRU flag when the need arises (hard limit exceeded). <p>
 * 
 * Note that it is the responsibility of the creator to actually start the
 * alarm by calling the {@link #start start()} method. <p>
 * 
 * <code>BackgroundLruEvictionStrategy</code> will not evict anything in the
 * cache until the cache grows to the preferred maximum value, as specified
 * by the <code>Cache</code>. <p>
 * 
 * <code>BackgroundLruEvictionStrategy</code> uses {@link Cache}'s {@link Cache#enumerateElements enumerateElements()} operation to obtain an
 * <code>Enumeration</code> of all the objects in the cache. This
 * <code>Enumeration</code> will be cached and reused for the life of the
 * <code>BackgroundLruEvictionStrategy</code>. <p>
 * 
 * <DL>
 * <DT><B>Known Users:</B>
 * <DD> {@link com.ibm.ws.runtime.EJBEngine EJBEngine}/ {@link com.ibm.ejs.container.EJSContainer EJSContainer} - as the {@link EvictionStrategy} for the EJB {@link Cache} <DD>
 * <B><code>com.ibm.ejs.sm.client.AttributeCache</code></B> - as the {@link EvictionStrategy} for an Attribute {@link Cache Cache} <DD>
 * <B><code>com.ibm.ejs.sm.client.DDCache</code></B> - as the {@link EvictionStrategy} for the DeploymentInfo {@link Cache Cache} </DL>
 * 
 * @see Cache
 * @see SweepLruEvictionStrategy
 **/
public final class BackgroundLruEvictionStrategy // d583637
implements EvictionStrategy,
                Runnable // d91878
{
    private static final TraceComponent tc =
                    Tr.register(com.ibm.ejs.util.cache.BackgroundLruEvictionStrategy.class
                                , "EJBCache"
                                , "com.ibm.ejs.container.container"); //p111002.4

    // When enabled, this trace string will only print a message at most once
    // every five minutes.  This can be used instead of EJBCache=all to avoid
    // overwhelming trace logs in long-run scenarios.
    private static final TraceComponent tcOOM =
                    Tr.register(BackgroundLruEvictionStrategy.class.getName() + "2"
                                , BackgroundLruEvictionStrategy.class
                                , "EJBContainer.OOM"
                                , "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.util.cache.BackgroundLruEvictionStrategy";

    //
    // Data
    //

    /** The cache associated with this EvictionStrategy. **/
    private Cache ivCache;

    /** Delay between sweeps of the cache in milliseconds. **/
    private long ivSweepInterval;

    /** Temporary variable to hold new sweep interval. **/
    private volatile long ivNewSweepInterval;

    /**
     * Number of sweeps that an element is allowed to age before it is evicted.
     * The discard threshold is a value between 2 and 60, depending on the
     * sweep interval. {@link #ivSweepInterval} * {@link #MAX_THRESHOLD_MULTIPLIER} must be less or equal to {@link #MAX_THRESHOLD_MULTIPLIER}. Basically,
     * the disacard threshold is 60 as long as 60 sweep intervals are < 2 minutes.
     **/
    // d112258
    private long ivDiscardThreshold = 60;

    /**
     * Performance/Tuning: Maximum number of sweeps that an element may ever be
     * allowed to age before becoming eligible for eviction.
     **/
    // d112258
    private long ivMaxDiscardThreshold;

    /**
     * Performance/Tuning: Minimum number of sweeps that an element must be
     * allowed to age before becoming eligible for eviction.
     **/
    // d112258
    private long ivMinDiscardThreshold;

    /**
     * Performance/Tuning: Internal upper limit to the cache size used for tuning
     * the eviction strategy to become more aggressive.
     **/
    // d112258
    private long ivUpperLimit;

    /**
     * Performance/Tuning: Number of sweeps that have resulted in a cache size
     * below the soft limit. Used to tune the eviction strategy to become less
     * agressive.
     **/
    // d112258
    private long ivNumBelowSoftLimit = 0;

    /** How far to reduce the cache below the soft limit. **/
    private int ivSoftLimitBuffer;

    /** Cached enumeration. Will be reset for each sweep. **/
    private final CacheElementEnumerator ivElements; // d103404.2

    /** Minimum value allowed for {@link #ivSweepInterval} : 1000 ms (1 second). **/
    private static final long MINIMUM_SWEEP_INTERVAL = 1000;

    /**
     * Multiplier used to determine when the discard threshold should be reduced. {@link #ivSweepInterval} * {@link #MAX_THRESHOLD_MULTIPLIER} must be
     * less than this value before the discard threshold is reduced below 20.
     **/
    // d112258
    private static final long MAX_THRESHOLD_MULTIPLIER = 120000;

    /**
     * Multiplier used to determine the minimum discard threshold. The minimum
     * discard threshold must require that the bean age at least 9000 ms
     * (9 seconds) before becoming eligible for eviction.
     **/
    // d112258
    private static final long MIN_THRESHOLD_MULTIPLIER = 9000;

    /**
     * Number of sweeps between EJBContainer.OOM trace output messages.
     * Combined with MINIMUM_SWEEP_INTERVAL, this ensures that messages are
     * printed no more often than once every 5 minutes.
     */
    private static final int NUM_SWEEPS_PER_OOMTRACE = 300; // d581579

    /**
     * d583637
     * Used to determine if this instance of the BackgroundLruEvictionStrategy
     * has been canceled - i.e. will not process any more.
     */
    private boolean ivIsCanceled = false;

    /**
     * d601399
     * Lock object used for syncing when determining whether to cancel or proceed
     */
    private final Object ivCancelLock = new Object();

    /**
     * Holds a reference to the Scheduled Future object
     */
    private ScheduledFuture<?> ivScheduledFuture;

    /**
     * Reference to the Scheduled Executor Service instance in use in this container.
     */
    private final ScheduledExecutorService ivScheduledExecutorService;

    /**
     * Reference to the deferrable Scheduled Executor Service available to the
     * container. This will be the same as ivScheduledEcecutorService if
     * a deferrable one is not available.
     */
    private final ScheduledExecutorService ivDeferrableScheduledExecutorService;

    /**
     * The size specified for the "preferred" cache size. This is not a strict
     * upper bound, simply the desired size.
     */
    protected int ivPreferredMaxSize;

    /**
     * Set when the preferred size is in the process of changing from and old
     * value to a new one.
     */
    protected volatile int ivNewPreferredMaxSize;

    //
    // Construction
    //

    /**
     * Construct a <code>BackgroundLruEvictionStrategy</code> object,
     * which selects eviction victims from a {@link Cache} using an LRU
     * algorithm. The cache is swept and internal data updated periodically,
     * at the specified interval. Based on the cache preferred maximum value,
     * elements may be evicted during a sweep. <p>
     * 
     * Note that it is the responsibility of the creator to actually start the
     * alarm by calling the {@link #start start()} method. <p>
     * 
     * @param cache cache that eviction is provided for.
     * @param preferredMaxSize the preferred max size that the eviction strategy
     *            should keep the cache at
     * @param sweepInterval interval, in milliseconds, at which to sweep
     *            the cache and update the LRU list.
     * @param scheduledExecutorService Configured Scheduled Executor Service
     *            for scheduling alarms
     * @param deferrableScheduledExecutorService A deferrable executor
     **/
    public BackgroundLruEvictionStrategy(Cache cache,
                                         int preferredMaxSize,
                                         long sweepInterval,
                                         ScheduledExecutorService scheduledExecutorService,
                                         ScheduledExecutorService deferrableScheduledExecutorService) // F73234
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.entry(tc.isEntryEnabled() ? tc : tcOOM, "BackgroundLruEvictionStrategy");

        ivScheduledExecutorService = scheduledExecutorService; // F73234
        ivDeferrableScheduledExecutorService = deferrableScheduledExecutorService;

        ivCache = cache;

        initializeCacheData(preferredMaxSize);
        ivNewPreferredMaxSize = preferredMaxSize;
        // Get a cache enumeration.  This will be cached and reused for
        // each sweep (reset called).                                    d103404.2
        ivElements = (CacheElementEnumerator) ivCache.enumerateElements();

        // Initialize all of the intervals used for sweeping the cache
        sweepInterval = getSweepInterval(sweepInterval);
        initializeSweepInterval(sweepInterval);
        ivNewSweepInterval = sweepInterval;

        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.exit(tc.isEntryEnabled() ? tc : tcOOM, "BackgroundLruEvictionStrategy");
    }

    /**
     * Initialize various optimization values used by the eviction strategy that
     * depend on the cache size.
     * 
     * @param size the size of the cache used to set these values
     */
    private void initializeCacheData(int cacheSize)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.entry(tc.isEntryEnabled() ? tc : tcOOM,
                     "initializeCacheData : " + ivCache.getName() + " preferred size = " +
                                     ivPreferredMaxSize);

        ivPreferredMaxSize = cacheSize;

        // Define an internal upper limit to the cache size. This is similar
        // to the 'hard' limit, but would never be enforced; it is merely
        // used to determine a point at which the eviction strategy should
        // dynamically tune itself to become more aggressive.               d112258
        ivUpperLimit = (long) (ivPreferredMaxSize * 1.1); // 10% over cache size

        // Give the cache a little breathing room (or buffer) below the
        // soft limit. By reducing to this far below the soft limit, it
        // is less likely that the next sweep will have to do anything.
        ivSoftLimitBuffer = ivPreferredMaxSize / 100; // 1% of the Cache Size
        if (ivSoftLimitBuffer > 50) // Don't let this get too big.
            ivSoftLimitBuffer = 50;

        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.exit(tc.isEntryEnabled() ? tc : tcOOM,
                    "initializeCacheData : " + ivCache.getName() + " preferred size = "
                                    + ivPreferredMaxSize + " limit = " + ivUpperLimit +
                                    ", buffer = " + ivSoftLimitBuffer);
    }

    /**
     * Initialize all of the intervals that are derived from the configurable
     * sweep interval. Can be called at any time during runtime.
     * 
     * @param sweepInterval the interval between sweeps that this eviction
     *            strategy uses
     */
    private void initializeSweepInterval(long sweepInterval)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.entry(tc.isEntryEnabled() ? tc : tcOOM,
                     "initializeSweepInterval : " + ivCache.getName() + " preferred size = " +
                                     ivPreferredMaxSize + ", sweep = " + sweepInterval);

        ivSweepInterval = sweepInterval;

        // Adjust the discard threshold according to the sweepInterval.
        // The discard threshold is a value between 2 and 20.              d112258
        if ((ivSweepInterval * ivDiscardThreshold) > MAX_THRESHOLD_MULTIPLIER)
        {
            ivDiscardThreshold = (MAX_THRESHOLD_MULTIPLIER / ivSweepInterval);
            if (ivDiscardThreshold < 2)
                ivDiscardThreshold = 2;
        }

        // Determine the upper and lower bounds for the discard threshold for
        // dynamic tuning of the eviction strategy. The max threshold will allow
        // objects to age for up to 1 minute (MAX_THRESHOLD_MULTIPLIER), and
        // the min threshold requires that objects age for at least 9 seconds
        // (MIN_THRESHOLD_MULTIPLIER).                                     d112258
        ivMaxDiscardThreshold = ivDiscardThreshold;
        ivMinDiscardThreshold = (MIN_THRESHOLD_MULTIPLIER / ivSweepInterval);
        if (ivMinDiscardThreshold < 2)
            ivMinDiscardThreshold = 2;

        if (isTraceOn && (tc.isEntryEnabled() || tcOOM.isEntryEnabled()))
            Tr.exit(tc.isEntryEnabled() ? tc : tcOOM,
                    "initializeSweepInterval : " + ivCache.getName() + " preferred size = "
                                    + ivPreferredMaxSize + ", sweep = " + ivSweepInterval +
                                    ", threshold = " + ivDiscardThreshold + ", buffer = " +
                                    ivSoftLimitBuffer);
    }

    /**
     * Given a sweep interval, make sure that it is above the minimum allowed.
     * 
     * @param sweepInterval interval to modify if needed to be within bounds
     * @return sweepInterval to use
     */
    private long getSweepInterval(long sweepInterval)
    {
        return Math.max(sweepInterval, MINIMUM_SWEEP_INTERVAL);
    }

    @Override
    public void setSweepInterval(long sweepInterval)
    {
        ivNewSweepInterval = getSweepInterval(sweepInterval);
    }

    @Override
    public void setPreferredMaxSize(int maxSize)
    {
        ivNewPreferredMaxSize = maxSize;
    }

    @Override
    public int getPreferredMaxSize()
    {
        return ivPreferredMaxSize;
    }

    public boolean preferredSizeReached()
    {
        return ivCache.getSize() >= ivPreferredMaxSize;
    }

    /**
     * Returns true if trace should be printed.
     */
    private boolean isTraceEnabled(boolean debug) // d581579
    {
        if (debug ? tc.isDebugEnabled() : tc.isEntryEnabled())
        {
            return true;
        }

        if (ivCache.numSweeps % NUM_SWEEPS_PER_OOMTRACE == 1 &&
            (debug ? tcOOM.isDebugEnabled() : tcOOM.isEntryEnabled()))
        {
            return true;
        }

        return false;
    }

    /**
     * Schedules the alarm using ScheduledExecutorService that will periodically sweep the associated {@link Cache}.
     **/
    public void start()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "start");

        ivScheduledFuture = ivDeferrableScheduledExecutorService.schedule(this,
                                                                          ivSweepInterval,
                                                                          TimeUnit.MILLISECONDS); // F73234

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    //
    // Alarm interface
    //

    /**
     * This method is called when Scheduled Future scheduled with
     * the Scheduled Executor Service has expired. <p>
     * 
     * This method sweeps the associated {@link Cache},
     * evicting eligible objects immediately if the cache has exceeded
     * the preferred maximum value associated with the cache. <p>
     * 
     * Once completed, the alarm is scheduled again, so that this method
     * continually sweeps the cache at the interval specified when
     * constructed. For performance, {@link #sweep sweep()} is only called
     * (and the elements walked) when the preferred maximum value has been
     * exceeded. <p>
     * 
     * Rather than building and maintaining a list of potential eviction
     * victims, a sweep count is maintained in the Cache and the Cache
     * updates each element with the current sweep count when accessed.
     * This allows a list of victims to be built on demand. <p>
     **/
    // d91878, F73234
    @Override
    public void run()
    {
        synchronized (ivCancelLock) { //d601399
            if (ivIsCanceled) { //d583637
                //if this instance has been canceled, we do no more processing.
                // this also guarantees that a future alarm is not created.
                return;
            }

            long newSweepInterval = ivNewSweepInterval;
            // NOTE: clamp happens before initializeSweepInterval call to avoid spurious
            // re-init for too-low minimum
            if (ivSweepInterval != newSweepInterval)
            {
                initializeSweepInterval(newSweepInterval);
            }

            int newPreferredCacheSize = ivNewPreferredMaxSize;
            if (ivPreferredMaxSize != newPreferredCacheSize)
            {
                initializeCacheData(newPreferredCacheSize);
            }

            try
            {
                // For dynamic tuning purposes, make the eviction strategy less
                // aggressive (i.e. allow objects to age in the cache longer)
                // if the last several sweeps have resulted in the cache being
                // reduced below the soft limit.                                d112258
                if (ivDiscardThreshold < ivMaxDiscardThreshold &&
                    ivNumBelowSoftLimit > ivMaxDiscardThreshold)
                {
                    ++ivDiscardThreshold;
                    ivNumBelowSoftLimit = 0;
                }

                // For LRU purposes, keep a count of the number of times this
                // alarm has fired. The cache inserts this data in each
                // element when it is last accessed, so it can be determined
                // how old it is in the cache.                                  d112258
                if (ivCache.numSweeps == Long.MAX_VALUE)
                    ivCache.numSweeps = 1;
                else
                    ivCache.numSweeps++;

                // If the soft limit has been reached, then sweep through
                // the cache elements looking for some to evict. Otherwise,
                // just increment the sweep count (above) for LRU purposes.
                if (preferredSizeReached())
                {
                    sweep();

                    // For dynamic tuning purposes, make the eviction strategy more
                    // aggressive (i.e. allow objects to be evicted from the cache
                    // earlier) if the above sweep did not succeed in reducing
                    // the cache below the internal 'upper' limit.               d112258
                    if (preferredSizeReached())
                    {
                        ivNumBelowSoftLimit = 0;

                        if (ivCache.getSize() > ivUpperLimit &&
                            ivDiscardThreshold > ivMinDiscardThreshold)
                        {
                            --ivDiscardThreshold;
                            sweep();
                        }
                    }
                    else
                    {
                        ++ivNumBelowSoftLimit;
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && isTraceEnabled(true))
                        Tr.debug(tc.isDebugEnabled() ? tc : tcOOM,
                                 ivCache.getName() + ": Sweep (" + ivCache.numSweeps +
                                                 "," + ivDiscardThreshold + ")" +
                                                 " - Cache limit not reached : " + ivCache.getSize() +
                                                 "/" + ivPreferredMaxSize);

                    ++ivNumBelowSoftLimit; // d112258
                }
            } catch (Throwable e) //PM11713
            {
                // Not much we can do; maybe it's transient and we'll be
                // successful on our next sweep
                FFDCFilter.processException(e, CLASS_NAME + ".alarm", "446", this);
                Tr.warning(tc, "LRU_THREAD_CAUGHT_EXCEPTION_CNTR0053W",
                           new Object[] { this, e }); //p111002.4
            } finally
            {
                // Unless the preferred size has been reached, use the deferrable 
                // scheduler.  They will be the same for traditional WAS, but in Liberty we 
                // use the deferrable until we need the non-deferrable one to keep
                // up with cache usage.
                ScheduledExecutorService executor = preferredSizeReached() ?
                                ivScheduledExecutorService :
                                ivDeferrableScheduledExecutorService;
                ivScheduledFuture = executor.schedule(this, ivSweepInterval, TimeUnit.MILLISECONDS); // F73234
            }
        }
    }

    //
    // Internal Methods
    //

    /**
     * Loop through the Cache, evicting all eligible objects based on an LRU
     * algorithm until the Cache size is below the soft limit as determined
     * by the {@link Cache}'s preferred maximum value. <p>
     * 
     * No need to reset the LRU data here, as that is done by the Cache when
     * the elements are accessed. <p>
     * 
     * This method is intended for use by the {@link #run run()} method.
     **/
    private void sweep()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && isTraceEnabled(false))
            Tr.entry(tc.isEntryEnabled() ? tc : tcOOM,
                     ivCache.getName() + ": Sweep (" + ivCache.numSweeps +
                                     "," + ivDiscardThreshold + ")" +
                                     " - Cache limit exceeded : " + ivCache.getSize() +
                                     "/" + ivPreferredMaxSize);

        int numEvicted = 0, numEvictedBelowSoftLimit = 0;
        long sweepCount = 0; // d112258

        try
        {
            while (ivElements.hasMoreElements() &&
                   (numEvictedBelowSoftLimit < ivSoftLimitBuffer ||
                   preferredSizeReached()))
            {
                Element element = ivElements.nextElement();

                // Basic operations on basic types are atomic, and we
                // don't care about out-of-sync reads, so we don't
                // bother synchronizing access to the LRU flag here
                if (canBeDiscarded(element))
                {
                    boolean evicted = false;

                    if (ivCache.ivEvictionLocks != null)
                    {
                        // DiscardStrategy requies a lock be obtained to perform
                        // eviction processing to avoid a potential deadlock.  PK04804
                        synchronized (ivCache.ivEvictionLocks.getLock(element.key))
                        {
                            evicted = ivCache.evictObject(element.key);
                        }
                    }
                    else
                    {
                        evicted = ivCache.evictObject(element.key);
                    }

                    if (evicted)
                    {
                        numEvicted++;
                        if (preferredSizeReached())
                            numEvictedBelowSoftLimit = 0;
                        else
                            numEvictedBelowSoftLimit++;
                    }
                }
            }
        } catch (NoSuchElementException e)
        {
            // Enumeration may fail earlier than expected due to
            // concurrent cache access; this is not an error
            FFDCFilter.processException(e, CLASS_NAME + ".sweep", "526", this);

            if (isTraceOn && isTraceEnabled(false))
                Tr.exit(tc.isEntryEnabled() ? tc : tcOOM,
                        ivCache.getName() + ": Sweep (" + ivCache.numSweeps +
                                        "," + ivDiscardThreshold + ")" +
                                        " - Evicted = " + numEvicted + " : " + ivCache.getSize() +
                                        " : NoSuchElementException");
            return;
        } finally { //PM11713

            // Reset the cached enumaration, so it is just like new.  This is
            // done for performance, so a new enumeration doesn't have to be
            // created (and discarded) every sweep.                          d103404.2
            // Note: performing the reset AFTER the sweep insures that the
            // cached enumerator does not hold any resources while not in
            // use (such as a copy of one of the cache buckets).               d310114 
            ivElements.reset();
        }

        if (isTraceOn && isTraceEnabled(false))
            Tr.exit(tc.isEntryEnabled() ? tc : tcOOM,
                    ivCache.getName() + ": Sweep (" + ivCache.numSweeps +
                                    "," + ivDiscardThreshold + ")" +
                                    " - Evicted = " + numEvicted + " : " + ivCache.getSize() +
                                    "/" + ivPreferredMaxSize);
    }

    //
    // EvictionStrategy interface
    //

    /**
     * Called by the cache when it needs to discard an object from the cache. <p>
     * 
     * The cache will be holding the lock on the bucket when this method is
     * invoked. Enables the eviction strategy to change the "decision" on
     * evicting an object with the bucket lock held (a consistent state). <p>
     * 
     * @param element element that is to be evicted.
     * 
     * @return boolean to indicate whether to proceed with eviction or not.
     **/
    @Override
    public boolean canBeDiscarded(Element element)
    {
        if (element.pinned != 0 || element.ivEvictionIneligible) { //d465813
            return false;
        }
        if (ContainerProperties.StrictMaxCacheSize) {
            return true;
        }

        // Very simple algorithm, if the element has been around for
        // more than the discard threshold number of sweep intervals,
        // we evict it.
        long sweepCount = 0;

        if (element.accessedSweep <= ivCache.numSweeps)
            sweepCount = ivCache.numSweeps - element.accessedSweep;
        else
            sweepCount = (Long.MAX_VALUE - element.accessedSweep) + ivCache.numSweeps;

        return sweepCount > ivDiscardThreshold;
    }

    /**
     * Cancel the Scheduled Future object
     */
    @Override
    public void cancel() { //d583637
        synchronized (ivCancelLock) { //d601399
            ivIsCanceled = true;
            if (ivScheduledFuture != null)
                ivScheduledFuture.cancel(false);
            ivCache = null;
        }
    }
}
