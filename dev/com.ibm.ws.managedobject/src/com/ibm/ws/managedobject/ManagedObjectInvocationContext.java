/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject;

import java.util.Map;

/**
 * Provides a context that supports calling @AroundConstruct interceptors
 * for ManagedObject creation.
 */
public interface ManagedObjectInvocationContext<T> {

    /**
     * Calls the @AroundConstruct interceptors for the managed object and then completes
     * instance creation by calling {@link ConstructionCallback#proceed} when the last
     * interceptor calls proceed. The managed object instance is returned.
     *
     * @param constructionCallback constructor callback to complete instance creation
     * @param parameters the parameters that will be passed to the managed object constructor.
     *            These parameters should be made available to {@link javax.interceptor.AroundConstruct} interceptors through the {@link InvocationContext#getParameters()} method.
     * @param data the context data associated with this {@link javax.interceptor.AroundConstruct} invocation.
     *            The data should be made available to {@link javax.interceptor.AroundConstruct} interceptors through {@link InvocationContext#getContextData()}.
     * @return the created instance
     */
    T aroundConstruct(ConstructionCallback<T> constructionCallback, Object[] parameters, Map<String, Object> data) throws Exception;

    ManagedObjectContext getManagedObjectContext();
}
