/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package cdi.entity.listeners.test.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import javax.inject.Inject;

/**
 * 
 * The SimpleTestServlet is intended for demonstrating a straightforward issue where no user activity
 * is required aside from accessing the servlet with a web browser.
 * 
 * For more complicated scenarios where it is desirable to step-by-step demonstrate a problem with
 * navigable steps, use the ActionedTestServlet.
 * 
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SimpleTestServlet")
public class SimpleTestServlet extends HttpServlet {
    @PersistenceContext(unitName = "TestPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Inject
    EmptyBean bean;

    private static boolean fieldBridgeCalled = false;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        bean.getString();
        pw.println("<h1>JPA Simple Test Case</h1>");
        pw.println("<hr>");
        try {
            // We trigger this method to indirectly call the field bridge.
            CommonTestCode.populate(request, response, em, tx);
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(pw);
            pw.println("<br>");
        }        

        pw.println("field bridge called: " + fieldBridgeCalled);
    }

    public static void registerFieldBridgeCalled() {
        fieldBridgeCalled = true;
    }
}
