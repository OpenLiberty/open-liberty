/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.beanelresolver.servlets;

import java.io.IOException;

import io.openliberty.el60.fat.beanelresolver.beans.TestBean;
import jakarta.el.ELProcessor;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet for the Expression Language 6.0 BeanELResolver.
 */
@WebServlet({ "/EL60BeanELResolverTestServlet" })
public class EL60BeanELResolverTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    ELProcessor elp = new ELProcessor();

    public EL60BeanELResolverTestServlet() {
        super();
        elp.defineBean("testBean", new TestBean());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String result;

        try {
            // Try to evaluate the test property of the TestBean.
            result = elp.eval("testBean.test");
            request.getServletContext().log(result);
        } catch (Exception e) {
            result = e.toString();
            request.getServletContext().log(e.toString());
        }

        response.getWriter().println(result);
    }

}
