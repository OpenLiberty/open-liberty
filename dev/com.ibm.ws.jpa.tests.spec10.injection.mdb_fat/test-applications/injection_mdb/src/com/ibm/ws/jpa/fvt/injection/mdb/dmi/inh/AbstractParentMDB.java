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

package com.ibm.ws.jpa.fvt.injection.mdb.dmi.inh;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.ws.jpa.fvt.injection.mdb.AbstractTestMDB;

/**
 *
 */
public abstract class AbstractParentMDB extends AbstractTestMDB {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    protected EntityManager em_cmts_common_ejb;
    protected EntityManager em_cmts_ejb_ejb;
    protected EntityManager em_cmts_common_earlib;
    protected EntityManager em_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    protected EntityManagerFactory emf_amjta_common_ejb;
    protected EntityManagerFactory emf_amjta_ejb_ejb;
    protected EntityManagerFactory emf_amjta_common_earlib;
    protected EntityManagerFactory emf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    protected EntityManagerFactory emf_amrl_common_ejb;
    protected EntityManagerFactory emf_amrl_ejb_ejb;
    protected EntityManagerFactory emf_amrl_common_earlib;
    protected EntityManagerFactory emf_amrl_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    protected EntityManager ovdem_cmts_common_ejb;
    protected EntityManager ovdem_cmts_ejb_ejb;
    protected EntityManager ovdem_cmts_common_earlib;
    protected EntityManager ovdem_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    protected EntityManagerFactory ovdemf_amjta_common_ejb;
    protected EntityManagerFactory ovdemf_amjta_ejb_ejb;
    protected EntityManagerFactory ovdemf_amjta_common_earlib;
    protected EntityManagerFactory ovdemf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    protected EntityManagerFactory ovdemf_amrl_common_ejb;
    protected EntityManagerFactory ovdemf_amrl_ejb_ejb;
    protected EntityManagerFactory ovdemf_amrl_common_earlib;
    protected EntityManagerFactory ovdemf_amrl_jpalib_earlib;
}
