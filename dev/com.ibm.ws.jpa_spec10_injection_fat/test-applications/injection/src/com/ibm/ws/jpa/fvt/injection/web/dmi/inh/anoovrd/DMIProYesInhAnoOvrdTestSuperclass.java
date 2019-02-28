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

package com.ibm.ws.jpa.fvt.injection.web.dmi.inh.anoovrd;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.jpa.fvt.injection.web.dmi.inh.DMIInhCommonServletSuperclass;

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Method
 * Field/Method Protection: Protected
 * Inheritance: Yes, Annotation Override of Superclass Injection Methods
 *
 *
 */
public abstract class DMIProYesInhAnoOvrdTestSuperclass extends DMIInhCommonServletSuperclass {
    private static final long serialVersionUID = -1820592768144941204L;

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getEm_cmts_common_webapp() {
        return em_cmts_common_webapp;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    protected void setEm_cmts_common_webapp(EntityManager emCmtsCommonWebapp) {
        em_cmts_common_webapp = emCmtsCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    public EntityManager getEm_cmts_webapp_webapp() {
        return em_cmts_webapp_webapp;
    }

    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION)
    protected void setEm_cmts_webapp_webapp(EntityManager emCmtsWebappWebapp) {
        em_cmts_webapp_webapp = emCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmts_common_earlib() {
        return em_cmts_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    protected void setEm_cmts_common_earlib(EntityManager emCmtsCommonEarlib) {
        em_cmts_common_earlib = emCmtsCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManager getEm_cmts_common_earroot() {
//        return em_cmts_common_earroot;
//    }
//
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION)
//    protected void setEm_cmts_common_earroot(EntityManager emCmtsCommonEarroot) {
//        em_cmts_common_earroot = emCmtsCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmts_jpalib_earlib() {
        return em_cmts_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    protected void setEm_cmts_jpalib_earlib(EntityManager emCmtsJpalibEarlib) {
        em_cmts_jpalib_earlib = emCmtsJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManager getEm_cmts_jparoot_earroot() {
//        return em_cmts_jparoot_earroot;
//    }
//
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION)
//    protected void setEm_cmts_jparoot_earroot(EntityManager emCmtsJparootEarroot) {
//        em_cmts_jparoot_earroot = emCmtsJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManagerFactory getEmf_amjta_common_webapp() {
        return emf_amjta_common_webapp;
    }

    @PersistenceUnit(unitName = "COMMON_JTA")
    protected void setEmf_amjta_common_webapp(EntityManagerFactory emfAmjtaCommonWebapp) {
        emf_amjta_common_webapp = emfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    public EntityManagerFactory getEmf_amjta_webapp_webapp() {
        return emf_amjta_webapp_webapp;
    }

    @PersistenceUnit(unitName = "WEBAPP_JTA")
    protected void setEmf_amjta_webapp_webapp(EntityManagerFactory emfAmjtaWebappWebapp) {
        emf_amjta_webapp_webapp = emfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amjta_common_earlib() {
        return emf_amjta_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    protected void setEmf_amjta_common_earlib(EntityManagerFactory emfAmjtaCommonEarlib) {
        emf_amjta_common_earlib = emfAmjtaCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getEmf_amjta_common_earroot() {
//        return emf_amjta_common_earroot;
//    }
//
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA")
//    protected void setEmf_amjta_common_earroot(
//            EntityManagerFactory emfAmjtaCommonEarroot) {
//        emf_amjta_common_earroot = emfAmjtaCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amjta_jpalib_earlib() {
        return emf_amjta_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_JTA")
    protected void setEmf_amjta_jpalib_earlib(EntityManagerFactory emfAmjtaJpalibEarlib) {
        emf_amjta_jpalib_earlib = emfAmjtaJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getEmf_amjta_jparoot_earroot() {
//        return emf_amjta_jparoot_earroot;
//    }
//
//    @PersistenceUnit(unitName="JPAROOT_JTA")
//    protected void setEmf_amjta_jparoot_earroot(
//            EntityManagerFactory emfAmjtaJparootEarroot) {
//        emf_amjta_jparoot_earroot = emfAmjtaJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    public EntityManagerFactory getEmf_amrl_common_webapp() {
        return emf_amrl_common_webapp;
    }

    @PersistenceUnit(unitName = "COMMON_RL")
    protected void setEmf_amrl_common_webapp(EntityManagerFactory emfAmrlCommonWebapp) {
        emf_amrl_common_webapp = emfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    public EntityManagerFactory getEmf_amrl_webapp_webapp() {
        return emf_amrl_webapp_webapp;
    }

    @PersistenceUnit(unitName = "WEBAPP_RL")
    protected void setEmf_amrl_webapp_webapp(EntityManagerFactory emfAmrlWebappWebapp) {
        emf_amrl_webapp_webapp = emfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amrl_common_earlib() {
        return emf_amrl_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    protected void setEmf_amrl_common_earlib(EntityManagerFactory emfAmrlCommonEarlib) {
        emf_amrl_common_earlib = emfAmrlCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getEmf_amrl_common_earroot() {
//        return emf_amrl_common_earroot;
//    }
//
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL")
//    protected void setEmf_amrl_common_earroot(EntityManagerFactory emfAmrlCommonEarroot) {
//        emf_amrl_common_earroot = emfAmrlCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    public EntityManagerFactory getEmf_amrl_jpalib_earlib() {
        return emf_amrl_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_RL")
    protected void setEmf_amrl_jpalib_earlib(EntityManagerFactory emfAmrlJpalibEarlib) {
        emf_amrl_jpalib_earlib = emfAmrlJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getEmf_amrl_jparoot_earroot() {
//        return emf_amrl_jparoot_earroot;
//    }
//
//    @PersistenceUnit(unitName="JPAROOT_RL")
//    protected void setEmf_amrl_jparoot_earroot(EntityManagerFactory emfAmrlJparootEarroot) {
//        emf_amrl_jparoot_earroot = emfAmrlJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManager getOvdem_cmts_common_webapp() {
        return ovdem_cmts_common_webapp;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_common_webapp")
    protected void setOvdem_cmts_common_webapp(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmts_common_webapp = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    public EntityManager getOvdem_cmts_webapp_webapp() {
        return ovdem_cmts_webapp_webapp;
    }

    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_webapp_webapp")
    protected void setOvdem_cmts_webapp_webapp(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmts_webapp_webapp = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmts_common_earlib() {
        return ovdem_cmts_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_common_earlib")
    protected void setOvdem_cmts_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmts_common_earlib = ovdemCmtsCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManager getOvdem_cmts_common_earroot() {
//        return ovdem_cmts_common_earroot;
//    }
//
//    @PersistenceContext(unitName="../jpapuroot.jar#COMMON_JTA", type=PersistenceContextType.TRANSACTION,
//            name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_common_earroot")
//    protected void setOvdem_cmts_common_earroot(EntityManager ovdemCmtsCommonEarroot) {
//        ovdem_cmts_common_earroot = ovdemCmtsCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmts_jpalib_earlib() {
        return ovdem_cmts_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    protected void setOvdem_cmts_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmts_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManager getOvdem_cmts_jparoot_earroot() {
//        return ovdem_cmts_jparoot_earroot;
//    }
//
//    @PersistenceContext(unitName="JPAROOT_JTA", type=PersistenceContextType.TRANSACTION,
//            name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_jparoot_earroot")
//    protected void setOvdem_cmts_jparoot_earroot(EntityManager ovdemCmtsJparootEarroot) {
//        ovdem_cmts_jparoot_earroot = ovdemCmtsJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    public EntityManagerFactory getOvdemf_amjta_common_webapp() {
        return ovdemf_amjta_common_webapp;
    }

    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_common_webapp")
    protected void setOvdemf_amjta_common_webapp(EntityManagerFactory ovdemfAmjtaCommonWebapp) {
        ovdemf_amjta_common_webapp = ovdemfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    public EntityManagerFactory getOvdemf_amjta_webapp_webapp() {
        return ovdemf_amjta_webapp_webapp;
    }

    @PersistenceUnit(unitName = "WEBAPP_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    protected void setOvdemf_amjta_webapp_webapp(
                                                 EntityManagerFactory ovdemfAmjtaWebappWebapp) {
        ovdemf_amjta_webapp_webapp = ovdemfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amjta_common_earlib() {
        return ovdemf_amjta_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_common_earlib")
    protected void setOvdemf_amjta_common_earlib(
                                                 EntityManagerFactory ovdemfAmjtaCommonEarlib) {
        ovdemf_amjta_common_earlib = ovdemfAmjtaCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amjta_common_earroot() {
//        return ovdemf_amjta_common_earroot;
//    }
//
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_JTA", name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_common_earroot")
//    protected void setOvdemf_amjta_common_earroot(
//            EntityManagerFactory ovdemfAmjtaCommonEarroot) {
//        ovdemf_amjta_common_earroot = ovdemfAmjtaCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amjta_jpalib_earlib() {
        return ovdemf_amjta_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    protected void setOvdemf_amjta_jpalib_earlib(
                                                 EntityManagerFactory ovdemfAmjtaJpalibEarlib) {
        ovdemf_amjta_jpalib_earlib = ovdemfAmjtaJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amjta_jparoot_earroot() {
//        return ovdemf_amjta_jparoot_earroot;
//    }
//
//    @PersistenceUnit(unitName="JPAROOT_JTA", name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_jparoot_earroot")
//    protected void setOvdemf_amjta_jparoot_earroot(
//            EntityManagerFactory ovdemfAmjtaJparootEarroot) {
//        ovdemf_amjta_jparoot_earroot = ovdemfAmjtaJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    public EntityManagerFactory getOvdemf_amrl_common_webapp() {
        return ovdemf_amrl_common_webapp;
    }

    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_common_webapp")
    protected void setOvdemf_amrl_common_webapp(
                                                EntityManagerFactory ovdemfAmrlCommonWebapp) {
        ovdemf_amrl_common_webapp = ovdemfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    public EntityManagerFactory getOvdemf_amrl_webapp_webapp() {
        return ovdemf_amrl_webapp_webapp;
    }

    @PersistenceUnit(unitName = "WEBAPP_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    protected void setOvdemf_amrl_webapp_webapp(
                                                EntityManagerFactory ovdemfAmrlWebappWebapp) {
        ovdemf_amrl_webapp_webapp = ovdemfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amrl_common_earlib() {
        return ovdemf_amrl_common_earlib;
    }

    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_common_earlib")
    protected void setOvdemf_amrl_common_earlib(
                                                EntityManagerFactory ovdemfAmrlCommonEarlib) {
        ovdemf_amrl_common_earlib = ovdemfAmrlCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amrl_common_earroot() {
//        return ovdemf_amrl_common_earroot;
//    }
//
//    @PersistenceUnit(unitName="../jpapuroot.jar#COMMON_RL", name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_common_earroot")
//    protected void setOvdemf_amrl_common_earroot(
//            EntityManagerFactory ovdemfAmrlCommonEarroot) {
//        ovdemf_amrl_common_earroot = ovdemfAmrlCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    public EntityManagerFactory getOvdemf_amrl_jpalib_earlib() {
        return ovdemf_amrl_jpalib_earlib;
    }

    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    protected void setOvdemf_amrl_jpalib_earlib(
                                                EntityManagerFactory ovdemfAmrlJpalibEarlib) {
        ovdemf_amrl_jpalib_earlib = ovdemfAmrlJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amrl_jparoot_earroot() {
//        return ovdemf_amrl_jparoot_earroot;
//    }
//
//    @PersistenceUnit(unitName="JPAROOT_RL", name="jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_jparoot_earroot")
//    protected void setOvdemf_amrl_jparoot_earroot(
//            EntityManagerFactory ovdemfAmrlJparootEarroot) {
//        ovdemf_amrl_jparoot_earroot = ovdemfAmrlJparootEarroot;
//    }
}
