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

package com.ibm.ws.jpa.fvt.injection.ejb.dmi.noinh;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateless(name = "DMIPubNoInhSLEJB")
@Local(DMIPubNoInhSLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DMIPubNoInhSLEJB extends BMTEJBTestVehicle {
    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    private EntityManager em_cmts_common_ejb;
    private EntityManager em_cmts_ejb_ejb;
    private EntityManager em_cmts_common_earlib;
    private EntityManager em_cmts_common_earroot;
    private EntityManager em_cmts_jpalib_earlib;
    private EntityManager em_cmts_jparoot_earroot;

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

    private EntityManager ovdem_cmts_common_ejb;
    private EntityManager ovdem_cmts_ejb_ejb;
    private EntityManager ovdem_cmts_common_earlib;
    private EntityManager ovdem_cmts_common_earroot;
    private EntityManager ovdem_cmts_jpalib_earlib;
    private EntityManager ovdem_cmts_jparoot_earroot;

    // Application Managed Persistence Unit, JTA-Transaction

    private EntityManagerFactory ovdemf_amjta_common_ejb;
    private EntityManagerFactory ovdemf_amjta_ejb_ejb;
    private EntityManagerFactory ovdemf_amjta_common_earlib;
    private EntityManagerFactory ovdemf_amjta_common_earroot;
    private EntityManagerFactory ovdemf_amjta_jpalib_earlib;
    private EntityManagerFactory ovdemf_amjta_jparoot_earroot;

    // Application Managed Persistence Unit, RL-Transaction

    private EntityManagerFactory ovdemf_amrl_common_ejb;
    private EntityManagerFactory ovdemf_amrl_ejb_ejb;
    private EntityManagerFactory ovdemf_amrl_common_earlib;
    private EntityManagerFactory ovdemf_amrl_common_earroot;
    private EntityManagerFactory ovdemf_amrl_jpalib_earlib;
    private EntityManagerFactory ovdemf_amrl_jparoot_earroot;

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getEm_cmts_common_ejb() {
        return em_cmts_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public void setEm_cmts_common_ejb(EntityManager emCmtsCommonWebapp) {
        em_cmts_common_ejb = emCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManager getEm_cmts_ejb_ejb() {
        return em_cmts_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.TRANSACTION)
    public void setEm_cmts_ejb_ejb(EntityManager emCmtsWebappWebapp) {
        em_cmts_ejb_ejb = emCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmts_common_earlib() {
        return em_cmts_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public void setEm_cmts_common_earlib(EntityManager emCmtsCommonEarlib) {
        em_cmts_common_earlib = emCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmts_jpalib_earlib() {
        return em_cmts_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    public void setEm_cmts_jpalib_earlib(EntityManager emCmtsJpalibEarlib) {
        em_cmts_jpalib_earlib = emCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManagerFactory getEmf_amjta_common_ejb() {
        return emf_amjta_common_ejb;
    }

    @PersistenceUnit(unitName = "COMMON_JTA")
    public void setEmf_amjta_common_ejb(EntityManagerFactory emfAmjtaCommonWebapp) {
        emf_amjta_common_ejb = emfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManagerFactory getEmf_amjta_ejb_ejb() {
        return emf_amjta_ejb_ejb;
    }

    @PersistenceUnit(unitName = "EJB_JTA")
    public void setEmf_amjta_ejb_ejb(EntityManagerFactory emfAmjtaWebappWebapp) {
        emf_amjta_ejb_ejb = emfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amjta_common_earlib() {
        return emf_amjta_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    public void setEmf_amjta_common_earlib(EntityManagerFactory emfAmjtaCommonEarlib) {
        emf_amjta_common_earlib = emfAmjtaCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amjta_jpalib_earlib() {
        return emf_amjta_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_JTA")
    public void setEmf_amjta_jpalib_earlib(EntityManagerFactory emfAmjtaJpalibEarlib) {
        emf_amjta_jpalib_earlib = emfAmjtaJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    public EntityManagerFactory getEmf_amrl_common_ejb() {
        return emf_amrl_common_ejb;
    }

    @PersistenceUnit(unitName = "COMMON_RL")
    public void setEmf_amrl_common_ejb(EntityManagerFactory emfAmrlCommonWebapp) {
        emf_amrl_common_ejb = emfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the EJB_RL in the Web App module
    public EntityManagerFactory getEmf_amrl_ejb_ejb() {
        return emf_amrl_ejb_ejb;
    }

    @PersistenceUnit(unitName = "EJB_RL")
    public void setEmf_amrl_ejb_ejb(EntityManagerFactory emfAmrlWebappWebapp) {
        emf_amrl_ejb_ejb = emfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amrl_common_earlib() {
        return emf_amrl_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    public void setEmf_amrl_common_earlib(EntityManagerFactory emfAmrlCommonEarlib) {
        emf_amrl_common_earlib = emfAmrlCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amrl_jpalib_earlib() {
        return emf_amrl_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_RL")
    public void setEmf_amrl_jpalib_earlib(EntityManagerFactory emfAmrlJpalibEarlib) {
        emf_amrl_jpalib_earlib = emfAmrlJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getOvdem_cmts_common_ejb() {
        return ovdem_cmts_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmts_common_ejb")
    public void setOvdem_cmts_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmts_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManager getOvdem_cmts_ejb_ejb() {
        return ovdem_cmts_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmts_ejb_ejb")
    public void setOvdem_cmts_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmts_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmts_common_earlib() {
        return ovdem_cmts_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmts_common_earlib")
    public void setOvdem_cmts_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmts_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmts_jpalib_earlib() {
        return ovdem_cmts_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubNoInhTestEJB/ovdem_cmts_jpalib_earlib")
    public void setOvdem_cmts_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmts_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManagerFactory getOvdemf_amjta_common_ejb() {
        return ovdemf_amjta_common_ejb;
    }

    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amjta_common_ejb")
    public void setOvdemf_amjta_common_ejb(EntityManagerFactory ovdemfAmjtaCommonWebapp) {
        ovdemf_amjta_common_ejb = ovdemfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the Web App module
    public EntityManagerFactory getOvdemf_amjta_ejb_ejb() {
        return ovdemf_amjta_ejb_ejb;
    }

    @PersistenceUnit(unitName = "EJB_JTA", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amjta_ejb_ejb")
    public void setOvdemf_amjta_ejb_ejb(
                                        EntityManagerFactory ovdemfAmjtaWebappWebapp) {
        ovdemf_amjta_ejb_ejb = ovdemfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amjta_common_earlib() {
        return ovdemf_amjta_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amjta_common_earlib")
    public void setOvdemf_amjta_common_earlib(
                                              EntityManagerFactory ovdemfAmjtaCommonEarlib) {
        ovdemf_amjta_common_earlib = ovdemfAmjtaCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amjta_jpalib_earlib() {
        return ovdemf_amjta_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amjta_jpalib_earlib")
    public void setOvdemf_amjta_jpalib_earlib(
                                              EntityManagerFactory ovdemfAmjtaJpalibEarlib) {
        ovdemf_amjta_jpalib_earlib = ovdemfAmjtaJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    public EntityManagerFactory getOvdemf_amrl_common_ejb() {
        return ovdemf_amrl_common_ejb;
    }

    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amrl_common_ejb")
    public void setOvdemf_amrl_common_ejb(
                                          EntityManagerFactory ovdemfAmrlCommonWebapp) {
        ovdemf_amrl_common_ejb = ovdemfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the EJB_RL in the Web App module
    public EntityManagerFactory getOvdemf_amrl_ejb_ejb() {
        return ovdemf_amrl_ejb_ejb;
    }

    @PersistenceUnit(unitName = "EJB_RL", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amrl_ejb_ejb")
    public void setOvdemf_amrl_ejb_ejb(
                                       EntityManagerFactory ovdemfAmrlWebappWebapp) {
        ovdemf_amrl_ejb_ejb = ovdemfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amrl_common_earlib() {
        return ovdemf_amrl_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amrl_common_earlib")
    public void setOvdemf_amrl_common_earlib(
                                             EntityManagerFactory ovdemfAmrlCommonEarlib) {
        ovdemf_amrl_common_earlib = ovdemfAmrlCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amrl_jpalib_earlib() {
        return ovdemf_amrl_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DMIPubNoInhTestEJB/ovdemf_amrl_jpalib_earlib")
    public void setOvdemf_amrl_jpalib_earlib(
                                             EntityManagerFactory ovdemfAmrlJpalibEarlib) {
        ovdemf_amrl_jpalib_earlib = ovdemfAmrlJpalibEarlib;
    }
}
