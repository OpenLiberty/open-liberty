/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache;

import java.io.NotSerializableException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This exception indicates that there was an error serializing an object before inserting
 * it into the JCache.
 */
@Trivial
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String notSerializableClassName = null;

    /**
     * Instantiate a new {@link SerializationException}.
     *
     * @param message The exception message.
     * @param cause   The cause.
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);

        if (cause instanceof NotSerializableException) {
            notSerializableClassName = cause.getMessage(); // The message is the class name.
        }
    }

    /**
     * If the cause was a {@link NotSerializableException}, return the class name from the exception.
     *
     * @return The class name, or null if the cause was not a {@link NotSerializableException}.
     */
    public String getNotSerializableClass() {
        return notSerializableClassName;
    }
}
