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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.beans;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors.ServiceMethodType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.interfaces.SendResponse;

/**
 *
 */
public class InterceptedBean {

    protected String value;

    /**
     * @param string
     */
    public void sendResponse(SendResponse sr, String resp, PrintWriter out) {
        sr.sendResponse(resp, out);
    }

    @ServiceMethodType
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        String interceptor = (String) req.getAttribute("Interceptor");

        if (interceptor != null && interceptor.equals("ServiceMethodInterceptor2"))
            out.println("Test Passed! InterceptedBean : ServiceMethodInterceptor was called.");
        else
            out.println("Test Failed! InterceptedBean : ServiceMethodInterceptor was not called.");
    }

}
