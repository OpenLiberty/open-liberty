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

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.util.TMService;
import com.ibm.ws.tx.jta.embeddable.EmbeddableTransactionSynchronizationRegistryFactory;

/**
 * This class is to implement TransactionSynchronizationRegistry.
 * 
 * @author emilyj
 * 
 */
@Component(service = TransactionSynchronizationRegistry.class)
public class TransactionSynchronizationRegistryService implements TransactionSynchronizationRegistry {
    private TransactionSynchronizationRegistry tsr;

    @Activate
    protected void activate(ComponentContext ctxt) {
        // use the embeddable factory
        tsr = EmbeddableTransactionSynchronizationRegistryFactory.getTransactionSynchronizationRegistry();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        tsr = null;
    }

    @Reference
    protected void setTmService(TMService tm) {}

    @Override
    public Object getResource(Object arg0) {
        if (tsr != null) {
            return tsr.getResource(arg0);
        } else {
            return null;
        }
    }

    @Override
    public boolean getRollbackOnly() {
        if (tsr != null) {
            return tsr.getRollbackOnly();
        } else {
            return false;
        }
    }

    @Override
    public Object getTransactionKey() {

        if (tsr != null) {
            return tsr.getTransactionKey();
        } else {
            return null;
        }
    }

    @Override
    public int getTransactionStatus() {
        if (tsr != null) {
            return tsr.getTransactionStatus();
        } else {
            return Status.STATUS_NO_TRANSACTION; // error condition
        }
    }

    @Override
    public void putResource(Object arg0, Object arg1) {
        if (tsr != null) {
            tsr.putResource(arg0, arg1);
        }
    }

    @Override
    public void registerInterposedSynchronization(Synchronization arg0) {
        if (tsr != null) {
            tsr.registerInterposedSynchronization(arg0);
        }
    }

    @Override
    public void setRollbackOnly() {
        if (tsr != null) {
            tsr.setRollbackOnly();
        }
    }
}
