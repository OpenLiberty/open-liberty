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

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Method
 * Field/Method Protection: Private
 * Inheritance: Yes, Annotation Override of Superclass Injection Methods
 *
 *
 */
public class DMIPriYesInhAnoOvrdTestServlet extends DMIPriYesInhAnoOvrdTestSuperclass {
    private static final long serialVersionUID = -2099717902840185172L;

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION) //,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_common_webapp")
    private void setOvdem_cmts_common_webapp(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmts_common_webapp = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @Override
    public EntityManager getOvdem_cmts_webapp_webapp() {
        return ovdem_cmts_webapp_webapp;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_WEBAPP_JTA", type = PersistenceContextType.TRANSACTION) //,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_webapp_webapp")
    private void setOvdem_cmts_webapp_webapp(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmts_webapp_webapp = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_common_earlib() {
        return ovdem_cmts_common_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION) //,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_common_earlib")
    private void setOvdem_cmts_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmts_common_earlib = ovdemCmtsCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManager getOvdem_cmts_common_earroot() {
//        return ovdem_cmts_common_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="../jpapuroot.jar#OVRD_COMMON_JTA", type=PersistenceContextType.TRANSACTION,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_common_earroot")
//    private void setOvdem_cmts_common_earroot(EntityManager ovdemCmtsCommonEarroot) {
//        ovdem_cmts_common_earroot = ovdemCmtsCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_jpalib_earlib() {
        return ovdem_cmts_jpalib_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_JPALIB_JTA", type = PersistenceContextType.TRANSACTION) //,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    private void setOvdem_cmts_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmts_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManager getOvdem_cmts_jparoot_earroot() {
//        return ovdem_cmts_jparoot_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceContext(unitName="OVRD_JPAROOT_JTA", type=PersistenceContextType.TRANSACTION,
//            name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdem_cmts_jparoot_earroot")
//    private void setOvdem_cmts_jparoot_earroot(EntityManager ovdemCmtsJparootEarroot) {
//        ovdem_cmts_jparoot_earroot = ovdemCmtsJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_webapp() {
        return ovdemf_amjta_common_webapp;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_COMMON_JTA") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_common_webapp")
    private void setOvdemf_amjta_common_webapp(EntityManagerFactory ovdemfAmjtaCommonWebapp) {
        ovdemf_amjta_common_webapp = ovdemfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amjta_webapp_webapp() {
        return ovdemf_amjta_webapp_webapp;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_WEBAPP_JTA") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    private void setOvdemf_amjta_webapp_webapp(EntityManagerFactory ovdemfAmjtaWebappWebapp) {
        ovdemf_amjta_webapp_webapp = ovdemfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_earlib() {
        return ovdemf_amjta_common_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_common_earlib")
    private void setOvdemf_amjta_common_earlib(EntityManagerFactory ovdemfAmjtaCommonEarlib) {
        ovdemf_amjta_common_earlib = ovdemfAmjtaCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amjta_common_earroot() {
//        return ovdemf_amjta_common_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#OVRD_COMMON_JTA", name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_common_earroot")
//    private void setOvdemf_amjta_common_earroot(EntityManagerFactory ovdemfAmjtaCommonEarroot) {
//        ovdemf_amjta_common_earroot = ovdemfAmjtaCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_jpalib_earlib() {
        return ovdemf_amjta_jpalib_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_JPALIB_JTA") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    private void setOvdemf_amjta_jpalib_earlib(EntityManagerFactory ovdemfAmjtaJpalibEarlib) {
        ovdemf_amjta_jpalib_earlib = ovdemfAmjtaJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_JTA in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amjta_jparoot_earroot() {
//        return ovdemf_amjta_jparoot_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="OVRD_JPAROOT_JTA", name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amjta_jparoot_earroot")
//    private void setOvdemf_amjta_jparoot_earroot(EntityManagerFactory ovdemfAmjtaJparootEarroot) {
//        ovdemf_amjta_jparoot_earroot = ovdemfAmjtaJparootEarroot;
//    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_webapp() {
        return ovdemf_amrl_common_webapp;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_COMMON_RL") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_common_webapp")
    private void setOvdemf_amrl_common_webapp(EntityManagerFactory ovdemfAmrlCommonWebapp) {
        ovdemf_amrl_common_webapp = ovdemfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amrl_webapp_webapp() {
        return ovdemf_amrl_webapp_webapp;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_WEBAPP_RL") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    private void setOvdemf_amrl_webapp_webapp(EntityManagerFactory ovdemfAmrlWebappWebapp) {
        ovdemf_amrl_webapp_webapp = ovdemfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_earlib() {
        return ovdemf_amrl_common_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_RL") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_common_earlib")
    private void setOvdemf_amrl_common_earlib(EntityManagerFactory ovdemfAmrlCommonEarlib) {
        ovdemf_amrl_common_earlib = ovdemfAmrlCommonEarlib;
    }

//    // This EntityManager should refer to the COMMON_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amrl_common_earroot() {
//        return ovdemf_amrl_common_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="../jpapuroot.jar#OVRD_COMMON_RL", name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_common_earroot")
//    private void setOvdemf_amrl_common_earroot(EntityManagerFactory ovdemfAmrlCommonEarroot) {
//        ovdemf_amrl_common_earroot = ovdemfAmrlCommonEarroot;
//    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_jpalib_earlib() {
        return ovdemf_amrl_jpalib_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceUnit(unitName = "OVRD_JPALIB_RL") //, name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    private void setOvdemf_amrl_jpalib_earlib(EntityManagerFactory ovdemfAmrlJpalibEarlib) {
        ovdemf_amrl_jpalib_earlib = ovdemfAmrlJpalibEarlib;
    }

//    // This EntityManager should refer to the JPAROOT_RL in the jar in the EAR file's root directory
//    public EntityManagerFactory getOvdemf_amrl_jparoot_earroot() {
//        return ovdemf_amrl_jparoot_earroot;
//    }
//
//    @SuppressWarnings("unused")
//    @PersistenceUnit(unitName="OVRD_JPAROOT_RL", name="jpa/DMIPriYesInhAnoOvrdTestServlet/ovdemf_amrl_jparoot_earroot")
//    private void setOvdemf_amrl_jparoot_earroot(EntityManagerFactory ovdemfAmrlJparootEarroot) {
//        ovdemf_amrl_jparoot_earroot = ovdemfAmrlJparootEarroot;
//    }
}
