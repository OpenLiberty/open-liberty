/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.appConfig.cdi.beans.SessionScopedConfigFieldInjectionBean;

@SuppressWarnings("serial")
@WebServlet("/system")
public class SysPropServlet extends HttpServlet {

    @Inject
    SessionScopedConfigFieldInjectionBean configBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();

        String key = req.getParameter("key");
        String value = req.getParameter("value");
        if (value != null && !"".equals(value)) {
            System.setProperty(key, value);
        } else {
            Object v = get(key);
            value = v == null ? "null" : v.toString();
        }

        pw.println(key + "=" + value);
    }

    public Object get(String key) {
        try {
            Method method = configBean.getClass().getMethod("get" + key);
            return method.invoke(configBean);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.getCause();
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }
}
