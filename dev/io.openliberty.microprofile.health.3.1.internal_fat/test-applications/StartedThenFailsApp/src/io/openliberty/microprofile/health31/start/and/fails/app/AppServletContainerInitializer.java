/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health31.start.and.fails.app;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 *
 */
public class AppServletContainerInitializer implements ServletContainerInitializer {

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
        /*
         * When this error is thrown, we will see a CWWKZ0012I thrown in the logs.
         */
        System.out.println("AppServletContainerInitializer: onStartup()");
        throw new Error("Stop! An error!");
    }
}