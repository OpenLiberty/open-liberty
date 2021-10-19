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

package com.ibm.ws.jpa.fvt.injection.ejb.jndi;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "AnnotatedJNDISFEJB")
@Local(AnnotatedJNDISLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
@PersistenceUnits({
                    // Persistence Units defined in the webapp
                    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/ejb/jndi/ano/ejb/common_jta"),
                    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/ejb/jndi/ano/ejb/common_rl"),
                    @PersistenceUnit(unitName = "EJB_JTA", name = "jpa/ejb/jndi/ano/ejb/ejb_jta"),
                    @PersistenceUnit(unitName = "EJB_RL", name = "jpa/ejb/jndi/ano/ejb/ejb_rl"),

                    // Persistence Units defined in the application's library jar
                    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/ejb/jndi/ano/earlib/common_jta"),
                    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/ejb/jndi/ano/earlib/common_rl"),
                    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/ejb/jndi/ano/earlib/jpalib_jta"),
                    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/ejb/jndi/ano/earlib/jpalib_rl"),
})
@PersistenceContexts({
                       // Persistence Units defined in the ejb
                       @PersistenceContext(unitName = "COMMON_JTA", name = "jpa/ejb/jndi/ano/ejb/common_cmts"),
                       @PersistenceContext(unitName = "EJB_JTA", name = "jpa/ejb/jndi/ano/ejb/ejb_cmts"),

                       // Persistence Units defined in the application's library jar
                       @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/ejb/jndi/ano/earlib/common_cmts"),
                       @PersistenceContext(unitName = "JPALIB_JTA", name = "jpa/ejb/jndi/ano/earlib/jpalib_cmts"),

                       // Cleanup Persistence Context
                       @PersistenceContext(unitName = "CLEANUP", name = "jpa/ejb/jndi/ano/cleanup_cmts")
})
public class AnnotatedJNDISFEJB extends BMTEJBTestVehicle {
    @Override
    @Remove
    public void release() {

    }
}
