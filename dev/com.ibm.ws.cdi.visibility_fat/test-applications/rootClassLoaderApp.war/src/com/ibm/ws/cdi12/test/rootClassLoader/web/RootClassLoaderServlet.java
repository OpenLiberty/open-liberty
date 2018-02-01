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
package com.ibm.ws.cdi12.test.rootClassLoader.web;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.rootClassLoader.extension.OSName;

@WebServlet("/")
public class RootClassLoaderServlet extends HttpServlet {

    @Inject
    Random random;

    @Inject
    Timer timer;

    @Inject
    @OSName
    String osName;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("random: " + random.nextInt());
        response.getWriter().write("\n");
        response.getWriter().write("Timer: " + timer);
        response.getWriter().write("\n");
        response.getWriter().write("OS Name: " + osName);
        response.getWriter().write("\n");
        if (random != null && timer != null && osName != null) {
            response.getWriter().write("done");
            response.getWriter().write("\n");
        }
    }

}
