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

@WebServlet(urlPatterns = "/GetReaderFirstTest", asyncSupported = true)
public class GetReaderFirstTest extends HttpServlet {

    /**  */

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(GetReaderFirstTest.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        LOG.info("GetReaderFirstTest entering servlet");

        String charset = req.getCharacterEncoding();

        PrintWriter pWriter = res.getWriter();

        boolean isValid = true, useThrown = false;

        try {
            req.getReader();
        } catch (UnsupportedEncodingException use) {
            useThrown = true;
        }

        LOG.info("GetReaderFirstTest : charset =  " + charset + ", useThrown = " + useThrown);

        try {
            isValid = req.getParameter("valid_charset").equalsIgnoreCase("TRUE");
        } catch (Exception exc) {
            LOG.info("GetReaderFirstTest : exception reading paarameter : " + exc);
            pWriter.println("FAIL1 : Exception reading parameter : " + exc);
            return;
        }
        pWriter.println("PASS1 : Parameter succesfully read.");

        LOG.info("GetReaderFirstTest : charset =  " + charset + ", is valid = " + isValid + " useThrown = " + useThrown);

        if (isValid && useThrown) {
            pWriter.println("FAIL2 : Unexpected UnsupportedEncodingException from getReader.");
        } else if (!isValid && useThrown) {
            pWriter.println("PASS2 : Expected UnsupportedEncodingException from getReader.");
        } else if (isValid && !useThrown) {
            pWriter.println("PASS2 : getReader worked.");
        } else {
            pWriter.println("FAIL2 : No UnsupportedEncodingException from getReader.");
        }

        // try another getReader()
        try {
            req.getReader();
        } catch (UnsupportedEncodingException use) {
            if (isValid) {
                pWriter.println("FAIL3 : Unexpected UnsupportedEncodingException from second getReader.");
            } else {
                pWriter.println("PASS3 : Expected UnsupportedEncodingException from second getReader.");
            }
            return;
        }

        if (isValid) {
            pWriter.println("PASS3 : getReader worked.");
        } else {
            pWriter.println("FAIL3 : No UnsupportedEncodingException from getReader.");
        }
    }
}
