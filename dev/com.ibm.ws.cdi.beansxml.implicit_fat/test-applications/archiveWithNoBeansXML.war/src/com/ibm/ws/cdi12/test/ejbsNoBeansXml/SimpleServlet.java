/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi12.test.ejbsNoBeansXml;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @EJB(beanName = "SimpleEJB")
    private ManagedSimpleBean bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        bean.setValue("value");
        out.println(getFieldInjectionResult());
    }

    public String getFieldInjectionResult() {
        String result;
        String beanValue = bean.getValue();
        if (beanValue.equals("value")) {
            result = ("Test PASSED bean value is " + beanValue);
        } else {
            result = ("Test FAILED bean value is " + beanValue);
        }
        return result;
    }
}
