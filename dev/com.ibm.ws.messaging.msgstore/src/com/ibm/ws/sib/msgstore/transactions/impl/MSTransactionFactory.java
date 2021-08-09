package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.ExternalXAResource;
import com.ibm.ws.sib.transactions.TransactionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class is a factory for the interfaces used to achieve transactional
 * behaviour within the MessageStore.
 */
public final class MSTransactionFactory implements TransactionFactory
{
    private static TraceComponent tc = SibTr.register(MSTransactionFactory.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);
    // The maximum number of operations permitted in a transaction.
    private int _maximumSize = DEFAULT_MAXIMUM_TRANSACTION_SIZE;
    
   
    private MessageStoreImpl   _ms;
    private PersistenceManager _persistence;
    private boolean            _persistenceSupports1PCOptimisation;


    public MSTransactionFactory(MessageStoreImpl ms, PersistenceManager persistence)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "MSTransactionFactory", "MessageStore="+ms+", Persistence="+persistence);

        _ms          = ms;
        _persistence = persistence;
        _persistenceSupports1PCOptimisation = _persistence.supports1PCOptimisation();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "MSTransactionFactory");
    }


    /**
     * This method returns an object that represents a zero-phase or AutoCommit 
     * transaction. It can be used to ensure that a piece of work is carried out 
     * at once, essentially outside of a transaction coordination scope.
     * 
     * @return An instance of AutoCommitTransaction
     */
    public ExternalAutoCommitTransaction createAutoCommitTransaction()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createAutoCommitTransaction");

        ExternalAutoCommitTransaction instance = new MSAutoCommitTransaction(_ms, _persistence, getMaximumTransactionSize());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createAutoCommitTransaction", "return="+instance);
        return instance;
    }


    /**
     * This method is used to create a LocalResource that can either be enlisted as 
     * a particpant in a WebSphere LocalTransactionCoordination scope or used directly
     * to demarcate a one-phase Resource Manager Local Transaction.
     * 
     * @return An instance of Object
     */
    public ExternalLocalTransaction createLocalTransaction()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createLocalTransaction");

        ExternalLocalTransaction instance;
        
        if (_persistenceSupports1PCOptimisation)
        {
            instance = new MSDelegatingLocalTransactionSynchronization(_ms, _persistence, getMaximumTransactionSize());
        }
        else
        {
            instance = new MSDelegatingLocalTransaction(_ms, _persistence, getMaximumTransactionSize());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createLocalTransaction", "return="+instance);
        return instance;
    }


    /**
     * This method is used to create an XAResource that can be enlisted as a 
     * particpant in a two-phase XA compliant transaction.
     * 
     * @return An instance of XAResource
     */
    public ExternalXAResource createXAResource()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createXAResource");

        ExternalXAResource instance = new MSDelegatingXAResource(_ms, _persistence, getMaximumTransactionSize());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createXAResource", "return="+instance);
        return instance;
    }
    
    public final int getMaximumTransactionSize()
    {
 	   return _maximumSize;
    }

    public final void setMaximumTransactionSize(int maximumSize) 
    {
        if (tc.isEntryEnabled()) SibTr.entry(tc, "setMaximumTransactionSize", "MaximumTranSize="+maximumSize);
        _maximumSize = maximumSize;
        if (tc.isEntryEnabled()) SibTr.exit(tc, "setMaximumTransactionSize");
    }
}

