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
package com.ibm.ws.classloading;

/**
 * Internal-use-only extension/addition to the ClassLoadingService SPI.
 */
public interface ClassLoaderIdentifierService {
    /**
     * Returns the identifier for the specified classloader.
     * 
     * @param classloader the classloader.
     * @return the identifier for the specified classloader.
     * @throws IllegalArgumentException if the classloader implementation is not recognized by the service.
     */
    String getClassLoaderIdentifier(ClassLoader classloader) throws IllegalArgumentException;

    /**
     * Returns the identifier for the context classloader of the given app / module / component.
     * 
     * @param type the module/comp type, either "WEB" or "EJB"
     * @param appName the application name
     * @param moduleName the module within the application
     * @param componentName the component within the module
     * 
     * @return the context classloader id
     */
    String getClassLoaderIdentifier(String type, String appName, String moduleName, String componentName);

    /**
     * Returns the classloader for the specified identifier. Null if no such classloader exists.
     * 
     * @param identifier the identifier for the classloader.
     * @return the classloader for the specified identifier. Null if no such classloader exists.
     * @throws IllegalArgumentException if the identifier is not recognized by the service.
     */
    ClassLoader getClassLoader(String identifier) throws IllegalArgumentException;

}
