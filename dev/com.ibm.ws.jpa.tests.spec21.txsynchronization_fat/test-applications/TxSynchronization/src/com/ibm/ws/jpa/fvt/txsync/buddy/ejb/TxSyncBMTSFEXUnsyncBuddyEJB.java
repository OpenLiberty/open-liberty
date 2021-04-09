/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.txsync.buddy.ejb;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import javax.transaction.UserTransaction;

import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTSFEXUnsyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.testlogic.BeanStore;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;

@Stateful(name = "TxSyncBMTSFEXUnsyncBuddyEJB")
@Local(TxSyncBMTSFEXUnsyncBuddyLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class TxSyncBMTSFEXUnsyncBuddyEJB implements BeanStore {
    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.EXTENDED,
                        synchronization = SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emCMEXTxUnsync;

    @Resource
    protected EJBContext ejbCtx;

    @Resource
    protected UserTransaction tx;

    private Map<String, Object> beanStore = new HashMap<String, Object>();

    @Override
    public Map<String, Object> getBeanStore() {
        return beanStore;
    }

    @PostConstruct
    protected void postConstruct() {

    }

    @PreDestroy
    protected void preDestroy() {

    }

    /*
     * Local Interface Methods
     */
    public Serializable doWorkRequest(TestWorkRequest work, TargetEntityManager targetEm) {
        EntityManager workEntityManager = null;

        switch (targetEm) {
            case TXSYNC1_UNSYNCHRONIZED:
                workEntityManager = emCMEXTxUnsync;
                break;
            default:
                throw new RuntimeException("Unknown TargetEntityManager type: " + targetEm);
        }

        return work.doTestWork(workEntityManager, tx, this);
    }

    @Remove
    public void close() {

    }
}
