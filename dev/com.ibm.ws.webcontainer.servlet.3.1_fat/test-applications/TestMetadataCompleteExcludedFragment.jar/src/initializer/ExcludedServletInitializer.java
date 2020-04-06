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
package initializer;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.http.HttpServlet;

import teststorage.SingletonStore;

@HandlesTypes({ HttpServlet.class })
public class ExcludedServletInitializer implements ServletContainerInitializer {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */

    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {

        if (arg0 == null)
            return;

        // SingletonStore.getInstance().called();
        for (Class<?> clazz : arg0) {
            System.out.println("Clazz: " + clazz);
            SingletonStore.getInstance().pushCall("ExcludedServletInitializer: " + clazz.getName());
        }

    }

}
