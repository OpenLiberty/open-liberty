/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public abstract class AbstractBeanServlet extends HttpServlet {

    public abstract Object getBean();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();

        String key = req.getParameter("key");
        if (key != null) {
            Object value = get(key);
            pw.println(key + "=" + value);
        } else {
            Object bean = getBean();
            Method[] allMethods = bean.getClass().getMethods();
            for (Method method : allMethods) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                    String mkey = method.getName().substring(3);
                    try {
                        Object value = method.invoke(bean);
                        pw.println(mkey + "=" + value);
                    } catch (Exception e) {
                        e.printStackTrace();
                        pw.println(mkey + "=" + e);
                    }
                }
            }
        }
    }

    public Object get(String key) {
        try {
            Object bean = getBean();
            Method method = bean.getClass().getMethod("get" + key);
            return method.invoke(bean);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.getCause();
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }
}
