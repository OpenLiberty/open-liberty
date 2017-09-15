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

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Provides a callback mechanism to complete construction of a managed object
 * after the @AroundConstruct callback interceptors have completed.
 */
public interface ConstructionCallback<T> {

    /**
     * Returns the constructor used for instance creation. {@link #proceed} will
     * perform instance creation, but the caller of proceed may use this method
     * to obtain information about the constructor; such as a list of the
     * parameter types.
     */
    Constructor<T> getConstructor();

    /**
     * Proceed to the next step in construction of the component. The constructed component
     * instance is returned.
     *
     * @param parameters the parameters to be passed to the component constructor
     * @param data the context data associated with the {@link ManagedObjectInvocationContext}
     * @return instance the constructed instance
     */
    T proceed(Object[] parameters, Map<String, Object> data) throws Exception;
}
