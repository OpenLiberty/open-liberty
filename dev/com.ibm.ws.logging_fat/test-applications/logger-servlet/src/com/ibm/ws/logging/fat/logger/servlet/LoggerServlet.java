/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat.logger.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
@SuppressWarnings("serial")
public class LoggerServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(LoggerServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Hello, this is just a SystemOut message!");
        System.err.println("Bye, this is just a SystemErr message!");
        response.getWriter().println("Hello world!");
        // Use severe, which is higher than AUDIT, to ensure this message would
        // normally show up in console.log if output wasn't disabled.
        logger.severe("Hello world!");
    }
}
