/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.transaction.nodistributedtransactions;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.omg.CORBA.SystemException;
import org.omg.CosTransactions.PropagationContext;

import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.transport.iiop.transaction.AbstractServerTransactionPolicyConfig;

/**
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class NoDtxServerTransactionPolicyConfig extends AbstractServerTransactionPolicyConfig {
    private static final TraceComponent tc = Tr.register(NoDtxServerTransactionPolicyConfig.class);

    private final TransactionManager transactionManager;

    /**
     * @param transactionManager
     */
    public NoDtxServerTransactionPolicyConfig(TransactionManager transactionManager) {
        super();
        this.transactionManager = transactionManager;
    }

    @Override
    protected void importTransaction(PropagationContext propagationContext) throws SystemException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "import transaction propagation context - ", propagationContext);

        //NOTE1: this code fragment is based on com.ibm.ws.Transaction.JTS.TxServerInterceptor.receive_request() in tWAS.
        //NOTE2: Tried new TX type. Doesn't work because the operations that place a Tran on the current thread, such as resume(),
        // need a TransactionImpl.
        //NOTE3: Might there be an existing tran currently on the thread? suspend() seems safe thing to do
        try
        {
            // Start the TX service if not already started
            TMHelper.checkTMState();

            // Need to create a new transaction of type TXTYPE_NONINTEROP_GLOBAL, so that the EJB container picks up that remote 
            // tx import has been attempted. This type will be installed on the thread.
            Transaction theTx = new TransactionImpl(UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL, 0);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set TxType to TXTYPE_NONINTEROP_GLOBAL in tran - ", theTx);

            transactionManager.resume(theTx);
        } catch (Exception ex)
        {
            // Swallow but trace the exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "caught exception - ", ex);
        }

    }
}
