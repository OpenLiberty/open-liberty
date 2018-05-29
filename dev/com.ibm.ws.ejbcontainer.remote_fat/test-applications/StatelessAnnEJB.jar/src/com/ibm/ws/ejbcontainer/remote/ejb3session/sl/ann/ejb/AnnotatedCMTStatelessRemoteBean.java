/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

/**
 * Bean implementation class for Enterprise Bean: BasicCMTStatelessRemote and
 * annotated interface AnnotatedCMTStatelessRemote
 **/
@Stateless
@Remote(BasicCMTStatelessRemote.class)
public class AnnotatedCMTStatelessRemoteBean implements AnnotatedCMTStatelessRemote {
    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    // Logger
    private static final String CLASS_NAME = AnnotatedCMTStatelessRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource
    private SessionContext ivContext;

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

    @Override
    public void tx_ADefault() {
        svLogger.info("Method tx_ADefault called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRED)
    public void tx_ARequired() {
        svLogger.info("Method tx_ARequired called successfully");
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void tx_ANotSupported() {
        svLogger.info("Method tx_ANotSupported called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void tx_ARequiresNew() {
        svLogger.info("Method tx_ARequiresNew called successfully");
    }

    @Override
    @TransactionAttribute(SUPPORTS)
    public void tx_ASupports() {
        svLogger.info("Method tx_ASupports called successfully");
    }

    @Override
    @TransactionAttribute(NEVER)
    public void tx_ANever() {
        svLogger.info("Method tx_ANever called successfully");
    }

    @Override
    @TransactionAttribute(MANDATORY)
    public void tx_AMandatory() {
        svLogger.info("Method tx_AMandatory called successfully");
    }

    public void test_getBusinessObject(boolean businessInterface) {
        Class<?> invokedClass = null;
        BasicCMTStatelessRemote bObject = null;
        try {
            invokedClass = ivContext.getInvokedBusinessInterface();
            if (businessInterface)
                assertEquals("getInvokedBusinessInterface returned class: " + invokedClass, invokedClass, BasicCMTStatelessRemote.class);
            else
                fail("getInvokedBusinessInterface passed unexpectedly");

        } catch (IllegalStateException isex) {
            if (businessInterface)
                fail("getInvokedBusinessInterface failed : " + isex);
            else
                svLogger.info("getInvokedBusinessInterface failed as expected: " + isex);
            invokedClass = BasicCMTStatelessRemote.class;
        }

        try {
            ivContext.getBusinessObject(null);
            fail("getBusinessObject passed unexpectedly");
        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);
        }

        try {
            ivContext.getBusinessObject(BasicCMTStatelessEJB.class);
            fail("getBusinessObject passed unexpectedly");
        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);
        }

        bObject = (BasicCMTStatelessRemote) ivContext.getBusinessObject(invokedClass);
        assertNotNull("getBusinessObject returned object", bObject);

        bObject.tx_Default();
        svLogger.info("Method called successfully on business object");
    }

    public AnnotatedCMTStatelessRemoteBean() {}
}