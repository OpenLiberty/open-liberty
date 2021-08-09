package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.transaction.Status;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is a specialisation of the MSDelegatingLocalTransaction which can also
 * act as a javax.transaction.Synchronization. This is used in the 1PC optimisation
 * case in which the persistence layer enlists the database with the Transaction
 * Service permitting connection sharing with the EJB Persistence Manager.
 */
public class MSDelegatingLocalTransactionSynchronization extends MSDelegatingLocalTransaction implements javax.transaction.Synchronization
{
    private static TraceNLS      nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(MSDelegatingLocalTransactionSynchronization.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);


    /**
     * The Default constructor for MSDelegatingLocalTransactionSynchronization.
     * 
     * @param ms      The {@link MessageStoreImpl MessageStore} that this transaction is a part of.
     * @param persistence The {@link PersistenceManager} implementation to be used this transaction.
     * @param maxSize The number of operations allowed in this transaction.
     */
    public MSDelegatingLocalTransactionSynchronization(MessageStoreImpl ms, PersistenceManager persistence, int maxSize)
    {
        super(ms, persistence, maxSize);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "MSDelegatingLocalTransactionSynchronization", "MessageStore="+ms+", Persistence="+persistence+", MaxSize="+maxSize);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "MSDelegatingLocalTransactionSynchronization");
    }


    /*************************************************************************/
    /*                      Synchronization Implementation                   */
    /*************************************************************************/

    
    public void beforeCompletion()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "beforeCompletion");

        if (_state != TransactionState.STATE_ACTIVE)
        {
            MessageStoreRuntimeException mre = new MessageStoreRuntimeException(nls.getFormattedMessage("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{}, null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Cannot complete Transaction. Transaction is complete or completing!", mre);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "beforeCompletion");
            throw mre;
        }

        for (int i = 0; i < _callbacks.size(); i++)
        {
            TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
            callback.beforeCompletion(this);
        }

        // Do we have work to do?
        if (_workList != null)
        {
            try
            {
                _workList.preCommit(this);
            }
            catch (Throwable t)
            {
                FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransactionSynchronization.beforeCompletion", "1:103:1.4.1.1", this);

                // An error has occurred during preCommit
                // so we need to trigger a rollback of our
                // transaction and therefore our connection
                // by throwing a runtime exception.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Throwing exception in beforeCompletion to ensure rollback due to Exception in preCommit!", t);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "beforeCompletion");
                throw new MessageStoreRuntimeException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {t}, null), t);
            }
        }

        // Need to change the state after we have 
        // called the callbacks and preCommitted 
        // the workList as they may trigger
        // an addWork call to add work to the
        // transaction.
        _state = TransactionState.STATE_COMMITTING_1PC;

        try
        {
            // Do we have work to do?
            if (_workList != null)
            {
                // Call the persistence layer to trigger our
                // database work on the shared connection.
                _persistence.beforeCompletion(this);
            }
        }
        catch (PersistenceException pe)
        {
            FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransactionSynchronization.beforeCompletion", "1:134:1.4.1.1", this);
            // An error has occurred during the persistence
            // phase so we need to trigger a rollback of our
            // transaction and therefore our connection by
            // throwing a runtime exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Rollback-only set on transaction due to PersistenceException in beforeCompletion!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "beforeCompletion");
            throw new MessageStoreRuntimeException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {pe}, null), pe);
        }
        catch (SevereMessageStoreException smse)
        {
            FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransactionSynchronization.beforeCompletion", "1:145:1.4.1.1", this);
            // An error has occurred during the persistence
            // phase so we need to trigger a rollback of our
            // transaction and therefore our connection by
            // throwing a runtime exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Rollback-only set on transaction due to PersistenceException in beforeCompletion!", smse);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "beforeCompletion");
            throw new MessageStoreRuntimeException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {smse}, null), smse);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "beforeCompletion");
    }


    public void afterCompletion(int status)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "afterCompletion");

        try
        {
            switch (status)
            {
            case Status.STATUS_COMMITTED:
            case Status.STATUS_COMMITTING:
                // Do we have work to do?
                if (_workList != null)
                {
                    _persistence.afterCompletion(this, true);
                    // As our database work has been completed successfully 
                    // we now need to reflect that in the in-memory model.
                    _workList.commit(this);
                }

                _state = TransactionState.STATE_COMMITTED;
                break;

            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_ROLLING_BACK:
                // Do we have work to do?
                if (_workList != null)
                {
                    _persistence.afterCompletion(this, false);
                    // As our database work has been completed successfully 
                    // we now need to reflect that in the in-memory model.
                    _workList.rollback(this);
                }

                _state = TransactionState.STATE_ROLLEDBACK;
                break;

            case Status.STATUS_UNKNOWN:
                // Defect 281425
                // A heuristic hazard has been detected so we need 
                // to bring down the ME and log a message.
                SibTr.error(tc, "HEURISTIC_HAZARD_SIMS1006");

                if (_ms != null)
                {
                    _ms.reportLocalError();
                }
                break;
            }
        }
        catch (Throwable t)
        {
            FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransactionSynchronization.afterCompletion", "1:210:1.4.1.1", this);

            // An error has occurred during completion
            // but there's nothing we can do at this point
            // so we might aswell just log the problem.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught completing transaction work list!", t);
        }
        finally
        {
            // We always ensure that all afterCompletion
            // callbacks are called even in the case of 
            // rollback.
            boolean committed = (_state == TransactionState.STATE_COMMITTED);

            try
            {
	            if (_workList != null)
	            {
	                _workList.postComplete(this, committed);
	            }
	
	            for (int i = 0; i < _callbacks.size(); i++)
	            {
	                TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
	                callback.afterCompletion(this, committed);
	            }
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransactionSynchronization.afterCompletion", "1:239:1.4.1.1", this);
                // An error has occurred during completion
                // but there's nothing we can do at this point
                // so we might aswell just log the problem.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught during post complete phase of transaction!", t);
            }

         
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "afterCompletion");
    }
}
