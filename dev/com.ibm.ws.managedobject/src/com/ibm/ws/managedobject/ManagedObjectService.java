/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public interface ManagedObjectService {
    /**
     * Creates a factory for a specified class.
     *
     * Note that this method will only work if the class is in the unrestricted set of bean types
     * for the managed bean. The {@link #createEJBManagedObjectFactory(Class<?>, String) createEJBManagedObjectFactory} method below is recommended for instantiating EJB classes.
     *
     * @param <T> the type of object being managed
     *
     * @param mmd the ModuleMetaData for the module which contains the managed object
     * @param klass the class of instances to create
     * @param requestManagingInjectionAndInterceptors true requests that the returned ManagedObjectFactory
     *            will perform dependency injection when creating an instance of the managed object and
     *            the instance will handle interceptors, including around construct.
     * @return the managed object factory
     * @throws ManagedObjectException if an exception occurs creating the factory
     */
    <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManagingInjectionAndInterceptors) throws ManagedObjectException;

    /**
     * Creates a factory for a specified class.
     *
     * Note that this method will only work if the class is in the unrestricted set of bean types
     * for the managed bean. The {@link #createEJBManagedObjectFactory(Class<?>, String) createEJBManagedObjectFactory} method below is recommended for instantiating EJB classes.
     *
     * @param <T> the type of object being managed
     *
     * @param mmd the ModuleMetaData for the module which contains the managed object
     * @param klass the class of instances to create
     * @param requestManagingInjectionAndInterceptors true requests that the returned ManagedObjectFactory
     *            will perform dependency injection when creating an instance of the managed object and
     *            the instance will handle interceptors, including around construct.
     * @param referenceContext the referenecContext which will be used for dependancy injection. If null, the WeldCreationalContext will be used.
     * @return the managed object factory
     * @throws ManagedObjectException if an exception occurs creating the factory
     */
    <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManagingInjectionAndInterceptors,
                                                           ReferenceContext referenceContext) throws ManagedObjectException;

    /**
     * Creates a factory for a specified ejb class.
     *
     * @param <T> the type of object being managed
     *
     * @param mmd the ModuleMetaData for the module which contains the managed object
     * @param klass the class of instances to create
     * @param ejbName the enterprise bean name of the ejb to be instantiated
     * @return the managed object factory
     * @throws ManagedObjectException if an exception occurs creating the factory
     */
    <T> ManagedObjectFactory<T> createEJBManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, String ejbName) throws ManagedObjectException;

    /**
     * Creates a factory for a specified interceptor class.
     *
     * @param <T> the type of object being managed
     *
     * @param mmd the ModuleMetaData for the module which contains the managed object
     * @param klass the class of instances to create
     * @return the managed object factory
     * @throws ManagedObjectException if an exception occurs creating the factory
     */
    <T> ManagedObjectFactory<T> createInterceptorManagedObjectFactory(ModuleMetaData mmd, Class<T> klass) throws ManagedObjectException;

}
