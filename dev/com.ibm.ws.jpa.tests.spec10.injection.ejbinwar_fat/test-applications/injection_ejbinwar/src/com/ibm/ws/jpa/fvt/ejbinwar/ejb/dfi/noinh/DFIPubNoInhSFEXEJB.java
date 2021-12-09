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

package com.ibm.ws.jpa.fvt.ejbinwar.ejb.dfi.noinh;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "DFIPubNoInhSFEXEJB")
@Local(DFIPubNoInhSFEXEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DFIPubNoInhSFEXEJB extends BMTEJBTestVehicle {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED)
    public EntityManager em_cmex_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED)
    public EntityManager em_cmex_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED)
    public EntityManager em_cmex_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED)
    public EntityManager em_cmex_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPubNoInhTestEJB/ovdem_cmex_common_ejb")
    public EntityManager ovdem_cmex_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPubNoInhTestEJB/ovdem_cmex_ejb_ejb")
    public EntityManager ovdem_cmex_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPubNoInhTestEJB/ovdem_cmex_common_earlib")
    public EntityManager ovdem_cmex_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPubNoInhTestEJB/ovdem_cmex_jpalib_earlib")
    public EntityManager ovdem_cmex_jpalib_earlib;

    @Override
    @Remove
    public void release() {

    }
}
