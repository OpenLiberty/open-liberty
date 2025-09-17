/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.binding.app;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/bindingTest")
public class ManagedBeanBindingTestServlet extends FATServlet {

    @Inject
    private BindingTestCDIBean bindingTestCdiBean;

    @Test
    public void testResourceInjection() {
        String tv1FromJndi = null;
        try {
            tv1FromJndi = (String) new InitialContext().lookup("testValue1");
        } catch (NamingException e) {
        }

        System.out.println("1 from JNDI: " + tv1FromJndi);

        System.out.println("1: " + bindingTestCdiBean.getTestValue1());
        System.out.println("2: " + bindingTestCdiBean.getTestValue2());
        System.out.println("3: " + bindingTestCdiBean.getTestValue3());

        // Loaded from resource directly
        assertEquals("testValue1", bindingTestCdiBean.getTestValue1());

        // Loaded via binding defined in ibm-managed-bean-bnd.xml
        assertEquals("testValue2", bindingTestCdiBean.getTestValue2());

        // Loaded via binding defined in server.xml
        assertEquals("testValue3", bindingTestCdiBean.getTestValue3());
    }
}
