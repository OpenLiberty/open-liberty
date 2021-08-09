/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

import javax.transaction.TransactionRolledbackException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanNotReentrantException;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.SessionBeanTimeoutException;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.ejs.container.StatefulBeanReaper;
import com.ibm.ejs.container.TimeoutElement;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.websphere.csi.CacheElement;
import com.ibm.websphere.csi.IllegalOperationException;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.failover.SfFailoverClient;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Base ActivationStrategy class for all Stateful Session Beans.
 */

public abstract class StatefulSessionActivationStrategy
                extends ActivationStrategy
{
    //
    // Construction
    //

    /**
     * NOTE: this CTOR should only used when creating a
     * StatefulActivateOnceActivationStrategy object. The other CTOR
     * should be used when creating a StatefulActivateTranActivationStrategy
     * object (since SFSB failover support may be needed).
     * Construct a new <code>StatefulSessionActivationStrategy</code>
     * instance, tied to the specified <code>Activator</code>.
     */
    StatefulSessionActivationStrategy(Activator activator,
                                      PassivationPolicy passivationPolicy)
    {
        this(activator, passivationPolicy, null); //LIDB2018-1
    }

    /**
     * NOTE: this CTOR should only used when creating a
     * StatefulActivateTranActivationStrategy object. The other CTOR
     * should be used when creating a StatefulActivateOnceActivationStrategy
     * object.
     * Construct a new <code>StatefulSessionActivationStrategy</code>
     * instance, tied to the specified <code>Activator</code>.
     */
    StatefulSessionActivationStrategy(Activator activator,
                                      PassivationPolicy passivationPolicy,
                                      SfFailoverCache failoverCache) //LIDB2018-1
    {
        super(activator);
        this.passivationPolicy = passivationPolicy;
        this.reaper = activator.statefulBeanReaper;
        this.ivPassivator = activator.passivator; // LIDB2018-1
        this.ivSfFailoverCache = failoverCache; // LIDB2018-1
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + this + ", " + passivationPolicy +
                         ", " + reaper + ", " + ivPassivator + ", " + failoverCache);
    }

    /**
     * Insert a stateful bean with the specified key into the cache. The caller
     * must be holding the lock associated with the key.
     */
    private void cacheInsert(MasterKey key, StatefulBeanO sfbean) {
        CacheElement cacheElement = cache.insert(key, sfbean);
        sfbean.ivCacheElement = cacheElement;
        sfbean.ivCacheKey = key;

        if (!sfbean.getHome().getBeanMetaData().isPassivationCapable()) {
            cache.markElementEvictionIneligible(cacheElement);
        }
    }

    //
    // Operations
    //

    @Override
    BeanO atCreate(ContainerTx tx, BeanO bean)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atCreate");

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        BeanId id = bean.getId();
        MasterKey key = new MasterKey(id);
        boolean success = false;

        sfbean.ivCacheLock = locks.getLock(key);

        try
        {
            synchronized (sfbean.ivCacheLock)
            {
                cacheInsert(key, sfbean);
                reaper.add(sfbean);

                if (sfbean.sfsbFailoverEnabled())
                {
                    sfbean.createFailoverEntry();
                }
            }

            if (!bean.enlist(tx))
            {
                // If bean.enlist doesn't require us to hold a
                // reference, drop the one taken by cache.insert
                cache.unpinElement(sfbean.ivCacheElement);
            }

            success = true;
        }

        finally
        {
            if (!success)
            {
                // TODO: Unsafe while not holding locks?
                bean.destroy();

                synchronized (sfbean.ivCacheLock)
                {
                    cache.removeElement(sfbean.ivCacheElement, true);
                    sfbean.ivCacheElement = null;
                    sfbean.ivCacheKey = null;

                    reaper.remove(id);

                    if (sfbean.sfsbFailoverEnabled())
                    {
                        sfbean.ivSfFailoverClient.removeEntry(id);
                    }
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "atCreate: exception");
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCreate");
        return null;
    }

    @Override
    void atCommit(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atCommit", new Object[] { tx, bean });

        StatefulBeanO sfbean = (StatefulBeanO) bean;

        synchronized (sfbean.ivCacheLock)
        {
            if (bean.isRemoved())
            {
                cache.removeElement(sfbean.ivCacheElement, true);
                bean.ivCacheKey = null;
                bean.destroy();
            }
            else
            {
                cache.unpinElement(sfbean.ivCacheElement);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit");
    }

    @Override
    void atEnlist(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atEnlist", new Object[] { tx, bean });

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        cache.pinElement(sfbean.ivCacheElement);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atEnlist");
    }

    @SuppressWarnings("null")
    @Override
    BeanO atActivate(EJBThreadData threadData, ContainerTx tx, BeanId beanId) // d630940
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atActivate (" + beanId + ") : " + tx);

        Throwable exception = null;
        MasterKey key = new MasterKey(beanId);
        TimeoutElement elt = null;
        boolean found = false;
        boolean locked = false;
        boolean pushedCallbackBeanO = false;
        EJSDeployedSupport methodContext = threadData.getMethodContext(); // d646139.1
        StatefulBeanO bean = methodContext.getCachedWrapperBeanO();
        Object lock = bean != null ? bean.ivCacheLock : locks.getLock(key);

        // Stateful beans now support container managed concurrency. The access
        // timeout value is obtained to determine how long to wait for the bean
        // to become available. The default is -1, which means wait forever, and
        // is the same as a wait(0). 0 is the legacy value, indicating not to
        // wait, so accessTimeoutReached is set to true. Otherwise, an end
        // time will be calculated before the first wait.               F743-22462
        long accessTimeoutMillis = Long.MIN_VALUE; // indicate not obtained yet
        boolean accessTimeoutReached = false;
        long waitTime = 0;
        long endTime = 0;

        synchronized (lock)
        {
            try
            {
                // Use the bean cached directly on the wrapper if possible. F61004.6
                if (bean == null || bean.ivCacheKey == null)
                {
                    bean = (StatefulBeanO) cache.find(key);
                }
                else
                {
                    // Pin the element like cache.find would have.
                    cache.pinElement(bean.ivCacheElement);
                }

                // Stateful beans now support container managed concurrency, so
                // the following code may need to 'wait' for access to the bean
                // before continuing.  All threads waiting on this bucket will
                // awake at once, so a loop is required to check for access to
                // the bean and try again until the bean is available or the
                // timeout has been reached.                              F743-22462
                for (;; bean = (StatefulBeanO) cache.find(key))
                {
                    found = bean != null;
                    if (!found)
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Bean not in cache");

                        elt = reaper.getTimeoutElement(beanId); // F61004.5

                        //LIDB2018-1 start
                        boolean timedOut;
                        if (EJSPlatformHelper.isZOS())
                        {
                            // For zOS, assumption is shared file system used for
                            // SFSB failover. Therefore, only timeout if the SFSB
                            // exists in reaper list AND it has timed out.
                            timedOut = reaper.beanExistsAndTimedOut(elt, beanId) ||
                                       passivationPolicy.equals(PassivationPolicy.ON_DEMAND);

                        }
                        else
                        {
                            // For distributed, if SFSB is not in the reaper list or
                            // the local failover cache list, then the bean previously timed
                            // out and was removed. Therefore, set timeout if bean does
                            // not exist OR it exists and has timed out.
                            timedOut = reaper.beanDoesNotExistOrHasTimedOut(elt, beanId) ||
                                       passivationPolicy.equals(PassivationPolicy.ON_DEMAND);
                        }

                        if (timedOut)
                        {
                            // To avoid customer confusion, only throw the
                            // StatefulBeanTimeoutException if the bean definitely
                            // exists and is in the timed out state.... otherwise,
                            // report NoSuchObjectException, since the bean may
                            // have been removed (or timed out).                   d253039
                            if (reaper.beanExistsAndTimedOut(elt, beanId))
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(tc, "Bean " + beanId +
                                                 " in passivated filesystem or SfFailoverCache, " +
                                                 " but timed out.");

                                throw new SessionBeanTimeoutException("Stateful bean " +
                                                                      beanId + " timed out.");
                            }
                            else
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(tc, "Bean " + beanId +
                                                 " not in the passivated filesystem either.");

                                throw new NoSuchObjectException("Stateful bean " + beanId +
                                                                " was removed or timed out.");
                            }
                        }
                        //LIDB2018-1 end

                        // At this point the bean is not in the
                        // cache, and has not been removed by the reaper
                        // (from either reaper list or failover cache).
                        bean = (StatefulBeanO) beanId.getHome().createBeanO(threadData, tx, beanId); // d630940
                        pushedCallbackBeanO = true;
                        bean.ivCacheLock = lock;
                        cacheInsert(key, bean);
                        found = true;

                        // Perform the activation with the lock in hand.  Either the
                        // cache cleanup thread or the reaper thread could be trying
                        // to discard/timeout the bean.

                        bean.initializeTimeout(elt); // F61004.5
                        bean.activate(beanId, tx); // d114677

                        if (elt == null) // F61004.5
                        {
                            // This can happen when bean is in failover cache and SFSB failover
                            // occurs. It was removed from local SfFailoverCache when
                            // bean was activated from failover cache.  Therefore, we need to add
                            // to reaper list.
                            reaper.add(bean);
                        }

                        // If we activated from data in failover cache, then
                        // we need to inform remote failover servers that the bean
                        // is now activated so that remote failover servers do not
                        // timeout the SFSB prematurely.
                        if (bean.sfsbFailoverEnabled()) //LIDB2018-1
                        {
                            bean.updateFailoverSetActiveProp(); //LIDB2018-1
                        }

                        bean.lock(methodContext, tx);
                        locked = true;

                        // We activated and locked the bean, so we're done.
                        break;
                    }

                    // The bean is in the cache... so only continue to use the
                    // bean if it can be locked (no other thread is using it, or
                    // this thread is already using it). The loop will be exited,
                    // and a ConcurrentAccessException will be thrown from enlist
                    // below if required (i.e. if reentrant call).   F743-22462.CR
                    locked = bean.lock(methodContext, tx);

                    // It is possible that when the lock was attempted, the thread
                    // was transitioned to a sticky tran associated with the sfsb
                    // (lock successful) or a different local tran (lock failed
                    // due to another thread resuming the sticky tran first).
                    // This will occur if the current thread was not aware of the
                    // sticky tran earlier in preInvoke, or while waiting, another
                    // thread called a method that began a sticky tran.  d671368.1
                    tx = methodContext.getCurrentTx();

                    if (locked) // d704504
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Found bean - locked, attempting to use");

                        // Set the found BeanO as the 'Callback' BeanO, as this is the
                        // BeanO that is becoming the active beanO for the thread.
                        // This will allow methods called by customer code (like Timer
                        // methods) to determine the state of the BeanO that is making
                        // the call.                                           d168509
                        threadData.pushCallbackBeanO(bean); // d630940
                        pushedCallbackBeanO = true;

                        // We locked the bean, so we're done.
                        break;
                    }

                    // If the lock was not obtained, then need to determine the
                    // access timeout setting and then either abort with a timeout
                    // exception or wait for the appropriate time.   F743-22464.CR

                    // Only obtain the config setting the current method the
                    // first time through the while loop.         F743-22462.CR
                    if (accessTimeoutMillis == Long.MIN_VALUE)
                    {
                        accessTimeoutMillis = methodContext.getConcurrencyAccessTimeout();
                        accessTimeoutReached = (accessTimeoutMillis == 0);
                    }

                    // If the bean is still in use, but the access-timeout has
                    // been reached, then a ConcurrentAccessException needs to
                    // be thrown. For compatibility with older application and
                    // the pre-existing container code that is expecting a
                    // RemoteException... throw a BeanNotReentrantException,
                    // and it will be mapped.  Previously, this would have been
                    // thrown from the enlist call below, but that method does
                    // not behave well for all BeanO states.               d648385
                    if (accessTimeoutReached)
                    {
                        // Clear the bean so it is not removed from the cache
                        // below in the finally, and release the cache pin taken
                        // on the find above.
                        bean = null;
                        cache.unpin(key);

                        String msg = "Stateful bean in use on another thread; " +
                                     "access-timeout reached (" + accessTimeoutMillis +
                                     " ms)";
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, msg);

                        throw new BeanNotReentrantException(msg, true); // d653777.1
                    }

                    // The bean is in the cache... and is in use by another
                    // thread. Release the pin that was acquired on the find
                    // above and clear the bean field so we will try again
                    // after waiting for the other thread to finish.       F743-22462
                    bean.addLockWaiter(); // F743-22462.CR
                    bean = null;
                    cache.unpin(key);

                    // First time through the loop - determine end if not forever
                    if (accessTimeoutMillis > 0 && waitTime == 0)
                    {
                        waitTime = TimeUnit.MILLISECONDS.toNanos(accessTimeoutMillis);
                        endTime = System.nanoTime() + waitTime;
                    }

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Found bean - in use," +
                                     " waiting : " + waitTime + " ns, lock = " +
                                     lock);

                    if (waitTime == 0) {
                        lock.wait();
                    } else {
                        TimeUnit.NANOSECONDS.timedWait(lock, waitTime);
                    }

                    // if not forever, determine if timeout has been reached
                    if (accessTimeoutMillis > 0)
                    {
                        waitTime = endTime - System.nanoTime();
                        if (waitTime <= 0)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "AccessTimeout reached");
                            accessTimeoutReached = true;
                        }
                    }
                }

                // At this point, the SFSB is activated if necessary (so its last
                // activated time has been read from failover cache if needed), so check
                // whether or not the bean has timed out.
                if (bean.isTimedOut()) // F61004.5
                {
                    throw new SessionBeanTimeoutException("Stateful bean " + bean +
                                                          " timed out");
                }

                if (bean.enlist(tx, methodContext.isTxEnlistNeededForActivate())) // d114677, F61004.1
                {
                    cache.pinElement(bean.ivCacheElement);
                }

            } catch (InterruptedException iex) {

                FFDCFilter.processException(iex, CLASS_NAME + ".atActivate", "384", this);
                exception = iex;
                throw new ContainerException(iex);

            } catch (NoSuchObjectException e) {

                // Expected for removed beans, don't FFDC log.               d740149
                // FFDCFilter.processException(e, CLASS_NAME + ".atActivate", "571", this);
                exception = e;
                throw e;

            } catch (RemoteException e) {

                FFDCFilter.processException(e, CLASS_NAME + ".atActivate", "169", this);
                exception = e;
                throw e;

            } catch (RuntimeException e) {

                FFDCFilter.processException(e, CLASS_NAME + ".atActivate", "175", this);
                exception = e;
                throw e;

            } finally {

                if (exception != null) {
                    if (isTraceOn && tc.isEventEnabled())
                        Tr.event(tc, "atActivation: exception raised", exception);
                }

                if (exception != null && bean != null)
                {
                    if (pushedCallbackBeanO)
                    {
                        threadData.popCallbackBeanO();
                    }

                    // Since an exception occurred and there is a bean, then
                    // cleanup is required.  If the bean is in the EJB Cache
                    // then it needs to be removed and destroyed, otherwise
                    // just destroy it. Note: Do NOT destroy the bean if
                    // it cannot be removed from the cache.                   PQ52534
                    if (found)
                    {
                        // Try to remove the bean here, is cleaner
                        try {
                            cache.remove(key, true);
                            bean.ivCacheKey = null; // d199233
                            reaper.remove(beanId);
                            bean.destroy();
                        } catch (IllegalOperationException ex) {
                            // Activation failed, drop the reference from find
                            // above. We expect this to happen if there is a
                            // reentrant call - do not destroy the bean, as
                            // another thread/method is using it.
                            // Do not ffdc, since expected (comment required).  d195475
                            //FFDCFilter.processException(ex, CLASS_NAME + ".atActivate",
                            //                            "204", this);
                            cache.unpin(key);
                        } finally {
                            if (locked) {
                                bean.unlock(lock);
                            }
                        }
                    }
                    else
                    {
                        // The bean is not in the EJB cache - it was just created
                        // but never used, so destroy it.                      PQ52534
                        bean.destroy();
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atActivate : " + bean); // d161864

        return bean;
    }

    @Override
    void atPostInvoke(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atPostInvoke : " + tx + ", " + bean);

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        BeanId id = bean.getId();

        //
        // If the bean was either removed or discarded during the method
        // invocation, remove it from the cache at this point
        // We should delist the bean from the transaction
        // No transaction callbacks will get driven on the bean.
        //
        Object lock = sfbean.ivCacheLock;
        synchronized (lock)
        {
            if (sfbean.ivCacheKey != null)
            {
                cache.unpinElement(sfbean.ivCacheElement);
                if (sfbean.isRemoved() || sfbean.isDiscarded()) {
                    if (tx != null) {
                        try {
                            tx.delist(sfbean);
                        } catch (TransactionRolledbackException ex) {
                            FFDCFilter.processException(ex, CLASS_NAME + ".atPostInvoke",
                                                        "261", this);
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "atPostInvoke : transaction has rolledback");
                        }
                    }

                    cache.removeElement(sfbean.ivCacheElement, true);
                    sfbean.destroy();
                    sfbean.ivCacheKey = null; // d199233
                    reaper.remove(id);
                } else {
                    sfbean.setLastAccessTime(System.currentTimeMillis()); // F61004.5
                }

                // The bean is no longer in a method, and may even have been
                // destroyed; notify all other threads waiting.   F743-22462 d650932
                sfbean.unlock(lock);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atPostInvoke");
    }

    @Override
    void atDiscard(BeanO bean) throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atDiscard : " + bean);

        try {

            // The lock on this bucket is currently held in the cache. So
            // we do not need to lock the entry here again.  Additionally, the
            // table lock for this bucket is held by the background thread, which
            // has also ensured the element is not pinned.

            BeanId beanId = ((StatefulBeanO) bean).getId();
            StatefulBeanO statefulBeanO = (StatefulBeanO) bean; //d204278.2
            bean.ivCacheKey = null; // d199233

            if (!statefulBeanO.isTimedOut()) //d204278, F61004.5
            {
                bean.passivate();
            }
            else
            {
                // if the session time out has expired timeout also we make
                // to call ejbRemove for this bean.
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "Discarding session bean : " + bean);
                reaper.remove(beanId);
                statefulBeanO.destroy(); //d204278.2
            }

        } catch (RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".atDiscard", "368", this);
            Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                       new Object[] { bean, this, e }); //p111002.3
            throw e;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atDiscard");
    }

    @Override
    void atRemove(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atRemove", new Object[] { tx, bean });

        StatefulBeanO sfbean = (StatefulBeanO) bean;

        synchronized (sfbean.ivCacheLock)
        {
            cache.removeElement(sfbean.ivCacheElement, true);
            bean.ivCacheKey = null;
        }

        reaper.remove(bean.getId());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atRemove");
    }

    /**
     * Called when the stateful bean's session has timed out. The bean needs
     * to be destroyed or passivated and destroyed. A call to ejbRemove will
     * not be made at this point.
     */
    @Override
    void atTimeout(BeanId beanId) throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "atTimeout " + beanId);
        }

        BeanO bean = null;
        Throwable exception = null;
        MasterKey key = new MasterKey(beanId);

        try
        {
            synchronized (locks.getLock(key))
            {
                // Since the StatefulBeanReaper may detect that beans have timed
                // out in various states (active or passivated), it is necessary
                // to handle all of the various situations here.  Note that the
                // bean may have even changed states, including being removed
                // since the reaper first detected it has timed out.         d112258
                if ((bean = (BeanO) cache.findDontPinNAdjustPinCount(key, 0)) == null)
                {
                    // If the bean is not in the cache, then either the bean
                    // has been passivated (normal case), and so the passivated
                    // file needs to be deleted.  Or, the application or the
                    // background cache eviction thread has removed the bean,
                    // in which case there is nothing to do.                  d112258
                    // If the bean is using transaction activation policy and timed
                    // out while in a method, post invoke will update its last
                    // access and passivate it.  For this scenario, we must verify
                    // that the bean is still timed out now that we have locks.
                    TimeoutElement elt = reaper.getTimeoutElement(beanId);
                    if (reaper.beanExistsAndTimedOut(elt, beanId))
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Bean not in cache: removing file");

                        // LIDB2018-1 begins
                        ivPassivator.remove(beanId, true); //LIDB2018-1 //d204278.2

                        // d204278.2
                        // Ensure bean is no longer in reaper list.
                        reaper.remove(beanId); // d103404.1

                        // StatefulPassivator.remove call did remove SFSB from
                        // either bean store or from local SfFailoverCache.  For the
                        // latter case, it did not remove from remote failover caches.
                        // So we need to ensure SFSB is removed from remote
                        // failover cache as well since a timeout occurred.
                        BeanMetaData bmd = beanId.getBeanMetaData();
                        SfFailoverClient failover = bmd.getSfFailoverClient();
                        if (failover != null)
                        {
                            failover.removeEntry(beanId);
                        }
                        // LIDB2018-1 ends
                    }
                    else
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Bean not in cache: already removed");
                    }
                }
                else
                {
                    // If the bean was found in the cache, then it is active
                    // (possibly even activated after the StatefulBeanReaper detected
                    // it had timed out). Since it is in the cache, it is possible
                    // that it is enlisted in a transaction, or was in a transaction
                    // and is now no longer timed out (because the timeout clock is
                    // reset at the end of a method). Double check that the bean is
                    // still timed out now that we've acquired locks.         d112258
                    StatefulBeanO sfbean = (StatefulBeanO) bean;
                    if (sfbean.isTimedOut()) //d204278, F61004.5
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Found bean in cache: passivating / removing");

                        try
                        {
                            // The bean is in the cache and timed out. Remove it from
                            // the cache, reaper, and uninstall it which will take it
                            // through the required EJB lifecycle (passivate).  d112258
                            bean = (BeanO) cache.remove(key, false);
                            bean.ivCacheKey = null; // d199233
                            reaper.remove(beanId);

                            //d523478 start
                            // Ensure SFSB is removed from remote failover cache as well since a timeout occurred.
                            BeanMetaData bmd = beanId.getBeanMetaData();
                            SfFailoverClient failover = bmd.getSfFailoverClient();
                            if (failover != null)
                            {
                                failover.removeEntry(beanId);
                            }
                            // d523478 end

                            // Now uninstall the bean.
                            ((StatefulBeanO) bean).uninstall();
                        } catch (IllegalOperationException ioe)
                        {
                            // This occurs if the bean timed out while enlisted with
                            // an active transaction (i.e. the timeout clock is
                            // reset at the end of trans).  Nothing should be done
                            // except release the pin taken above on the find.  d112258
                            // Do not ffdc, since expected (comment required).  d195475
                            //FFDCFilter.processException(ioe, CLASS_NAME + ".atTimeout",
                            //                            "457", this);
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Found bean in cache: active");
                        }
                    }
                    else
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Found bean in cache: no longer timed out");
                    }
                }
            }
        } catch (RemoteException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".atTimeout", "474", this);
            exception = e;
            throw e;
        } catch (RuntimeException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".atTimeout", "480", this);
            exception = e;
            throw e;
        } finally
        {
            if (exception != null) {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "atTimeout: exception raised", exception);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atTimeout");
        }
    }

    /**
     * This method will be invoked when the server invokes one of passivate
     * or passivateAll on the ManagedContainer interface.
     *
     * On z/OS, this method will be invoked when the server invokes one of passivateAll
     * on the ManagedContainer interface or during normal server stop,
     * starting at EJSContainer.stopBean->Activator.uninstallBean. This is not
     * the same as Distributed. In Distributed the atUninstall method is called
     * on this class because for some reason they feel that normal server
     * stop is like server going down. And they want to get rid of all the Stateful
     * session beans, cached and passivated. See atUninstall description.
     */
    @Override
    public void atPassivate(BeanId beanId) throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atPassivate " + beanId);

        BeanO bean = null;
        MasterKey key = new MasterKey(beanId);

        try {

            synchronized (locks.getLock(key)) {

                if ((bean = (BeanO) cache.findDontPinNAdjustPinCount(key, 0)) != null)
                {
                    bean.passivate();
                    cache.remove(key, false);
                    bean.ivCacheKey = null; // d199233
                    if (EJSPlatformHelper.isZOS() || // LIDB2775-23.4
                        passivationPolicy.equals(PassivationPolicy.ON_DEMAND))
                    {
                        reaper.remove(beanId);
                    }
                }
            }

        } catch (RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".atPassivate", "525", this);
            Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                       new Object[] { bean, this, ex }); //p111002.3
            throw ex;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atPassivate");
    }

    @Override
    BeanO atGet(ContainerTx tx, BeanId id)
    {
        throw new IllegalStateException();
    }

    /**
     * When stateful beans are uninstalled we destroy the beans. The beans
     * are removed from the cache or passivation directory and removed from
     * the Stateful Bean Reaper. <p>
     *
     * If the module is reinstalled and the same reference is reused, then an
     * Exception will occur indicating that the object could not be found. <p>
     *
     * Previously, the beans were passivated, but in addition to this causing
     * too much overhead during server shutdown, it is also likely that the
     * bean class has been changed in an incompatible way across a module
     * stop and start. d106838
     * <p>
     * Also, removing the bean (i.e. calling ejbRemove) can also be
     * problematic as ejbRemove frequently contains calls to remove other
     * beans. <p>
     *
     * Currently, they are "uninstalled", which transitions the bean
     * the passivated and then destroyed without actually streaming the
     * instance to a file. d112866
     *
     * For z/OS, probably need a way to call this when a bean is truly being
     * uninstalled so that we can delete Stateful Session beans for the same
     * reason stated earlier. This method is not called on z/OS. Instead we
     * call atPassivate from Activator.uninstallBean.
     */
    @Override
    void atUninstall(BeanId beanId, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atUninstall (" + beanId + ")");

        // The 'bean' parameter may be null (i.e. when calling to uninstall
        // a passivated bean), so don't count on it.                      LI3408

        MasterKey key = new MasterKey(beanId);

        synchronized (locks.getLock(key)) {

            if ((bean = (BeanO) cache.find(key)) != null)
            {
                // Regardless of whether the bean has timed out or not, remove it
                // from the cache and reaper and call uninstall, which will result
                // in ejbPassivate being called for this bean, but skip streaming
                // the bean out to a file.                         d106838 d112866
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "Found bean in cache: uninstalling");
                try
                {
                    cache.remove(key, true);
                    bean.ivCacheKey = null; // d199233
                    reaper.remove(beanId); // d159666.1
                    ((StatefulBeanO) bean).uninstall(); // d112866
                } catch (IllegalOperationException ioe)
                {
                    // This occurs if the application is stopped while the bean
                    // is enlisted with an active transaction (i.e. probably hung).
                    // Nothing should be done except release the pin taken above
                    // on the find.                                       d159666.1
                    FFDCFilter.processException(ioe, CLASS_NAME + ".atUninstall",
                                                "590", this);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Found bean in cache: active!");

                    cache.unpin(key);
                }
            }
            else
            {
                // Since the bean is not in the cache, most likely, the bean
                // is in the passivated state, so the file just needs to be
                // deleted. However, it is also possible the StatefulBeanReaper
                // has already removed the file.                           d129562
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Bean not in cache: removing file");

                try
                {
                    // Check if in bean is in the reaper list.  If so, then
                    // delete the file used when bean was passivated. Note,
                    // we do not want to remote the failover cache entry if the reason
                    // we are uninstalling is because server is shutting down.
                    // That is why we pass false to the Passivator remove method.
                    if (reaper.remove(beanId))
                    {
                        ivPassivator.remove(beanId, false); //LIDB2018-1
                    }
                } catch (RemoteException rex)
                {
                    // Just log the exception.  Not being able to remove the file
                    // should not be cause to fail the uninstall of the bean.
                    FFDCFilter.processException(rex, CLASS_NAME + ".atUninstall",
                                                "598", this);
                    Tr.warning(tc, "REMOVE_FROM_PASSIVATION_STORE_FAILED_CNTR0016W"
                               , new Object[] { beanId, rex });
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atUninstall");
    }

    // LIDB2775-23.4 Begins
    public StatefulBeanReaper getReaper()
    {
        return reaper;
    }

    // LIDB2775-23.4 Ends

    //
    // Data
    //

    protected PassivationPolicy passivationPolicy; //d204278.2

    protected StatefulBeanReaper reaper;

    public StatefulPassivator ivPassivator; //LIDB2018-1

    /**
     * The failover cache that this server uses to cache replicated
     * SFSB data from other servers. A null reference if SFSB
     * failover is not enabled.
     */
    public SfFailoverCache ivSfFailoverCache; //LIDB2018 -1

    // Tracing
    private static final TraceComponent tc =
                    Tr.register(StatefulSessionActivationStrategy.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.activator.StatefulSessionActivationStrategy";
}
