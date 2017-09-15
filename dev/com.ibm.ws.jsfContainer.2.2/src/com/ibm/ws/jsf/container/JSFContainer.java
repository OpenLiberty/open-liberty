/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container;

import javax.faces.application.Application;

public class JSFContainer {

    public static boolean isBeanValidationEnabled() {
        try {
            Class.forName("com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void initializeBeanValidation() {
        try {
            // Cannot directly reference class because it would trigger a classload of classes
            // that may not be available if the beanValidation-1.1 feature is not enabled
            Class<?> BvalJSFInitializer = Class.forName("com.ibm.ws.jsf.container.bval.BvalJSFInitializer");
            BvalJSFInitializer.getMethod("initialize").invoke(null);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static boolean isCDIEnabled() {
        try {
            Class.forName("javax.enterprise.inject.spi.BeanManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void initializeCDI(Application application) {
        try {
            // Cannot directly reference class because it would trigger a classload of classes
            // that may not be available if the cdi-1.X feature is not enabled
            Class<?> CDIJSFInitializer = Class.forName("com.ibm.ws.jsf.container.cdi.CDIJSFInitializer");
            CDIJSFInitializer.getMethod("initialize", Application.class).invoke(null, application);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

}
