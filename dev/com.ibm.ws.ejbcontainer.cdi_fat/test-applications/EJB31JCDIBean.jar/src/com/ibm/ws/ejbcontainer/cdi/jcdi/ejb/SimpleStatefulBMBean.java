/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import static javax.ejb.TransactionManagementType.BEAN;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.transaction.UserTransaction;

/**
 * Simple Stateful bean-managed bean to create and remove
 **/
@Stateful(name = "SimpleStatefulBM")
@TransactionManagement(BEAN)
public class SimpleStatefulBMBean extends SimpleStatefulBean implements SimpleStatefulBMRemove {
    private static final String CLASS_NAME = SimpleStatefulBMBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource
    UserTransaction userTransaction;

    public SimpleStatefulBMBean() {
        ivEJBName = "SimplestatefulBM";
    }

    /**
     * Begins a UserTransaction, leaving it active so the bean runs in
     * a sticky global transaction.
     **/
    @Override
    public void beginUserTransaction() {
        svLogger.info("- " + ivEJBName + ".beginUserTransaction()");
        try {
            userTransaction.begin();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected exception beginning transaction : ", ex);
        }
    }

    /**
     * Commits the UserTransaction that was started with a call to
     * beginUserTransaction.
     */
    @Override
    public void commitUserTransaction() {
        svLogger.info("- " + ivEJBName + ".commitUserTransaction()");
        try {
            userTransaction.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected exception committing transaction : ", ex);
        }
    }

}
