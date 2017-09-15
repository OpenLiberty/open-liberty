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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which prints an exception.
 */
@WebServlet("/SpecUsingServlet")
public class SpecUsingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SpecUsingServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        response.getWriter().println("This servlet uses specification classes, but you shouldn't see this message.");
        // Generate an exception which has a few lines of javax.servlet.* content in it
        Cookie cookie = new Cookie(null, null);
        response.addCookie(cookie);

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
