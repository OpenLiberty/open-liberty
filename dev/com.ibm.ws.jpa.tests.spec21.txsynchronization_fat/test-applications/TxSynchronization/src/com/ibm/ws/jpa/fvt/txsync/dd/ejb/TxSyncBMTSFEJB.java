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

package com.ibm.ws.jpa.fvt.txsync.dd.ejb;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;

import com.ibm.ws.jpa.fvt.txsync.ejblocal.TxSyncBMTSFLocal;
import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "TxSyncBMTSFEJB")
@Local(TxSyncBMTSFLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class TxSyncBMTSFEJB extends BMTEJBTestVehicle {
    // Container Managed Transaction Scope
//    @PersistenceContext(unitName="TxSync",
//            type=PersistenceContextType.TRANSACTION,
//            synchronization=SynchronizationType.SYNCHRONIZED)
    private EntityManager emCMTSTxSync;

//    @PersistenceContext(unitName="TxSync",
//            type=PersistenceContextType.TRANSACTION,
//            synchronization=SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emCMTSTxUnsync;
}
