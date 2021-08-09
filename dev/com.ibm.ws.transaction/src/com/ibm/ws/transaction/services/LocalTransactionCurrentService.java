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
package com.ibm.ws.transaction.services;

import org.osgi.service.component.ComponentContext;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 *
 */
public class LocalTransactionCurrentService implements LocalTransactionCurrent {

    private LocalTransactionCurrent ltc;

    protected void activate(ComponentContext ctxt) {
        //The assumption is that we get an instance of EmbeddableLocalTranCurrentSet
        //which has a "self()" that provides an implementation of the interface
        //SynchronizationRegistryUOWScope.  
        ltc = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();

    }

    public void setUOWEventListener(UOWEventListener el) {
        LocalTranCurrentSet.instance().setUOWEventListener(el);
    }

    public void unsetUOWEventListener(UOWEventListener el) {
        LocalTranCurrentSet.instance().unsetUOWEventListener(el);
    }

    protected void deactivate(ComponentContext ctxt) {
        ltc = null;
    }

    /** {@inheritDoc} */
    @Override
    public void begin() throws IllegalStateException {
        if (ltc != null) {
            ltc.begin();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void begin(boolean arg0) throws IllegalStateException {
        if (ltc != null) {
            ltc.begin(arg0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void begin(boolean arg0, boolean arg1, boolean arg2) throws IllegalStateException {
        if (ltc != null) {
            ltc.begin(arg0, arg1, arg2);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void beginShareable(boolean arg0, boolean arg1, boolean arg2) throws IllegalStateException {
        if (ltc != null) {
            ltc.beginShareable(arg0, arg1, arg2);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() throws InconsistentLocalTranException, IllegalStateException, RolledbackException {
        if (ltc != null) {
            ltc.cleanup();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void complete(int arg0) throws InconsistentLocalTranException, RolledbackException, IllegalStateException {
        if (ltc != null) {
            ltc.complete(arg0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void end(int arg0) throws InconsistentLocalTranException, RolledbackException, IllegalStateException {
        if (ltc != null) {
            ltc.end(arg0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public LocalTransactionCoordinator getLocalTranCoord() {
        if (ltc != null) {
            return ltc.getLocalTranCoord();
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasOutstandingWork() {
        if (ltc != null) {
            return ltc.hasOutstandingWork();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void registerCallback(UOWScopeCallback arg0) {
        if (ltc != null) {
            ltc.registerCallback(arg0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resume(LocalTransactionCoordinator arg0) throws IllegalStateException {
        if (ltc != null) {
            ltc.resume(arg0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public LocalTransactionCoordinator suspend() {
        if (ltc != null) {
            return ltc.suspend();
        }
        return null;
    }

}
