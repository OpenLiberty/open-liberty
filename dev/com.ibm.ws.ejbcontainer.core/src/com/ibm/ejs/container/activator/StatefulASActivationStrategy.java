/*******************************************************************************
 * Copyright (c) 2002, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import java.rmi.RemoteException;

import javax.transaction.TransactionRolledbackException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerAS;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.websphere.csi.IllegalOperationException;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Overrides the pin behavior of the StatefulActivateTran activation strategy
 * to hold a cache pin for the duration of an activity session, instead of that
 * of a method. <p>
 * 
 * When parent methods are invoked, they will either acquire a lock for the
 * transaction and method, or assume there is a pin for each. The overrides
 * in this class will decide to either release extra locks taken for method
 * calls, or take ownership of a method pin as the activity session pin. <p>
 * 
 * BeanManaged activity sessions can be especially tricky. When a method is
 * called prior to an activity session, it will obtain a local tran pin
 * and a method pin. When an activity session is then started, the local tran
 * pin will be released, the method pin will be transferred to become the
 * activity session pin, and a new pin for a new local tran will be obtained. <p>
 * 
 * Similarly, when the activity session completes, the activity session pin
 * will not be released, but instead transferred back to the method pin, which
 * will then be released during atPostInvoke of the parent strategy. <p>
 * 
 * For ContainerManaged activity sessions, transfers of pins between method
 * and activity session are less confusing, as the method and activity
 * session will have the same duration. <p>
 */
public class StatefulASActivationStrategy
                extends StatefulActivateTranActivationStrategy
{
    private static final String CLASS_NAME = StatefulASActivationStrategy.class.getName();

    private static final TraceComponent tc = Tr.register(StatefulASActivationStrategy.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    public StatefulASActivationStrategy(Activator activator,
                                        PassivationPolicy policy,
                                        SfFailoverCache failoverCache) // d204278
    {
        super(activator, policy, failoverCache);
    }

    @Override
    BeanO atCreate(ContainerTx tx, BeanO bean)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atCreate (" + tx + ", " + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = tx.getContainerAS();

        // Creation of a Stateful bean cannot result in a 'duplicate' (i.e. found
        // in cache) as the key will always be a new key, so rely on the parent
        // strategy to do the right thing, and then enlist in the AS if present
        // and insure proper pinning.                                      d655854
        super.atCreate(tx, bean); // d672339

        // If there is an AS, then perform any ActivitySession specific stuff...
        if (as != null)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "atCreate : running in AS : " + as);

            // In any case make sure the beanO is associated with the current TX
            bean.setContainerTx(tx);

            // Now enlist the bean in the AS. If successful take another pin count
            // for the AS. The parent call only takes a pin for the transaction.
            if (as.enlist(bean))
            {
                cache.pinElement(((StatefulBeanO) bean).ivCacheElement);
            }
        }

        // Only pre-existing beans are returned, otherwise null. For Stateful,
        // there can never be a pre-existing bean with the same ID, so always
        // just return null.                                               d672339
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCreate : null");
        return null;
    }

    @Override
    BeanO atActivate(EJBThreadData threadData, ContainerTx tx, BeanId beanId) // d630940
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atActivate (" + tx + ", " + beanId + ")");

        // Get the current ContainerAS
        ContainerAS as = tx.getContainerAS();

        // Nothing prevents a Stateful bean from participating in multiple
        // activity sessions, only multiple transactions. So, rely on the
        // parent strategy to insure proper concurrency locking of the bean.
        // This call will only return successfully when the bean is either
        // not in use by another transaction, or another thread. Once obtained
        // enlist in the AS if present, and insure proper pinning.         d655854
        BeanO bean = super.atActivate(threadData, tx, beanId);

        // If there is an AS, then perform any ActivitySession specific stuff...
        if (as != null)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "atActivate : running in AS : " + as);

            // Make sure the beanO is associated with the current TX
            bean.setContainerTx(tx);

            // Enlist the bean with the AS; a no-op if already enlisted.
            // Another pin is not needed, and in fact one may need to be dropped.
            // The super atAtivate will insure there are 2 pins; one for the tx
            // and one for the current method... but this strategy does not pin
            // for methods, but instead ActivitySessions... so the method pin is
            // used as the AS pin, but that is not needed if already enlisted,
            // and so should be dropped.                                    d655854
            if (!as.enlist(bean))
            {
                cache.unpinElement(((StatefulBeanO) bean).ivCacheElement);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atActivate : " + bean);

        return bean;
    }

    @Override
    void atCommit(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atCommit (" + tx + ", " + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = ContainerAS.getContainerAS(tx);

        // if no session or not enlisted in the session, delegate to the non
        // activity session activation strategy. A bean may not be enlisted in
        // an activity session if this bean uses bean managed activity sessions
        // and local tran boundary is method and this method started the
        // current activity session.                                       d655854
        if (as == null || !as.isEnlisted(bean.getId()))
        {
            super.atCommit(tx, bean);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atCommit");
            return;
        }

        resetBeanOContainerTx(bean);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit");
    }

    private void resetBeanOContainerTx(BeanO bean)
    {
        StatefulBeanO sfbean = (StatefulBeanO) bean; // d655854
        Object lock = sfbean.ivCacheLock;

        synchronized (lock)
        {
            if (bean.ivCacheKey != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "resetBeanOContainerTx : " + sfbean);

                // Drop pin held for tran                                    d130022
                cache.unpinElement(sfbean.ivCacheElement);
                sfbean.setContainerTx(null);

                // The bean is no longer in a transaction, so notify all other
                // threads waiting.                                          d655854
                sfbean.unlock(lock);
            }
        }
    }

    @Override
    void atRollback(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atRollback (" + tx + ", " + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = ContainerAS.getContainerAS(tx);

        // if no session or not enlisted in the session, delegate to the non
        // activity session activation strategy. A bean may not be enlisted in
        // an activity session if this bean uses bean managed activity sessions
        // and local tran boundary is method and this method started the
        // current activity session.                                       d655854
        if (as == null || !as.isEnlisted(bean.getId()))
        {
            super.atRollback(tx, bean);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atRollback");
            return;
        }

        resetBeanOContainerTx(bean);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atRollback");
    }

    /**
     * Overridden to enlist the bean in the current activity session,
     * if one exists. <p>
     * 
     * This method is called when a user transaction or user activity
     * session is beginning or ending. An activity session will only
     * be active for the case where beginSession is being called. <p>
     */
    @Override
    void atEnlist(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atEnlist (" + tx + ", " + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = ContainerAS.getContainerAS(tx);

        // Allow the parent to properly enlist the bean in the transaction.
        // Really, just takes a pin, as processTxContextChange did the enlist.
        super.atEnlist(tx, bean);

        // If there is an AS, then perform any ActivitySession specific stuff...
        if (as != null)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "atEnlist : running in AS : " + as);

            // Make sure the beanO is associated with the current TX
            bean.setContainerTx(tx);

            // Enlist the bean with the AS; a no-op if already enlisted.
            // Another pin is not needed, nor does one need to be dropped.
            //
            // Here are the possible scenarios:
            // 1 - UAS.beginSession: a pin will have been taken for the method
            //     call... that becomes the AS pin.
            // 2 - UAS completion: AS is not active, AS pin is now the pin
            //     for the current method call.
            // 3 - UTx.begin: either an AS is not present or the bean is already
            //     enlisted with the AS and pinned. If there is an AS, there is
            //     no method pin, as atAtivate would not have taken one.
            // 4 - UTx completion - if an AS is present, it already has a pin
            //     and a method pin was not taken. If no AS, then method has
            //     a pin.
            //
            // Net is, we must insure the bean is enlisted with the AS, as it
            // takes over ownership of one of the pins. If not enlisted here,
            // then the next activate will take an extra pin.               d655854
            as.enlist(bean);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atEnlist");
    }

    @Override
    void atPostInvoke(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atPostInvoke (" + tx + ", " + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = ContainerAS.getContainerAS(tx);

        // if no session or not enlisted in the session, delegate to the non
        // activity session activation strategy. A bean may not be enlisted in
        // an activity session if this bean uses bean managed activity sessions
        // and local tran boundary is method and this method started the
        // current activity session.                                       d655854
        if (as == null || !as.isEnlisted(bean.getId()))
        {
            super.atPostInvoke(tx, bean);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atPostInvoke");
            return;
        }

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        BeanId id = bean.getId();
        Object lock = sfbean.ivCacheLock;

        //
        // If the bean was either removed or discarded during the method
        // invocation, remove it from the cache at this point
        // We should delist the bean from the transaction and activity session
        // No transaction callbacks will get driven on the bean.
        //
        synchronized (lock)
        {
            if (bean.ivCacheKey != null)
            {
                if (bean.isRemoved() || bean.isDiscarded())
                {
                    // Drop reference taken by activation
                    cache.unpinElement(sfbean.ivCacheElement);

                    if (tx != null) {
                        try {
                            tx.delist(bean);
                        } catch (TransactionRolledbackException ex) {
                            FFDCFilter.processException(ex, CLASS_NAME + ".atPostInvoke",
                                                        "258", this);
                            Tr.debug(tc, "atPostInvoke : transaction has rolledback");
                        }
                    }

                    bean.setContainerTx(null);
                    as.delist(bean); // delist from activity session

                    cache.removeElement(sfbean.ivCacheElement, true);
                    sfbean.destroy();
                    bean.ivCacheKey = null; // d199233
                    reaper.remove(id);
                }
                else
                {
                    sfbean.setLastAccessTime(System.currentTimeMillis()); // F61004.5
                }

                // The bean is no longer in a method, and may even have been
                // destroyed; notify all other threads waiting.              d655854
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
            Tr.entry(tc, "atDiscard (" + bean + ")");

        // Get the current ContainerAS
        ContainerAS as = ContainerAS.getCurrentContainerAS();

        // if no session or not enlisted in the session, delegate to the non
        // activity session activation strategy. A bean may not be enlisted in
        // an activity session if this bean uses bean managed activity sessions
        // and local tran boundary is method and this method started the
        // current activity session.                                       d655854
        if (as == null || !as.isEnlisted(bean.getId()))
        {
            super.atDiscard(bean);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atDiscard");
            return;
        }

        try
        {
            // The lock on this bucket is currently held in the cache. So
            // we do not need to lock the entry here again.  Additionally, the
            // table lock for this bucket is held by the background thread, which
            // has also ensured the element is not pinned.

            BeanId beanId = ((StatefulBeanO) bean).getId();
            StatefulBeanO statefulBeanO = (StatefulBeanO) bean; //d204278.2

            if (!statefulBeanO.isTimedOut()) //d204278, F61004.5
            {
                as.delist(bean); // delist the bean from the activity session
                bean.setContainerTx(null);
                bean.ivCacheKey = null; // d199233
                bean.passivate();
            }
            else
            {
                // if the session time out has expired timeout also we make
                // to call ejbRemove for this bean.
                Tr.event(tc, "Discarding session bean", bean);
                reaper.remove(beanId);
                statefulBeanO.destroy(); //d204278.2
                bean.ivCacheKey = null; // d199233
            }

        } catch (RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".atDiscard", "484", this);
            Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                       new Object[] { bean, this, e });
            throw e;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atDiscard");
    }

    @Override
    void atUnitOfWorkEnd(ContainerAS as, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atUnitOfWorkEnd (" + as + ", " + bean + ")");

        // if no session or not enlisted in the session, delegate to the non
        // activity session activation strategy. A bean may not be enlisted in
        // an activity session if this bean uses bean managed activity sessions
        // and local tran boundary is method and this method started the
        // current activity session.                                       d655854
        if (as == null || !as.isEnlisted(bean.getId()))
        {
            super.atUnitOfWorkEnd(as, bean);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "atUnitOfWorkEnd");
            return;
        }

        Object key = bean.ivCacheKey; // MasterKey                d199233
        StatefulBeanO sfbean = null; // d655854

        synchronized (locks.getLock(key))
        {
            sfbean = (StatefulBeanO) cache.find(key);

            if (sfbean != null)
            {
                cache.unpin(key); // drop pin taken above

                // If the bean has not been removed and is not in a method
                // (i.e. BMAS), passivate the bean just like the ActivateTran
                // strategy. There is no need to 'unlock' the bean, as the
                // concurrency lock is only held for transactions, not ASs.  d655854
                if (!sfbean.isRemoved() && sfbean.getState() != StatefulBeanO.IN_METHOD)
                {
                    try
                    {
                        cache.remove(key, true);
                        sfbean.ivCacheKey = null; // d199233
                        bean.setContainerTx(null);

                        sfbean.passivate();
                    } catch (IllegalOperationException ioe)
                    {
                        // FFDCFilter.processException(ioe, CLASS_NAME + ".atUnitOfWorkEnd",
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Ignoring IllegalOperationException.");

                        cache.unpin(key);
                    } catch (RemoteException ex)
                    {
                        FFDCFilter.processException(ex, CLASS_NAME + ".atUnitOfWorkEnd", "64", this);
                        Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                                   new Object[] { bean, this, ex }); //p111002.3
                    }
                }
            }
            else
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Bean not found in cache");
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atUnitOfWorkEnd");
    }

}
