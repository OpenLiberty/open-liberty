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

package com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

public abstract class DMIInhCommonServletSuperclass extends JPATestServlet {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    /**
     *
     */
    private static final long serialVersionUID = 4012862680805874068L;
    protected EntityManager em_cmts_common_webapp;
    protected EntityManager em_cmts_webapp_webapp;
    protected EntityManager em_cmts_common_earlib;
    protected EntityManager em_cmts_common_earroot;
    protected EntityManager em_cmts_jpalib_earlib;
    protected EntityManager em_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    protected EntityManagerFactory emf_amjta_common_webapp;
    protected EntityManagerFactory emf_amjta_webapp_webapp;
    protected EntityManagerFactory emf_amjta_common_earlib;
    protected EntityManagerFactory emf_amjta_common_earroot;
    protected EntityManagerFactory emf_amjta_jpalib_earlib;
    protected EntityManagerFactory emf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    protected EntityManagerFactory emf_amrl_common_webapp;
    protected EntityManagerFactory emf_amrl_webapp_webapp;
    protected EntityManagerFactory emf_amrl_common_earlib;
    protected EntityManagerFactory emf_amrl_common_earroot;
    protected EntityManagerFactory emf_amrl_jpalib_earlib;
    protected EntityManagerFactory emf_amrl_jparoot_earroot;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    protected EntityManager ovdem_cmts_common_webapp;
    protected EntityManager ovdem_cmts_webapp_webapp;
    protected EntityManager ovdem_cmts_common_earlib;
    protected EntityManager ovdem_cmts_common_earroot;
    protected EntityManager ovdem_cmts_jpalib_earlib;
    protected EntityManager ovdem_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    protected EntityManagerFactory ovdemf_amjta_common_webapp;
    protected EntityManagerFactory ovdemf_amjta_webapp_webapp;
    protected EntityManagerFactory ovdemf_amjta_common_earlib;
    protected EntityManagerFactory ovdemf_amjta_common_earroot;
    protected EntityManagerFactory ovdemf_amjta_jpalib_earlib;
    protected EntityManagerFactory ovdemf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    protected EntityManagerFactory ovdemf_amrl_common_webapp;
    protected EntityManagerFactory ovdemf_amrl_webapp_webapp;
    protected EntityManagerFactory ovdemf_amrl_common_earlib;
    protected EntityManagerFactory ovdemf_amrl_common_earroot;
    protected EntityManagerFactory ovdemf_amrl_jpalib_earlib;
    protected EntityManagerFactory ovdemf_amrl_jparoot_earroot;
}
