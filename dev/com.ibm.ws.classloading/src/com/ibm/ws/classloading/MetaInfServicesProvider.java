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
package com.ibm.ws.classloading;

/**
 * Interface for a service that makes services providers in META-INF/services available to the thread context class loader.
 * Service properties must include
 * <ul>
 * <li>implementation.class - String value indicating the fully qualified name of the service provider implementation class</li>
 * <li>file.path - String value indicating the path within the JAR file, excluding the initial / for META-INF/services/{fully.qualified.interface.name}</li>
 * <li>file.url - URL value indicating the location of the META-INF/services/{fully.qualified.interface.name} file</li>
 * </ul>
 */
public interface MetaInfServicesProvider {
    /**
     * Returns the implementation class of the service provider.
     *
     * @return the implementation class of the service provider.
     */
    Class<?> getProviderImplClass();
}
