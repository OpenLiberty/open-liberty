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
package com.ibm.ws.cdi.impl.managedobject;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedConstructor;

import org.jboss.weld.construction.api.ConstructionHandle;

import com.ibm.ws.managedobject.ConstructionCallback;

/**
 * The implementation for ConstructionCallback
 */
public class ConstructionCallbackImpl<T> implements ConstructionCallback<T> {

    private final ConstructionHandle<T> handle;
    private final AnnotatedConstructor<T> constructor;

    public ConstructionCallbackImpl(ConstructionHandle<T> handle, AnnotatedConstructor<T> constructor) {
        this.handle = handle;
        this.constructor = constructor;
    }

    /** {@inheritDoc} */
    @Override
    public T proceed(Object[] parameters, Map<String, Object> data) {
        return handle.proceed(parameters, data);
    }

    /** {@inheritDoc} */
    @Override
    public Constructor<T> getConstructor() {
        return constructor.getJavaMember();
    }

}
