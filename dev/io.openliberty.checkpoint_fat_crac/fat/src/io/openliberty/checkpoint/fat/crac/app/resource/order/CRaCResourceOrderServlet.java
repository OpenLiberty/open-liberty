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
package io.openliberty.checkpoint.fat.crac.app.resource.order;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
public class CRaCResourceOrderServlet extends FATServlet {
    final AtomicInteger currentResource = new AtomicInteger(3);

    class TestResource implements Resource {
        private final int expectedResource;

        public TestResource(int expectedResource) {
            this.expectedResource = expectedResource;
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            int actualResource = currentResource.getAndDecrement();
            assertEquals("Wrong resource called for beforeCheckpoint", expectedResource, actualResource);
            System.out.println("TESTING - beforeCheckpoint " + expectedResource);
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            int actualResource = currentResource.incrementAndGet();
            assertEquals("Wrong resource called for afterCheckpoint", expectedResource, actualResource);
            System.out.println("TESTING - afterRestore " + expectedResource);
        }
    }

    final List<TestResource> testResources;

    public CRaCResourceOrderServlet() {
        List<TestResource> newTestResources = new ArrayList<>();
        newTestResources.add(new TestResource(1));
        newTestResources.add(new TestResource(2));
        newTestResources.add(new TestResource(3));
        this.testResources = Collections.unmodifiableList(newTestResources);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(getClass().getSimpleName() + ": Registering test resources.");
        testResources.forEach((r) -> Core.getGlobalContext().register(r));
    }

    @Test
    public void testResourceOrder(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        resp.getWriter().println("Running test method " + this.getClass().getSimpleName());
    }
}
