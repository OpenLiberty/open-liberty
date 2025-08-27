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
package concurrent.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * An application scoped bean that inject
 */
@ApplicationScoped
public class AppBean {

    @Inject
    private ManagedThreadFactory defaultManagedThreadFactory;

    public void testDefaultManagedThreadFactoryClassloader(CompletableFuture<String> future) {
        assertNotNull(defaultManagedThreadFactory);

        // Requires the application's classloader (to access application scoped classes)
        Runnable task = () -> {
            try {
                assertNotNull("Expected context classloader to be non-null",
                              Thread.currentThread().getContextClassLoader());

                assertEquals("Expected context classloader and MyAsync classloader to be the same",
                             MyAsync.class.getClassLoader(),
                             Thread.currentThread().getContextClassLoader());
            } catch (AssertionError e) {
                future.completeExceptionally(e);
            }

            try {
                Class.forName("java.lang.Integer"); //Exists as part of JVM
                Class.forName("concurrent.cdi.web.MyAsync"); //Exists inside Web Module
                Class.forName("concurrent.cdi.ext.ConcurrentCDIExtension"); // Exists outside Web Module
                future.complete("SUCCESS");
            } catch (ClassNotFoundException e) {
                future.completeExceptionally(e);
            }
        };

        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();
    }

}
