/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.web;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int.InterceptorLocal;
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int.InterceptorStatefulLocal;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> InterceptorIntegrationTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support of CDI Interceptors; with and without
 * EJB Interceptors. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li> testEjbWithNoInterceptors
 * - verifies that an EJB in a CDI enabled module works fine
 * without either EJB or CDI interceptors.
 * <li> testEjbWithEJBInterceptors
 * - verifies that an EJB in a CDI enabled module works fine
 * with an EJB interceptor and no CDI interceptors.
 * <li> testEjbWithCDIInterceptors
 * - verifies that an EJB in a CDI enabled module works fine
 * with a CDI interceptor and no EJB interceptors.
 * <li> testEjbWithBothInterceptors
 * - verifies that an EJB in a CDI enabled module works fine
 * with an EJB interceptor and a CDI interceptor.
 * <li> testStatefulEjbWithBothInterceptors
 * - verifies that a Stateful EJB in a CDI enabled module works
 * fine with an EJB interceptor and two CDI interceptors,
 * covering all lifecycle callbacks.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/InterceptorIntegrationServlet")
public class InterceptorIntegrationServlet extends FATServlet {
    private static final String EJB_INT_CDI_MODULE_NAME = "EJB31InterceptorJCDIBean";

    /**
     * Tests that an EJB in a CDI enabled module works fine
     * without either EJB or CDI interceptors. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testEjbWithNoInterceptors() throws Exception {
        // Locate Stateless local bean
        InterceptorLocal bean = (InterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorLocal.class.getName(),
                                                                                            EJB_INT_CDI_MODULE_NAME,
                                                                                            "NoInterceptorBasicStateless");

        // Verify that no interceptors are called
        bean.verifyInterceptorCalls(new ArrayList<String>());
    }

    /**
     * Tests that an EJB in a CDI enabled module works fine
     * with an EJB interceptor and no CDI interceptors. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testEjbWithEJBInterceptors() throws Exception {
        // Locate Stateless local bean
        InterceptorLocal bean = (InterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorLocal.class.getName(),
                                                                                            EJB_INT_CDI_MODULE_NAME,
                                                                                            "EJBInterceptorStateless");

        // Verify that the correct interceptors are called
        bean.verifyInterceptorCalls(new ArrayList<String>());
    }

    /**
     * Tests that an EJB in a CDI enabled module works fine
     * with a CDI interceptor and no EJB interceptors. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    //@Ignore
    public void testEjbWithCDIInterceptors() throws Exception {
        // Locate Stateless local bean
        InterceptorLocal bean = (InterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorLocal.class.getName(),
                                                                                            EJB_INT_CDI_MODULE_NAME,
                                                                                            "CDIInterceptorStateless");

        // Verify that the correct interceptors are called
        bean.verifyInterceptorCalls(new ArrayList<String>());
    }

    /**
     * Tests that an EJB in a CDI enabled module works fine
     * with an EJB interceptor and a CDI interceptor. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    //@Ignore
    public void testEjbWithBothInterceptors() throws Exception {
        // Locate Stateless remote bean
        InterceptorLocal bean = (InterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorLocal.class.getName(),
                                                                                            EJB_INT_CDI_MODULE_NAME,
                                                                                            "BothInterceptorStateless");
//      InterceptorRemote bean = FATHelper.lookupRemoteBinding("java:app/EJB31InterceptorJCDIBean/BothInterceptorStateless",
//                                                             InterceptorRemote.class);

        // Verify that the correct interceptors are called
        bean.verifyInterceptorCalls(new ArrayList<String>());
    }

    /**
     * Tests that a Stateful EJB in a CDI enabled module works fine
     * with an EJB interceptor and two CDI interceptors, covering
     * all lifecycle callbacks. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    //@Ignore
    @SkipForRepeat({ EE9_FEATURES })
    public void testStatefulEjbWithBothInterceptors() throws Exception {
        // Locate Stateful local bean
        InterceptorStatefulLocal bean = (InterceptorStatefulLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorStatefulLocal.class.getName(),
                                                                                                            EJB_INT_CDI_MODULE_NAME,
                                                                                                            "BothInterceptorStateful");

        // Remove this first bean just to get a PreDestroy call stack
        bean.remove(new ArrayList<String>());

        // Locate a second Stateful local bean to test
        bean = (InterceptorStatefulLocal) FATHelper.lookupDefaultBindingEJBJavaApp(InterceptorStatefulLocal.class.getName(),
                                                                                   EJB_INT_CDI_MODULE_NAME,
                                                                                   "BothInterceptorStateful");

        // Verify that the correct interceptors are/were called
        bean.verifyInterceptorCalls(new ArrayList<String>());
    }

}
