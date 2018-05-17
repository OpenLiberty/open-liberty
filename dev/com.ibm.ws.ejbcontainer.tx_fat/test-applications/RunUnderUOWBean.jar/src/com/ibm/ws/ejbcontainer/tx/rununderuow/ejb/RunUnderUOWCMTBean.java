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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

public class RunUnderUOWCMTBean {
    private static final Logger svLogger = Logger.getLogger(RunUnderUOWCMTBean.class.getName());

    @Resource(name = "BeanName")
    String beanName;

    public UOWManager uowManager;

    @PostConstruct
    private void postConstruct() {
        try {
            uowManager = (UOWManager) new InitialContext().lookup("java:comp/websphere/UOWManager");
        } catch (NamingException nex) {
            nex.printStackTrace();
            throw new EJBException("Unable to obtain UOWManager : " + nex);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void notSupported(int uowType) {
        svLogger.info("> " + beanName + ".notSupported(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".notSupported()");
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void supports(int uowType) {
        svLogger.info("> " + beanName + ".supports(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".supports()");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void required(int uowType) {
        svLogger.info("> " + beanName + ".required(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".required()");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void requiresNew(int uowType) {
        svLogger.info("> " + beanName + ".requiresNew(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".requiresNew()");
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void never(int uowType) {
        svLogger.info("> " + beanName + ".never(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".never()");
    }

    @Remove
    public void remove(int uowType) {
        svLogger.info("> " + beanName + ".remove(" + (uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION ? "Local" : "Global") + ")");
        runUnderUOW(uowType);
        svLogger.info("< " + beanName + ".remove()");
    }

    private void runUnderUOW(int uowType) {
        try {
            uowManager.runUnderUOW(uowType, false, new UOWAction() {
                public void run() throws Exception {
                    svLogger.info("--> Inside the UOWActions's run() method");
                    // nothing to do for CMT
                    svLogger.info("--> Leaving the UOWActions's run() method");
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Failure from UOWManager.runUnderUOW : " + ex);
        }
    }
}
