package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.HashSet;
import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.wsspi.tx.UOWEventListener;

public class TranManagerImpl
{
    private static final TraceComponent tc = Tr.register(TranManagerImpl.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    private static final int DEFAULT_TX_TIMEOUT = 0; // d1528

    private static Set<UOWEventListener> _UOWEventListeners;

    protected int txTimeout;

    protected TransactionImpl tx;

    /**
     * Transaction Manager Constructor.
     */
    public TranManagerImpl()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TranManagerImpl");

        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();

        if (cp == null)
        {
            throw new IllegalStateException();
        }

        txTimeout = cp.getTotalTransactionLifetimeTimeout();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TranManagerImpl", this);
    }

    public void begin(int timeout) throws NotSupportedException, SystemException /* @512190C */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "begin", "(SPI)");

        if (tx == null) {
            tx = createNewTransaction(timeout);
        } else {
            Tr.error(tc, "WTRN0017_UNABLE_TO_BEGIN_NESTED_TRANSACTION");
            final NotSupportedException nse = new NotSupportedException("Nested transactions are not supported.");
            FFDCFilter.processException(nse, "com.ibm.tx.jta.impl.TranManagerImpl.begin", "135", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "begin", new Object[] {"(SPI)", nse});
            throw nse;
        }

        invokeEventListener(tx, UOWEventListener.POST_BEGIN, null);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "begin", "(SPI)");
    }

    //-------------------------------------------------------------------------
    //
    // javax.transaction.TransactionManager method implementations
    //
    //-------------------------------------------------------------------------

    public void begin() throws NotSupportedException, SystemException { /* @512190C */
        begin(txTimeout);
    }

    protected TransactionImpl createNewTransaction(int timeout)
                    throws SystemException /* @512190A */
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "createNewTransaction", timeout);

        final TransactionImpl tx = new TransactionImpl(timeout);
        tx.setMostRecentThread(Thread.currentThread());

        return tx;
    }

    /**
     * Used by UserTransaction to create a transaction
     */
    public void beginUserTran()
                    throws NotSupportedException, SystemException /* @512190C */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "beginUserTran", this);

        if (tx == null)
        {
            tx = createNewTransaction(txTimeout);
        }
        else
        {
            Tr.error(tc, "WTRN0017_UNABLE_TO_BEGIN_NESTED_TRANSACTION");
            final NotSupportedException nse = new NotSupportedException("Nested transactions are not supported.");
            FFDCFilter.processException(nse, "com.ibm.tx.jta.impl.TranManagerImpl.beginUserTran", "159", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "beginUserTran", nse);
            throw nse;
        }
        invokeEventListener(tx, UOWEventListener.POST_BEGIN, null);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "beginUserTran", tx);
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                    SecurityException, IllegalStateException, SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", "(SPI)");

        if (tx == null)
        {
            final String msg = "No transaction associated with this thread";
            final IllegalStateException ise = new IllegalStateException(msg);
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.TranManagerImpl.commit", "167", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", new Object[] {"(SPI)", ise});
            throw ise;
        }

        // Tran is gonna get taken off the thread as part of commit processing so save a reference here
        final UOWCoordinator completingTx = tx;

        try
        {
            tx.commit();
        } finally
        {
            invokeEventListener(completingTx, UOWEventListener.POST_END, null);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "(SPI)");
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", "(SPI)");

        if (tx == null)
        {
            final String msg = "No transaction associated with this thread";
            final IllegalStateException ise = new IllegalStateException(msg);
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.TranManagerImpl.rollback", "193", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", new Object[] {"(SPI)", ise});
            throw ise;
        }

        // Tran is gonna get taken off the thread as part of commit processing so save a reference here
        final UOWCoordinator completingTx = tx;

        try
        {
            tx.rollback();
        } finally
        {
            invokeEventListener(completingTx, UOWEventListener.POST_END, null);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "(SPI)");
        }
    }

    public Transaction suspend()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "suspend", new Object[] {"(SPI)", this});

        TransactionImpl suspended = null;

        if (tx != null)
        {
            if (tx.getTxType() != UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
            {
                suspended = tx;
                tx = null;
                invokeEventListener(suspended, UOWEventListener.SUSPEND, null);
            }
            else
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "the tx is NONINTEROP_GLOBAL it should not be suspended");
                tx = null;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "suspend", new Object[] {"(SPI)", suspended});
        return suspended;
    }

    public void resume(Transaction t) throws InvalidTransactionException, IllegalStateException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resume", new Object[] {"(SPI)", t});

        if (tx != null)
        {
            final String msg = "Thread is already associated with a transaction";
            final IllegalStateException ise = new IllegalStateException(msg);
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.TranManagerImpl.resume", "249", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "resume", new Object[] {"(SPI)", ise});
            throw ise;
        }

        try
        {
            // If the transaction to be resumed
            // is null then the call is a no-op,
            // otherwise we must check that it's
            // in a valid state to be resumed.
            if (t != null)
            {
                switch (((TransactionImpl) t).getTransactionState().getState())
                {
                // These states imply the transaction is invalid (non-existant or finished)
                    case TransactionState.STATE_COMMITTED:
                    case TransactionState.STATE_ROLLED_BACK:
                    case TransactionState.STATE_NONE:
                        // Treat the transaction as inactive - the catch block will convert this into
                        // InvalidTransactionException which is the required exception for this case
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Attempting to resume an inactive transaction");
                        throw new IllegalStateException(); // Generate an FFDC
                    default:
                        // Any other state is valid for resume
                        tx = (TransactionImpl) t;
                }

                tx.setMostRecentThread(Thread.currentThread());
            }
        } catch (Throwable e)
        {
            // Either the supplied transaction is not a TransactionImpl or it is an invalid state for resume
            FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TranManagerImpl.resume", "201", this);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception caught checking transaction state", e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "resume", "(SPI)");

            throw new InvalidTransactionException();
        }

        // RTC 171838. In the special case where the tx is TXTYPE_NONINTEROP_GLOBAL, we do not need to alert event listeners that a tran
        // has been resumed.
        if (tx != null && tx.getTxType() != UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
        {
            invokeEventListener(tx, UOWEventListener.RESUME, null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "resume", "(SPI)");
    }

    public void setRollbackOnly() throws IllegalStateException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRollbackOnly", "(SPI)");

        if (tx == null)
        {
            final String msg = "No transaction associated with this thread";
            final IllegalStateException ise = new IllegalStateException(msg);
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.TranManagerImpl.setRollbackOnly", "303", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setRollbackOnly", new Object[] {"(SPI)", ise});
            throw ise;
        }

        try
        {
            tx.setRollbackOnly();
        } finally
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setRollbackOnly", "(SPI)");
        }
    }

    public void setTransactionTimeout(int timeout) throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setTransactionTimeout", new Object[] {"(SPI)", timeout});

        if (timeout > 0)
        {
            if (timeout == Integer.MAX_VALUE) // 68 years
            {
                txTimeout = 0; // disable timeout
            }
            else
            {
                txTimeout = timeout;
            }
        }
        else if (timeout == 0)
        {
            txTimeout = ConfigurationProviderManager.getConfigurationProvider().getTotalTransactionLifetimeTimeout();

            if (txTimeout == 0)
            {
                txTimeout = DEFAULT_TX_TIMEOUT;
            }
        }
        else
        {
            final SystemException se = new SystemException("Transaction timeout value must be >= 0");
            FFDCFilter.processException(se, "com.ibm.tx.jta.impl.TranManagerImpl.setTransactionTimeout", "206", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setTransactionTimeout", new Object[] {"(SPI)", se});
            throw se;
        }
        // Note: TransactionImpl will later check against maximumTransactionTimeout - its is done there as it is common for CMT/BMT

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setTransactionTimeout", new Object[] {"(SPI)", txTimeout});
    }

    public int getStatus()
    {
        int status = Status.STATUS_NO_TRANSACTION;

        if (tx != null)
        {
            status = tx.getStatus();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getStatus (SPI)", Util.printStatus(status));

        return status;
    }

    public Transaction getTransaction()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTransaction (SPI)", new Object[] { this, tx, new Exception("SPI Stack Trace") });

        return tx;
    }

    public TransactionImpl getTransactionImpl()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionImpl", new Object[] { this, tx });

        return tx;
    }

    // Enlists a 2PC resource with the transaction,
    // that this class is managing.
    public boolean enlist(XAResource xaRes, int recoveryId)
                    throws RollbackException, IllegalStateException, SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "enlist", new Object[] { xaRes, recoveryId });

        if (tx == null)
        {
            final String msg = "No transaction associated with this thread";
            final IllegalStateException ise = new IllegalStateException(msg);
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.TranManagerImpl.enlist", "470", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlist", ise);
            throw ise;
        }

        boolean ret = false;
        try
        {
            ret = tx.enlistResource(xaRes, recoveryId);
        } finally
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlist", ret);
        }
        return ret;
    }

    public boolean delist(XAResource xaRes, int flag)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "delist", new Object[] { xaRes, Util.printFlag(flag) });

        if (tx == null)
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "The transaction was not found.");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "delist", Boolean.FALSE);
            return false;
        }

        boolean ret = false;
        try
        {
            ret = tx.delistResource(xaRes, flag);
        } catch (Exception e)
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "delist exception absorbed", e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "delist", ret);

        return ret;
    }

    /**
     * Return the current UOW coordinator (either local or global)
     */
    public UOWCoordinator getUOWCoord()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getUOWCoord", this);

        UOWCoordinator uow = tx;
        if (uow == null)
        {
            // can optimise this by caching the threadlocal 'LocalTranCurrentSet.instance().self'
            uow = (UOWCoordinator) LocalTranCurrentSet.instance().getLocalTranCoord();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getUOWCoord", uow);
        return uow;
    }

    public synchronized void setUOWEventListener(UOWEventListener el)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setUOWEventListener", el);

        if (_UOWEventListeners == null)
        {
            _UOWEventListeners = new HashSet<UOWEventListener>();
        }

        _UOWEventListeners.add(el);
    }

    public synchronized void unsetUOWEventListener(UOWEventListener el)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetUOWEventListener", el);

        if (_UOWEventListeners != null)
        {
            _UOWEventListeners.remove(el);
        }
    }

    public synchronized void invokeEventListener(UOWCoordinator uowc, int event, Object data)
    {
        if (_UOWEventListeners != null)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "invokeEventListener", new Object[] { uowc, event, data });

            for (UOWEventListener el : _UOWEventListeners)
            {
                el.UOWEvent(uowc, event, data);
            }
        }
    }
}
