package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.ws.uow.UOWScopeCallback;

public class ClientUserTransactionImpl extends UserTransactionImpl implements EmbeddableWebSphereUserTransaction
{
    private static final EmbeddableWebSphereUserTransaction _instance = new ClientUserTransactionImpl();

    public static EmbeddableWebSphereUserTransaction newOne() {
        return _instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#begin()
     */
    @Override
    public void begin() throws NotSupportedException, SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#commit()
     */
    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#getStatus()
     */
    @Override
    public int getStatus() throws SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#rollback()
     */
    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.UserTransaction#setTransactionTimeout(int)
     */
    @Override
    public void setTransactionTimeout(int arg0) throws SystemException {
        throw new SystemException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction#registerCallback(com.ibm.ws.uow.UOWScopeCallback)
     */
    @Override
    public void registerCallback(UOWScopeCallback callback) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction#unregisterCallback(com.ibm.ws.uow.UOWScopeCallback)
     */
    @Override
    public void unregisterCallback(UOWScopeCallback callback) {
        throw new IllegalStateException();
    }
}