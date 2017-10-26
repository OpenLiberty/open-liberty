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

public class JSFContainer {

    public static final String MOJARRA_APP_FACTORY = "com.sun.faces.application.ApplicationFactoryImpl";
    public static final String MYFACES_APP_FACTORY = "org.apache.myfaces.application.ApplicationFactoryImpl";

    public static enum JSF_PROVIDER {
        MOJARRA,
        MYFACES
    }

    public static boolean isBeanValidationEnabled() {
        return tryLoad("com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor") != null;
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

    /**
     * Try to load the given class. If the class is not found, null is returned.
     */
    public static Class<?> tryLoad(String className) {
        try {
            return Class.forName(className, false, JSFContainer.class.getClassLoader());
        } catch (ClassNotFoundException notFound) {
            return null;
        }
    }

    public static JSF_PROVIDER getJSFProvider() throws ClassNotFoundException {
        // First check manifest for the 'Implementation-Title' header
        String implTitle = javax.faces.application.ApplicationFactory.class.getPackage().getImplementationTitle();
        if (implTitle != null) {
            if (implTitle.toUpperCase().contains("MOJARRA"))
                return JSF_PROVIDER.MOJARRA;
            if (implTitle.toUpperCase().contains("MYFACES"))
                return JSF_PROVIDER.MYFACES;
        }

        // Fall back to classloading checks
        if (tryLoad(MOJARRA_APP_FACTORY) != null)
            return JSF_PROVIDER.MOJARRA;
        if (tryLoad(MYFACES_APP_FACTORY) != null)
            return JSF_PROVIDER.MYFACES;

        throw new ClassNotFoundException();
    }
}
