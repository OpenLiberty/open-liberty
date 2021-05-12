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

package com.ibm.ws.jpa.fvt.ejbinwar.ejb.dmi.inh.anoovrd;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import com.ibm.ws.jpa.fvt.ejbinwar.ejb.dmi.inh.DMIInhCommonEJBEXSuperclass;

/**
 * JPA Injection Test EJB
 *
 * Injection Type: Method
 * Field/Method Protection: Package
 * Inheritance: Yes, Annotation Override of Superclass Injection Methods
 *
 *
 */
public abstract class DMIPkgYesInhAnoOvrdTestEXSuperclass extends DMIInhCommonEJBEXSuperclass {
    // This EntityManager should refer to the COMMON_JTA in the EJB module
    public EntityManager getEm_cmex_common_ejb() {
        return em_cmex_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED)
    void setEm_cmex_common_ejb(EntityManager emCmtsCommonWebapp) {
        em_cmex_common_ejb = emCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    public EntityManager getEm_cmex_ejb_ejb() {
        return em_cmex_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED)
    void setEm_cmex_ejb_ejb(EntityManager emCmtsWebappWebapp) {
        em_cmex_ejb_ejb = emCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmex_common_earlib() {
        return em_cmex_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED)
    void setEm_cmex_common_earlib(EntityManager emCmtsCommonEarlib) {
        em_cmex_common_earlib = emCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getEm_cmex_jpalib_earlib() {
        return em_cmex_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED)
    void setEm_cmex_jpalib_earlib(EntityManager emCmtsJpalibEarlib) {
        em_cmex_jpalib_earlib = emCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the EJB module
    public EntityManager getOvdem_cmex_common_ejb() {
        return ovdem_cmex_common_ejb;
    }

    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPkgYesInhAnoOvrdTestEJB/ovdem_cmex_common_ejb")
    void setOvdem_cmex_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmex_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    public EntityManager getOvdem_cmex_ejb_ejb() {
        return ovdem_cmex_ejb_ejb;
    }

    @PersistenceContext(unitName = "EJB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPkgYesInhAnoOvrdTestEJB/ovdem_cmex_ejb_ejb")
    void setOvdem_cmex_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmex_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmex_common_earlib() {
        return ovdem_cmex_common_earlib;
    }

    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPkgYesInhAnoOvrdTestEJB/ovdem_cmex_common_earlib")
    void setOvdem_cmex_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmex_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    public EntityManager getOvdem_cmex_jpalib_earlib() {
        return ovdem_cmex_jpalib_earlib;
    }

    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIPkgYesInhAnoOvrdTestEJB/ovdem_cmex_jpalib_earlib")
    void setOvdem_cmex_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmex_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }
}
