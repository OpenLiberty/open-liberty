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
package com.ibm.ws.cdi12.test.passivation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class PassivationServlet extends HttpServlet {

    @Inject
    BeanHolder bh;

    int i = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        bh.doNothing();

        PrintWriter pw = response.getWriter();

        for (String s : GlobalState.getOutput()) {
            pw.write(s);
        }

        pw.flush();
        pw.close();

    }

    @Inject
    public void transientVisit(@TransientReference TransiantDependentScopedBeanTwo bean) {
        bean.doNothing();
    }

}
