/*******************************************************************************
 * Copyright (c) 1998, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TETxLifeCycleInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * UserTransactionWrapper is a wrapper for UserTransaction which
 * coordinates bean-managed transactions with TransactionControl
 * and the container. When a TX_BEAN_MANAGED bean calls
 * getUserTransaction on EJBContext, an instance of this wrapper
 * is returned. It performs actions when begin() is called, and
 * connects the bean to the new transaction.
 */

public class UserTransactionWrapper
                implements UserTransaction, java.io.Serializable //92682.1
{
    private static final long serialVersionUID = 8016621367906146400L;

    private static final TraceComponent tc =
                    Tr.register(UserTransactionWrapper.class,
                                "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.UserTransactionWrapper";

    public static final UserTransaction INSTANCE = new UserTransactionWrapper(); // d631349

    //92682.1 make transient
    private transient EJSContainer container;
    private transient UserTransaction userTransactionImpl;
    private transient EmbeddableWebSphereTransactionManager txCurrent; //LIDB1673.2.1.5 //d135218

    public UserTransactionWrapper() // d114291
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
        initialize(); // d638520
    }

    private void readObject(ObjectInputStream in) // d638520
    throws IOException, ClassNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "readObject");
        initialize();
    }

    private void initialize() // d638520
    {
        this.container = EJSContainer.getDefaultContainer(); // d631349
        this.userTransactionImpl = container.userTransactionImpl; // d631349
        this.txCurrent = EmbeddableTransactionManagerFactory.getTransactionManager(); //d165585
    }

    //
    // UserTransaction interface
    //

    public void begin()
                    throws NotSupportedException, SystemException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled)
            Tr.entry(tc, "UserTransactionWrapper.begin");

        // LIDB1181.23.5.1
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            LocalTransactionCurrent ltcCurrent =
                            EmbeddableTransactionManagerFactory.getLocalTransactionCurrent(); //LIDB1673.2.1.5 // 120870.3
            LocalTransactionCoordinator lCoord = ltcCurrent.getLocalTranCoord();
            if (lCoord != null) {
                Tr.event(tc, "Tx Service will complete LTC cntxt: tid=" +
                             Integer.toHexString(lCoord.hashCode()) + "(LTC)");

            }
        }

        EJBThreadData threadData = EJSContainer.getUserTransactionThreadData(); // d704496

        // Start a transaction
        userTransactionImpl.begin();

        // d135218
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()
            && txCurrent != null) {
            Tr.event(tc, "User Code began TX cntxt: " +
                         txCurrent.getTransaction()); //LIDB1673.2.1.5
        }

        try {
            container.processTxContextChange(threadData, false); // d704496
        } catch (java.rmi.RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".begin", "145", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d144064
                Tr.exit(tc, "Exception during begin()", e);
            userTransactionImpl.rollback();
            throw new SystemException(e.toString());
        }

        if (entryEnabled)
            Tr.exit(tc, "UserTransactionWrapper.begin");
    }

    public void commit()
                    throws RollbackException,
                    HeuristicMixedException,
                    HeuristicRollbackException,
                    SecurityException,
                    IllegalStateException,
                    SystemException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled)
            Tr.entry(tc, "UserTransactionWrapper.commit");

        // d135218
        Transaction transaction = null; // d167935
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()
            && txCurrent != null)
        {
            transaction = txCurrent.getTransaction(); // d167935
            Tr.event(tc, "User Code committing TX cntxt: " +
                         transaction); // LIDB1673.2.1.5 d167935
        }

        // get the transaction id before commit, otherwise will be no tx.
        // d165585 Begins
        String idStr = null;

        if (TraceComponent.isAnyTracingEnabled() && // d527372
            TETxLifeCycleInfo.isTraceEnabled()) // d171555
        { // d171555
            if (txCurrent != null && transaction != null) // d167935
            {
                idStr = transaction.toString(); // d167935
            }
            else {
                idStr = "NoTxCurrent";
            }
        }
        // d165585 Ends

        EJBThreadData threadData = EJSContainer.getUserTransactionThreadData(); // d704496

        try {
            userTransactionImpl.commit();

            // d165585 Begins
            if (TraceComponent.isAnyTracingEnabled() && // d527372
                TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
            { // PQ74774
                int idx;
                idStr = (idStr != null)
                                ? (((idx = idStr.indexOf("(")) != -1)
                                                ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                : (((idx = idStr.indexOf("tid=")) != -1)
                                                                ? idStr.substring(idx + 4)
                                                                : idStr))
                                : "NoTx";
                TETxLifeCycleInfo.traceUserTxCommit(idStr, "User Tx Commit");
            } // PQ74774
            // d165585 Ends

            changeToLocalContext(threadData); // d704496

        } catch (javax.transaction.RollbackException rbe)
        {
            // When a RollbackException occurs, it means that the transaction
            // has been rolled back, rather than committed... so the normal
            // post rollback processing should be perfomred.  Most importantly,
            // this means performing the context change, from the global tran
            // context that was present, to the new Local Tran that the Tx
            // Service began automatically.                                 d303100
            FFDCFilter.processException(rbe, CLASS_NAME + ".commit", "285", this);
            if (TraceComponent.isAnyTracingEnabled()) // d527372
            {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "Exception during commit()", rbe);

                if (TETxLifeCycleInfo.isTraceEnabled())
                {
                    int idx;
                    idStr = (idStr != null)
                                    ? (((idx = idStr.indexOf("(")) != -1)
                                                    ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                    : (((idx = idStr.indexOf("tid=")) != -1)
                                                                    ? idStr.substring(idx + 4)
                                                                    : idStr))
                                    : "NoTx";
                    TETxLifeCycleInfo.traceUserTxCommit(idStr, "User Tx Commit Failed");
                }
            }

            try
            {
                changeToLocalContext(threadData); // d704496
            } catch (Throwable ex)
            {
                FFDCFilter.processException(rbe, CLASS_NAME + ".commit", "312", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "Exception during changeToLocalContext()", ex);
            }

            throw rbe;
        } catch (java.rmi.RemoteException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".commit", "197", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d144064
                Tr.exit(tc, "Exception during commit()", e);
            throw new SystemException(e.toString());
        }

        if (entryEnabled)
            Tr.exit(tc, "UserTransactionWrapper.commit");
    }

    public void rollback()
                    throws IllegalStateException,
                    SecurityException,
                    SystemException
    {
        String idStr = null;

        if (TraceComponent.isAnyTracingEnabled())
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "UserTransactionWrapper.rollback");

            // d135218
            Transaction transaction =
                            (txCurrent != null) ? txCurrent.getTransaction() : null; // d167935
            if (tc.isEventEnabled())
            {
                Tr.event(tc, "User Code rolling back TX cntxt: " +
                             transaction); // LIDB1673.2.1.5 d167935
            }

            // get the transaction id before rollback, otherwise will be no tx.
            // d165585 Begins
            if (TETxLifeCycleInfo.isTraceEnabled()) // d171555
            { // d171555
                if (txCurrent != null && transaction != null) // d167935
                {
                    idStr = transaction.toString(); // d167935
                }
                else {
                    idStr = "NoTxCurrent";
                }
            }
            // d165585 Ends
        }

        EJBThreadData threadData = EJSContainer.getUserTransactionThreadData(); // d704496

        userTransactionImpl.rollback();

        try
        {
            // d165585 Begins
            if (TraceComponent.isAnyTracingEnabled() && // d527372
                TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
            { // PQ74774
                int idx;
                idStr = (idStr != null)
                                ? (((idx = idStr.indexOf("(")) != -1)
                                                ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                : (((idx = idStr.indexOf("tid=")) != -1)
                                                                ? idStr.substring(idx + 4)
                                                                : idStr))
                                : "NoTx";
                TETxLifeCycleInfo.traceUserTxCommit(idStr, "User Tx Rollback");
            } // PQ74774
            // d165585 Ends

            changeToLocalContext(threadData); // d704496

        } catch (java.rmi.RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".rollback", "237", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d144064
                Tr.exit(tc, "Exception during rollback()", e);
            throw new SystemException(e.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "UserTransactionWrapper.rollback");
    }

    public void setRollbackOnly()
                    throws IllegalStateException, SystemException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled)
            Tr.entry(tc, "UserTransactionWrapper.setRollbackOnly");

        userTransactionImpl.setRollbackOnly();

        if (entryEnabled)
            Tr.exit(tc, "UserTransactionWrapper.setRollbackOnly");
    }

    public int getStatus()
                    throws SystemException
    {
        return userTransactionImpl.getStatus();
    }

    public void setTransactionTimeout(int seconds)
                    throws SystemException
    {
        userTransactionImpl.setTransactionTimeout(seconds);
        // d165585 Begins
        if (TraceComponent.isAnyTracingEnabled() && // d527372
            TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
        { // PQ74774
            String idStr = "NoTxCurrent";
            if (txCurrent != null)
            {
                Transaction transaction = txCurrent.getTransaction(); // d167935
                if (transaction != null) // d167935
                { // d167935
                    idStr = transaction.toString(); // d167935
                } // d167935
                int idx;
                idStr = (idStr != null)
                                ? (((idx = idStr.indexOf("(")) != -1)
                                                ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                : (((idx = idStr.indexOf("tid=")) != -1)
                                                                ? idStr.substring(idx + 4) //, idStr.indexOf( ")" ) )
                                                                : idStr))
                                : "NoTx";
            }
            TETxLifeCycleInfo.traceUserTxSetTimeout(idStr, "User Tx Set Timeout=" +
                                                           seconds);
        } // PQ74774
        // d165585 Ends
    }

    /**
     * Internal method that performs all of the necessary work to perform when
     * switching from a global tran context that has just been committed or
     * rolled back to the new local tran context that the Tx Service has
     * begun automatically.
     **/
    // d303100
    private void changeToLocalContext(EJBThreadData threadData) // d704496
    throws java.rmi.RemoteException
    {
        // LIDB1181.23.5.1
        // For EJB 2.0 beans, ending global tx causes Tx Service to
        // automatically begin a local tx.  Create new ContainerTx.
        // For EJB 1.1 beans, we transition to no tx context here.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            LocalTransactionCurrent ltcCurrent =
                            EmbeddableTransactionManagerFactory.getLocalTransactionCurrent(); //LIDB1673.2.1.5 // 120870.3

            LocalTransactionCoordinator lCoord = ltcCurrent.getLocalTranCoord();
            if (lCoord != null) {
                Tr.event(tc, "Tx Service began LTC cntxt: tid=" +
                             Integer.toHexString(lCoord.hashCode()) + "(LTC)");
            }
        }

        container.processTxContextChange(threadData, true); // d704496
    }

} // UserTransactionWrapper
