package com.ibm.ws.cdi12.aftertypediscovery.test;

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class AfterTypeServlet extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Inject
    AfterTypeInterface b;

    @Inject
    InterceptedBean ib;

    @Inject
    AfterTypeAlternativeInterface altOne;

    @Inject
    @UseAlternative
    AfterTypeAlternativeInterface altTwo;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        ib.doNothing();

        PrintWriter pw = response.getWriter();

        pw.write(b.getMsg() + System.lineSeparator());

        for (String s : GlobalState.getOutput()) {
            pw.write(s + System.lineSeparator());
        }

        pw.write("expecting one: " + altOne.getMsg() + System.lineSeparator());
        pw.write("expecting two: " + altTwo.getMsg() + System.lineSeparator());

        pw.flush();
        pw.close();
    }
}
