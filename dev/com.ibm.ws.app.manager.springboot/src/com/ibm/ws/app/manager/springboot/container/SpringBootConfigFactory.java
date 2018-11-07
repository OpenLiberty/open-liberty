/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * A factory for creating Spring Boot configurations for configuring an embedded
 * container. The embedded container can be a web container or some other type
 * of container, for example, a reactive container.
 * <p>
 * A factory is a singleton for a Spring Boot application.
 */
public interface SpringBootConfigFactory {

    /**
     *
     * @return Used to synchronize with the spring application
     *         initialization completion.
     */
    CountDownLatch getApplicationReadyLatch();

    /**
     * Creates a new Spring Boot configuration.
     *
     * @return a new Spring Boot configuration.
     */
    SpringBootConfig createSpringBootConfig();

    /**
     * Adds a hook that will be called when the application is requested to stop.
     *
     * @param hook the shutdown hook to add
     */
    void addShutdownHook(Runnable hook);

    /**
     * Removes a shutdown hook.
     *
     * @param hook the shutdown hook to remove
     */
    void removeShutdownHook(Runnable hook);

    /**
     * Informs the factory that the root application context
     * of the Spring Boot application has been closed
     */
    void rootContextClosed();

    /**
     * Returns the root server directory
     *
     * @return the root server directory
     */
    File getServerDir();

    /**
     * Finds the Spring Boot configuration factory for a Spring Boot application based
     * on the class loader of a given token object.
     *
     * @param token an object with a class that is loaded by the class loader of
     *                  the Spring Boot application
     * @return The Spring Boot configuration factory for a Spring Boot application.
     */
    static SpringBootConfigFactory findFactory(Object token) {
        // A SpringBootConfigFactory is registered with the gateway bundle for the application
        // here we find the gateway bundle by looking for a BundleReference in the class
        // loader hierarchy.
        ClassLoader cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> token.getClass().getClassLoader());
        while (cl != null && (!(cl instanceof BundleReference))) {
            cl = cl.getParent();
        }
        if (cl == null) {
            throw new IllegalStateException("Did not find a BundleReference class loader.");
        }

        final Bundle b = ((BundleReference) cl).getBundle();
        SpringBootConfigFactory result = AccessController.doPrivileged((PrivilegedAction<SpringBootConfigFactory>) () -> {
            ServiceReference<?>[] services = b.getRegisteredServices();
            if (services != null) {
                for (ServiceReference<?> service : services) {
                    String[] objectClass = (String[]) service.getProperty(Constants.OBJECTCLASS);
                    for (String name : objectClass) {
                        if (SpringBootConfigFactory.class.getName().equals(name)) {
                            BundleContext context = b.getBundleContext();
                            try {
                                return (SpringBootConfigFactory) context.getService(service);
                            } finally {
                                context.ungetService(service);
                            }
                        }
                    }
                }
            }
            return null;
        });
        if (result == null) {
            throw new IllegalStateException("No SpringBootConfigFactory service found for: " + b);
        }
        return result;
    }
}
