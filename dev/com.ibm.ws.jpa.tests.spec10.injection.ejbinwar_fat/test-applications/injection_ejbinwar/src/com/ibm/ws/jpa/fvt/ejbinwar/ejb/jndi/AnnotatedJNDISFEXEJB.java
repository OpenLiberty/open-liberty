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

package com.ibm.ws.jpa.fvt.ejbinwar.ejb.jndi;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceContexts;

import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateful(name = "AnnotatedJNDISFEXEJB")
@Local(AnnotatedJNDISLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
@PersistenceContexts({
                       // Persistence Units defined in the ejb
                       @PersistenceContext(unitName = "COMMON_JTA",
                                           type = PersistenceContextType.EXTENDED,
                                           name = "jpa/ejbinwar/jndi/ano/ejbinwar/common_cmex"),
                       @PersistenceContext(unitName = "EJB_JTA",
                                           type = PersistenceContextType.EXTENDED,
                                           name = "jpa/ejbinwar/jndi/ano/ejbinwar/ejb_cmex"),

                       // Persistence Units defined in the application's library jar
                       @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA",
                                           type = PersistenceContextType.EXTENDED,
                                           name = "jpa/ejbinwar/jndi/ano/earlib/common_cmex"),
                       @PersistenceContext(unitName = "JPALIB_JTA",
                                           type = PersistenceContextType.EXTENDED,
                                           name = "jpa/ejbinwar/jndi/ano/earlib/jpalib_cmex"),

                       // Cleanup Persistence Context
                       @PersistenceContext(unitName = "CLEANUP", name = "jpa/ejbinwar/jndi/ano/cleanup_cmts")
})
public class AnnotatedJNDISFEXEJB extends BMTEJBTestVehicle {
    @Override
    @Remove
    public void release() {

    }
}
