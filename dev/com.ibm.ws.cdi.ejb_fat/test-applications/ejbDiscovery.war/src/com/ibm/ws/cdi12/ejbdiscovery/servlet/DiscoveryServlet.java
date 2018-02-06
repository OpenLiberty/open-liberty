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
package com.ibm.ws.cdi12.ejbdiscovery.servlet;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.ejbdiscovery.extension.DiscoveryExtension;

@SuppressWarnings("serial")
@WebServlet("/")
public class DiscoveryServlet extends HttpServlet {

    @Inject
    private DiscoveryExtension extension;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        ServletOutputStream out = resp.getOutputStream();

        for (Class<?> clazz : extension.getObservedTypes()) {
            out.println("Observed type: " + clazz.getName());
        }

        for (Class<?> clazz : extension.getObservedBeans()) {
            out.println("Observed bean: " + clazz.getName());
        }

        for (Type type : extension.getObservedBeanTypes()) {
            out.println("Observed bean type: " + type.toString());
        }
    }

}
