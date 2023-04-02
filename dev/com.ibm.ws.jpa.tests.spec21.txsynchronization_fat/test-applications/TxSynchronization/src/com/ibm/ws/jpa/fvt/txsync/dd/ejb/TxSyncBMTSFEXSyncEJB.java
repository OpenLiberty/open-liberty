/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.txsync.dd.ejb;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;

import com.ibm.ws.jpa.fvt.txsync.ejblocal.TxSyncBMTSFEXSyncLocal;
import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "TxSyncBMTSFEXSyncEJB")
@Local(TxSyncBMTSFEXSyncLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class TxSyncBMTSFEXSyncEJB extends BMTEJBTestVehicle {
    // Container Managed Extended Scope
//    @PersistenceContext(unitName="TxSync",
//            type=PersistenceContextType.EXTENDED,
//            synchronization=SynchronizationType.SYNCHRONIZED)
    private EntityManager emCMEXTxSync;
}
