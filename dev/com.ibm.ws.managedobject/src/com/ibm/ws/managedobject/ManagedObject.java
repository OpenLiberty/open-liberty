/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject;

/**
 * A convenience wrapper around an object and its context. Instances are created
 * using {@link ManagedObjectFactory#create} and are disposed using {@link #release}.
 */
public interface ManagedObject<T>
{
    /**
     * Return the object being managed.
     */
    T getObject();

    /**
     * Returns the context for this object, or null if there is context.
     */
    ManagedObjectContext getContext();

    /**
     * A convenience method to return {@link ManagedObjectContext#getContextData} if {@link #getContext} is non-null.
     * Otherwise, returns null.
     *
     * @param klass the data type
     * @return the context data, or null if the data type is unrecognized
     */
    <R> R getContextData(Class<R> klass);

    /**
     * A convenience method to invoke {@link ManagedObjectContext#release} if {@link #getContext} is non-null.
     */
    void release();

    /**
     * Returns true if the factory is doing managed object creation as opposed to
     * being a simple wrapper around {@link Class#newInstance}.
     *
     * {@see ManagedObjectFactory#isManaged}
     */
    boolean isLifecycleManaged();

    /**
     * @return the Bean's scope, null if it is not bean
     */
    String getBeanScope();
}
