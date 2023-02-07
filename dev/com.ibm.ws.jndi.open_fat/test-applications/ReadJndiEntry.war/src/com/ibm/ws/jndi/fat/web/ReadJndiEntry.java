/*
 * =============================================================================
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet will read a JNDI entry and print it to the output.
 */
@SuppressWarnings("serial")
@WebServlet("/ReadJndiEntry")
public class ReadJndiEntry extends HttpServlet {

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("**BEGIN LOOKING FOR ENTRIES");
        // Get the OSGi bundle context so we can look up the service registrations
        findJndiEntry(String.class, "stringJndiEntry", writer);
        findJndiEntry(Double.class, "doubleJndiEntry", writer);
    }

    private void findJndiEntry(Class jndiClass, String jndiName, PrintWriter writer) {
        writer.println();
        writer.println("**LOOKING FOR " + jndiName);
        try {
            Context ctx = new InitialContext();
            Object o = ctx.lookup(jndiName);
            if (o == null) {
                writer.println("The JNDI entry for " + jndiName + " was null");
                return;
            }
            if (!!!(jndiClass.isInstance(o))) {
                writer.println("The JNDI entry for " + jndiName + " was of an unexpected type: " + o.getClass());
            }
            writer.println("JNDI Entry found for " + jndiName);
            writer.println("Value of JNDI Entry is: " + o);
        } catch (NamingException e) {
            writer.println("Caught exception: " + e);
            e.printStackTrace(writer);
        }
    }
}
