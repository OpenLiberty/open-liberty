/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.jta;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "AppLevelJTADPUFieldInjectionSFEXEJB")
@Local(AppLevelJTADPUFieldInjectionSFEXEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class AppLevelJTADPUFieldInjectionSFEXEJB extends BMTEJBTestVehicle {
    @SuppressWarnings("unused")
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager cmexEM;

    @Override
    @Remove
    public void release() {

    }
}
