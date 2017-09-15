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
package com.ibm.ws.uow.embeddable;

import javax.transaction.Synchronization;

import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.tx.jta.embeddable.UserTransactionController;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;

/**
 *
 */
public class ClientUOWManagerImpl implements UOWManager, UOWScopeCallback, UserTransactionController {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#runUnderUOW(int, boolean, com.ibm.wsspi.uow.UOWAction)
     */
    @Override
    public void runUnderUOW(int uowType, boolean join, UOWAction uowAction) throws UOWActionException, UOWException {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#getUOWTimeout()
     */
    @Override
    public int getUOWTimeout() throws IllegalStateException {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#getUOWExpiration()
     */
    @Override
    public long getUOWExpiration() throws IllegalStateException {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#setUOWTimeout(int, int)
     */
    @Override
    public void setUOWTimeout(int uowType, int timeout) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#runUnderUOW(int, boolean, com.ibm.wsspi.uow.ExtendedUOWAction, java.lang.Class[], java.lang.Class[])
     */
    @Override
    public Object runUnderUOW(int uowType, boolean join, ExtendedUOWAction uowAction, Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) throws Exception {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getLocalUOWId()
     */
    @Override
    public long getLocalUOWId() {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getResource(java.lang.Object)
     */
    @Override
    public Object getResource(Object key) throws NullPointerException {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getRollbackOnly()
     */
    @Override
    public boolean getRollbackOnly() {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getUOWStatus()
     */
    @Override
    public int getUOWStatus() {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getUOWType()
     */
    @Override
    public int getUOWType() {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#putResource(java.lang.Object, java.lang.Object)
     */
    @Override
    public void putResource(Object key, Object value) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#registerInterposedSynchronization(javax.transaction.Synchronization)
     */
    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly() {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.uow.UOWSynchronizationRegistry#getUOWName()
     */
    @Override
    public String getUOWName() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.tx.jta.embeddable.UserTransactionController#disable()
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.tx.jta.embeddable.UserTransactionController#enable()
     */
    @Override
    public void setEnabled(boolean enabled) {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.UOWScopeCallback#contextChange(int, com.ibm.ws.uow.UOWScope)
     */
    @Override
    public void contextChange(int changeType, UOWScope uowScope) throws IllegalStateException {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#suspend()
     */
    @Override
    public UOWToken suspend() throws SystemException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#resume(com.ibm.ws.uow.embeddable.UOWToken)
     */
    @Override
    public void resume(UOWToken uowToken) throws IllegalThreadStateException, IllegalArgumentException, SystemException {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#suspendAll()
     */
    @Override
    public UOWToken suspendAll() throws SystemException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#resumeAll(com.ibm.ws.uow.embeddable.UOWToken)
     */
    @Override
    public void resumeAll(UOWToken uowToken) throws Exception {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#registerCallback(com.ibm.ws.uow.UOWScopeCallback)
     */
    @Override
    public void registerCallback(UOWScopeCallback callback) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#registerRunUnderUOWCallback(com.ibm.ws.Transaction.UOWCallback)
     */
    @Override
    public void registerRunUnderUOWCallback(UOWCallback callback) {
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.uow.embeddable.UOWManager#getUOWScope()
     */
    @Override
    public UOWScope getUOWScope() throws SystemException {
        return null;
    }
}
