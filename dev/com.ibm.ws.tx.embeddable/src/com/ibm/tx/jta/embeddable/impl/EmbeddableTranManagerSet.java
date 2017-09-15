package com.ibm.tx.jta.embeddable.impl;
/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.ltc.embeddable.impl.LTCCallbacks;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackManager;
import com.ibm.wsspi.tx.UOWEventListener;

public class EmbeddableTranManagerSet extends com.ibm.tx.jta.impl.TranManagerSet implements EmbeddableWebSphereTransactionManager
{
    private static final TraceComponent tc = Tr.register(EmbeddableTranManagerSet.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected static UOWScopeCallbackManager _callbackManager;

    // This class is a singleton so the default
    // constructor is declared private to
    // prevent instantiation.
    protected EmbeddableTranManagerSet() {}   

    public static synchronized EmbeddableWebSphereTransactionManager instance()
    {
        if (_instance == null)
        {
            _instance = new EmbeddableTranManagerSet();
            _callbackManager = new UOWScopeCallbackManager();
            _thread = new ThreadLocal<EmbeddableTranManagerImpl>()
            {
                public EmbeddableTranManagerImpl initialValue()
                {
                    return new EmbeddableTranManagerImpl();
                }
            };      
        }
        
        return (EmbeddableWebSphereTransactionManager)_instance;
    }

    @Override
    protected EmbeddableTranManagerImpl self()
    {
        return (EmbeddableTranManagerImpl)_thread.get();
    }

    /**
     *  Complete processing of passive transaction timeout.
     */
    public void completeTxTimeout() throws TransactionRolledbackException
    {
       self().completeTxTimeout();
    }

    /**
     * Start an inactivity timer and call alarm method of parameter when
     * timeout expires.
     * Returns false if transaction is not active.
     *
     * @param t    Transaction associated with this timer.
     * @param iat  callback object to be notified when timer expires.
     * @return     boolean to indicate whether timer started
     */
    public boolean startInactivityTimer (Transaction t, InactivityTimer iat)
    {
        if (t != null)
            return ((EmbeddableTransactionImpl) t).startInactivityTimer(iat);

        return false;
    }

    /**
     * Stop inactivity timer associated with transaction.
     *
     * @param t    Transaction associated with this timer.
     */
    public void stopInactivityTimer  (Transaction t)
    {
       if (t != null)
          ((EmbeddableTransactionImpl) t).stopInactivityTimer();
    }

    public void registerCallback(UOWScopeCallback callback)
    {
        _callbackManager.addCallback(callback);
    }


    /**
     * Variation on enlist which accepts UOWCoordinator for improved performance.
     *
     * @param coord UOWCoordinator previously obtained from UOWCurrent.
     * @param xaRes The XAResource object representing the resource to enlist.
     * @param recoveryId The identifier returned from a call to registerResourceInfo
     *                   associating the appropriate xaResFactoryClassName/xaResInfo
     *                   necessary for produce a XAResource object.
     * @param branchCoupling Set to xa_start flag value for the XAResource as required
     *                   by the branch coupling setting, or 0 for unset/default.
     *
     * @return <i>true</i> if the resource was enlisted successfully;
     *            otherwise <i>false</i>.
     */
    /* Called by XATransactionWrapper and CScopeRootImpl */
    public boolean enlist(UOWCoordinator coord, XAResource xaRes, int recoveryIndex, int branchCoupling)
    throws RollbackException, IllegalStateException, SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled()) Tr.entry(tc, "enlist", new Object[] {xaRes, recoveryIndex, branchCoupling});

        boolean result = false;

        if (coord instanceof TransactionImpl)
        {
            try
            {
                result = ((TransactionImpl)coord).enlistResource(xaRes, recoveryIndex, branchCoupling);
            }
            finally
            {
                if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "enlist", result);
            }
        }
        else
        {
            final SystemException se = new SystemException("Invalid UOWCoordinator");
            FFDCFilter.processException(se, "com.ibm.tx.jta.impl.EmbeddableTranManagerSet.enlist", "154", this);
            if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "enlist", se);
            throw se;
        }
        
        return result;
    }


    /**
     * Method to enlist resource with JTA tran via UOWCoordinator
     * for improved performance.
     *
     * @param coord UOWCoordinator previously obtained from UOWCurrent.
     * @param xaRes The XAResource object representing the resource to enlist.
     *
     * @return <i>true</i> if the resource was enlisted successfully;
     *            otherwise <i>false</i>.
     *
     * We should consider deprecating this now that UOWCoordinator is a javax...Transaction
     */
    /* Called by ConnectO and LocalTransactionWrapper */
    public boolean enlistOnePhase(UOWCoordinator coord, OnePhaseXAResource opXaRes)
    throws RollbackException, IllegalStateException, SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled()) Tr.entry(tc, "enlistOnePhase", new Object[] {coord, opXaRes});

        boolean result = false;

        if (coord instanceof TransactionImpl)
        {
            try
            {
                result = ((TransactionImpl)coord).enlistResource(opXaRes);
            }
            finally
            {
                if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "enlistOnePhase", result);
            }
        }
        else
        {
            final SystemException se = new SystemException("Invalid UOWCoordinator");
            FFDCFilter.processException(se, "com.ibm.ws.Transaction.JTA.TranManagerSet.enlistOnePhase", "405", this);
            if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "enlistOnePhase", se);
            throw se;
        }
        
        return result;
    }


    /**
     * Method to register synchronization object with JTA tran via UOWCoordinator
     * for improved performance.
     *
     * @param coord UOWCoordinator previously obtained from UOWCurrent.
     * @param sync  Synchronization object to be registered.
     *
     * We should deprecate this now that UOWCoordinator is a javax...Transaction
     */
    public void registerSynchronization(UOWCoordinator coord,
                                        Synchronization sync)
    throws RollbackException, IllegalStateException, SystemException
    {
        registerSynchronization(coord, sync, EmbeddableWebSphereTransactionManager.SYNC_TIER_NORMAL);
    }
    
    public void registerSynchronization(UOWCoordinator coord, Synchronization sync, int tier)
    throws RollbackException, IllegalStateException, SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled()) Tr.entry(tc, "registerSynchronization", new Object[] {coord, sync, tier});

        if (coord instanceof TransactionImpl)
        {
        	self().invokeEventListener(coord, UOWEventListener.REGISTER_SYNC, sync);
            ((TransactionImpl)coord).registerSynchronization(sync, tier);
        }
        else
        {
            final SystemException se = new SystemException("Invalid UOWCoordinator");
            FFDCFilter.processException(se, "com.ibm.tx.jta.impl.EmbeddableTranManagerSet.registerSynchronization", "148", this);
            if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "registerSynchronization", se);
            throw se;
        }
        
        if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "registerSynchronization");
    }

    @Override
    public void registerLTCCallback(UOWCallback callback)
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled()) Tr.entry(tc, "registerLTCCallback", callback);

        //
        // Delegate this registration down to the singleton
        // callback handling object.
        //
        LTCCallbacks.instance().registerCallback(callback);

        if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "registerLTCCallback");
    }

}
