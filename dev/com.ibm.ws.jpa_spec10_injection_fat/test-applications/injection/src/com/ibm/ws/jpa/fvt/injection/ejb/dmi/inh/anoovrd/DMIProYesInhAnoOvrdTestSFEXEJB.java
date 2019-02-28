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

package com.ibm.ws.jpa.fvt.injection.ejb.dmi.inh.anoovrd;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

@Stateful(name = "DMIProYesInhAnoOvrdTestSFEXEJB")
@Local(DMIProYesInhAnoOvrdTestSFEXEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DMIProYesInhAnoOvrdTestSFEXEJB extends DMIProYesInhAnoOvrdTestEXSuperclass {
    // This EntityManager should refer to the COMMON_JTA in the EJB module
    @Override
    public EntityManager getOvdem_cmex_common_ejb() {
        return ovdem_cmex_common_ejb;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIProYesInhAnoOvrdTestEJB/ovdem_cmex_common_ejb")
    protected void setOvdem_cmex_common_ejb(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmex_common_ejb = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the EJB_JTA in the EJB module
    @Override
    public EntityManager getOvdem_cmex_ejb_ejb() {
        return ovdem_cmex_ejb_ejb;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_EJB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIProYesInhAnoOvrdTestEJB/ovdem_cmex_ejb_ejb")
    protected void setOvdem_cmex_ejb_ejb(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmex_ejb_ejb = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmex_common_earlib() {
        return ovdem_cmex_common_earlib;
    }

    @Override
    @PersistenceContext(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIProYesInhAnoOvrdTestEJB/ovdem_cmex_common_earlib")
    protected void setOvdem_cmex_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmex_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmex_jpalib_earlib() {
        return ovdem_cmex_jpalib_earlib;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_JPALIB_JTA", type = PersistenceContextType.EXTENDED,
                        name = "jpa/DMIProYesInhAnoOvrdTestEJB/ovdem_cmex_jpalib_earlib")
    protected void setOvdem_cmex_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmex_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    @Override
    @Remove
    public void release() {

    }
}
