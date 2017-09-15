/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.AccessController;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.session.ILoader;

/**
 * This class is an implementation of the ILoader interface and it allows the store to
 * load objects from their serialized forms. This object needs to be primed
 * with the classloader to use before it can do its task.
 * 
 */
public class SessionLoader implements ILoader {

    private static final ThreadContextAccessor threadContextAccessor =
                    AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    private static ClassLoader getContextClassLoader() {
        return threadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
    }

    private final SerializationService _serializationService;

    private ClassLoader _classLoader;

    public SessionLoader(SerializationService serializationService, ClassLoader classLoader, boolean isApplicationSession) {
        _serializationService = serializationService;
        _classLoader = isApplicationSession ? null : classLoader;
    }

    /**
     * Loads an object using the classloader associated with this loader.
     * <p>
     * 
     * @see com.ibm.wsspi.session.ILoader#loadObject(java.io.InputStream)
     */
    public Object loadObject(InputStream inputStream) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = _classLoader;
        if (classLoader == null) {
            classLoader = getContextClassLoader();
        }

        ObjectInputStream objectInputStream = _serializationService.createObjectInputStream(inputStream, classLoader);
        Object object = null;
        try {
            object = objectInputStream.readObject();
        } catch (Throwable t) {
            FFDCFilter.processException(
                                        t,
                                        "com.ibm.ws.session.SessionLoader.loadObject",
                                        "82",
                                        this);

            if (t instanceof IOException) {
                throw (IOException) t;
            }

            if (t instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) t;
            }
        }

        return object;
    }
}
