/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.ejb.TransactionManagementType.CONTAINER;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.LocalHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

/**
 * Bean implementation class for Enterprise Bean: AdvCompCMTStatelessLocal
 **/
@SuppressWarnings("serial")
@Stateless(name = "AdvCompCMTStatelessLocal")
@LocalHome(AdvCMTStatelessEJBLocalHome.class)
@TransactionManagement(CONTAINER)
public class AdvCompCMTStatelessLocalBean implements SessionBean {
    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    // Logger
    private static final String CLASS_NAME = AdvCompCMTStatelessLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private SessionContext ivContext;

    @EJB(beanInterface = AdvCMTStatelessEJBLocalHome.class, beanName = "AdvCompCMTStatelessLocal")
    public AdvCMTStatelessEJBLocalHome ivHomeInjected;

    @TransactionAttribute(NOT_SUPPORTED)
    public void adv_Tx_NotSupported() {
        svLogger.info("Method adv_Tx_NotSupported called successfully");
    }

    @TransactionAttribute(MANDATORY)
    public void adv_Tx_Mandatory() {
        svLogger.info("Method adv_Tx_Mandatory called successfully");
    }

    public void tx_Default() {
        svLogger.info("Method tx_Default called successfully");
    }

    @TransactionAttribute(REQUIRED)
    public void tx_Required() {
        svLogger.info("Method tx_Required called successfully");
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void tx_NotSupported() {
        svLogger.info("Method tx_NotSupported called successfully");
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void tx_RequiresNew() {
        svLogger.info("Method tx_RequiresNew called successfully");
    }

    @TransactionAttribute(SUPPORTS)
    public void tx_Supports() {
        svLogger.info("Method tx_Supports called successfully");
    }

    @TransactionAttribute(NEVER)
    public void tx_Never() {
        svLogger.info("Method tx_Never called successfully");
    }

    @TransactionAttribute(MANDATORY)
    public void tx_Mandatory() {
        svLogger.info("Method tx_Mandatory called successfully");
    }

    public void test_getBusinessObject(boolean businessInterface) {
        try {
            ivContext.getInvokedBusinessInterface();
            if (!businessInterface)
                fail("getInvokedBusinessInterface passed unexpectedly");
        } catch (IllegalStateException isex) {
            if (businessInterface)
                fail("getInvokedBusinessInterface failed : " + isex);
            else
                svLogger.info("getInvokedBusinessInterface failed as expected: " + isex);
        }

        try {
            ivContext.getBusinessObject(null);
            fail("getBusinessObject passed unexpectedly");
        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);
        }

        try {
            ivContext.getBusinessObject(BasicCMTStatelessEJBLocal.class);
            fail("getBusinessObject passed unexpectedly");
        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);
        }
    }

    public void verifyEJBFieldInjection() throws Exception {
        svLogger.info("Home = " + ivHomeInjected);
        assertNotNull("Injected Home is set : " + ivHomeInjected, ivHomeInjected);

        AdvCMTStatelessEJBLocal bean = ivHomeInjected.create();
        assertNotNull("Create called successfully on Injected Home : " + bean, bean);
    }

    public void verifyNoEJBFieldInjection() {}

    public void verifyEJBMethodInjection() throws Exception {}

    public void verifyNoEJBMethodInjection() {}

    public AdvCompCMTStatelessLocalBean() {}

    public void ejbCreate() throws CreateException {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}