/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.cdi.injectInjectionPointParam;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class InjectInjectionPointServlet extends HttpServlet {

    @Inject 
    TestBean bean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        if (bean.test()) {
            writer.write("passed");
        } else { 
            writer.write("failed");
        }
        writer.flush();
        writer.close();
    }

}
