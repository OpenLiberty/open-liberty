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

import java.rmi.RemoteException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Base ActivationStrategy for Commit Option A and read-only Entity beans.
 */

public abstract class SingletonActivationStrategy extends ActivationStrategy
{
    //
    // Construction
    //

    /**
     * Construct a new <code>SingletonActivationStrategy</code> instance,
     * tied to the specified <code>Activator</code>.
     */
    SingletonActivationStrategy(Activator activator)
    {
        super(activator);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> complete");
    }

    /**
     * Internal method used by subclasses to activate a bean
     */
    protected BeanO doActivation(EJBThreadData threadData, ContainerTx tx, BeanId beanId,
                                 boolean takeInvocationRef)
                    throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "doActivation",
                     new Object[] { tx, beanId,
                                   new Boolean(takeInvocationRef) });
        }

        BeanO bean = null;
        Throwable exception = null;
        MasterKey key = new MasterKey(beanId);
        boolean activate = false;
        boolean pushedCallbackBeanO = false;

        try {

            synchronized (locks.getLock(key)) {

                if ((bean = (BeanO) cache.find(key)) == null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Bean not in cache");

                    bean = beanId.getHome().createBeanO(threadData, tx, beanId); // d630940
                    pushedCallbackBeanO = true;
                    cache.insert(key, bean);
                    bean.ivCacheKey = key; // d199233
                    activate = true;
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Found bean in cache");

                    // Set the found BeanO as the 'Callback' BeanO, as this is the
                    // BeanO that is becoming the active beanO for the thread.
                    // This will allow methods called by customer code (like Timer
                    // methods) to determine the state of the BeanO that is making
                    // the call.                                              d168509
                    threadData.pushCallbackBeanO(bean); // d630940
                    pushedCallbackBeanO = true;
                }
            }

            boolean pin = false;

            if (activate) {
                bean.activate(beanId, tx); // d114677
            }

            pin = bean.enlist(tx); // d114677

            if (takeInvocationRef && pin) {
                // We need to take an additional reference
                cache.pin(key);
            } else if (!takeInvocationRef && !pin) {
                // Need to drop reference taken by find or insert
                cache.unpin(key);
            }

        } catch (RemoteException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".doActivation", "123", this);
            exception = e;
            throw e;

        } catch (RuntimeException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".doActivation", "129", this);
            exception = e;
            throw e;

        } finally {

            if (exception != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "doActivation: exception raised", exception);
            }

            if (exception != null && bean != null)
            {
                if (pushedCallbackBeanO)
                {
                    threadData.popCallbackBeanO();
                }

                bean.destroy();

                if (activate) {
                    // Synchronize to insure that a temp pin obtained by getBean
                    // doesn't cause the remove to fail due to too many pins. PQ53065
                    synchronized (locks.getLock(key)) {
                        cache.remove(key, true);
                        bean.ivCacheKey = null; // d199233
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "doActivation", bean);
        }

        return bean;
    }

    //
    // Operations
    //

    void atCommit(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atCommit", new Object[] { tx, bean });

        Object key = bean.ivCacheKey; // MasterKey                d199233

        synchronized (locks.getLock(key)) {

            bean = (BeanO) cache.find(key);

            cache.unpin(key); // drop transaction reference

            if (!bean.isRemoved()) {
                cache.unpin(key); // drop reference from find(), above
            } else {
                ((BeanO) cache.remove(key, true)).destroy();
                bean.ivCacheKey = null; // d199233
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit");
    }

    void atEnlist(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atEnlist", new Object[] { tx, bean });

        cache.pin(bean.ivCacheKey); // take transaction reference (MasterKey)

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atEnlist");
    }

    void atRemove(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atRemove", new Object[] { tx, bean });

        final Object key = bean.ivCacheKey; // MasterKey          d199233

        synchronized (locks.getLock(key)) {
            cache.remove(key, true);
            bean.ivCacheKey = null; // d199233
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atRemove");
    }

    BeanO atGet(ContainerTx tx, BeanId id)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atGet", new Object[] { tx, id });

        final MasterKey key = new MasterKey(id);
        BeanO result;

        synchronized (locks.getLock(key)) {
            result = (BeanO) cache.find(key);
            if (result != null) {
                cache.unpin(key);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atGet", result);
        return result;
    }

    //
    // Data
    //

    // Tracing
    private final static TraceComponent tc = Tr.register(SingletonActivationStrategy.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.activator.SingletonActivationStrategy";
}
