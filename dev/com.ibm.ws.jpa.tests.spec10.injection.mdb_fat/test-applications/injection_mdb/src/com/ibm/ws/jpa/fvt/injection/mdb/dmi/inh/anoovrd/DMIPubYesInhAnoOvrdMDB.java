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

package com.ibm.ws.jpa.fvt.injection.mdb.dmi.inh.anoovrd;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "DMIPubYesInhAnoOvrdMDB_Queue")

},
               name = "DMIPubYesInhAnoOvrdMDB")
@TransactionManagement(TransactionManagementType.BEAN)
public class DMIPubYesInhAnoOvrdMDB extends DMIPubYesInhAnoOvrdTestSuperclass {
    // This EntityManager should refer to the COMMON_JTA in the EJB module
    @Override
    public EntityManager getOvdem_cmts_common_ejb() {
        return ovdem_cmts_common_ejb;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdem_cmts_common_ejb")
    public void setOvdem_cmts_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmts_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    @Override
    public EntityManager getOvdem_cmts_ejb_ejb() {
        return ovdem_cmts_ejb_ejb;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_EJB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdem_cmts_ejb_ejb")
    public void setOvdem_cmts_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmts_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_common_earlib() {
        return ovdem_cmts_common_earlib;
    }

    @Override
    @PersistenceContext(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdem_cmts_common_earlib")
    public void setOvdem_cmts_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmts_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_jpalib_earlib() {
        return ovdem_cmts_jpalib_earlib;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdem_cmts_jpalib_earlib")
    public void setOvdem_cmts_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmts_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the EJB module
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_ejb() {
        return ovdemf_amjta_common_ejb;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_COMMON_JTA", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amjta_common_ejb")
    public void setOvdemf_amjta_common_ejb(EntityManagerFactory ovdemfAmjtaCommonWebapp) {
        ovdemf_amjta_common_ejb = ovdemfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    @Override
    public EntityManagerFactory getOvdemf_amjta_ejb_ejb() {
        return ovdemf_amjta_ejb_ejb;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_EJB_JTA", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amjta_ejb_ejb")
    public void setOvdemf_amjta_ejb_ejb(
                                        EntityManagerFactory ovdemfAmjtaWebappWebapp) {
        ovdemf_amjta_ejb_ejb = ovdemfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_earlib() {
        return ovdemf_amjta_common_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amjta_common_earlib")
    public void setOvdemf_amjta_common_earlib(
                                              EntityManagerFactory ovdemfAmjtaCommonEarlib) {
        ovdemf_amjta_common_earlib = ovdemfAmjtaCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_jpalib_earlib() {
        return ovdemf_amjta_jpalib_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_JPALIB_JTA", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amjta_jpalib_earlib")
    public void setOvdemf_amjta_jpalib_earlib(
                                              EntityManagerFactory ovdemfAmjtaJpalibEarlib) {
        ovdemf_amjta_jpalib_earlib = ovdemfAmjtaJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_RL in the EJB module
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_ejb() {
        return ovdemf_amrl_common_ejb;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_COMMON_RL", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amrl_common_ejb")
    public void setOvdemf_amrl_common_ejb(
                                          EntityManagerFactory ovdemfAmrlCommonWebapp) {
        ovdemf_amrl_common_ejb = ovdemfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the EJB_RL in the EJB module
    @Override
    public EntityManagerFactory getOvdemf_amrl_ejb_ejb() {
        return ovdemf_amrl_ejb_ejb;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_EJB_RL", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amrl_ejb_ejb")
    public void setOvdemf_amrl_ejb_ejb(
                                       EntityManagerFactory ovdemfAmrlWebappWebapp) {
        ovdemf_amrl_ejb_ejb = ovdemfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_earlib() {
        return ovdemf_amrl_common_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_RL", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amrl_common_earlib")
    public void setOvdemf_amrl_common_earlib(
                                             EntityManagerFactory ovdemfAmrlCommonEarlib) {
        ovdemf_amrl_common_earlib = ovdemfAmrlCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_jpalib_earlib() {
        return ovdemf_amrl_jpalib_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_JPALIB_RL", name = "jpa/DMIPubYesInhAnoOvrdTestEJB/ovdemf_amrl_jpalib_earlib")
    public void setOvdemf_amrl_jpalib_earlib(
                                             EntityManagerFactory ovdemfAmrlJpalibEarlib) {
        ovdemf_amrl_jpalib_earlib = ovdemfAmrlJpalibEarlib;
    }
}
