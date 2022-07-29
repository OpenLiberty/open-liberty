/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency.web.error.config;

import static org.junit.Assert.fail;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@ManagedThreadFactoryDefinition(name = "java:comp/concurrent/threadfactory-error-1", priority = -1)
@SuppressWarnings("serial")
@WebServlet("/*")
public class ResourceDefinitionErrorServlet extends FATServlet {
    /**
     */
    @Test
    public void testPriorityOutOfRange() throws Exception {
        try {
            ManagedThreadFactory threadFactory = InitialContext.doLookup("java:comp/concurrent/threadfactory-error-1");
            Thread thread = threadFactory.newThread(() -> {
            });
            fail("Able to create thread with negative priority: " + thread);
        } catch (IllegalArgumentException x) {
            // expected for priority = -1
        }
    }
}
