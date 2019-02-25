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

package com.ibm.ws.jpa.fvt.injection.web.dfi.inh.ddovrd;

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
 * Field/Method Protection: Private
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public abstract class DFIPriYesInhDDOvrdTestSuperclass extends JPATestServlet {
    private static final long serialVersionUID = 6463928897417476980L;

    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    private EntityManager em_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION)
    private EntityManager em_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    private EntityManager em_cmts_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION)
//    private EntityManager em_cmts_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    private EntityManager em_cmts_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION)
//    private EntityManager em_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "COMMON_JTA")
    private EntityManagerFactory emf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "WEBAPP_JTA")
    private EntityManagerFactory emf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    private EntityManagerFactory emf_amjta_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA")
//    private EntityManagerFactory emf_amjta_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "JPALIB_JTA")
    private EntityManagerFactory emf_amjta_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="JPAROOT_JTA")
//    private EntityManagerFactory emf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "COMMON_RL")
    private EntityManagerFactory emf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "WEBAPP_RL")
    private EntityManagerFactory emf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    private EntityManagerFactory emf_amrl_common_earlib;

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL")
//    private EntityManagerFactory emf_amrl_common_earroot;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "JPALIB_RL")
    private EntityManagerFactory emf_amrl_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="JPAROOT_RL")
//    private EntityManagerFactory emf_amrl_jparoot_earroot;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_common_webapp")
    private EntityManager ovdem_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_webapp_webapp")
    private EntityManager ovdem_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_common_earlib")
    private EntityManager ovdem_cmts_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION,
//                        name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_common_earroot")
//    private EntityManager ovdem_cmts_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    private EntityManager ovdem_cmts_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION,
//                        name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdem_cmts_jparoot_earroot")
//    private EntityManager ovdem_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_common_webapp")
    private EntityManagerFactory ovdemf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "WEBAPP_JTA", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    private EntityManagerFactory ovdemf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_common_earlib")
    private EntityManagerFactory ovdemf_amjta_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA", name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_common_earroot")
//    private EntityManagerFactory ovdemf_amjta_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    private EntityManagerFactory ovdemf_amjta_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="JPAROOT_JTA", name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amjta_jparoot_earroot")
//    private EntityManagerFactory ovdemf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_common_webapp")
    private EntityManagerFactory ovdemf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "WEBAPP_RL", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    private EntityManagerFactory ovdemf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_common_earlib")
    private EntityManagerFactory ovdemf_amrl_common_earlib;

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL", name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_common_earroot")
//    private EntityManagerFactory ovdemf_amrl_common_earroot;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    private EntityManagerFactory ovdemf_amrl_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="JPAROOT_RL", name="jpa/DFIPriYesInhDDOvrdTestServlet/ovdemf_amrl_jparoot_earroot")
//    private EntityManagerFactory ovdemf_amrl_jparoot_earroot;
}
