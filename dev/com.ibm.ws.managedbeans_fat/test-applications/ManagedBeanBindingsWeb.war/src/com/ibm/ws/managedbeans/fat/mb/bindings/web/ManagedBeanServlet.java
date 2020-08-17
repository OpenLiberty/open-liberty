/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.bindings.web;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.managedbeans.fat.mb.bindings.ejb.StatelessEJBforTestingManagedBean;

import componenttest.app.FATServlet;

@WebServlet("/ManagedBeanServlet")
public class ManagedBeanServlet extends FATServlet {

    private static final long serialVersionUID = -1228705158181850229L;

    private static final String JAVA_GLOBAL = "java:global/ManagedBeanBindingsApp/ManagedBeanBindingsEJB/";
    private static final String TEST_INTERFACE = "!" + StatelessEJBforTestingManagedBean.class.getName();

    /**
     * Tests that a simple ManagedBean (with no injection or interceptors) may
     * be injected into an EJB. A lookup should fail, since the ManagedBean
     * does not have a name.
     *
     * The SimpleManagedBean has a set of bindings to test that a
     * managed bean without a name is bound correctly.
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testSimpleManagedBeanInjectionAndLookupBindings() throws Exception {
        Context svContext = new InitialContext();
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) svContext.lookup(JAVA_GLOBAL + "StatelessEJBforSimpleManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected, but not looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean (with no injection or interceptors) may
     * be injected into an EJB and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testNamedManagedBeanInjectionAndLookupBindings() throws Exception {
        Context svContext = new InitialContext();
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) svContext.lookup(JAVA_GLOBAL + "StatelessEJBforNamedManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection (no interceptors) may
     * be injected into an EJB and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectionManagedBeanInjectionAndLookupBindings() throws Exception {
        Context svContext = new InitialContext();
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) svContext.lookup(JAVA_GLOBAL + "StatelessEJBforInjectionManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection and AroundInvoke
     * interceptors may be injected into an EJB and looked up in both java:app
     * and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInterceptorManagedBeanInjectionAndLookupBindings() throws Exception {
        Context svContext = new InitialContext();
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) svContext.lookup(JAVA_GLOBAL + "StatelessEJBforInterceptorManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

}
