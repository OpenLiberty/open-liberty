package com.ibm.ws.tx.jta.embeddable;

/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkException;
import javax.transaction.SystemException;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.impl.JCARecoveryData;
import com.ibm.tx.jta.impl.JCATranWrapper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

public class EmbeddableTxExecutionContextHandler extends com.ibm.tx.jta.impl.TxExecutionContextHandler
{
    private static EmbeddableWebSphereTransactionManager _tm = EmbeddableTransactionManagerFactory.getTransactionManager();

    private static EmbeddableTxExecutionContextHandler _instance;

    @Override
    public void associate(ExecutionContext ec, String providerId) throws WorkCompletedException
    {
        try {
            if (_tm.getTransaction() != null) {
                // There's already a global tx on this thread
                final WorkCompletedException wce = new WorkCompletedException("Already associated", WorkException.TX_RECREATE_FAILED);
                throw wce;
            }
        } catch (SystemException e) {
            FFDCFilter.processException(e, "com.ibm.ws.tx.jta.embeddable.EmbeddableTxExecutionContextHandler", "54", this);
        }

        super.associate(ec, providerId);
    }

    @Override
    protected JCATranWrapper createWrapper(int timeout, Xid xid, JCARecoveryData jcard) throws WorkCompletedException /* @512190C */
    {
        return new EmbeddableJCATranWrapperImpl(timeout, xid, jcard);
    }

    public static EmbeddableTxExecutionContextHandler instance()
    {
        if (_instance == null) {
            _instance = new EmbeddableTxExecutionContextHandler();
        }
        return _instance;
    }
}
