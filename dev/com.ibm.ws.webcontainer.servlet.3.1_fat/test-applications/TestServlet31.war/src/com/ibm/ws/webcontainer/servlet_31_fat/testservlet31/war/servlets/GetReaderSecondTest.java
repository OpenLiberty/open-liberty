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
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/GetReaderSecondTest", asyncSupported = true)
public class GetReaderSecondTest extends HttpServlet {

    /**  */

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(GetReaderSecondTest.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        LOG.info("GetReaderSecondTest entering servlet");

        String charset = req.getCharacterEncoding();

        LOG.info("GetReaderSecondTest : charset =  " + charset);

        PrintWriter pWriter = res.getWriter();

        boolean isValid = true;

        try {
            isValid = req.getParameter("valid_charset").equalsIgnoreCase("TRUE");
        } catch (Exception exc) {
            LOG.info("GetReaderSecondTest : exception reading paarameter : " + exc);
            pWriter.println("FAIL1 : Exception reading parameter : " + exc);
            return;
        }

        pWriter.println("PASS1 : Parameter succesfully read.");

        LOG.info("GetReaderTest : charset =  " + charset + ", is valid = " + isValid);

        try {
            req.getReader();
        } catch (UnsupportedEncodingException use) {
            if (isValid) {
                pWriter.println("FAIL2 : Unexpected UnsupportedEncodingException from getReader.");
            } else {
                pWriter.println("PASS2 : Expected UnsupportedEncodingException from getReader.");
            }
            return;
        }

        if (isValid) {
            pWriter.println("PASS2 : getReader worked.");
        } else {
            pWriter.println("FAIL2 : No UnsupportedEncodingException from getReader.");
        }
    }
}
