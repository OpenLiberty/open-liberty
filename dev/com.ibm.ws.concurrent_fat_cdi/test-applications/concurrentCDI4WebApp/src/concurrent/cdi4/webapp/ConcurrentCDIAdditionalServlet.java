/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package concurrent.cdi4.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDIAdditionalServlet extends FATServlet {

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    ManagedThreadFactory defaultManagedThreadFactory;

    @Inject
    TestBean testBean;

    // Task that will verify that the same ManagedThreadFactory is used throughout
    // the application and that the same Thread Context ClassLoader is used.
    public static Runnable getTCCLTask(final CompletableFuture<String> result) {
        return () -> {
            try {
                assertNotNull("Expected context classloader to be non-null",
                              Thread.currentThread().getContextClassLoader());

                System.out.println("Using thread context classloader: " + Thread.currentThread().getContextClassLoader());
            } catch (AssertionError e) {
                result.completeExceptionally(e);
            }

            try {
                Class.forName("java.lang.Integer"); //Exists as part of JVM

                Class.forName("concurrent.cdi4.webapp.TestException"); //Exists inside WAR/Web Module
            } catch (ClassNotFoundException e) {
                result.completeExceptionally(e);
            }

            // NOTE: The ConcurrentCDITest deploys both the ConcurrentCDITest.ear and concurrentCDIApp2.war
            // applications so this class should be available (just not by this application classloader)
            try {
                Class.forName("concurrent.cdi.web.MyAsync"); //Exists in a different application
                result.completeExceptionally(new IllegalStateException("Should not have been able to load a class from another application."));
            } catch (ClassNotFoundException e) {
                // expected
            }

            result.complete("SUCCESS");
        };
    }

    /**
     * Inject an instance of the default ManagedThreadFactory resource
     * and verify the Thread Context ClassLoader is scoped to the application.
     */
    @Test
    public void testInjectManagedThreadFactoryDefaultTCCLServlet() throws Exception {
        assertNotNull(defaultManagedThreadFactory);

        CompletableFuture<String> future = new CompletableFuture<>();
        Runnable task = getTCCLTask(future);

        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();

        String result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("SUCCESS", result);
    }

    /**
     * Inject an instance of the default ManagedThreadFactory resource into an app bean
     * and verify the Thread Context ClassLoader is scoped to the application.
     */
    @Test
    public void testInjectManagedThreadFactoryDefaultTCCLBean() throws Exception {
        assertNotNull(testBean);

        CompletableFuture<String> future = new CompletableFuture<>();
        Runnable task = getTCCLTask(future);

        testBean.runTaskUsingDefaultManagedThreadFactory(task);

        String result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("SUCCESS", result);
    }

    /**
     * Lookup an instance of the default ManagedThreadFactory resource using CDI
     * and verify the Thread Context ClassLoader is scoped to the application.
     */
    @Test
    public void testInjectManagedThreadFactoryDefaultTCCLLookup() throws Exception {
        Instance<ManagedThreadFactory> defaultManagedThreadFactoryInstance = CDI.current() //
                        .select(ManagedThreadFactory.class, new Annotation[] { Default.Literal.INSTANCE });

        assertTrue("ManagedTheadFactoryBean should have been avaialble with default qualifier",
                   defaultManagedThreadFactoryInstance.isResolvable());

        ManagedThreadFactory defaultManagedThreadFactory = defaultManagedThreadFactoryInstance.get();

        assertNotNull(defaultManagedThreadFactory);

        CompletableFuture<String> future = new CompletableFuture<>();
        Runnable task = getTCCLTask(future);

        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();

        String result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("SUCCESS", result);
    }
}
