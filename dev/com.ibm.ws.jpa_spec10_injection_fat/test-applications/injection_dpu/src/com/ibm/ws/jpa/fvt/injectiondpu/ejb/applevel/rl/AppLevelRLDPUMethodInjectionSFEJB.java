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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.rl;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "AppLevelRLDPUMethodInjectionSFEJB")
@Local(AppLevelRLDPUMethodInjectionSFEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class AppLevelRLDPUMethodInjectionSFEJB extends BMTEJBTestVehicle {
    private EntityManagerFactory amrlEMF;

    public EntityManagerFactory getAmrlEMF() {
        return amrlEMF;
    }

    @PersistenceUnit
    public void setAmrlEMF(EntityManagerFactory amrlEMF) {
        this.amrlEMF = amrlEMF;
    }

    @Override
    @Remove
    public void release() {

    }
}
