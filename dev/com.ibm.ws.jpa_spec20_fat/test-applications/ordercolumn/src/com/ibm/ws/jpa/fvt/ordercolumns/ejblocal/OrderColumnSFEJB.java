/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.fvt.ordercolumns.ejblocal;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "OrderColumnSFEJB")
@Local(OrderColumnSFEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class OrderColumnSFEJB extends BMTEJBTestVehicle {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OrderColumn_JEE", type = PersistenceContextType.TRANSACTION)
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OrderColumn_JEE")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OrderColumn_JEE_RL")
    private EntityManagerFactory amrlEmf;
}
