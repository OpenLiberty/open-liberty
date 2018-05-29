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
import static javax.ejb.TransactionManagementType.CONTAINER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * (remote)Bean implementation class for Enterprise Bean:
 * AdvCompCMTStatelessRemote
 **/
@SuppressWarnings("serial")
@Stateless(name = "AdvCompCMTStatelessRemote")
@Remote({ BasicCMTStatelessRemote.class, AdvCMTStatelessRemote.class, EJBInjectionRemote.class })
@RemoteHome(AdvCMTStatelessEJBHome.class)
@TransactionManagement(CONTAINER)
public class AdvCompCMTStatelessRemoteBean implements SessionBean {
    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    private static final String CLASS_NAME = AdvCompCMTStatelessRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private SessionContext ivContext;
    @EJB(beanInterface = BasicCMTStatelessRemote.class, beanName = "AdvCompCMTStatelessRemote")
    public BasicCMTStatelessRemote ivFieldInjected;
    @EJB(beanInterface = AdvCMTStatelessEJBHome.class, beanName = "AdvCompCMTStatelessRemote")
    public AdvCMTStatelessEJBHome ivHomeInjected;
    public BasicCMTStatelessRemote ivMethodInjected;

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

    public void verifyEJBFieldInjection() throws Exception {
        String envName = null;
        assertNotNull("Injected Field is set : " + ivFieldInjected, ivFieldInjected);
        ivFieldInjected.tx_Default();
        svLogger.info("Method called successfully on Injected Field");

        svLogger.info("Home = " + ivHomeInjected);
        assertNotNull("Injected Home is set : " + ivHomeInjected, ivHomeInjected);

        AdvCMTStatelessEJB bean = ivHomeInjected.create();
        assertNotNull("Create called successfully on Injected Home : " + bean, bean);

        // Next, ensure the above may be looked up in the global namespace

        Context initCtx = new InitialContext();
        Context myEnv = (Context) initCtx.lookup("java:comp/env");

        envName = CLASS_NAME + "/ivFieldInjected";
        ivFieldInjected = (BasicCMTStatelessRemote) myEnv.lookup(envName);
        assertNotNull("global lookup(" + envName + ") not null :" + ivFieldInjected, ivFieldInjected);

        ivFieldInjected.tx_Default();
        svLogger.info("Method called successfully on looked up field");

        // Next, ensure the above may be looked up from the SessionContext
        envName = CLASS_NAME + "/ivFieldInjected";
        ivFieldInjected = (BasicCMTStatelessRemote) ivContext.lookup(envName);
        assertNotNull("context lookup(" + envName + ") not null :" + ivFieldInjected, ivFieldInjected);

        ivFieldInjected.tx_Default();
        svLogger.info("Method called successfully on looked up field");

        // Finally, reset all of the fields to ensure injection does not occur
        // when object is re-used from the pool.
        ivFieldInjected = null;
    }

    public void verifyNoEJBFieldInjection() {
        assertNull("Injected Field is not set : " + ivFieldInjected, ivFieldInjected);
        ivFieldInjected = null;
    }

    public void verifyEJBMethodInjection() throws Exception {
        String envName = null;
        assertNotNull("Injected Method Field is set : " + ivMethodInjected, ivMethodInjected);
        ivMethodInjected.tx_Default();
        svLogger.info("Method called successfully on Injected Method Field");

        // Next, ensure the above may be looked up in the global namespace
        Context initCtx = new InitialContext();
        Context myEnv = (Context) initCtx.lookup("java:comp/env");
        envName = CLASS_NAME + "/ivMethodInjected";
        ivMethodInjected = (BasicCMTStatelessRemote) myEnv.lookup(envName);
        assertNotNull("global lookup(" + envName + ") not null :" + ivMethodInjected, ivMethodInjected);
        ivMethodInjected.tx_Default();
        svLogger.info("Method called successfully on looked up method");

        // Next, ensure the above may be looked up from the SessionContext
        envName = CLASS_NAME + "/ivMethodInjected";
        ivMethodInjected = (BasicCMTStatelessRemote) ivContext.lookup(envName);
        assertNotNull("context lookup(" + envName + ") not null :" + ivMethodInjected, ivMethodInjected);
        ivMethodInjected.tx_Default();
        svLogger.info("Method called successfully on looked up method");

        // Finally, reset all of the fields to ensure injection does not occur
        // when object is re-used from the pool.
        ivMethodInjected = null;
    }

    public void verifyNoEJBMethodInjection() {
        assertNull("Injected Method Field is not set : " + ivMethodInjected, ivMethodInjected);
        ivMethodInjected = null;
    }

    @EJB(beanInterface = BasicCMTStatelessRemote.class, beanName = "AdvCompCMTStatelessRemote")
    public void setIvMethodInjected(BasicCMTStatelessRemote ejb) {
        ivMethodInjected = ejb;
    }

    public AdvCompCMTStatelessRemoteBean() {}

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