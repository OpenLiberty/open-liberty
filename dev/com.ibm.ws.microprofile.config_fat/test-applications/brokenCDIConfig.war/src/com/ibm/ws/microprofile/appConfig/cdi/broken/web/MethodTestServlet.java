/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.broken.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.appConfig.cdi.broken.beans.ConfigUnnamedMethodInjectionBean;
import com.ibm.ws.microprofile.appConfig.cdi.web.AbstractBeanServlet;

@SuppressWarnings("serial")
@WebServlet("/methodUnnamed")
public class MethodTestServlet extends AbstractBeanServlet {

    @Inject
    ConfigUnnamedMethodInjectionBean configBean3;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            super.doGet(req, resp);
        } catch (IllegalArgumentException t) {
            //we know this is going to break
            resp.getWriter().println("java.lang.IllegalArgumentException: " + t.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean3;
    }
}
