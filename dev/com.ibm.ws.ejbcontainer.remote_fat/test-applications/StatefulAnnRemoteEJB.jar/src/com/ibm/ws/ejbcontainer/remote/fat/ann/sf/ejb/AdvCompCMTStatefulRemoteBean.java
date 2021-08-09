/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.ejb.TransactionManagementType.CONTAINER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrentAccessException;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Bean implementation class for Enterprise Bean: AdvBasicCMTStatefulRemote
 **/
@Stateful(name = "AdvCompCMTStatefulRemote")
@Remote({ BasicCMTStatefulRemote.class, AdvCMTStatefulRemote.class,
          EJBInjectionRemote.class })
@RemoteHome(AdvCMTStatefulEJBRemoteHome.class)
@TransactionManagement(CONTAINER)
public class AdvCompCMTStatefulRemoteBean implements SessionBean {

    /**  */
    private static final long serialVersionUID = 3630537714386395139L;
    private static final String CLASS_NAME = AdvCompCMTStatefulRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    @Resource
    private SessionContext ivContext;

    @EJB(beanName = "NoCMTStatefulRemoteBean")
    public BasicCMTStatefulRemote ivFieldInjected;

    public BasicCMTStatefulRemote ivMethodInjected;

    @TransactionAttribute(NOT_SUPPORTED)
    public void adv_Tx_NotSupported() {
        svLogger.info("Method adv_Tx_NotSupported called successfully");
    }

    @TransactionAttribute(MANDATORY)
    public void adv_Tx_Mandatory() {
        svLogger.info("Method adv_Tx_Mandatory called successfully");
    }

    @AccessTimeout(0)
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
                svLogger.info("getInvokedBusinessInterface failed as expected: "
                              + isex);
            invokedClass = BasicCMTStatefulRemote.class;
        }

        try {
            ivContext.getBusinessObject(null);
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
            svLogger.info("ConcurrentAccessException occurred as expected : "
                          + caex);
        }
    }

    public void verifyEJBFieldInjection() throws Exception {
        String envName = null;
        try {
            assertNotNull("Injected Field is set : " + ivFieldInjected,
                          ivFieldInjected);
            ivFieldInjected.tx_Default();
            svLogger.info("Method called successfully on Injected Field");

            // Next, insure the above may be looked up in the global namespace

            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivFieldInjected";
            ivFieldInjected = (BasicCMTStatefulRemote) myEnv.lookup(envName);
            assertNotNull("global lookup(" + envName + ") not null :"
                          + ivFieldInjected, ivFieldInjected);
            ivFieldInjected.tx_Default();
            svLogger.info("Method called successfully on looked up field");

            // Next, insure the above may be looked up from the SessionContext
            envName = CLASS_NAME + "/ivFieldInjected";
            ivFieldInjected = (BasicCMTStatefulRemote) ivContext.lookup(envName);
            assertNotNull("context lookup(" + envName + ") not null :"
                          + ivFieldInjected, ivFieldInjected);
            ivFieldInjected.tx_Default();
            svLogger.info("Method called successfully on looked up field");
            // Finally, reset all of the fields to insure injection does not
            // occur
            // when object is re-used from the pool.

        } finally {
            ivFieldInjected = null;
        }
    }

    public void verifyNoEJBFieldInjection() {
        try {
            assertNull("Injected Field is not set : " + ivFieldInjected,
                       ivFieldInjected);
        } finally {
            ivFieldInjected = null;
        }
    }

    public void verifyEJBMethodInjection() throws Exception {
        String envName = null;
        try {
            assertNotNull("Injected Method Field is set : " + ivMethodInjected,
                          ivMethodInjected);
            ivMethodInjected.tx_Default();
            svLogger.info("Method called successfully on Injected Method Field");
            // Next, insure the above may be looked up in the global namespace
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");
            envName = CLASS_NAME + "/ivMethodInjected";
            ivMethodInjected = (BasicCMTStatefulRemote) myEnv.lookup(envName);
            assertNotNull("global lookup(" + envName + ") not null :"
                          + ivMethodInjected, ivMethodInjected);
            ivMethodInjected.tx_Default();
            svLogger.info("Method called successfully on looked up method");
            // Next, insure the above may be looked up from the SessionContext
            envName = CLASS_NAME + "/ivMethodInjected";
            ivMethodInjected = (BasicCMTStatefulRemote) ivContext.lookup(envName);
            assertNotNull("context lookup(" + envName + ") not null :"
                          + ivMethodInjected, ivMethodInjected);
            ivMethodInjected.tx_Default();
            svLogger.info("Method called successfully on looked up method");
        } finally {
            // Finally, reset all of the fields to insure injection does not
            // occur
            // when object is re-used from the pool.
            ivMethodInjected = null;
        }
    }

    public void verifyNoEJBMethodInjection() {
        try {
            assertNull(
                       "Injected Method Field is not set : " + ivMethodInjected,
                       ivMethodInjected);
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail("Method Injection failed : " + ex);
        }
        ivMethodInjected = null;
    }

    @EJB(beanInterface = BasicCMTStatefulRemote.class, beanName = "NoCMTStatefulRemoteBean")
    public void setIvMethodInjected(BasicCMTStatefulRemote ejb) {
        ivMethodInjected = ejb;
    }

    public AdvCompCMTStatefulRemoteBean() {
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
