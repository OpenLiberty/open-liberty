/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.tx;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

@TransactionManagement(TransactionManagementType.CONTAINER)
public abstract class AbstractBusinessTxRemoteBean implements BusinessTxRemote {

    @Resource
    private SessionContext context;

    @Override
    public void test() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void testTransactionRequired() {
        ((TxBean) context.lookup("java:module/TxBean")).work();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void testTransactionSupports() {
        ((TxBean) context.lookup("java:module/TxBean")).work();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void testTransactionMandatory() {
        ((TxBean) context.lookup("java:module/TxBean")).work();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void testTransactionNotSupported() {
        ((TxBean) context.lookup("java:module/TxBean")).work();
    }

}
