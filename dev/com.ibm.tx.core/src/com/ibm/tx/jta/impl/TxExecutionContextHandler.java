package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.*;
import com.ibm.tx.TranConstants;
import com.ibm.tx.util.ByteArray;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;

/**
 * An implementation of the com.ibm.ws.j2c.work.ExecutionContextHandler interface
 */
public class TxExecutionContextHandler
{
    private static final TraceComponent tc = Tr.register(TxExecutionContextHandler.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static final TxExecutionContextHandler _instance = new TxExecutionContextHandler();

    // Hashtable of Xid->JCATranWrapper
    protected static final HashMap<ByteArray, JCATranWrapper> txnTable = new HashMap<ByteArray, JCATranWrapper>();

    /* (non-Javadoc)
     * Associates an ExecutionContext with the current thread.
     * This is called on the *dispatch thread*.
     * @see com.ibm.ws.j2c.work.ExecutionContextHandler#associate(javax.resource.spi.work.ExecutionContext, java.lang.String)
     */
    public void associate(ExecutionContext ec, String providerId) throws WorkCompletedException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "doAssociate", new Object[] { ec, providerId });

        // Check that a transaction context is present
        if (null == ec)
        {
            final WorkCompletedException wce = new WorkCompletedException("Invalid ExecutionContext", WorkException.TX_RECREATE_FAILED);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { null, null });
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Invalid ExecutionContext");
            throw wce;
        }

        final Xid xid = ec.getXid();
        if (null == xid)
        {
            // Nothing to do
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Null Xid");
            return;
        }

        // If xid is present, it's got to be valid
        if (!TxXATerminator.isValid(xid))
        {
            final WorkCompletedException wce = new WorkCompletedException("Invalid Xid", WorkException.TX_RECREATE_FAILED);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { ec, ec.getTransactionTimeout()});
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Invalid Xid");
            throw wce;
        }

        int status = Status.STATUS_NO_TRANSACTION;

        try
        {
            status = TransactionManagerFactory.getTransactionManager().getStatus();
        }
        catch(SystemException e)
        {
            final WorkCompletedException wce = new WorkCompletedException(WorkException.TX_RECREATE_FAILED, e);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { ec, ec.getTransactionTimeout()});
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", wce);
            throw wce;
        }

        if (status != Status.STATUS_NO_TRANSACTION)
        {
            // There's already a global tx on this thread
            final WorkCompletedException wce = new WorkCompletedException("Already associated", WorkException.TX_RECREATE_FAILED);
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Already associated");
            throw wce;
        }

        if (providerId == null)
        {
            final WorkCompletedException wce = new WorkCompletedException("Null providerId", WorkException.TX_RECREATE_FAILED);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { ec, ec.getTransactionTimeout()});
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Invalid providerId: " + providerId);
            throw wce;
        }

        // Check the txnTable for a previous occurance of this XID
        // If this returns null it means either that the transaction
        // is already associated or that is has already been prepared.
        // d240298 - findTxWrapper will also suspend any LTC and save in the wrapper for resuming later
        final JCATranWrapper txWrapper;
        try
        {
            txWrapper = findTxWrapper((int) ec.getTransactionTimeout(), xid, providerId);
        }
        catch(WorkCompletedException wce)
        {
            // Must be quiescing
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Can't create new tx while quiescing");
            throw wce;
        }

        if (txWrapper == null)
        {
            // Must already have had an association or been prepared
            final WorkCompletedException wce = new WorkCompletedException("Already have an association or already prepared", WorkException.TX_CONCURRENT_WORK_DISALLOWED);
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "Already have an association or already prepared");
            throw wce;
        }

        // Resume the new transaction
        try
        {
        	((TranManagerSet)TransactionManagerFactory.getTransactionManager()).resume(txWrapper.getTransaction());
        }
        catch (InvalidTransactionException e)
        {
            final WorkCompletedException wce = new WorkCompletedException("resume threw InvalidTransactionException", e);
            wce.setErrorCode(WorkException.TX_RECREATE_FAILED);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { ec, ec.getTransactionTimeout()});
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "resume threw InvalidTransactionException");
            throw wce;
        }
        catch (IllegalStateException e)
        {
            final WorkCompletedException wce = new WorkCompletedException("resume threw IllegalStateException", e);
            wce.setErrorCode(WorkException.TX_RECREATE_FAILED);
            Tr.error(tc, "WTRN0091_ASSOCIATE_FAILED", new Object[] { ec, ec.getTransactionTimeout()});
            if (tc.isEntryEnabled()) Tr.exit(tc, "associate", "resume threw IllegalStateException");
            throw wce;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "associate");
    }

    /**
     * Dissociates any ExecutionContext on the current thread
     */
    public void dissociate()
    {
        doDissociate();
    }

    public static TransactionImpl doDissociate()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "doDissociate");

        final TransactionImpl oldTxn = ((TranManagerSet)TransactionManagerFactory.getTransactionManager()).getTransactionImpl();
        if (oldTxn != null)
        {
        	((TranManagerSet)TransactionManagerFactory.getTransactionManager()).suspend();

            final Xid xid = oldTxn.getXid();

            final ByteArray key = new ByteArray(xid.getGlobalTransactionId());

            final JCATranWrapper txWrapper;

            synchronized (txnTable)
            {
                txWrapper = txnTable.get(key);

                if (null != txWrapper)
                {
                    txWrapper.removeAssociation();
                }
                else
                {
                    // No imported transaction to disassociate
                    if (tc.isEntryEnabled()) Tr.exit(tc, "doDissociate", oldTxn);
                    return oldTxn;
                }
            }

            // Now we resume the local transaction
            // if we suspended it in associate
            txWrapper.resume();
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "doDissociate", oldTxn);
        return oldTxn;
    }

    /**
     * Given an Xid, returns the corresponding JCATranWrapper from the table of
     * imported transactions, or null if no entry exists.
     * @param xid
     * @param addAssociation
     * @return
     * @throws XAException
     */
    public static JCATranWrapper getTxWrapper(Xid xid, boolean addAssociation) throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "getTxWrapper", new Object[] { xid, addAssociation});

        final ByteArray key = new ByteArray(xid.getGlobalTransactionId());

        final JCATranWrapper txWrapper;

        synchronized (txnTable)
        {
            txWrapper = txnTable.get(key);

            if (txWrapper != null)
            {
                if (addAssociation)
                {
                    if (!txWrapper.hasAssociation())
                    {
                        txWrapper.addAssociation();
                    }
                    else
                    {
                        // Already associated
                        if (tc.isEntryEnabled()) Tr.exit(tc, "getTxWrapper", "throwing XAER_PROTO");
                        throw new XAException(XAException.XAER_PROTO);
                    }
                }
            }
            else
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "getTxWrapper", "throwing XAER_NOTA");
                throw new XAException(XAException.XAER_NOTA);
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "getTxWrapper", txWrapper);
        return txWrapper;
    }

    /**
     * Retrieve a JCATranWrapper from the table.
     * Insert it first if it wasn't already there.
     * Returns null if association already existed or if transaction has been prepared.
     * @param timeout
     * @param xid
     * @return
     */
    protected JCATranWrapper findTxWrapper(int timeout, Xid xid, String providerId) throws WorkCompletedException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "findTxWrapper", new Object[] { timeout, xid, providerId });

        final JCATranWrapper txWrapper;
        final ByteArray key = new ByteArray(xid.getGlobalTransactionId());

        synchronized (txnTable)
        {
            if (!txnTable.containsKey(key))
            {
                // XID has not been encountered - create a new TransactionImpl and add it to the table
                
                // ......unless we're quiescing
                if(!((TranManagerSet)TransactionManagerFactory.getTransactionManager()).isQuiesced())
                {
                    final JCARecoveryData jcard = (JCARecoveryData) ((TranManagerSet)TransactionManagerFactory.getTransactionManager()).registerJCAProvider(providerId);

                    try
                    {
                        jcard.logRecoveryEntry();
                    }
                    catch(Exception e)
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "findTxWrapper", e);
                        throw new WorkCompletedException(e.getLocalizedMessage(), WorkException.TX_RECREATE_FAILED);
                    }

                    // Create a new wrapper, suspend any transaction, create the new TransactionImpl and mark associated
                    txWrapper = createWrapper(timeout, xid, jcard);
                    
                    txnTable.put(key, txWrapper);
                }
                else
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "findTxWrapper", "quiescing");
                    throw new WorkCompletedException("In quiesce period", WorkException.TX_RECREATE_FAILED);
                }
            }
            else
            {
                // XID has already been imported, retrieve JCATranWrapper from table
                if (tc.isEventEnabled()) Tr.event(tc, "Already encountered", key);

                txWrapper = txnTable.get(key);

                // If we already had an association, return null so
                // caller knows to throw an exception.
                if (!txWrapper.hasAssociation())
                {
                    // If we were already prepared, return null so
                    // caller knows to throw an exception.
                    if (!txWrapper.isPrepared())
                    {
                        txWrapper.addAssociation();
                    }
                    else
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "findTxWrapper", "already prepared");
                        return null;
                    }
                }
                else
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "findTxWrapper", "already associated");
                    return null;
                }

                // d240298 - Suspend any local transaction before we return,
                // save it in the wrapper for resuming later
                txWrapper.suspend();   // @D240298A
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "findTxWrapper", txWrapper);
        return txWrapper;
    }

    // Overridden in WAS
    protected JCATranWrapper createWrapper(int timeout, Xid xid, JCARecoveryData jcard) throws WorkCompletedException /* @512190C*/
    {
        return new JCATranWrapperImpl(timeout, xid, jcard); // @D240298C
    }

    /**
     * To be called by recovery manager
     * @param txn
     */
    public static void addTxn(TransactionImpl txn)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "addTxn", txn);

        final ByteArray key = new ByteArray(txn.getXid().getGlobalTransactionId());

        synchronized (txnTable)
        {
            if (!txnTable.containsKey(key))
            {
                txnTable.put(key, new JCATranWrapperImpl(txn, true, false)); // @LIDB2110C
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "addTxn");
    }

    /**
     * Called in getTransaction
     * @param gtid
     * 
     */
    public static final void removeTxn(Xid xid)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "removeTxn", xid);

        final ByteArray key = new ByteArray(xid.getGlobalTransactionId());

        final JCATranWrapper wrapper;

        synchronized (txnTable)
        {
            wrapper = txnTable.remove(key);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "removeTxn", wrapper);
    }

    /**
     * @param txn
     */
    protected void reAssociate(TransactionImpl txn)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "reAssociate", txn);

        // If it was imported we need to properly reAssociate
        // otherwise we just need to resume
        if (txn.isRAImport())
        {
            // Dummy up an Execution context
            final ExecutionContext ec = new ExecutionContext();
            ec.setXid(txn.getXid());

            try
            {
                associate(ec, txn.getJCARecoveryData().getWrapper().getProviderId());
            }
            catch (WorkCompletedException e)
            {
                // won't get here in this case
                if (tc.isEventEnabled()) Tr.exit(tc, "reAssociate", e);
            }
        }
        else
        {
            try
            {
            	((TranManagerSet)TransactionManagerFactory.getTransactionManager()).resume(txn);
            }
            catch (Exception e)
            {
                if (tc.isEventEnabled()) Tr.event(tc, "reAssociate", e);
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "reAssociate");
    }

    /**
     * @param providerId
     * @param flag ignored
     * @return
     * @throws XAException
     */
    public static Xid[] recover(int flag) throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "recover", Util.printFlag(flag));

        if (((TranManagerSet)TransactionManagerFactory.getTransactionManager()).isReplayComplete())
        {
            final Xid[] xids;

            synchronized (txnTable)
            {
                final ArrayList<Xid> xidList = new ArrayList<Xid>();                   // @LI3187-29.2C


                // Single process - we can check our own in-process list of JCA txns
                for (Iterator i = txnTable.values().iterator(); i.hasNext();)
                {
                    final JCATranWrapper txWrapper = (JCATranWrapper) i.next();

                    final TransactionImpl txn = txWrapper.getTransaction();

                    switch (txn.getTransactionState().getState())
                    {
                    case TransactionState.STATE_HEURISTIC_ON_COMMIT :
                    case TransactionState.STATE_HEURISTIC_ON_ROLLBACK :
                    case TransactionState.STATE_PREPARED :

                        if (tc.isDebugEnabled()) Tr.debug(tc, "recovering txn with state: " + txn.getTransactionState());
                        final Xid xid = txn.getJCAXid();
                        xidList.add(xid);
                        break;

                    default :
                        break;
                    }
                }

                xids = xidList.toArray(new Xid[0]);
            }

            if (tc.isEntryEnabled()) Tr.exit(tc, "recover", xids);
            return xids;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "recover", "throwing XAER_RMFAIL");
        throw new XAException(XAException.XAER_RMFAIL);
    }

    /**
     * @param txWrapper
     */
    public static void removeAssociation(JCATranWrapper txWrapper)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "removeAssociation", txWrapper);

        synchronized (txnTable)
        {
            txWrapper.removeAssociation();
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "removeAssociation");
    }

    public static TxExecutionContextHandler instance()
    {
        return _instance;
    }
}
