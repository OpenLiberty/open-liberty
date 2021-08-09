/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.tx.rununderuow.ejb;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

public class RunUnderUOWBMTBean {
    private static final Logger svLogger = Logger.getLogger(RunUnderUOWBMTBean.class.getName());

    @Resource(name="BeanName")
    String beanName;

    public UOWManager uowManager;
    public UserTransaction userTransaction;

    @PostConstruct
    private void postConstruct() {
        try {
            uowManager = (UOWManager)new InitialContext().lookup("java:comp/websphere/UOWManager");
        } catch (NamingException nex) {
            nex.printStackTrace();
            throw new EJBException("Unable to obtain UOWManager : " + nex);
        }
        try {
            userTransaction = (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException nex) {
            nex.printStackTrace();
            throw new EJBException("Unable to obtain UserTransaction : " + nex);
        }
    }

    public void beanManaged(int uowType) {
        svLogger.info("> " + beanName + ".beanManaged(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".beanManaged()");
    }

    @Remove
    public void remove(int uowType) {
        svLogger.info("> " + beanName + ".remove(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".remove()");
    }

    private void runUnderUOW(final int uowType) {
        try {
            uowManager.runUnderUOW(uowType, false, new UOWAction() {
                public void run() throws Exception {
                    svLogger.info("--> Inside the UOWActions's run() method");
                    if (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION) {
                        try {
                            svLogger.info("--> Beginning a Global Transaction");
                            userTransaction.begin();
                            svLogger.info("--> Committing the Global Transaction");
                            userTransaction.commit();
                        } catch (Exception ex) {
                            if (beanName.endsWith("SF")) {
                                svLogger.info("--> Caught expected Exception : " + ex);
                            } else {
                                svLogger.info("--> Caught un-expected Exception : " + ex);
                                ex.printStackTrace();
                                throw ex;
                            }
                        }
                    }
                    svLogger.info("--> Leaving the UOWActions's run() method");
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Failure from UOWManager.runUnderUOW : " + ex);
        }
    }
}
