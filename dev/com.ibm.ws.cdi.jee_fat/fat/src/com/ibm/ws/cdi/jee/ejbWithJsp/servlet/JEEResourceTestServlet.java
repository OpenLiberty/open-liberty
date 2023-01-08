/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi.jee.ejbWithJsp.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean1;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean2;

@WebServlet("/")
public class JEEResourceTestServlet extends HttpServlet {

    @Inject
    HelloWorldExtensionBean2 hello;

    @EJB
    MySessionBean1 bean1;

    @EJB
    MySessionBean2 bean2;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.write(bean1.hello());
        pw.write(bean2.hello());
        pw.flush();
        pw.close();
    }

}
