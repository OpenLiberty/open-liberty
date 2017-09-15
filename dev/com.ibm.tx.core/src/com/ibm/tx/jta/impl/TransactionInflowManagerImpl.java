package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.transaction.NotSupportedException;

import com.ibm.tx.jta.*;
import com.ibm.tx.util.TMHelper;
import com.ibm.tx.util.logging.FFDCFilter;

public class TransactionInflowManagerImpl implements TransactionInflowManager
{
    private static TransactionInflowManager _instance;

    private TransactionInflowManagerImpl(){}

    public static synchronized TransactionInflowManager instance()
    {
        if (_instance == null)
        {
            _instance = new TransactionInflowManagerImpl();
        }

        return _instance;
    }

    public void associate(ExecutionContext ec, String inflowCoordinatorName) throws WorkCompletedException
    {
        try
        {
            TMHelper.checkTMState();
        }
        catch(NotSupportedException e)
        {
            FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TxExecutionContextHandler.associate", "105", this);
            throw new WorkCompletedException(e);
        }

        TxExecutionContextHandler.instance().associate(ec, inflowCoordinatorName);
    }

    public void dissociate()
    {
        TxExecutionContextHandler.doDissociate();
    }

    public XATerminator getXATerminator(String inflowCoordinatorName)
    {
        return TxXATerminator.instance(inflowCoordinatorName);
    }
}