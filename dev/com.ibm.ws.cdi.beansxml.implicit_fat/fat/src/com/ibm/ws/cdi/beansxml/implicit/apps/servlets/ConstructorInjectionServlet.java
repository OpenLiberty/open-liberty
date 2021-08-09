/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.servlets;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.implicit.apps.ejb.FirstManagedBeanInterface;
import com.ibm.ws.cdi.beansxml.implicit.apps.ejb.SecondManagedBeanInterface;

import componenttest.app.FATServlet;

@WebServlet("/ConstructorInjectionServlet")
public class ConstructorInjectionServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";

    @EJB(beanName = "OtherSimpleEJB")
    private FirstManagedBeanInterface bean1;

    @EJB(beanName = "FinalEJB")
    private SecondManagedBeanInterface bean2;

    @Test
    public void testConstructorInjection() throws ServletException, IOException {
        bean1.setValue1(VALUE1);
        bean2.setValue2(VALUE2);
        String bean1value = bean1.getValue1();
        String bean2value = bean2.getValue2();
        if (!(bean1value.equals(VALUE1) && bean2value.equals(VALUE2))) {
            fail("Test FAILED bean values are " + bean1value + " and " + bean2value);
        }
    }
}
