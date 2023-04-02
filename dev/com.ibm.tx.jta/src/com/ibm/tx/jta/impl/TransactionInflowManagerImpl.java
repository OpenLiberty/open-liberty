package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.transaction.NotSupportedException;

import com.ibm.tx.jta.TransactionInflowManager;
import com.ibm.tx.util.TMHelper;
import com.ibm.ws.ffdc.FFDCFilter;

public class TransactionInflowManagerImpl implements TransactionInflowManager {
    private static TransactionInflowManager _instance;

    private TransactionInflowManagerImpl() {
    }

    public static synchronized TransactionInflowManager instance() {
        if (_instance == null) {
            _instance = new TransactionInflowManagerImpl();
        }

        return _instance;
    }

    public void associate(ExecutionContext ec, String inflowCoordinatorName) throws WorkCompletedException {
        try {
            TMHelper.checkTMState();
        } catch (NotSupportedException e) {
            FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TxExecutionContextHandler.associate", "105", this);
            throw new WorkCompletedException(e);
        }

        TxExecutionContextHandler.instance().associate(ec, inflowCoordinatorName);
    }

    @Override
    public void dissociate() {
        TxExecutionContextHandler.doDissociate();
    }

    @Override
    public XATerminator getXATerminator(String inflowCoordinatorName) {
        return TxXATerminator.instance(inflowCoordinatorName);
    }
}