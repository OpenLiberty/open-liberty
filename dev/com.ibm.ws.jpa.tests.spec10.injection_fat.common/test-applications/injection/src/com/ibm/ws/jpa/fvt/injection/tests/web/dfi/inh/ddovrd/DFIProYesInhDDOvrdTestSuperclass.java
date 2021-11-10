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

package com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Field
 * Field/Method Protection: Protected
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public abstract class DFIProYesInhDDOvrdTestSuperclass extends JPATestServlet {
    private static final long serialVersionUID = -6348756387008245182L;

    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    protected EntityManager em_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION)
    protected EntityManager em_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    protected EntityManager em_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    protected EntityManager em_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA")
    protected EntityManagerFactory emf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA")
    protected EntityManagerFactory emf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    protected EntityManagerFactory emf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA")
    protected EntityManagerFactory emf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL")
    protected EntityManagerFactory emf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL")
    protected EntityManagerFactory emf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    protected EntityManagerFactory emf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL")
    protected EntityManagerFactory emf_amrl_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdem_cmts_common_webapp")
    protected EntityManager ovdem_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdem_cmts_webapp_webapp")
    protected EntityManager ovdem_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdem_cmts_common_earlib")
    protected EntityManager ovdem_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    protected EntityManager ovdem_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amjta_common_webapp")
    protected EntityManagerFactory ovdemf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    protected EntityManagerFactory ovdemf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amjta_common_earlib")
    protected EntityManagerFactory ovdemf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    protected EntityManagerFactory ovdemf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amrl_common_webapp")
    protected EntityManagerFactory ovdemf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    protected EntityManagerFactory ovdemf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amrl_common_earlib")
    protected EntityManagerFactory ovdemf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DFIProYesInhDDOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    protected EntityManagerFactory ovdemf_amrl_jpalib_earlib;

}
