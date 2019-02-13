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
 * Field/Method Protection: Package
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public abstract class DFIPkgYesInhDDOvrdTestSuperclass extends JPATestServlet {
    private static final long serialVersionUID = -1677533931520079014L;

    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    EntityManager em_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION)
    EntityManager em_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    EntityManager em_cmts_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION)
//    EntityManager em_cmts_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    EntityManager em_cmts_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION)
//    EntityManager em_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA")
    EntityManagerFactory emf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA")
    EntityManagerFactory emf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    EntityManagerFactory emf_amjta_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA")
//    EntityManagerFactory emf_amjta_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA")
    EntityManagerFactory emf_amjta_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="JPAROOT_JTA")
//    EntityManagerFactory emf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL")
    EntityManagerFactory emf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL")
    EntityManagerFactory emf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    EntityManagerFactory emf_amrl_common_earlib;

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL")
//    EntityManagerFactory emf_amrl_common_earroot;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL")
    EntityManagerFactory emf_amrl_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="JPAROOT_RL")
//    EntityManagerFactory emf_amrl_jparoot_earroot;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_common_webapp")
    EntityManager ovdem_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_webapp_webapp")
    EntityManager ovdem_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_common_earlib")
    EntityManager ovdem_cmts_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION,
//                        name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_common_earroot")
//    EntityManager ovdem_cmts_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    EntityManager ovdem_cmts_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION,
//                        name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdem_cmts_jparoot_earroot")
//    EntityManager ovdem_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_common_webapp")
    EntityManagerFactory ovdemf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    EntityManagerFactory ovdemf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_common_earlib")
    EntityManagerFactory ovdemf_amjta_common_earlib;

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA", name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_common_earroot")
//    EntityManagerFactory ovdemf_amjta_common_earroot;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    EntityManagerFactory ovdemf_amjta_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="JPAROOT_JTA", name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amjta_jparoot_earroot")
//    EntityManagerFactory ovdemf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_common_webapp")
    EntityManagerFactory ovdemf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    EntityManagerFactory ovdemf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_common_earlib")
    EntityManagerFactory ovdemf_amrl_common_earlib;

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL", name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_common_earroot")
//    EntityManagerFactory ovdemf_amrl_common_earroot;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    EntityManagerFactory ovdemf_amrl_jpalib_earlib;

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    @PersistenceUnit(unitName="JPAROOT_RL", name="jpa/DFIPkgYesInhDDOvrdTestServlet/ovdemf_amrl_jparoot_earroot")
//    EntityManagerFactory ovdemf_amrl_jparoot_earroot;

}
