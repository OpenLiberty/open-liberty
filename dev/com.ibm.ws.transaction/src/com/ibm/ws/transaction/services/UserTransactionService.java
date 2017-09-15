/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.embeddable.impl.ClientUserTransactionImpl;
import com.ibm.tx.jta.embeddable.impl.EmbeddableUserTransactionImpl;
import com.ibm.tx.util.TMService;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 * This class is to implement UserTransaction by using the
 * embeddable version of UserTransaction
 * 
 * @author emilyj
 * 
 */
@Component(service = { UserTransaction.class, EmbeddableWebSphereUserTransaction.class })
public class UserTransactionService implements EmbeddableWebSphereUserTransaction {
    private EmbeddableWebSphereUserTransaction ut;

    @Activate
    protected void activate(BundleContext context) {
        if (WsLocationConstants.LOC_PROCESS_TYPE_SERVER.equals(context.getProperty(WsLocationConstants.LOC_PROCESS_TYPE))) {
            ut = EmbeddableUserTransactionImpl.instance();
        } else {
            ut = ClientUserTransactionImpl.newOne();
        }
    }

    @Deactivate
    protected void deactivate(BundleContext ctxt) {
        ut = null;
    }

    @Reference
    protected void setTmService(TMService tm) {}

    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (ut != null) {
            ut.begin();
        }
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        if (ut != null) {
            ut.commit();
        }
    }

    @Override
    public int getStatus() throws SystemException {
        if (ut != null) {
            return ut.getStatus();
        } else {
            return Status.STATUS_NO_TRANSACTION; // error condition
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        if (ut != null) {
            ut.rollback();
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (ut != null) {
            ut.setRollbackOnly();
        }
    }

    @Override
    public void setTransactionTimeout(int arg0) throws SystemException {
        if (ut != null) {
            ut.setTransactionTimeout(arg0);
        }
    }

    @Override
    public synchronized void registerCallback(UOWScopeCallback callback) {
        if (ut != null) {
            ut.registerCallback(callback);
        }
    }

    @Override
    public synchronized void unregisterCallback(UOWScopeCallback callback) {
        if (ut != null) {
            ut.unregisterCallback(callback);
        }
    }
}
