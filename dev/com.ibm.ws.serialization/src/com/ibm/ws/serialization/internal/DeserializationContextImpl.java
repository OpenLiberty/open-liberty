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
package com.ibm.ws.serialization.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationContext;
import com.ibm.ws.serialization.DeserializationObjectResolver;

public class DeserializationContextImpl implements DeserializationContext {
    private final SerializationServiceImpl service;
    private List<DeserializationObjectResolver> resolvers;

    public DeserializationContextImpl(SerializationServiceImpl service) {
        this.service = service;
    }

    @Override
    public void addObjectResolver(DeserializationObjectResolver resolver) {
        if (resolvers == null) {
            resolvers = new ArrayList<DeserializationObjectResolver>();
        }
        resolvers.add(resolver);
    }

    @Override
    public ObjectInputStream createObjectInputStream(InputStream input, ClassLoader classLoader) throws IOException {
        return new DeserializationObjectInputStreamImpl(input, classLoader, this);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return service.loadClass(name);
    }

    public boolean isResolveObjectNeeded() {
        return resolvers != null || service.isResolveObjectNeeded();
    }

    /**
     * @param object the serialization object
     * @return the resolved object (if any) or the serialization object
     */
    @Sensitive
    public Object resolveObject(@Sensitive Object object) throws IOException {
        if (resolvers != null) {
            for (DeserializationObjectResolver resolver : resolvers) {
                Object resolvedObject = resolver.resolveObject(object);
                if (resolvedObject != null) {
                    return resolvedObject;
                }
            }
        }

        return service.resolveObjectWithException(object);
    }
}
