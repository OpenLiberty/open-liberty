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

package com.ibm.ws.jpa.fvt.txsync.web;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

public class TxSyncTestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.SYNCHRONIZED)
    private EntityManager emCMTSTxSync;

    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emCMTSTxUnsync;
}
