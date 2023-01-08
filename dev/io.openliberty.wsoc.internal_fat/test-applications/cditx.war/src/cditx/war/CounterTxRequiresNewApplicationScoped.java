/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package cditx.war;

import java.io.Serializable;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

/**
 * Counter for the application scope.
 */
@ApplicationScoped
@Named
public class CounterTxRequiresNewApplicationScoped extends Counter {

    private static final long serialVersionUID = 1L;
    /**  */
    private static final String filter = "(testfilter=jon)";

    @Override
    @Transactional(value = TxType.REQUIRES_NEW)
    public int getNext() {
        System.out.println("CounterTxRequiresNewApplicationScoped getNext() called");
        // Do some transactional work, using test XAResources
        try {
            final ExtendedTransactionManager tm = TransactionManagerFactory
                            .getTransactionManager();
            final Serializable xaResInfo1 = XAResourceInfoFactory
                            .getXAResourceInfo(0);
            final Serializable xaResInfo2 = XAResourceInfoFactory
                            .getXAResourceInfo(1);
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

        } catch (Exception e) {
            System.out.println("CounterTxRequiresNewApplicationScoped getNext() caught exc: " + e);
            e.printStackTrace();
        }
        return super.getNext();
    }

    @PreDestroy
    public void destruct() {}

}