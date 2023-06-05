/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.interceptors;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class InterceptorsTestServlet extends FATServlet {

    @Inject
    private RequestScopedBean bean;

    @Test
    public void testEJB() {
        String msg = bean.message();
        //testing that the EJB was intercepted twice
        assertEquals("Intercepted by InterceptorSubClass2! Intercepted by InterceptorSubClass1! Hello", msg);
    }

}
