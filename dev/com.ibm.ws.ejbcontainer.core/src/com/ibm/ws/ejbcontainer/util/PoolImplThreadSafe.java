/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.util.LockFreeIndexedStack;

public final class PoolImplThreadSafe extends PoolImplBase {
    private static final TraceComponent tc = Tr.register(PoolImplThreadSafe.class, "EJBContainer", null);

    /**
     * Percent an inactive pool should be drained each drain interval.
     */
    private static final int DrainAggressivenessPercentage = 20; // F96377

    /**
     * Size data for the pool that needs to remain in synch. The following
     * fields are included:
     * 
     * --- minSize ---
     * Minimum number of instances the pool may be reduced to.
     * 
     * --- maxSize ---
     * Maximum number of instances the pool may hold.
     * 
     * --- maxDrainAmount ---
     * Maximum number of elements to drain at a time. This value controls how
     * rapidly the the pool will be drained to its minimum size.
     */
    private static class SizeData {
        int minSize;
        int maxSize;
        int maxDrainAmount;
    }

    /**
     * Size information indicating the pool is disabled (min=max=0)
     */
    // F96377
    private static final SizeData DISABLED_SIZE = new SizeData();

    /**
     * Minimum and maximum number of instances to keep in this pool. <p>
     */
    // F96377
    protected volatile SizeData poolSize = DISABLED_SIZE;

    /**
     * The objects managed by this pool.
     */
    private final LockFreeIndexedStack<Object> buffer = new LockFreeIndexedStack<Object>();

    /**
     * Discard strategy this pool uses when throwing away items.
     */
    private final PoolDiscardStrategy discardStrategy;

    /**
     * Count of the number of times periodicDrain has been call and no objects
     * have been drained, since the last time the pool was marked inactive.
     * Reset each time the pool is removed from the manager.
     * 
     * Access to this variable is not synchronized. It must only be accessed
     * from the periodicDrain callback. The manager must ensure that method is
     * called from a single thread only.
     **/
    // d376426
    private int ivInactiveNoDrainCount;

    /**
     * Indicates whether or not the pool is currently being managed by the
     * PoolManager that created it. When a pool becomes inactive, and is drained
     * to the minimum level, it will be removed from the list of managed pools,
     * until it becomes active again.
     * 
     * Access to this variable is synchronized by this instance.
     **/
    // d376426
    private boolean ivManaged;

    /**
     * PMI data
     */
    private final EJBPMICollaborator beanPerf;

    /**
     * <code>Pool</code> instances can only be allocated by the
     * <code>PoolManager</code>.
     * <p>
     */
    PoolImplThreadSafe(int min, int max, EJBPMICollaborator pmiBean, PoolDiscardStrategy d, PoolManagerImpl poolManager) {
        setPoolSize(min, max); // F96377
        discardStrategy = d;
        poolMgr = poolManager;
        beanPerf = pmiBean;

        if (beanPerf != null) {
            beanPerf.poolCreated(0);
        }
    } // PoolImplThreadSafe

    /**
     * Sets the following size data for the pool:
     * <ul>
     * <li>minimum pool size - minimum number of instances the pool may be reduced to.
     * <li>maximum pool size - maximum number of instances the pool may hold.
     * <li>maximum drain amount - maximum number of instances to be drained at one time.
     */
    // F96377
    private void setPoolSize(int min, int max)
    {
        SizeData newSize = new SizeData();
        newSize.minSize = min;
        newSize.maxSize = max;

        // Calculate the maximum drain amount to be <drainAggressivenessPercentage>
        // percent of the total drain opportunity. This will require the pool be
        // inactive for <100/drainAggressivenessPercentage> drain intervals before
        // it is completely drained.
        int drainOpportunity = max - min;
        if (drainOpportunity <= 0)
            newSize.maxDrainAmount = 0;
        else if (drainOpportunity <= 100 / DrainAggressivenessPercentage)
            newSize.maxDrainAmount = drainOpportunity;
        else
            newSize.maxDrainAmount = drainOpportunity * DrainAggressivenessPercentage / 100;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setPoolSize: min=" + newSize.minSize + ", max=" + newSize.maxSize + ", drain=" + newSize.maxDrainAmount);

        poolSize = newSize;
    }

    /**
     * Retrieve an object from this pool.
     * 
     * @return This method will return null if this pool is empty.
     */
    @Override
    public final Object get() {
        Object o = buffer.pop();

        if (beanPerf != null) { // Update PMI data
            beanPerf.objectRetrieve(buffer.size(), (o != null));
        }

        return o;
    } // get

    /**
     * Return an object instance to this pool.
     * 
     * <p>
     * 
     * If there is no room left in the pool the instance will be discarded; in
     * that case if the PooledObject or DiscardStrategy interfaces are being
     * used, the appropriate callback method will be called.
     */
    @Override
    public final void put(Object o) {
        boolean discarded = false;

        if (inactive) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "setting active: " + this);

            // If the pool was marked inactive by the pool manager alarm, then
            // switch it back to active.  Hopefully this happens before too
            // many other threads notice and cause contention with the
            // following sync block.
            inactive = false;

            // If this pool has been inactive long enough to be marked
            // unmanaged, then add it back as managed again.
            synchronized (this) {
                if (!ivManaged) {
                    poolMgr.add(this);
                    ivManaged = true;
                }
            }
        }

        discarded = !buffer.pushWithLimit(o, poolSize.maxSize);

        if (discarded) {
            if (discardStrategy != null) {
                discardStrategy.discard(o);
            }
        }

        if (beanPerf != null) { // Update PMI data
            beanPerf.objectReturn(buffer.size(), discarded);
        }
    } // put

    /**
     * Remove a percentage of the elements from this pool down to its minimum
     * value. If the pool becomes active while draining discontinue.
     */
    @Override
    final void periodicDrain() {
        // Drain to the minimum size but only by the maxDrainAmount.
        // This slows the drain process, allowing for the pool to become
        // active before becoming fully drained (to minimum level).
        SizeData size = poolSize;
        int numDiscarded = drainToSize(size.minSize, size.maxDrainAmount);

        // When a pool becomes inactive, and is drained to the minimum level
        // then there is really no point to continue calling periodicDrain.
        // To avoid this, the pool is removed from the list of managed pools,
        // and since the pool has a reference to the pool manager, it can
        // easily add itself back to the list if it becomes active again.
        // Note that this has the side effect of allowing orphaned pools
        // to be garbage collected... since the PoolManager will also drop
        // its reference. d376426
        if (numDiscarded == 0) {
            // Do not remove immediately, but keep a count and only
            // remove after a few iterations of no drain... to avoid
            // some of the churn of add/remove for pools that are
            // used regularly, but lightly.
            ++ivInactiveNoDrainCount;
            if (ivInactiveNoDrainCount > 4) {
                synchronized (this) {
                    poolMgr.remove(this);
                    ivManaged = false;
                }

                // Reset the count for the next time it becomes active.
                ivInactiveNoDrainCount = 0;
            }
        } else
            ivInactiveNoDrainCount = 0;
    } // periodicDrain

    /**
     * Drain the pool by the specified maximum discard value, but to a level
     * no lower than the specified minimum pooled value.
     * 
     * @param minPooled minimum number to leave in the pool
     * @param maxDiscard maximum number to drain
     * @return the number of instances drained from the pool
     */
    // F96377
    private int drainToSize(int minPooled, int maxDiscard)
    {
        // Stop draining if we have reached the maximum drain amount.
        // This slows the drain process, allowing for the pool to become
        // active before becoming fully drained (to minimum level).
        int numDiscarded = 0;
        Object o = null;

        while (numDiscarded < maxDiscard) {
            o = buffer.popWithLimit(minPooled);

            if (o != null) {
                ++numDiscarded;
                if (discardStrategy != null) {
                    discardStrategy.discard(o);
                }
            } else
                break;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "drainToSize: numDiscarded=" + numDiscarded + ", inactive=" + ivInactiveNoDrainCount + ", " + this);

        if (beanPerf != null) { // Update PMI data
            beanPerf.poolDrained(buffer.size(), numDiscarded);
        }
        return numDiscarded;
    }

    /**
     * 
     * Remove all of the elements from this pool.
     */
    @Override
    final void completeDrain() {
        Object o = null;
        int numDiscarded = buffer.size();
        LockFreeIndexedStack.StackNode<Object> oldTop = buffer.clean();

        while (oldTop != null) {
            o = oldTop.getValue();

            if (discardStrategy != null) {
                discardStrategy.discard(o);
            }

            oldTop = oldTop.getNext();
        }

        if (beanPerf != null) { // Update PMI data
            beanPerf.poolDrained(0, numDiscarded);
        }
    } // completeDrain

    @Override
    void disable()
    {
        poolSize = DISABLED_SIZE;
    }

    @Override
    public int getMaxSize()
    {
        return poolSize.maxSize;
    }

    /**
     * Sets the maximum number of instances to keep in the pool.
     */
    // F96377
    @Override
    public void setMaxSize(int maxSize)
    {
        SizeData size = poolSize;
        if (size.maxSize != maxSize)
        {
            setPoolSize(Math.min(maxSize, size.minSize), maxSize);
            drainToSize(maxSize, Integer.MAX_VALUE);
        }
    }

} // PoolImplThreadSafe
