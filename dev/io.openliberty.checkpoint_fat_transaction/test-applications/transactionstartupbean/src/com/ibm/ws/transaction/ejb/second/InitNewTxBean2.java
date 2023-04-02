/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.ejb.second;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

import com.ibm.tx.jta.UserTransactionFactory;

/**
 * A local singleton startup bean (EJB session bean) that performs a user transaction 
 * at creation.
 */
@Singleton
@Startup
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class InitNewTxBean2 {
    private static final Logger logger = Logger.getLogger(InitNewTxBean2.class.getName());

    @Resource
    UserTransaction ut;

    @PostConstruct
    public void initTx2() {
        logger.info("---- InitTx2 invoked ----");
        try {
            ut.begin();
            ut.commit();
            logger.info("---- InitTx2 committed user transaction ----");
        } catch (Exception e) {
            logger.info("---- InitTx2 caught exception processing user transaction: " + e + " ----");
        }
    }
}
