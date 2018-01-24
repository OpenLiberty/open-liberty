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
package com.ibm.ws.logging.fat.broken.servlet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which prints an exception.
 */
@WebServlet("/ExceptionPrintingServlet")
public class ExceptionPrintingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ExceptionPrintingServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        response.getWriter().println("Well, hello there. This servlet is working.");

        // Generate a few lines of java stack trace
        Set<ExceptionGeneratingObject> set = new HashSet<ExceptionGeneratingObject>();
        set.add(new ExceptionGeneratingObject(true));
        set.add(new ExceptionGeneratingObject(false));

        response.getWriter().println("There should be an exception in your logs.");

    }

    static class SpecialPrintingException extends Exception {

        private static final long serialVersionUID = 1L;

    }

    static class ExceptionGeneratingObject {
        private final boolean shouldPrintException;

        public ExceptionGeneratingObject(boolean b) {
            shouldPrintException = b;
        }

        @Override
        public int hashCode() {
            if (shouldPrintException) {
                new SpecialPrintingException().printStackTrace();
                return 1;
            } else {
                return -1;
            }
        }
    }

}
