/*
 * =============================================================================
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.fat.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet will read a JNDI URL entry and print it to the output.
 */
@SuppressWarnings("serial")
@WebServlet("/ReadJndiURLEntry")
public class ReadJndiURLEntry extends HttpServlet {

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("**BEGIN LOOKING FOR ENTRIES");
        // Get the OSGi bundle context so we can look up the service registrations
        URL url1 = findJndiURLEntry("stringJndiURLEntry", writer);
        URL url2;
        try {
            url2 = (URL) new InitialContext().lookup("stringJndiURLEntry");
        } catch (NamingException e) {
            url2 = null;
            e.printStackTrace();
        }
        if (url1 != null && url2 != null && url1 != url2) {
            //test that we have two different URL objects - required by the spec
            writer.println("Multiple lookups of the same string resulted in different URL instances - as expected");
        }
    }

    private URL findJndiURLEntry(String jndiURLName, PrintWriter writer) {
        URL url = null;
        writer.println();
        writer.println("**LOOKING FOR " + jndiURLName);
        try {
            Context ctx = new InitialContext();
            Object o = ctx.lookup(jndiURLName);
            if (o == null) {
                writer.println("The JNDI URL entry for " + jndiURLName + " was null");
                return null;
            }
            if (!!!(URL.class.isInstance(o))) {
                writer.println("The JNDI URL entry for " + jndiURLName + " was of an unexpected type: " + o.getClass());
                return null;
            }
            url = (URL) o;
            writer.println("JNDI URL Entry found for " + jndiURLName);
            writer.println("Value of JNDI URL Entry is: " + o.toString());
        } catch (NamingException e) {
            writer.println("Caught exception: " + e);
            e.printStackTrace(writer);
        }
        return url;
    }
}
