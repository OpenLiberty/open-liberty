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
package com.ibm.ws.transaction.ejb.first;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

/**
 * A local singleton startup bean (EJB session bean) that requires the container to
 * create a new transaction context when the bean is created.
 */
@Singleton
@Startup
@LocalBean
public class InitNewTxBean1 {
    private static final Logger logger = Logger.getLogger(InitNewTxBean1.class.getName());

    public UOWManager uowManager;

    @PostConstruct
    @TransactionAttribute(REQUIRES_NEW)
    public void initTx1() {
        logger.info("---- initTx1 invoked ----");
        try {
            uowManager = (UOWManager) new InitialContext().lookup("java:comp/websphere/UOWManager");
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to obtain UOWManager : " + e);
        }

        runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    private void runUnderUOW(int uowType) {
        try {
            uowManager.runUnderUOW(uowType, false, new UOWAction() {
                @Override
                public void run() throws Exception {
                    logger.info("--> Inside the UOWActions's run() method");
                    // nothing to do for CMT
                    logger.info("--> Leaving the UOWActions's run() method");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failure from UOWManager.runUnderUOW : " + e);
        }
    }
}
