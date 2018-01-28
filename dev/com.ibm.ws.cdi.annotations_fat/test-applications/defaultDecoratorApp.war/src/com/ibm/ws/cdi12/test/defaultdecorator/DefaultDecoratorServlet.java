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
package com.ibm.ws.cdi12.test.defaultdecorator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.Conversation;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class DefaultDecoratorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Conversation c;

    private static List<String> output = new LinkedList<String>();

    public static void addOutput(String s) {
        output.add(s);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        c.isTransient();

        PrintWriter pw = response.getWriter();
        for (String s : output) {
            pw.write(s);
        }
        pw.flush();
        pw.close();

    }

}
