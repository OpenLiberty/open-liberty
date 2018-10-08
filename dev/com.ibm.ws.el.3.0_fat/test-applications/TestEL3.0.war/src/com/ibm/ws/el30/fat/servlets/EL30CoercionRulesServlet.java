/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.el.ExpressionFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet test the coercion rule 1.23.1 which states:
 * To Coerce a Value X to Type Y
 * If X is null and Y is not a primitive type and also not a String, return null.
 */
@WebServlet("/EL30CoercionRulesServlet")
public class EL30CoercionRulesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public EL30CoercionRulesServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ExpressionFactory factory = ExpressionFactory.newInstance();
        PrintWriter pw = response.getWriter();

        // Initialize number Integer to null
        Integer number = null;

        try {
            // Use the ExpressionFactory coerceToType method to coerce an Integer to a Double
            pw.println("Testing Coercion of a Value X to Type Y.");
            pw.println("Test if X is null and Y is not a primitive type and also not a String, (Expected return null): " + factory.coerceToType(number, java.lang.Double.class));
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
    }
}
