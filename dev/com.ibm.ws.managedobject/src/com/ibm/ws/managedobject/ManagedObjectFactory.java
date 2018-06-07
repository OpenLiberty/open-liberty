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

import java.lang.reflect.Constructor;

/**
 * An factory used by runtime containers to create objects that they managed.
 * Conceptually, this is a wrapper around {@link Class#newInstance}, though the
 * implementation might not actually create a new object, or it might associate
 * additional context with the object before returning it.
 */
public interface ManagedObjectFactory<T> {
    /**
     * Returns true if the factory is doing managed object creation as opposed to
     * being a simple wrapper around {@link Class#newInstance}.
     */
    boolean isManaged();

    /**
     * Returns true if this factory will manage all injection (both CDI and Java EE)
     * as well as all interceptors, including @AroundConstruct, @AroundInvoke,
     *
     * @PostConstruct and @PreDestroy. <p>
     *
     *                When true is returned, the consumer of this factory may assume that the
     *                result of {@link #createManagedObject} has already been injected and
     * @PostConstruct called. @PreDestroy will be called when {@link ManagedObjectContext#release} is called.
     */
    boolean managesInjectionAndInterceptors();

    /**
     * Returns the type of the managed object created by this factory.
     */
    Class<T> getManagedObjectClass();

    /**
     * Return the constructor that should be used by the caller with {@link #createArguments} to construct instances of the managed object.
     */
    Constructor<T> getConstructor();

    /**
     * Create a context that can be used to construct a new object.
     *
     * @return an object context or null if no context is required
     * @throws ManagedObjectException
     */
    ManagedObjectContext createContext() throws ManagedObjectException;

    /**
     * Create a managed object for the class managed by the factory
     *
     * @throws ManagedObjectException
     */
    ManagedObject<T> createManagedObject() throws ManagedObjectException;

    /**
     * Creates an instance of the managed object using an invocation context that
     * supports @AroundConstruct interceptors.
     *
     * @param invocationContext the @AroundConstruct interceptor invocation context
     * @return the managed object instance
     * @throws ManagedObjectException
     */
    ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException;

    /**
     * Creates a managed object around an existing instance.
     *
     * @param existingInstance the existing instance
     * @param invocationContext the @AroundConstruct interceptor invocation context
     * @return the managed object
     * @throws ManagedObjectException
     */
    ManagedObject<T> createManagedObject(T existingInstance, ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException;

}
