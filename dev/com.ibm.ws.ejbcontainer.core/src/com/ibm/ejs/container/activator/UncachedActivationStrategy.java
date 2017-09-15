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
 * UncachedActivationStrategy provides the caching behavior for Stateless
 * Session Beans, including Homes.
 */

public final class UncachedActivationStrategy extends ActivationStrategy
{

    //
    // Construction
    //

    /**
     * Construct a new <code>UncachedActivationStrategy</code> instance, tied
     * to the specified <code>Activator</code>.
     */

    public UncachedActivationStrategy(Activator activator)
    {
        super(activator);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> complete");
    }

    //
    // Operations
    //

    BeanO atActivate(EJBThreadData threadData, ContainerTx tx, BeanId beanId) // d630940
    throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atActivate (" + beanId + ")", tx);

        BeanO bean = null;
        boolean popCallbackBeanO = false;

        try
        {
            bean = beanId.getHome().createBeanO(threadData, tx, beanId); // d630940
            popCallbackBeanO = true;
            bean.activate(beanId, tx); // d114677
            bean.enlist(tx); // d114677
            popCallbackBeanO = false;
        } catch (RemoteException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".atActivate", "78", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "bean activation failed", e);

            if (bean != null) {
                bean.destroy();
            }
            throw e;
        } finally
        {
            // The callback bean is not pushed for home beans, which can be
            // identified by a null home.
            if (popCallbackBeanO && bean.getHome() != null)
            {
                threadData.popCallbackBeanO();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atActivate", bean);
        return bean;
    }

    void atPostInvoke(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atPostInvoke", new Object[] { tx, bean });

        // Nothing to do

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atPostInvoke");
    }

    BeanO atCreate(ContainerTx tx, BeanO bean)
                    throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atCreate", new Object[] { tx, bean });
        bean.enlist(tx); // d114677
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atCreate");
        return null;
    }

    void atCommit(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atCommit", new Object[] { tx, bean });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atCommit");
    }

    void atRollback(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atRollback", new Object[] { tx, bean });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atRollback");
    }

    void atEnlist(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atEnlist", new Object[] { tx, bean });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atEnlist");
    }

    void atRemove(ContainerTx tx, BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atRemove", new Object[] { tx, bean });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atRemove");
    }

    void atDiscard(BeanO bean)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atDiscard", bean);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atDiscard");
    }

    BeanO atGet(ContainerTx tx, BeanId id)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atGet", new Object[] { tx, id });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atGet", null);
        return null;
    }

    //
    // Data
    //

    // Tracing
    //d121558
    private static final TraceComponent tc =
                    Tr.register(UncachedActivationStrategy.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.activator.UncachedActivationStrategy";
}
