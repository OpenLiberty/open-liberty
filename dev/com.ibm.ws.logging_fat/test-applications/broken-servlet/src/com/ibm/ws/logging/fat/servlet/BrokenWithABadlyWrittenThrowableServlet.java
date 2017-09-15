/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which throws an exception. The getStackTrace() method on the exception returns null.
 */
@WebServlet("/BrokenWithABadlyWrittenThrowableServlet")
public class BrokenWithABadlyWrittenThrowableServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public BrokenWithABadlyWrittenThrowableServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        // Whoops, we seem to have a problem! Oh dear, how unexpected!
        throw new BadlyWrittenException();

    }

    static class BadlyWrittenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        @Override
        public StackTraceElement[] getStackTrace() {
            // Can our logging code handle this?
            return null;
        }

    }

}
