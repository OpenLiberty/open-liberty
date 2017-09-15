/*******************************************************************************
 * Copyright (c) 1998, 2010 IBM Corporation and others.
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
import javax.ejb.DuplicateKeyException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerAS;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.ejs.container.util.locking.LockTable;

import com.ibm.websphere.csi.EJBCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *
 */
public abstract class ActivationStrategy
{
    //
    // Construction
    //

    /**
     * Construct a new <code>ActivationStrategy</code> instance, tied to
     * the specified <code>Activator</code>.
     */
    ActivationStrategy(Activator activator)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", activator);
        cache = activator.beanCache;
        locks = activator.activationLocks;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    //
    // Operations
    //

    /**
     * Activates the specified bean and sets the bean as the callback bean in
     * <tt>threadData</tt>.
     */
    abstract BeanO atActivate(EJBThreadData threadData, ContainerTx tx, BeanId beanId) // d114677, d630940
    throws RemoteException;

    abstract void atPostInvoke(ContainerTx tx, BeanO beanO);

    abstract BeanO atCreate(ContainerTx tx, BeanO bean)
                    throws DuplicateKeyException, RemoteException;

    boolean atLock(ContainerTx tx, BeanId id)
                    throws RemoteException
    {
        // default is to do nothing
        return false;
    }

    abstract void atCommit(ContainerTx tx, BeanO beanO);

    void atUnitOfWorkEnd(ContainerAS as, BeanO beanO)
    {
        // default: do nothing
        return;
    }

    abstract void atRollback(ContainerTx tx, BeanO beanO);

    abstract void atEnlist(ContainerTx tx, BeanO beanO);

    abstract void atRemove(ContainerTx tx, BeanO beanO);

    void atTimeout(BeanId id)
                    throws RemoteException
    {
        // do nothing
        return;
    }

    void atPassivate(BeanId id) throws RemoteException {
        return;
    }

    abstract BeanO atGet(ContainerTx tx, BeanId id);

    void atDiscard(BeanO bean) throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "atDiscard", bean);
        try {

            bean.passivate();
            bean.destroy();

        } catch (RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".atDiscard", "106", this);
            Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                       new Object[] { bean, this, e }); //p111002.3
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "atDiscard");
    }

    void atUninstall(BeanId id, BeanO bean)
    {
        // Default implementation, do nothing
    }

    //
    // Data
    //

    final EJBCache cache;
    final LockTable locks;

    // Tracing
    private static final TraceComponent tc = Tr.register(ActivationStrategy.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.activator.ActivationStrategy";
}
