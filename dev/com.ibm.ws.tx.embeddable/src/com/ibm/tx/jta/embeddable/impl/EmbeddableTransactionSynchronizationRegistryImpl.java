/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable.impl;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.impl.TranManagerSet;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.jta.impl.TransactionSynchronizationRegistryImpl;

public class EmbeddableTransactionSynchronizationRegistryImpl extends TransactionSynchronizationRegistryImpl {

    public EmbeddableTransactionSynchronizationRegistryImpl() {
        super();
    }

    /**
     * Ensure we use the EmbeddableTransactionManagerFactory, since the super class uses the non-embeddable version
     * in its implementation of "getTransaction"
     */
    @Override
    protected TransactionImpl getTransaction() {
        return getTransactionManager().getTransactionImpl();
    }

    /**
     * Convenience method to get the tran manager
     */
    protected TranManagerSet getTransactionManager() {
        return (TranManagerSet) EmbeddableTransactionManagerFactory.getTransactionManager();
    }

    /**
     * This method was using the non-embeddable tran manager in the super class
     */
    @Override
    public int getTransactionStatus() {
        return getTransactionManager().getStatus();
    }

    /**
     * This method was using the non-embeddable tran manager in the super class
     */
    @Override
    public void setRollbackOnly() {
        getTransactionManager().setRollbackOnly();
    }

}
