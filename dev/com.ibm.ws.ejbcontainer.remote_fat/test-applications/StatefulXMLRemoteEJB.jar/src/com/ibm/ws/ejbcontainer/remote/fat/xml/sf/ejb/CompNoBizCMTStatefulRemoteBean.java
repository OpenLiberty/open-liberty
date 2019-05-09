/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Bean implementation class for Enterprise Bean: CompNoBizCMTStatefulRemoteBean
 **/

@SuppressWarnings("serial")
public class CompNoBizCMTStatefulRemoteBean implements SessionBean {

    private static final String CLASS_NAME = CompNoBizCMTStatefulRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private SessionContext ivContext;

    public void tx_Default() {
        svLogger.info("Method tx_Default called successfully");
    }

    public void tx_Required() {
        svLogger.info("Method tx_Required called successfully");
    }

    public void tx_NotSupported() {
        svLogger.info("Method tx_NotSupported called successfully");
    }

    public void tx_RequiresNew() {
        svLogger.info("Method tx_RequiresNew called successfully");
    }

    public void tx_Supports() {
        svLogger.info("Method tx_Supports called successfully");
    }

    public void tx_Never() {
        svLogger.info("Method tx_Never called successfully");
    }

    public void tx_Mandatory() {
        svLogger.info("Method tx_Mandatory called successfully");
    }

    public void test_getBusinessObject(boolean businessInterface) {
        Class<?> invokedClass = null;
        BasicCMTStatefulRemote bObject = null;

        try {
            invokedClass = ivContext.getInvokedBusinessInterface();
            if (businessInterface)
                assertEquals("getInvokedBusinessInterface returned class: "
                             + invokedClass, invokedClass,
                             BasicCMTStatefulRemote.class);
            else
                fail("getInvokedBusinessInterface passed unexpectedly");

        } catch (IllegalStateException isex) {
            if (businessInterface)
                fail("getInvokedBusinessInterface failed : " + isex);
            else
                svLogger.info("getInvokedBusinessInterface failed as expected: " + isex);
            invokedClass = BasicCMTStatefulRemote.class;

        }

        try {
            ivContext.getBusinessObject(null);
            fail("getBusinessObject passed unexpectedly");

        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);

        }

        try {
            ivContext.getBusinessObject(BasicCMTStatefulEJBRemote.class);
            fail("getBusinessObject passed unexpectedly");

        } catch (IllegalStateException isex) {
            svLogger.info("getBusinessObject failed as expected: " + isex);

        }

        bObject = (BasicCMTStatefulRemote) ivContext.getBusinessObject(invokedClass);
        assertNotNull("getBusinessObject returned object", bObject);

        try {
            bObject.tx_Default();
            fail("Method called successfully on business object; should fail!");

        } catch (ConcurrentAccessException caex) {
            // Should fail because this would be a re-entrant call!
            svLogger.info("ConcurrentAccessException occurred as expected : " + caex);

        }

    }

    public CompNoBizCMTStatefulRemoteBean() {
    }

    public void ejbCreate() throws CreateException {
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}
