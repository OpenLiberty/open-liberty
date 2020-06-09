/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.constructor;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.extension.spi.test.constructor.exception.InterfaceRegisteredBean;
import com.ibm.ws.cdi.extension.spi.test.constructor.exception.ExtensionRegisteredBean;

@WebServlet("/")
public class TestServlet extends HttpServlet {

    @Inject
    InterfaceRegisteredBean bean;

    @Inject
    DummyBean db;

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        db.iExist();

        String unregString = "";

        try {
            ExtensionRegisteredBean ub = javax.enterprise.inject.spi.CDI.current().select(ExtensionRegisteredBean.class).get();
            unregString = "Found unregistered bean";
        } catch (UnsatisfiedResolutionException e) {
            unregString = "Could not find unregistered bean";
        }

        PrintWriter pw = response.getWriter();
        pw.println("Test Results:");
        pw.println(bean.toString());
        pw.println(unregString);

    }
}
