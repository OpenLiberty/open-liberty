/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat.crac.app.request.succeed;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/crac-test", loadOnStartup = 1)
public class CRaCResourceRequestSucceedServlet extends FATServlet {

    class TestResource implements Resource {
        private final int expectedResource;

        public TestResource(int expectedResource) {
            this.expectedResource = expectedResource;
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            System.out.println("TESTING - beforeCheckpoint " + expectedResource);
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            System.out.println("TESTING - afterRestore " + expectedResource);
        }
    }

    final TestResource testResource = new TestResource(1);

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(getClass().getSimpleName() + ": Registering test resources.");
        Core.getGlobalContext().register(testResource);
        try {
            Core.checkpointRestore();
            System.out.println("TESTING - control back after restore");
        } catch (Exception e) {
            new ServletException(e);
        }
    }

    @Test
    public void testResourceInitiateCheckpoint(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        resp.getWriter().println("Running test method " + this.getClass().getSimpleName());
    }
}
