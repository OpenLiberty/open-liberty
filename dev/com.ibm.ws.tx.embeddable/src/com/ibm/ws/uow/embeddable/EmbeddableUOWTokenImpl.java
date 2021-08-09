/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow.embeddable;

import javax.transaction.Transaction;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

public class EmbeddableUOWTokenImpl implements UOWToken
{
    private static final TraceComponent tc = Tr.register(UOWToken.class, TranConstants.TRACE_GROUP, null);

    protected Transaction _transaction;
    protected LocalTransactionCoordinator _localTranCoord;
    
    protected EmbeddableUOWTokenImpl(Transaction transaction, LocalTransactionCoordinator localTranCoord)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "UOWToken", new Object[]{transaction, localTranCoord});

        _transaction = transaction;
        _localTranCoord = localTranCoord;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "UOWToken", this);
    }

    public final Transaction getTransaction()
    {        
        return _transaction;
    }

    public final LocalTransactionCoordinator getLocalTransactionCoordinator()
    {       
        return _localTranCoord;
    }

    public String toString()
    {
        final StringBuffer buffer = new StringBuffer();
    
        buffer.append("UOWToken [ ");
        buffer.append("Transaction: ");
        buffer.append(_transaction);
        buffer.append(", ");
        buffer.append("LocalTranCoord: ");
        buffer.append(_localTranCoord);
        buffer.append(" ]");
    
        return buffer.toString();
    }
}
