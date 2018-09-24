/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.async.warn.web;

import static junit.framework.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.session.async.warn.ejb.InitRecoveryLogBean;
import com.ibm.ws.ejbcontainer.session.async.warn.shared.AsyncInLocalIf;
import com.ibm.ws.ejbcontainer.session.async.warn.shared.AsyncInRemoteIf;
import com.ibm.ws.ejbcontainer.session.async.warn.shared.AsyncNotInLocalIf;
import com.ibm.ws.ejbcontainer.session.async.warn.shared.AsyncNotInRemoteIf;

import componenttest.app.FATServlet;

@WebServlet("/AsyncWarnServlet")
public class AsyncWarnServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = AsyncWarnServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String JNDI_INIT_RECOVERY_LOG_BEAN = "java:app/AsyncWarnTestBean/InitRecoveryLogBean";

    public void initRecoveryLog() throws Exception {
        // Call a method on a bean to initialize the server recovery log
        svLogger.info("initialize the server recovery log");
        InitRecoveryLogBean bean = (InitRecoveryLogBean) new InitialContext().lookup(JNDI_INIT_RECOVERY_LOG_BEAN);
        bean.getInvocationTime();
    }

    /* @Asynchronous defined on bean class and interface class */
    public void testInLocalIf_asyncOnBeanClass() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInLocalIf.class.getName();
        String Application = "AsyncInLocalIf1Bean";
        String Module = "AsyncInLocalIf1Bean";
        String BeanName = "AsyncInLocalIf1Bean";
        AsyncInLocalIf bean = null;

        try {
            bean = (AsyncInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            svLogger.info("--> Found bean of type : " + bean.getClass().getName());
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInLocalIf_asyncOnBeanClass", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean class and interface class */
    public void testInRemoteIf_asyncOnBeanClass() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInRemoteIf.class.getName();
        String Application = "AsyncInRemoteIf1Bean";
        String Module = "AsyncInRemoteIf1Bean";
        String BeanName = "AsyncInRemoteIf1Bean";
        AsyncInRemoteIf bean = null;

        try {
            bean = (AsyncInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            svLogger.info("--> Found bean of type : " + bean.getClass().getName());
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInRemoteIf_asyncOnBeanClass", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean methods and interface class. */
    public void testInLocalIf_asyncOnBeanMethods() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInLocalIf.class.getName();
        String Application = "AsyncInLocalIf2Bean";
        String Module = "AsyncInLocalIf2Bean";
        String BeanName = "AsyncInLocalIf2Bean";
        AsyncInLocalIf bean = null;

        try {
            bean = (AsyncInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInLocalIf_asyncOnBeanMethods", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean methods and interface class. */
    public void testInRemoteIf_asyncOnBeanMethods() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInRemoteIf.class.getName();
        String Application = "AsyncInRemoteIf2Bean";
        String Module = "AsyncInRemoteIf2Bean";
        String BeanName = "AsyncInRemoteIf2Bean";
        AsyncInRemoteIf bean = null;

        try {
            bean = (AsyncInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInRemoteIf_asyncOnBeanMethods", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on interface class only. */
    public void testInLocalIf_NOasyncOnBean() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInLocalIf.class.getName();
        String Application = "AsyncInLocalIf3Bean";
        String Module = "AsyncInLocalIf3Bean";
        String BeanName = "AsyncInLocalIf3Bean";
        AsyncInLocalIf bean = null;

        try {
            bean = (AsyncInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInLocalIf_NOasyncOnBean", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on interface class only. */
    public void testInRemoteIf_NOasyncOnBean() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncInRemoteIf.class.getName();
        String Application = "AsyncInRemoteIf3Bean";
        String Module = "AsyncInRemoteIf3Bean";
        String BeanName = "AsyncInRemoteIf3Bean";
        AsyncInRemoteIf bean = null;

        try {
            bean = (AsyncInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testInRemoteIf_NOasyncOnBean", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean class. */
    public void testNotInLocalIf_asyncOnBeanClass() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInLocalIf.class.getName();
        String Application = "AsyncNotInLocalIf1Bean";
        String Module = "AsyncNotInLocalIf1Bean";
        String BeanName = "AsyncNotInLocalIf1Bean";
        AsyncNotInLocalIf bean = null;

        try {
            bean = (AsyncNotInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            // call method to insure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInLocalIf_asyncOnBeanClass", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean class. */
    public void testNotInRemoteIf_asyncOnBeanClass() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInRemoteIf.class.getName();
        String Application = "AsyncNotInRemoteIf1Bean";
        String Module = "AsyncNotInRemoteIf1Bean";
        String BeanName = "AsyncNotInRemoteIf1Bean";
        AsyncNotInRemoteIf bean = null;

        try {
            bean = (AsyncNotInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            // call method to insure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInRemoteIf_asyncOnBeanClass", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean methods. */
    public void testNotInLocalIf_asyncOnBeanMethods() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInLocalIf.class.getName();
        String Application = "AsyncNotInLocalIf2Bean";
        String Module = "AsyncNotInLocalIf2Bean";
        String BeanName = "AsyncNotInLocalIf2Bean";
        AsyncNotInLocalIf bean = null;

        try {
            bean = (AsyncNotInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInLocalIf_asyncOnBeanMethods", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on bean methods. */
    public void testNotInRemoteIf_asyncOnBeanMethods() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInRemoteIf.class.getName();
        String Application = "AsyncNotInRemoteIf2Bean";
        String Module = "AsyncNotInRemoteIf2Bean";
        String BeanName = "AsyncNotInRemoteIf2Bean";
        AsyncNotInRemoteIf bean = null;

        try {
            bean = (AsyncNotInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInRemoteIf_asyncOnBeanMethods", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on interface class only. */
    public void testNotInLocalIf_NOasyncOnBean() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInLocalIf.class.getName();
        String Application = "AsyncNotInLocalIf3Bean";
        String Module = "AsyncNotInLocalIf3Bean";
        String BeanName = "AsyncNotInLocalIf3Bean";
        AsyncNotInLocalIf bean = null;

        try {
            bean = (AsyncNotInLocalIf) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInLocalIf_NOasyncOnBean", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }

    /* @Asynchronous defined on interface class only. */
    public void testNotInRemoteIf_NOasyncOnBean() throws Exception {
        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = AsyncNotInRemoteIf.class.getName();
        String Application = "AsyncNotInRemoteIf3Bean";
        String Module = "AsyncNotInRemoteIf3Bean";
        String BeanName = "AsyncNotInRemoteIf3Bean";
        AsyncNotInRemoteIf bean = null;

        try {
            bean = (AsyncNotInRemoteIf) FATHelper.lookupDefaultBindingsEJBRemoteInterface(Interface, Application, Module, BeanName);
            // call method to ensure bean has been initialized
            bean.test2();
        } catch (Exception e) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "testNotInRemoteIf_NOasyncOnBean", "--> Caught Exception:", e);
            fail("--> Unexpected Exception: " + e.getClass().getName() + ":" + e);
        }
    }
}
