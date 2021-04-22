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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.jta;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateless(name = "EarLibLevelJTADPUMethodInjectionSLEJB")
@Local(EarLibLevelJTADPUMethodInjectionSLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class EarLibLevelJTADPUMethodInjectionSLEJB extends BMTEJBTestVehicle {
    private EntityManager cmtsEM;

    private EntityManagerFactory amjtaEMF;

    public EntityManager getCmtsEM() {
        return cmtsEM;
    }

    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    public void setCmtsEM(EntityManager cmtsEM) {
        this.cmtsEM = cmtsEM;
    }

    public EntityManagerFactory getAmjtaEMF() {
        return amjtaEMF;
    }

    @PersistenceUnit
    public void setAmjtaEMF(EntityManagerFactory amjtaEMF) {
        this.amjtaEMF = amjtaEMF;
    }
}
