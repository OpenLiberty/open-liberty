/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package listeners;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Test ServletContainerInitializer to add a ServletContextListener in a programmatic way.
 */
public class MyServletContainerInitializer implements ServletContainerInitializer {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext sc) throws ServletException {
        // Lets add a ServletContextListener in a programmatic way and then call one of the methods that should throw 
        // an UnsupportedOperationException in the ServletContextListener.
        sc.addListener(MyProgrammaticServletContextListener.class);
    }

}
