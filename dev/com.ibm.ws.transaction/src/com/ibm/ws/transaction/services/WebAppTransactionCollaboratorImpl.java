/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionRolledbackException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.TxCollaboratorConfig;

/**
 * This class implements the IWebAppTransactionCollaborator and its main job is to ensure
 * proper start/end of the LTC on the thread.
 * 
 * LIMITATIONS: In its first version I am not implementing the "shared LTC" logic (see story 50487)
 * 
 * 
 */
public class WebAppTransactionCollaboratorImpl implements IWebAppTransactionCollaborator {

    /** Logger */
    private static final TraceComponent tc = Tr.register(WebAppTransactionCollaboratorImpl.class);

    /** Get the LTC ref */
    private final AtomicServiceReference<LocalTransactionCurrent> ltCurrentRef = new AtomicServiceReference<LocalTransactionCurrent>("ltCurrent");

    /** Get the transaction manager reference */
    private final AtomicServiceReference<EmbeddableWebSphereTransactionManager> tranMgrRef = new AtomicServiceReference<EmbeddableWebSphereTransactionManager>("tranMgr");;

    /** Get the UserTransaction reference */
    private final AtomicServiceReference<EmbeddableWebSphereUserTransaction> userTranRef = new AtomicServiceReference<EmbeddableWebSphereUserTransaction>("userTran");;

    /** Get the UOWScopeCallback reference */
    private final AtomicServiceReference<LTCUOWCallbackService> uowCallbackRef = new AtomicServiceReference<LTCUOWCallbackService>("uowCallback");

    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating WebAppTransactionCollaborator Service!");
        }
        ltCurrentRef.activate(ctx);
        tranMgrRef.activate(ctx);
        userTranRef.activate(ctx);
        uowCallbackRef.activate(ctx);

        final EmbeddableWebSphereUserTransaction userTran = getUserTran();
        final LTCUOWCallbackService ltcCallback = getUowCallback();

        if (userTran == null || ltcCallback == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to register a LTC callback.  userTran:" + userTran + " | ltcCallback:" + ltcCallback);
            }
            return;
        }

        //Register a callback so that we get completed when a UserTransaction is started
        userTran.registerCallback(ltcCallback);
    }

    /*
     * Invoked by SCR to deactivate this service
     */
    protected void deactivate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating WebAppTransactionCollaborator Service!");
        }

        ltCurrentRef.deactivate(ctx);
        tranMgrRef.deactivate(ctx);
        userTranRef.deactivate(ctx);
        uowCallbackRef.deactivate(ctx);

        final EmbeddableWebSphereUserTransaction userTran = getUserTran();
        final LTCUOWCallbackService ltcCallback = getUowCallback();

        if (userTran == null || ltcCallback == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to unregister a LTC callback.  userTran:" + userTran + " | ltcCallback:" + ltcCallback);
            }
            return;
        }

        //Unregister the LTC callback, since this service is being deactivated
        userTran.unregisterCallback(ltcCallback);
    }

    /*
     * Invoked by SCR to inject dependency
     */
    protected void setLtCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        this.ltCurrentRef.setReference(ref);
    }

    /*
     * Invoked by SCR to remove dependency
     */
    protected void unsetLtCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        this.ltCurrentRef.unsetReference(ref);
    }

    private LocalTransactionCurrent getLtCurrent() {
        return this.ltCurrentRef.getService();
    }

    /*
     * Invoked by SCR to inject dependency
     */
    protected void setUowCallback(ServiceReference<LTCUOWCallbackService> ref) {
        this.uowCallbackRef.setReference(ref);
    }

    /*
     * Invoked by SCR to remove dependency
     */
    protected void unsetUowCallback(ServiceReference<LTCUOWCallbackService> ref) {
        this.uowCallbackRef.unsetReference(ref);
    }

    private LTCUOWCallbackService getUowCallback() {
        return this.uowCallbackRef.getService();
    }

    /*
     * Invoked by SCR to inject dependency
     */
    protected void setTranMgr(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        this.tranMgrRef.setReference(ref);
    }

    protected void setUserTran(ServiceReference<EmbeddableWebSphereUserTransaction> ref) {
        this.userTranRef.setReference(ref);
    }

    /*
     * Invoked by SCR to remove dependency
     */
    protected void unsetTranMgr(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        this.tranMgrRef.unsetReference(ref);
    }

    protected void unsetUserTran(ServiceReference<EmbeddableWebSphereUserTransaction> ref) {
        this.userTranRef.unsetReference(ref);
    }

    private EmbeddableWebSphereTransactionManager getTranMgr() {
        return this.tranMgrRef.getService();
    }

    private EmbeddableWebSphereUserTransaction getUserTran() {
        return this.userTranRef.getService();
    }

    /**
     * Collaborator preInvoke.
     * 
     * Called before the webapp is invoked, to ensure that a LTC is started
     * in the absence of a global transaction. A global tran may be active
     * on entry if the webapp that caused this dispatch left a global tran
     * active. The WebAppRequestDispatcher ensures that any global tran started
     * during a dispatch is rolledback if not completed during the dispatch.
     * If there is a global tran on entry, then we don't start an LTC.
     * If there isn't a global tran on entry, then we do start an LTC after
     * first suspending any LTC started by a previous dispatch.
     * If an LTC is suspended, the LTC is returned by this method and
     * passed back to the caller who will resupply it to postInvoke() who will resume the
     * suspended Tx.
     * 
     * Request is not null for a proper servlet service call. It is null if called for
     * a servlet context change, for an init or destroy servlet.
     * 
     * @param isServlet23 represents if we're dealing with servlet 2.3
     */
    @Override
    public TxCollaboratorConfig preInvoke(final HttpServletRequest request, final boolean isServlet23)
                    throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Calling preInvoke. Request=" + request + " | isServlet23=" + isServlet23);
        }

        //First we check if there's a global transaction
        try {
            final EmbeddableWebSphereTransactionManager tranManager = getTranMgr();
            if (tranManager != null) {
                final Transaction incumbentTx = tranManager.getTransaction();

                if (incumbentTx != null) {
                    TransactionImpl incumbentTxImpl = null;
                    if (incumbentTx instanceof TransactionImpl)
                        incumbentTxImpl = (TransactionImpl) incumbentTx;

                    if (incumbentTxImpl != null && incumbentTxImpl.getTxType() == UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "the tx is NONINTEROP_GLOBAL set it to null");
                        }
                        // The following call should nullify the current tx on the current thread (in this special case where the
                        // TxType is TXTYPE_NONINTEROP_GLOBAL
                        tranManager.suspend();
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Global transaction was present.");
                        }
                        //Yes! There's a global transaction.  Check for timeout
                        checkGlobalTimeout();

                        //Create config return object
                        final TxCollaboratorConfig retConfig = new TxCollaboratorConfig();
                        retConfig.setIncumbentTx(incumbentTx);

                        return retConfig;
                    }
                }
            }
        } catch (SystemException e) {
            Tr.error(tc, "UNEXPECTED_TRAN_ERROR", e.toString());
            ServletException se = new ServletException("Unexpected error during transaction fetching: ", e);
            throw se;
        }

        //Ensure we have a LocalTransactionCurrent to work with
        LocalTransactionCurrent ltCurrent = getLtCurrent();
        if (ltCurrent == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to resolve LocalTransactionCurrent");
            }
            return null;
        }

        //Create config return object     
        TxCollaboratorConfig retConfig = new TxCollaboratorConfig();

        // see if there is a local transaction on the thread
        LocalTransactionCoordinator ltCoord = ltCurrent.getLocalTranCoord();
        if (ltCoord != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Suspending LTC's coord: " + ltCoord);
            }

            //there was a transaction! We need to suspend it and put its coordinator into the config
            //so that we can resume it later
            retConfig.setSuspendTx(ltCurrent.suspend());
        }

        //begin a new local transaction on the thread
        ltCurrent.begin();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Began LTC's coord:" + ltCurrent.getLocalTranCoord());
        }

        return retConfig;
    }

    private void checkGlobalTimeout() throws ServletException {
        try {
            final EmbeddableWebSphereTransactionManager tranManager = getTranMgr();
            if (tranManager != null) {
                tranManager.completeTxTimeout();
            }
        } catch (TransactionRolledbackException e) {
            Tr.error(tc, "GLOBAL_TRAN_ROLLBACK", e.toString());
            ServletException se = new ServletException("Global transaction rolled-back due to timeout", e);
            throw se;
        }
    }

    @Override
    public void postInvoke(HttpServletRequest request, Object txConfig,
                           boolean isServlet23) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Calling postInvoke. Request=" + request + " | isServlet23=" + isServlet23);
        }

        LocalTransactionCurrent ltCurrent = getLtCurrent();
        if (ltCurrent == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to resolve LocalTransactionCurrent");
            }
            return;
        }

        //Get LTC coord
        LocalTransactionCoordinator currentCoord = ltCurrent.getLocalTranCoord();
        if (currentCoord != null) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ending LTC's coord:" + currentCoord);
                    Tr.debug(tc, "Unregistering LTC callback");
                }

                //End the current LTC
                currentCoord.end(LocalTransactionCoordinator.EndModeCommit);

            } catch (IllegalStateException e1) {
                //Absorb cleanup exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "IllegalStateException", e1);
                }
            } catch (InconsistentLocalTranException e1) {
                //Absorb cleanup exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "InconsistentLocalTranException", e1);
                }
            } catch (RolledbackException e1) {
                //Absorb cleanup exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "RolledbackException", e1);
                }
            }

            resumeSuspendedLTC(ltCurrent, txConfig);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There must be a global tran on the thread");
            }
            boolean resumeSuspendedLTC = false;
            // Check to see if the transaction coming into the dispatch is
            // the same as the one
            // coming out. If they are different then the servlet started a
            // new transaction
            // and it needs to be checked for possible rollback
            final EmbeddableWebSphereTransactionManager tranManager = getTranMgr();
            if (tranManager != null) {
                try {
                    final Transaction tx = tranManager.getTransaction();

                    if (tx != null) {
                        Object incumbentTx = null;

                        if (txConfig instanceof TxCollaboratorConfig) {
                            incumbentTx = ((TxCollaboratorConfig) txConfig).getIncumbentTx();
                        }

                        if (!tx.equals(incumbentTx)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Servlet started Tx", tx);
                            }

                            resumeSuspendedLTC = true;
                            // rollback the Transaction
                            if (tranManager.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                                tranManager.rollback();
                            }
                        }
                    }
                } catch (SystemException e) {
                    FFDCFilter.processException(e, this.getClass().getCanonicalName() + "postInvoke", "374");
                }
            }

            if (resumeSuspendedLTC) {
                resumeSuspendedLTC(ltCurrent, txConfig);
            } else {
                // we had a Global Transaction.   Check for timeout
                checkGlobalTimeout();
            }
        }
    }

    /**
     * Resumes the LTC that was suspended during preInvoke, if one was suspended.
     */
    private void resumeSuspendedLTC(LocalTransactionCurrent ltCurrent, Object txConfig) throws ServletException {
        //Check for any suspended LTC that needs to be resumed
        if (txConfig instanceof TxCollaboratorConfig) {
            Object suspended = ((TxCollaboratorConfig) txConfig).getSuspendTx();

            if (suspended instanceof LocalTransactionCoordinator) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Resuming previously suspended LTC coord:" + suspended);
                    Tr.debug(tc, "Registering LTC callback");
                }

                try {
                    ltCurrent.resume(((LocalTransactionCoordinator) suspended));
                } catch (IllegalStateException ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "IllegalStateException", ex);
                    }
                    try {
                        // Clean-up the Tx
                        ltCurrent.cleanup();
                    } catch (InconsistentLocalTranException iltex) {
                        // Absorb any exception from cleanup - it doesn't really
                        // matter if there are inconsistencies in cleanup.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "InconsistentLocalTranException", iltex);
                        }
                    } catch (RolledbackException rbe) {
                        // We need to inform the user that completion
                        // was affected by a call to setRollbackOnly
                        // so rethrow as a ServletException.
                        ServletException se = new ServletException("LocalTransaction rolled-back due to setRollbackOnly", rbe);
                        throw se;
                    }

                }
            }
        }
    }
}
