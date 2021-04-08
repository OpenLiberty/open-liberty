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

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

@Stateful(name = "DMIPriYesInhAnoOvrdTestSFEXEJB")
@Local(DMIPriYesInhAnoOvrdTestSFEXEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DMIPriYesInhAnoOvrdTestSFEXEJB extends DMIPriYesInhAnoOvrdTestEXSuperclass {
    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_COMMON_JTA", type = PersistenceContextType.EXTENDED)
    private void setOvdem_cmex_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmex_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    @Override
    public EntityManager getOvdem_cmex_ejb_ejb() {
        return ovdem_cmex_ejb_ejb;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_EJB_JTA", type = PersistenceContextType.EXTENDED)
    private void setOvdem_cmex_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmex_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmex_common_earlib() {
        return ovdem_cmex_common_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", type = PersistenceContextType.EXTENDED)
    private void setOvdem_cmex_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmex_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmex_jpalib_earlib() {
        return ovdem_cmex_jpalib_earlib;
    }

    @SuppressWarnings("unused")
    @PersistenceContext(unitName = "OVRD_JPALIB_JTA", type = PersistenceContextType.EXTENDED)
    private void setOvdem_cmex_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmex_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    @Override
    @Remove
    public void release() {

    }
}
