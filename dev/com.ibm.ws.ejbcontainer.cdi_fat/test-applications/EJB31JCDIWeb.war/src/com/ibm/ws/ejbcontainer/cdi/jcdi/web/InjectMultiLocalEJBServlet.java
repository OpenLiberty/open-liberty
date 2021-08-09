/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.web;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.MultiLocalInjectSfEjbBean;
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.MultiLocalInjectSlEjbBean;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> InjectMultiLocalEJBTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests injection of EJBs with multiple interfaces using the @Inject
 * annotation. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li> testInjectOfStatelessMultiInterfaceEJB
 * - verifies that a Stateless EJB with multiple local interfaces
 * may be injected into an EJB using the @Inject annotation.
 * <li> testInjectOfStatefulMultiInterfaceEJB
 * - verifies that a Stateful EJB with multiple local interfaces
 * (including a No-Interface view) may be injected into an EJB
 * using the @Inject annotation.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/InjectMultiLocalEJBServlet")
public class InjectMultiLocalEJBServlet extends FATServlet {
    private static final String CLASS_NAME = InjectMultiLocalEJBServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String JAVA_GLOBAL_PREFIX = "java:global/EJB31JCDITestApp/EJB31JCDIBean/";
    private InitialContext svCtx;

    @Override
    protected void before() throws Exception {
        svLogger.info("> " + CLASS_NAME + ".before");

        svCtx = new InitialContext();

        svLogger.info("< " + CLASS_NAME + ".before");
    }

    /**
     * Tests that a Stateless EJB with multiple local interfaces may be injected
     * into an EJB using the @Inject annotation.
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectOfStatelessMultiInterfaceEJB() throws Exception {
        // Locate Stateless local bean with @Inject of stateless EJBs
        MultiLocalInjectSlEjbBean bean = (MultiLocalInjectSlEjbBean) svCtx.lookup(JAVA_GLOBAL_PREFIX + "MultiLocalInjectSlEjb");

        // Verify that the EJB was injected properly
        bean.verifyEJBInjection();
    }

    /**
     * Tests that a Stateful EJB with multiple local interfaces (including a
     * No-Interface view) may be injected into an EJB using the @Inject
     * annotation.
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectOfStatefulMultiInterfaceEJB() throws Exception {
        // Locate Stateless local bean with @Inject of stateful EJBs
        MultiLocalInjectSfEjbBean bean = (MultiLocalInjectSfEjbBean) svCtx.lookup(JAVA_GLOBAL_PREFIX + "MultiLocalInjectSfEjb");

        // Verify that the EJB was injected properly
        bean.verifyEJBInjection();
    }

}
