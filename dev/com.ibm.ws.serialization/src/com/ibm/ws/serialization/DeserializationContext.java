/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * A context for deserializing objects.
 */
public interface DeserializationContext {
    /**
     * Add an object resolver local to this context only.
     * 
     * @param resolver the resolver to add
     */
    void addObjectResolver(DeserializationObjectResolver resolver);

    /**
     * Create a stream for deserializing objects using this context. When
     * deserializing application objects, the specified class loader is
     * typically the thread context class loader.
     * 
     * @param input the input stream containing serialized object data
     * @param classLoader the class loader for resolving classes
     * @return a stream for deserialization
     * @throws IOException if the {@link ObjectInputStream} constructor throws
     *             an exception
     */
    ObjectInputStream createObjectInputStream(InputStream input, ClassLoader classLoader) throws IOException;
}
