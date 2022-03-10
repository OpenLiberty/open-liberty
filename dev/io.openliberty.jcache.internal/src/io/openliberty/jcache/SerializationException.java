/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This exception indicates that there was an error serializing an object before inserting
 * it into the JCache.
 */
@Trivial
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Instantiate a new {@link SerializationException}.
     *
     * @param message The exception message.
     * @param cause   The cause.
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
