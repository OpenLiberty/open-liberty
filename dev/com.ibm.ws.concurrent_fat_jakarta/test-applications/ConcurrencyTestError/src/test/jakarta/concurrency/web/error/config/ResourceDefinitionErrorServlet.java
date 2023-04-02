/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency.web.error.config;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.fail;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;

@ManagedExecutorDefinition(name = "java:app/concurrent/executor-with-non-context-service",
                           context = "java:app/concurrent/env/not-a-context-service")
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/scheduled-executor-with-context-service-not-found",
                                    context = "java:comp/concurrent/does-not-exist")
@ManagedThreadFactoryDefinition(name = "java:module/concurrent/thread-factory-with-non-context-service",
                                context = "java:app/concurrent/env/not-a-context-service")
@ManagedThreadFactoryDefinition(name = "java:comp/concurrent/threadfactory-error-1", priority = -1)
@SuppressWarnings("serial")
@WebServlet("/*")
public class ResourceDefinitionErrorServlet extends FATServlet {
    @Resource(name = "java:app/concurrent/env/not-a-context-service", lookup = "java:comp/DefaultManagedExecutorService")
    ManagedExecutorService defaultManagedExecutor;

    /**
     * Attempt to use a ManagedExecutorService with its context service configured to be something that isn't a context service.
     */
    @AllowedFFDC("java.lang.IllegalStateException")
    @Mode(FULL)
    @Test
    public void testManagedExecutorWithInvalidContextService() throws Exception {
        try {
            ManagedExecutorService executor = InitialContext.doLookup("java:app/concurrent/executor-with-non-context-service");
            fail("Able to look up ManagedExecutorService that specifies a context service which is not a context service: " + executor);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause()) {
                String message = cause.getMessage();
                if (message != null && message.startsWith("CWWKC1201E"))
                    return; // pass
            }
            throw x;
        }
    }

    /**
     * Attempt to use a ManagedScheduledExecutorService that is configured with a context service that cannot be found.
     */
    @AllowedFFDC("java.lang.IllegalStateException")
    @Mode(FULL)
    @Test
    public void testManagedScheduledExecutorWithContextServiceNotFound() throws Exception {
        try {
            ManagedScheduledExecutorService executor = InitialContext.doLookup("java:comp/concurrent/scheduled-executor-with-context-service-not-found");
            fail("Able to look up ManagedScheduledExecutorService that specifies a context service which is not found: " + executor);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause()) {
                String message = cause.getMessage();
                if (message != null && message.startsWith("CWWKC1201E"))
                    return; // pass
            }
            throw x;
        }
    }

    /**
     * Attempt to use a ManagedThreadFactory with its context service configured to be something that isn't a context service.
     */
    @AllowedFFDC("java.lang.IllegalStateException")
    @Mode(FULL)
    @Test
    public void testManagedThreadFactoryWithInvalidContextService() throws Exception {
        try {
            ManagedThreadFactory threadFactory = InitialContext.doLookup("java:module/concurrent/thread-factory-with-non-context-service");
            fail("Able to look up ManagedThreadFactory that specifies a context service which is not a context service: " + threadFactory);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause()) {
                String message = cause.getMessage();
                if (message != null && message.startsWith("CWWKC1201E"))
                    return; // pass
            }
            throw x;
        }
    }

    /**
     * Attempt to use a ManagedThreadFactory that is configured with a priority value that is outside of the supported range.
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
