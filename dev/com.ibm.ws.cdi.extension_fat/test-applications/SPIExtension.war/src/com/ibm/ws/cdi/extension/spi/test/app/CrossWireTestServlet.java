/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.extension.spi.test.bundle.UnregisteredBean;
import com.ibm.ws.cdi.extension.spi.test.bundle.extension.MyExtensionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.producer.MyProducedString;

@WebServlet("/CrossWire")
public class CrossWireTestServlet extends HttpServlet {

    @Inject
    OuterBean outerBean;

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        PrintWriter pw = response.getWriter();
        pw.println("Test Results:");
        pw.println(outerBean.toString());

    }
}
