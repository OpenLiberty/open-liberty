/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.WriteListener;


/**
 * Servlet that writes a string with multi-byte characters to the ServletOutputStream
 */
@WebServlet(urlPatterns = "/MultiByteEncodingServlet", asyncSupported = true)
public class MultiByteEncodingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public MultiByteEncodingServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!Boolean.parseBoolean(request.getParameter("isAsync"))) {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");    
            try (ServletOutputStream sos = response.getOutputStream()) {
                // verify both print and println
                sos.print("Привет ");
                sos.println("мир");
            }
        }
        else {
            AsyncContext ac = request.startAsync();
            ac.getResponse().setContentType("text/html");
            ac.getResponse().setCharacterEncoding("UTF-8");
            ServletOutputStream sos = response.getOutputStream();
            sos.setWriteListener(new MultiByteEncodingServletWriteListener(sos, ac));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * WriteListener implementation
     */
    class MultiByteEncodingServletWriteListener implements WriteListener {

        private ServletOutputStream output = null;
        private AsyncContext ac = null;

        /**
         * @param s
         * @param c
         */
        public MultiByteEncodingServletWriteListener(ServletOutputStream s, AsyncContext c) {
            output = s;
            ac = c;
        }
    
        @Override
        public void onWritePossible() throws IOException {
            if (output.isReady()) {
                output.println("Привет мир");
                ac.complete();
            }
        }
    
        @Override
        public void onError(final Throwable t) {
            String outError = t.getMessage();
            if (output.isReady()) {
                try {
                    output.print("MultiByteEncodingServletWriteListener onError method is called ! " + outError);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ac.complete();
                }
            }
        }
    }
}
