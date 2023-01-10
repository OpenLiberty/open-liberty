/*******************************************************************************
 * Copyright (c) 2011,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * This class allows for deserializing objects with a specific ClassLoader.
 */
public class SerialObjectInputStream extends ObjectInputStream {
    /**
     * ClassLoader to use in deserialization
     */
    private final ClassLoader loader;

    /**
     * Construct an object input stream with a user defined ClassLoader
     * 
     * @param in input stream to read from
     * @param loader class loader to use during read
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public SerialObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
        super(in);
        this.loader = loader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        return Class.forName(name, false, loader);
    }
}
