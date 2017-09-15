/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class StatefulActivateOnceActivationStrategy
                extends StatefulSessionActivationStrategy
{
    public StatefulActivateOnceActivationStrategy(Activator activator,
                                                  PassivationPolicy policy)
    {
        super(activator, policy);
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
            // Drop transaction pin, and remove the bean if needed.
            if (!sfbean.isRemoved()) { // d173022.12
                cache.unpinElement(sfbean.ivCacheElement);
            } else {
                cache.removeElement(sfbean.ivCacheElement, true);
                sfbean.destroy();
                sfbean.ivCacheKey = null; // d199233
                reaper.remove(sfbean.getId());
            }
            sfbean.setCurrentTx(null); //PQ99986

            // The bean is no longer in a transaction, so notify all other
            // threads waiting.                          F743-22462 d648183 d650932
            sfbean.unlock(lock);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit: " + bean);
    }

    @Override
    void atRollback(ContainerTx tx, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "atRollback: " + bean, tx);

        // Rollback of a Stateful Session bean needs to be pretty much the
        // same as commit.  There is no way to undo a remove(), so even though
        // the tran is rolling back, the bean still needs to be removed from
        // the EJB Cache, just like in commit. The same is also true if the
        // bean's beforeCompletion throws an exception and the bean is
        // discarded (isRemoved returns true for this as well).
        // Net is - never leave a DESTROYED bean in the EJB Cache.         d160910

        StatefulBeanO sfbean = (StatefulBeanO) bean;
        Object lock = sfbean.ivCacheLock;

        synchronized (lock)
        {
            // Drop transaction pin, and remove the bean if needed.
            if (!sfbean.isRemoved()) { // d173022.12
                cache.unpinElement(sfbean.ivCacheElement);
            } else {
                cache.removeElement(sfbean.ivCacheElement, true);
                sfbean.destroy();
                sfbean.ivCacheKey = null; // d199233
                reaper.remove(sfbean.getId());
            }
            sfbean.setCurrentTx(null); //PQ99986

            // The bean is no longer in a transaction, so notify all other
            // threads waiting.                          F743-22462 d648183 d650932
            sfbean.unlock(lock);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "atRollback: " + bean);
    }

    //d121558
    private static final TraceComponent tc =
                    Tr.register(StatefulActivateOnceActivationStrategy.class,
                                "EJBContainer", "com.ibm.ejs.container.container");
}
