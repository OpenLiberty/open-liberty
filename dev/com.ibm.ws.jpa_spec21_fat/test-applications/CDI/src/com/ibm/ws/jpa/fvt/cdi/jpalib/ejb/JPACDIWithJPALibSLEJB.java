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

package com.ibm.ws.jpa.fvt.cdi.jpalib.ejb;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.jpa.fvt.cdi.jpalib.CDITestComponent;
import com.ibm.ws.jpa.fvt.cdi.jpalib.model.LoggingService;
import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateless(name = "JPACDIWithJPALibSLEJB")
@Local(JPACDIWithJPALibSLLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class JPACDIWithJPALibSLEJB extends BMTEJBTestVehicle implements CDITestComponent {
    @Inject
    // used for checking callbacks to entity listener
    private LoggingService logger;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "TestCDIJPALib")
    private EntityManager cmtsEM;

    // Application Managed JTA
    @PersistenceUnit(unitName = "TestCDIJPALib")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "TestCDIJPALib_RL")
    private EntityManagerFactory amrlEmf;

    @Override
    public List<String> getEntityListenerMessages() {
        return logger.getAndClearMessages();
    }
}
