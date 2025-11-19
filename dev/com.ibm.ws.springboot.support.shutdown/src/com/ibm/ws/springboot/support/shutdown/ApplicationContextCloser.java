/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.shutdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;

public class ApplicationContextCloser implements EnvironmentPostProcessor {
    private static final Object token = new Object() {
    };

    /**
     * Generate the resource name for a specified class.
     *
     * Convert '.' into '/' and append '.class'.
     *
     * Note: This will not work for inner classes, which convert one or
     * more '.' into '$' instead of '/'.
     *
     * @param className A fully qualified non-inner class name.
     *
     * @return The name of the resource of the class.
     */
    protected static String asResourceName(String className) {
        return className.replace('.', '/') + ".class";
    }

    /**
     * Tell if a class is available in the current classloading environment.
     *
     * That is, tell if the resource of the class is available using this class's
     * classloader.
     *
     * Note: This does not work for inner classes. See {@link #asResourceName(String)}.
     *
     * @param className The name of the class which is to be located.
     *
     * @return True or false telling if the class resource was located.
     */
    protected static boolean isClassAvailable(String className) {
        ClassLoader classLoader = FeatureAuditor.class.getClassLoader();
        String resourceName = asResourceName(className);
        boolean foundClass = (classLoader.getResource(resourceName) != null);

        // System.out.println("FeatureAuditor: Found [ " + foundClass + " ] class [ " + className + " ] as [ " + resourceName + " ]");
        // System.out.println("FeatureAuditor: Using [ " + classLoader + " ]");

        return foundClass;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        boolean updatedEnvironmentProcessorAvailable = isClassAvailable("org.springframework.boot.EnvironmentPostProcessor");
        // org.springframework.boot.EnvironmentPostProcessor was added in Spring Boot version 4.0.0-M3.
        // They decided to keep the lagacy org.springframework.boot.env.EnvironmentPostProcessor from Spring Boot versions 4.0.0-RC1 until 4.2.0 (See https://github.com/spring-projects/spring-boot/issues/47272)
        // Once the legacy org.springframework.boot.env.EnvironmentPostProcessor is removed, this class (com.ibm.ws.springboot.support.shutdown.ApplicationContextCloser) will no longer be looked up by the spring code.
        // And the class which implements the org.springframework.boot.EnvironmentPostProcessor will only be looked up.
        if (updatedEnvironmentProcessorAvailable) {
            return;
        }

        if (env.getPropertySources().contains("bootstrap")) {
            return;
        }
        final SpringBootConfigFactory factory = SpringBootConfigFactory.findFactory(token);
        app.addInitializers((c) -> {
            factory.addShutdownHook(() -> {
                c.close();
            });
            c.addApplicationListener((e) -> {
                if (e instanceof ContextClosedEvent) {
                    factory.rootContextClosed();
                } else if (e instanceof ApplicationReadyEvent) {
                    factory.getApplicationReadyLatch().countDown();
                }
            });
        });
    }
}
