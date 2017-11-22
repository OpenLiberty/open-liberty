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
package com.ibm.ws.cdi12.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

    private final String message = "Hello World!";

    @Inject
    TestBean bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        bean.setMessage(this.message);
        PrintWriter out = response.getWriter();
        out.println(getResponse());
    }

    private String getResponse() {
        if (bean.getMessage().equals(this.message)) {
            return "PASSED";
        }
        else {
            return ("FAILED message received was " + bean.getMessage());
        }
    }
}
