/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testhandlestypesclasses.servlets;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import testhandlestypesclasses.examples.TestServletContainerInitializer;

/**
 * This tests the functionality of the
 * ServletContainerInitializer.onStartup(Set<Class<?>> c, ServletContext ctx) API.
 *
 * This test expects a SCI "TestServletContainerInitializer" to be registered, which
 * will set an attribute under "ON_STARTUP_SET_KEY" containing a string representation
 * of the Set of classes passed to it during onStartup(). That set of classes should
 * not contain the classes which were specified as @HandlesTypes parameters; if only
 * "MyImpl", "MyAbstractClass", and "SubMyInterface" are rerturned then this servlet
 * will print "PASS", and otherwise it will print "FAIL".
 *
 * @See https://github.com/OpenLiberty/open-liberty/issues/16598
 *
 */
@WebServlet(urlPatterns = "/*", name = "GetHandlesTypesClassesServlet")
public class GetHandlesTypesClassesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("++++++++ Enter GetHandlesTypesClassesServlet");

        ServletContext context = request.getSession().getServletContext();
        String onStartupSet = (String) context.getAttribute(TestServletContainerInitializer.ON_STARTUP_SET_KEY);
        System.out.println("++++++++ classes passed to onStartup(): [ " + onStartupSet + " ]");

        if (onStartupSet != null && !onStartupSet.isEmpty() && onStartupSet.contains("testhandlestypesclasses.examples.MyImpl")
            && onStartupSet.contains("testhandlestypesclasses.examples.MyAbstractClass") && onStartupSet.contains("testhandlestypesclasses.examples.SubMyInterface")
            && !!!onStartupSet.contains("testhandlestypesclasses.examples.MyInterface") && !!!onStartupSet.contains("testhandlestypesclasses.examples.AbstractClass")) {
            response.getWriter().append("PASS: TestServletContainerInitializer.onStartup() was passed the correct set of @HandlesTypes implementation classes");
        } else {
            response.getWriter().append("FAIL: TestServletContainerInitializer.onStartup() was passed unexpected classes:\n" + onStartupSet);
        }

        System.out.println("-------- Exit GetHandlesTypesClassesServlet");
    }

}
