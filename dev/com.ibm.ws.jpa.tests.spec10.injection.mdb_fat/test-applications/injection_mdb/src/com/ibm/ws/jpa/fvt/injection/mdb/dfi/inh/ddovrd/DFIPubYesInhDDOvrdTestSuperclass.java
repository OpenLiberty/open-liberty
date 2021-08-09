/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injection.mdb.dfi.inh.ddovrd;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.jpa.fvt.injection.mdb.AbstractTestMDB;

/**
 * JPA Injection Test EJB
 *
 * Injection Type: Field
 * Field/Method Protection: Public
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public abstract class DFIPubYesInhDDOvrdTestSuperclass extends AbstractTestMDB {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA")
    public EntityManagerFactory emf_amjta_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceUnit(unitName = "EJB_JTA")
    public EntityManagerFactory emf_amjta_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    public EntityManagerFactory emf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA")
    public EntityManagerFactory emf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL")
    public EntityManagerFactory emf_amrl_common_ejb;

    // This EntityManager should refer to the EJB_RL in the Web App module
    @PersistenceUnit(unitName = "EJB_RL")
    public EntityManagerFactory emf_amrl_ejb_ejb;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    public EntityManagerFactory emf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL")
    public EntityManagerFactory emf_amrl_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubYesInhDDOvrdMDB/ovdem_cmts_common_ejb")
    public EntityManager ovdem_cmts_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubYesInhDDOvrdMDB/ovdem_cmts_ejb_ejb")
    public EntityManager ovdem_cmts_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubYesInhDDOvrdMDB/ovdem_cmts_common_earlib")
    public EntityManager ovdem_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubYesInhDDOvrdMDB/ovdem_cmts_jpalib_earlib")
    public EntityManager ovdem_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amjta_common_ejb")
    public EntityManagerFactory ovdemf_amjta_common_ejb;

    // This EntityManager should refer to the EJB_JTA in the Web App module
    @PersistenceUnit(unitName = "EJB_JTA", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amjta_ejb_ejb")
    public EntityManagerFactory ovdemf_amjta_ejb_ejb;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amjta_common_earlib")
    public EntityManagerFactory ovdemf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amjta_jpalib_earlib")
    public EntityManagerFactory ovdemf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amrl_common_ejb")
    public EntityManagerFactory ovdemf_amrl_common_ejb;

    // This EntityManager should refer to the EJB_RL in the Web App module
    @PersistenceUnit(unitName = "EJB_RL", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amrl_ejb_ejb")
    public EntityManagerFactory ovdemf_amrl_ejb_ejb;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amrl_common_earlib")
    public EntityManagerFactory ovdemf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DFIPubYesInhDDOvrdMDB/ovdemf_amrl_jpalib_earlib")
    public EntityManagerFactory ovdemf_amrl_jpalib_earlib;
}
