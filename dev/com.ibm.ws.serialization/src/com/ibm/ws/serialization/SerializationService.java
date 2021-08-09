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
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * A service for serializing user objects that can contain references to
 * product internal objects.
 *
 * <p>As an alternative to this service, if the serialization/deserialization
 * will always happen using an application thread context class loader, then {@link DeserializationObjectInputStream} could
 * be used with thread context class loader if the necessary classes are all
 * exported with the thread-context attribute. This approach does not work if
 * an application thread context class loader is not used or if replacers or
 * resolvers are needed.
 */
public interface SerializationService {
    /**
     * Creates a serialization context that uses all globally registered {@link SerializationObjectReplacer},
     * and which allows additional customizations.
     */
    SerializationContext createSerializationContext();

    /**
     * Create a stream for serializing objects using a default context, which
     * uses all registered {@link SerializationObjectReplacer}.
     *
     * @param output the output stream to write serialized object data
     * @return a stream for serialization
     * @throws IOException if the {@link ObjectOutputStream} constructor throws
     *             an exception
     */
    ObjectOutputStream createObjectOutputStream(OutputStream output) throws IOException;

    /**
     * Creates a deserialization context that uses all globally registered {@link DeserializationObjectResolver} and {@link DeserializationClassProvider},
     * and which allows additional customizations.
     */
    DeserializationContext createDeserializationContext();

    /**
     * Create a stream for deserializing objects using a default context, which
     * uses all registered {@link DeserializationClassProvider} and {@link DeserializationObjectReplacer}.
     * When deserializing application objects, the specified class loader is
     * typically the thread context class loader.
     *
     * @param input the input stream containing serialized object data
     * @return a stream for deserialization
     * @param classLoader the class loader for resolving classes
     * @throws IOException if the {@link ObjectInputStream} constructor throws
     *             an exception
     */
    ObjectInputStream createObjectInputStream(InputStream input, ClassLoader classLoader) throws IOException;

    /**
     * Attempt to return an object appropriate for serialization. This can be
     * used by services that need to know whether or not an object can be
     * serialized. If non-null is returned, the object should be passed to
     * ObjectOutputStream.writeObject.
     *
     * @param object an object potentially for serialization
     * @return an object for serialization, or null if the object cannot be serialized
     * @see #resolveObject
     */
    Object replaceObjectForSerialization(Object object);

    /**
     * Resolve an object returned by replaceObjectForSerialization. If an
     * error occurs while resolving an object, a RuntimeException will be thrown.
     *
     * @param object an object returned by {@link #replaceObjectForSerialization}
     * @return a resolved object, or the input object if it does not need to be
     *         resolved
     * @throws RuntimeException if an error occurs while resolving the object
     */
    Object resolveObject(Object object);

    /**
     * Resolve an object returned by replaceObjectForSerialization.
     *
     * @param object an object returned by {@link #replaceObjectForSerialization}
     * @return a resolved object, or the input object if it does not need to be
     *         resolved
     */
    Object resolveObjectWithException(Object object) throws IOException;
}
