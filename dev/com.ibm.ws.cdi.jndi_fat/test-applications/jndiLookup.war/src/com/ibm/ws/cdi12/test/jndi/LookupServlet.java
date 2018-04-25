/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.jndi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ws.cdi12.test.jndi.observer.ObserverBean;

@WebServlet("")
public class LookupServlet extends HttpServlet {

    @Inject
    JNDIStrings jndiStrings;

    @Inject
    ObserverBean observerBean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();
        pw.append("From Config: " + jndiStrings.getFromConfig() + "\n");
        pw.append("From Bind: " + jndiStrings.getFromBind() + "\n");
        pw.append("From ObserverBean: " + observerBean.getResult() + "\n");
    }
}
