/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.impl;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackManager;

public class UserTransactionImpl implements UserTransaction
{
    private static TraceComponent tc = Tr.register(com.ibm.tx.jta.impl.UserTransactionImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected final UOWScopeCallbackManager _callbackManager = new UOWScopeCallbackManager();
 
    protected static TransactionManager _tm;

    private static UserTransactionImpl _instance = new UserTransactionImpl();

    protected UserTransactionImpl(){}
    
    public static UserTransactionImpl instance()
    {
        return _instance;
    }

    protected TransactionManager getTM()
    {
        if (_tm instanceof TranManagerSet)
        {
            return _tm;
        }
        
        _tm = TransactionManagerFactory.getTransactionManager();
        return _tm;
    }
    
    public void begin() throws NotSupportedException, SystemException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "begin");

        try
        {
            // Call registered users giving notification of BEGIN starting
            _callbackManager.notifyCallbacks(UOWScopeCallback.PRE_BEGIN, null); // Defect 130321

            ((TranManagerSet)getTM()).beginUserTran();

        }
        finally
        {
            // Defect 130321
            //
            // Call registered users giving notification of BEGIN ending.
            // If an exception is thrown then we should set the new
            // transaction to RollbackOnly.
            //
            try
            {
                UOWScope uow = null;
                
                try
                {
                    uow = (UOWScope)getTM().getTransaction();
                }
                catch(RuntimeException e)
                {
                    // Tried to start a user tran with TM in wrong state - probably not started
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.UserTransactionImpl.begin", "59", this);
                }

                _callbackManager.notifyCallbacks(UOWScopeCallback.POST_BEGIN, uow);
            }
            catch (IllegalStateException ise)
            {
                FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.UserTransactionImpl.begin", "148", this);
                //
                // Error occurred in POST_BEGIN so we
                // need to mark the transaction as
                // RollbackOnly
                //
                try
                {
                    getTM().setRollbackOnly();
                    if (tc.isEventEnabled()) Tr.event(tc, "begin", "Exception caught in POST_BEGIN. Transaction marked RollbackOnly.");
                }
                catch (IllegalStateException ise2)
                {
                    FFDCFilter.processException(ise2, "com.ibm.tx.jta.impl.UserTransactionImpl.begin", "161", this);
                    if (tc.isEventEnabled()) Tr.event(tc, "begin", "IllegalStateException caught setting RollbackOnly.");
                }
            }

            if (tc.isEntryEnabled()) Tr.exit(tc, "begin");
        }

    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc,"commit");

        final UOWScope coord = (UOWScope)getTM().getTransaction();
        if (coord == null /* || !coord.isGlobal() */)
        {
            final IllegalStateException ise = new IllegalStateException("No Global Transaction exists to commit.");
            if (tc.isEntryEnabled()) Tr.exit(tc, "commit", ise);
            throw ise;
        }

        // Defect 130321
        //
        // Execute the PRE_END callback, if an exception
        // occurs then we need to set RollbackOnly on the
        // completing transaction.
        //
        try
        {
            _callbackManager.notifyCallbacks(UOWScopeCallback.PRE_END, coord);
        }
        catch (IllegalStateException ise)
        {
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.UserTransactionImpl.commit", "220", this);
            try
            {
                getTM().setRollbackOnly();
                if (tc.isEventEnabled()) Tr.event(tc,"commit", "Exception caught in PRE_END. Transaction marked RollbackOnly.");
            }
            catch (IllegalStateException ise2)
            {
                FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.UserTransactionImpl.commit", "228", this);
                if (tc.isEventEnabled()) Tr.event(tc,"commit", "IllegalStateException caught setting RollbackOnly.");
            }
            catch (SystemException se)
            {
                FFDCFilter.processException(se, "com.ibm.tx.jta.impl.UserTransactionImpl.commit", "229", this);
            }
        }

        try
        {
            getTM().commit();
        }
        finally
        {
            // Defect 130321
            //
            // Call registered users giving notification
            // of END (Commit or Rollback) ending
            //
            try
            {
                _callbackManager.notifyCallbacks(UOWScopeCallback.POST_END, null);
            }
            catch (IllegalStateException ise)
            {
                // No FFDC Code Needed.
                if (tc.isEventEnabled()) Tr.event(tc,"commit", "Exception caught in POST_END.");
            }

            if (tc.isEntryEnabled()) Tr.exit(tc,"commit");
        }
    }

    public int getStatus() throws SystemException
    {
        return getTM().getStatus();
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc,"rollback");

        // Defect 130321
        //
        // We need to check if there is an active
        // transaction before current.rollback as if
        // there isn't no context change will be
        // driven on the callbacks.
        //
        final UOWScope coord = (UOWScope)getTM().getTransaction();
        if (coord == null /* || !coord.isGlobal()*/)
        {
            final IllegalStateException ise = new IllegalStateException("No Global Transaction exists to rollback.");
            if (tc.isEntryEnabled()) Tr.exit(tc, "rollback (API)", ise);
            throw ise;
        }

        // Defect 130321
        //
        // Execute the PRE_END callback, if an exception
        // occurs then we need to set RollbackOnly on the
        // completing transaction.
        //
        try
        {
            _callbackManager.notifyCallbacks(UOWScopeCallback.PRE_END, coord);
        }
        catch (IllegalStateException ise)
        {
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.UserTransactionImpl.rollback", "343", this);

            try
            {
                getTM().setRollbackOnly();
                if (tc.isEventEnabled()) Tr.event(tc,"rollback", "Exception caught in PRE_END. Transaction marked RollbackOnly.");
            }
            catch (IllegalStateException ise2)
            {
                FFDCFilter.processException(ise2, "com.ibm.tx.jta.impl.UserTransactionImpl.rollback", "351", this);
                if (tc.isEventEnabled()) Tr.event(tc,"rollback", "IllegalStateException caught setting RollbackOnly.");
            }
            catch (SystemException se)
            {
                FFDCFilter.processException(se, "com.ibm.tx.jta.impl.UserTransactionImpl.rollback", "352", this);
                if (tc.isEventEnabled()) Tr.event(tc,"rollback", "SystemException caught setting RollbackOnly.");
            }
        }

        try
        {
            getTM().rollback();
        }
        finally
        {
            // Defect 130321
            //
            // Call registered users giving notification
            // of END (Commit or Rollback) ending
            //
            try
            {
                _callbackManager.notifyCallbacks(UOWScopeCallback.POST_END, null);
            }
            catch (IllegalStateException ise)
            {
                // No FFDC Code Needed.
                if (tc.isEventEnabled()) Tr.event(tc,"rollback", "Exception caught in POST_END.");
            }

            if (tc.isEntryEnabled()) Tr.exit(tc,"rollback (API)");
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException
    {
        getTM().setRollbackOnly();
    }

    public void setTransactionTimeout(int timeout) throws SystemException
    {
        getTM().setTransactionTimeout(timeout);
    }


    /**
     * Register users who want notification on UserTransaction Begin and End
     *
     * @param callback
     */
    public void registerCallback(UOWScopeCallback callback)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "registerCallback", new Object[]{callback, this});
        
        _callbackManager.addCallback(callback);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "registerCallback");
    }

    /**
     * unregister users who want notification on UserTransaction Begin and End
     *
     * @param callback
     */
    public void unregisterCallback(UOWScopeCallback callback)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "unregisterCallback", new Object[]{callback, this});
        
        _callbackManager.removeCallback(callback);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "unregisterCallback");
    }

}
