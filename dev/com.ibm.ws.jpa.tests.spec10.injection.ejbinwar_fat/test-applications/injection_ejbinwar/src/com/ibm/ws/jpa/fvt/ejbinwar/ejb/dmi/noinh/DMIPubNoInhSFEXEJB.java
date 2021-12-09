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

package com.ibm.ws.jpa.fvt.ejbinwar.ejb.dmi.noinh;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "DMIPubNoInhSFEXEJB")
@Local(DMIPubNoInhSFEXEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DMIPubNoInhSFEXEJB extends BMTEJBTestVehicle {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    private EntityManager em_cmex_common_ejb;
    private EntityManager em_cmex_ejb_ejb;
    private EntityManager em_cmex_common_earlib;
    private EntityManager em_cmex_common_earroot;
    private EntityManager em_cmex_jpalib_earlib;
    private EntityManager em_cmex_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    private EntityManagerFactory emf_amjta_common_ejb;
    private EntityManagerFactory emf_amjta_ejb_ejb;
    private EntityManagerFactory emf_amjta_common_earlib;
    private EntityManagerFactory emf_amjta_common_earroot;
    private EntityManagerFactory emf_amjta_jpalib_earlib;
    private EntityManagerFactory emf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    private EntityManagerFactory emf_amrl_common_ejb;
    private EntityManagerFactory emf_amrl_ejb_ejb;
    private EntityManagerFactory emf_amrl_common_earlib;
    private EntityManagerFactory emf_amrl_common_earroot;
    private EntityManagerFactory emf_amrl_jpalib_earlib;
    private EntityManagerFactory emf_amrl_jparoot_earroot;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    private EntityManager ovdem_cmex_common_ejb;
    private EntityManager ovdem_cmex_ejb_ejb;
    private EntityManager ovdem_cmex_common_earlib;
    private EntityManager ovdem_cmex_common_earroot;
    private EntityManager ovdem_cmex_jpalib_earlib;
    private EntityManager ovdem_cmex_jparoot_earroot;

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getEm_cmex_common_ejb() {
        return em_cmex_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED)
    public void setEm_cmex_common_ejb(EntityManager emCmtsCommonWebapp) {
        em_cmex_common_ejb = emCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManager getEm_cmex_ejb_ejb() {
        return em_cmex_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED)
    public void setEm_cmex_ejb_ejb(EntityManager emCmtsWebappWebapp) {
        em_cmex_ejb_ejb = emCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmex_common_earlib() {
        return em_cmex_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED)
    public void setEm_cmex_common_earlib(EntityManager emCmtsCommonEarlib) {
        em_cmex_common_earlib = emCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmex_jpalib_earlib() {
        return em_cmex_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED)
    public void setEm_cmex_jpalib_earlib(EntityManager emCmtsJpalibEarlib) {
        em_cmex_jpalib_earlib = emCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getOvdem_cmex_common_ejb() {
        return ovdem_cmex_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmex_common_ejb")
    public void setOvdem_cmex_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmex_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManager getOvdem_cmex_ejb_ejb() {
        return ovdem_cmex_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmex_ejb_ejb")
    public void setOvdem_cmex_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmex_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmex_common_earlib() {
        return ovdem_cmex_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmex_common_earlib")
    public void setOvdem_cmex_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmex_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmex_jpalib_earlib() {
        return ovdem_cmex_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmex_jpalib_earlib")
    public void setOvdem_cmex_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmex_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    @Override
    @Remove
    public void release() {

    }
}
