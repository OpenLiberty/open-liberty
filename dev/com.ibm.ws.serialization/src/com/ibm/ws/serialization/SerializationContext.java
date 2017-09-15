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
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * A context for serializing objects.
 */
public interface SerializationContext {
    /**
     * Add an object replacer local to this context only.
     * 
     * @param resolver the resolver to add
     */
    void addObjectReplacer(SerializationObjectReplacer replacer);

    /**
     * Create a stream for serializing objects using this context.
     * 
     * @param output the output stream to write serialized object data
     * @return a stream for serialization
     * @throws IOException if the {@link ObjectOutputStream} constructor throws
     *             an exception
     */
    ObjectOutputStream createObjectOutputStream(OutputStream output) throws IOException;
}
