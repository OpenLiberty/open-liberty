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

package com.ibm.ws.ejbcontainer.session.async.err.web;

import static org.junit.Assert.assertNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.session.async.err.shared.EmptyMethNameXMLLocal;
import com.ibm.ws.ejbcontainer.session.async.err.shared.NoMethNameXMLLocal;
import com.ibm.ws.ejbcontainer.session.async.err.shared.Style1XMLwithParamsLocal;

import componenttest.app.FATServlet;

@WebServlet("/AsyncXMLErrorServlet")
public class AsyncXMLErrorServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private final static String CLASSNAME = AsyncXMLErrorServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Follow throwable chain and return root cause of a Throwable.
     */
    private String findRootCause(Throwable t) {
        Throwable root = t;
        Throwable cause = t.getCause();
        while (cause != null) {
            root = cause;
            cause = root.getCause();
        }

        return root.toString();
    }

    /**
     * Verify that the container throws an exception, CNTR0203E, when the
     * method-name element of the async-methodType XML is not present.
     */
    public void testNoMethNameXML() throws Exception {
        svLogger.info("--> This test is looking for a specific error message - see test for details.");

        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = NoMethNameXMLLocal.class.getName();
        String Application = "AsyncXMLErr1BeanApp";
        String Module = "AsyncXMLErr1Bean";
        String BeanName = "NoMethNameXMLBean";

        NoMethNameXMLLocal bean = null;

        try {
            bean = (NoMethNameXMLLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
        }

        catch (Exception e) {
            svLogger.info("--> Caught Exception:" + e);

            // look at cause and see if it is EJBConfigurationException
            String cause = findRootCause(e);
            if (cause.contains("com.ibm.ejs.container.EJBConfigurationException")) {
                svLogger.info("--> The Exception received had the expected caused by: " + cause);
            } else {
                svLogger.info("--> The Exception received did NOT have the expected caused by: " + cause);
                throw e;
            }
        }

        assertNull("Application should have failed to start, but we successfully looked up the NoMethNameXMLBean bean", bean);
    }

    /**
     * Verify that the container throws an exception, CNTR0203E, when the
     * method-name element of the async-methodType XML is an empty string.
     */
    public void testEmptyMethNameXML() throws Exception {
        svLogger.info("--> This test is looking for a specific error message - see test for details.");

        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = EmptyMethNameXMLLocal.class.getName();
        String Application = "AsyncXMLErr3BeanApp";
        String Module = "AsyncXMLErr3Bean";
        String BeanName = "EmptyMethNameXMLBean";

        EmptyMethNameXMLLocal bean = null;

        try {
            bean = (EmptyMethNameXMLLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
        }

        catch (Exception e) {
            svLogger.info("--> Caught Exception:" + e);

            // look at cause and see if it is EJBConfigurationException
            String cause = findRootCause(e);

            if (cause.contains("com.ibm.ejs.container.EJBConfigurationException")) {
                svLogger.info("--> The Exception received had the expected caused by: " + cause);
            } else {
                svLogger.info("--> The Exception received did NOT have the expected caused by: " + cause);
                throw e;
            }
        }

        assertNull("Application should have failed to start, but we successfully looked up the EmptyMethNameXMLBean bean", bean);
    }

    /**
     * Verify that the container throws an exception, CNTR0204E, when method-params
     * are defined while using Style 1 XML (i.e. * for method-name).
     */
    public void testStyle1XMLwithParams() throws Exception {
        svLogger.info("--> This test is looking for a specific error message - see test for details.");

        // Names of interface, application, module, and bean used in the test for lookup.
        String Interface = Style1XMLwithParamsLocal.class.getName();
        String Application = "AsyncXMLErr2BeanApp";
        String Module = "AsyncXMLErr2Bean";
        String BeanName = "Style1XMLwithParamsBean";

        Style1XMLwithParamsLocal bean = null;

        try {
            bean = (Style1XMLwithParamsLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(Interface, Application, Module, BeanName);
        } catch (Exception e) {
            svLogger.info("--> Caught Exception:" + e);

            // look at cause and see if it is EJBConfigurationException
            String cause = findRootCause(e);

            if (cause.contains("com.ibm.ejs.container.EJBConfigurationException")) {
                svLogger.info("--> The Exception received had the expected caused by: " + cause);
            } else {
                svLogger.info("--> The Exception received did NOT have the expected caused by: " + cause);
                throw e;
            }
        }

        assertNull("Application should have failed to start, but we successfully looked up the Style1XMLwithParamsBean bean", bean);
    }
}
