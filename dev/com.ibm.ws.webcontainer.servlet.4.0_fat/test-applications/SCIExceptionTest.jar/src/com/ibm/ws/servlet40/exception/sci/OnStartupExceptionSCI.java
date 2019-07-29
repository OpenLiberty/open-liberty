/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.servlet40.exception.sci;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * A simple ServletContainerInitializer to throw an Exception when onStartup is invoked.
 */
public class OnStartupExceptionSCI implements ServletContainerInitializer {

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
        // Throw an exception to test the translated message.
        throw new ServletException("Test Exception thrown from OnStartupExceptionSCI.onStartup");

    }

}
