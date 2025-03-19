/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.invocationContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/InvocationContextTestServlet")
public class InvocationContextTestServlet extends FATServlet {

    public static boolean bindingInterceptorRan = false;
    public static boolean nonBindingInterceptorRan = false;
    public static boolean nonBindingInterceptorAroundConstructRan = false;
    public static boolean nonBindingInterceptorPostConstructRan = false;

    @Inject
    InterceptedBean ib;

    @Inject
    InterceptedMethodsBean imb;

    @Test
    public void testGetInterceptorBindingsFromInvocationContext() {

        //test for interceptor bindings on a class.
        assertFalse(bindingInterceptorRan);
        assertFalse(nonBindingInterceptorRan);
        assertFalse(nonBindingInterceptorAroundConstructRan);
        assertFalse(nonBindingInterceptorPostConstructRan);
        ib.iExist();  //The real test takes place inisde the interceptors' methods.
        assertTrue(bindingInterceptorRan);
        assertTrue(nonBindingInterceptorRan);
        assertTrue(nonBindingInterceptorAroundConstructRan);
        assertTrue(nonBindingInterceptorPostConstructRan);

        //clean up then test for interceptor bindings on a method.
        bindingInterceptorRan = false;
        nonBindingInterceptorRan = false;
        nonBindingInterceptorAroundConstructRan = false;
        nonBindingInterceptorPostConstructRan = false;

        assertFalse(bindingInterceptorRan);
        assertFalse(nonBindingInterceptorRan);
        assertFalse(nonBindingInterceptorAroundConstructRan);
        assertFalse(nonBindingInterceptorPostConstructRan);
        imb.iExist();  //The real test takes place inisde the interceptors' methods.
        assertTrue(bindingInterceptorRan);
        assertTrue(nonBindingInterceptorRan);
        assertFalse(nonBindingInterceptorAroundConstructRan); //Since the interceptor is bound to the method, we don't expect object construction interception to occur
        assertFalse(nonBindingInterceptorPostConstructRan);
    }

}
