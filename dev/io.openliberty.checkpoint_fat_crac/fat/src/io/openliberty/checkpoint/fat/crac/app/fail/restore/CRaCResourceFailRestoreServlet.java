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
package io.openliberty.checkpoint.fat.crac.app.fail.restore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/crac-test", loadOnStartup = 1)
public class CRaCResourceFailRestoreServlet extends HttpServlet {
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
            throw new RuntimeException("TESTING - restore failed " + expectedResource);
        }
    }

    final List<TestResource> testResources;

    public CRaCResourceFailRestoreServlet() {
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
}
