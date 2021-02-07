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

import com.ibm.ws.cdi.beansxml.implicit.apps.ejb.ApplicationScopedEjbBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.ejb.DependentEjbBean;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@WebServlet("/ejbServlet")
@Mode(TestMode.FULL)
public class EjbServlet extends FATServlet {

    private static final long serialVersionUID = -4123846097364914982L;
    public static final String MESSAGE1 = "Message1";
    public static final String MESSAGE2 = "Message2";

    @EJB(beanName = "EjbBean")
    DependentEjbBean bean1;

    @EJB(beanName = "EjbBean2")
    ApplicationScopedEjbBean bean2;

    @Test
    public void test() throws ServletException, IOException {
        bean1.setMessage(MESSAGE1);
        bean2.setMessage(MESSAGE2);
        String message1 = bean1.getMessage();
        String message2 = bean2.getMessage();
        if (!(message1.equals(MESSAGE1) && message2.equals(MESSAGE2))) {
            fail("FAILED messages are " + message1 + " and " + message2);
        }
    }
}
