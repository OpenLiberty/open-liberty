/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
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

import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.websphere.csi.IllegalOperationException;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ffdc.FFDCFilter;

public class StatefulActivateTranActivationStrategy
                extends StatefulSessionActivationStrategy
{
    public StatefulActivateTranActivationStrategy(Activator activator,
                                                  PassivationPolicy policy)
    {
        super(activator, policy, null); //LIDB2018-1
    }

    public StatefulActivateTranActivationStrategy(Activator activator,
                                                  PassivationPolicy policy,
                                                  SfFailoverCache failoverCache) //LIDB2018-1
    {
        super(activator, policy, failoverCache); //LIDB2018-1
    }

    @Override
    void atCommit(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atCommit: " + bean, tx);

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        Object lock = sfbean.ivCacheLock;

        synchronized (lock)
        {
            try {
                // A 'removed' bean would have been delisted, but a bean could
                // be in the DESTROYED state due to an exception thrown from
                // SessionSynch.afterCompletion. Need to insure a DESTROYED
                // bean is not left in the EJB Cache.                     d160910
                cache.removeElement(sfbean.ivCacheElement, true); // drop transaction reference

                sfbean.ivCacheKey = null; // d199233
                sfbean.setCurrentTx(null); //PQ99986

                if (!sfbean.isRemoved()) {
                    sfbean.passivate();
                }
                else
                {
                    reaper.remove(bean.getId());
                }
            } catch (IllegalOperationException ioe) {
                // FFDCFilter.processException(ioe, CLASS_NAME + ".atCommit",
                //                             "79", this); d348420
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Ignoring IllegalOperationException. OK for BMT/BMAS case.");//153743
                cache.unpinElement(sfbean.ivCacheElement);
                sfbean.setCurrentTx(null); // d258770
            } catch (RemoteException ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".atCommit", "82", this);
                Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                           new Object[] { sfbean, this, ex }); //p111002.3
            } finally
            {
                // The bean is no longer in a transaction, so notify all other
                // threads waiting.                       F743-22462 d648183 d650932
                sfbean.unlock(lock);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit: " + bean, tx);
    }

    @Override
    void atRollback(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atRollback: " + bean, tx);

        // Rollback of a Stateful Session bean needs to be pretty much the
        // same as commit.  If the bean is in a 'normal' state, then the tran
        // is over, so the bean should be passivated and removed from the
        // cache.  Also, there is no way to undo a remove(), so even though
        // the tran is rolling back, the bean still needs to be removed from
        // the EJB Cache, just like in commit. The same is also true if the
        // bean's beforeCompletion throws an exception and the bean is
        // discarded (isRemoved returns true for this as well).
        // Net is - never leave a DESTROYED bean in the EJB Cache.         d160910

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        Object lock = sfbean.ivCacheLock;

        synchronized (lock)
        {
            try {
                // A 'removed' bean would have been delisted, but a bean could
                // be in the DESTROYED state due to an exception thrown from
                // SessionSynch.beforeCompletion. Need to insure a DESTROYED
                // bean is not left in the EJB Cache.                     d160910
                cache.removeElement(sfbean.ivCacheElement, true); // drop transaction reference

                sfbean.ivCacheKey = null; // d199233
                sfbean.setCurrentTx(null); //PQ99986

                if (!sfbean.isRemoved())
                {
                    sfbean.passivate();
                }
                else
                {
                    reaper.remove(bean.getId());
                }
            } catch (IllegalOperationException ioe) {
                // FFDCFilter.processException(ioe, CLASS_NAME + ".atRollback",
                //                             "133", this);                 d348420
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Ignoring IllegalOperationException. OK for BMT/BMAS case.");//153743
                cache.unpinElement(sfbean.ivCacheElement);
                sfbean.setCurrentTx(null); // d258770
            } catch (RemoteException ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".atRollback",
                                            "137", this);
                Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                           new Object[] { sfbean, this, ex }); //p111002.3
            } finally
            {
                // The bean is no longer in a transaction, so notify all other
                // threads waiting.                       F743-22462 d648183 d650932
                sfbean.unlock(lock);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atRollback: " + bean, tx);
    }

    private static final TraceComponent tc =
                    Tr.register(StatefulActivateTranActivationStrategy.class,
                                "EJBContainer", "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.activator.StatefulActivateTranActivationStrategy";
}
