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

package com.ibm.ws.jpa.fvt.injection.ejb.dfi.inh.ddovrd;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

/**
 * JPA Injection Test EJB
 *
 * Injection Type: Field
 * Field/Method Protection: Private
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public abstract class DFIPriYesInhDDOvrdTestEXSuperclass extends BMTEJBTestVehicle {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED)
    private EntityManager em_cmex_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED)
    private EntityManager em_cmex_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED)
    private EntityManager em_cmex_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED)
    private EntityManager em_cmex_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPriYesInhDDOvrdTestEJB/ovdem_cmex_common_ejb")
    private EntityManager ovdem_cmex_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPriYesInhDDOvrdTestEJB/ovdem_cmex_ejb_ejb")
    private EntityManager ovdem_cmex_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPriYesInhDDOvrdTestEJB/ovdem_cmex_common_earlib")
    private EntityManager ovdem_cmex_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DFIPriYesInhDDOvrdTestEJB/ovdem_cmex_jpalib_earlib")
    private EntityManager ovdem_cmex_jpalib_earlib;
}
