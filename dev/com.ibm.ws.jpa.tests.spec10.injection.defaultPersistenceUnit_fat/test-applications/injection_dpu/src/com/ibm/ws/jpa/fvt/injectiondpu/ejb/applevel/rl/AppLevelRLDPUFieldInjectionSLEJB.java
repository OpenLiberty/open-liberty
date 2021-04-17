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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.rl;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateless(name = "AppLevelRLDPUFieldInjectionSLEJB")
@Local(AppLevelRLDPUFieldInjectionSLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class AppLevelRLDPUFieldInjectionSLEJB extends BMTEJBTestVehicle {
    @SuppressWarnings("unused")
    @PersistenceUnit
    private EntityManagerFactory amrlEMF;
}
