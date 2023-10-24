/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.servlet;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 *
 */
public class ServletInitializer implements ServletContainerInitializer {

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
        // Throwing an error breaks CDI in unfortunate ways
        throw new Error("Test error");
    }
}
