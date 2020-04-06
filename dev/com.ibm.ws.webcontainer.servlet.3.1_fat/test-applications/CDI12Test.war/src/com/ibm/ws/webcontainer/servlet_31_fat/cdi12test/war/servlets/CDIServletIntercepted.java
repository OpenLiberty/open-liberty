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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.beans.InterceptedBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors.SendResponseType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors.ServiceMethodType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.interfaces.SendResponse;

// sendResponse method is intercepted

@WebServlet("/CDIServletIntercepted")
public class CDIServletIntercepted extends HttpServlet implements SendResponse {

    private static final long serialVersionUID = 1L;

    @Inject
    InterceptedBean bean;

    @Override
    @ServiceMethodType
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        String interceptor = (String) req.getAttribute("Interceptor");

        out.println("interceptor attribute = " + interceptor);
        if (interceptor != null && interceptor.equals("ServiceMethodInterceptor1"))
            bean.sendResponse(this, "Test Passed! CDIServletIntercepted : ServiceMethodInterceptor was called.", out);
        else
            bean.sendResponse(this, "Test Failed! CDIServletIntercepted : ServiceMethodInterceptor was not called.", out);

        bean.service(req, resp);
    }

    @Override
    @SendResponseType
    public void sendResponse(String resp, PrintWriter out) {
        out.println(resp);
        if (resp.contains(":SendResponseInterceptor:")) {
            out.println("Test Passed! CDIServletIntercepted : SendResponseInterceptor was called.");
        } else {
            out.println("Test Failed! CDIServletIntercepted : SendResponseInterceptor was not called.");
        }
    }
}
