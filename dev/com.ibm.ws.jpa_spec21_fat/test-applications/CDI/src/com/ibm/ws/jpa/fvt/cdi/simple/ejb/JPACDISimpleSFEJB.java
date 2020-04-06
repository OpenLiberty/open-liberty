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

package com.ibm.ws.jpa.fvt.cdi.simple.ejb;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.jpa.fvt.cdi.simple.CDITestComponent;
import com.ibm.ws.jpa.fvt.cdi.simple.model.LoggingService;
import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "JPACDISimpleSFEJB")
@Local(JPACDISimpleSFLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class JPACDISimpleSFEJB extends BMTEJBTestVehicle implements CDITestComponent {
    @Inject
    // used for checking callbacks to entity listener
    private LoggingService logger;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "TestCDI")
    private EntityManager cmtsEM;

    // Application Managed JTA
    @PersistenceUnit(unitName = "TestCDI")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "TestCDI_RL")
    private EntityManagerFactory amrlEmf;

    @Override
    public List<String> getEntityListenerMessages() {
        return logger.getAndClearMessages();
    }
}
