/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.persistent;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Exception indicating a failure related to the persistent store.
 * The original exception must be chained as the cause.
 */
// TODO switch to proposed spec class
public class PersistentStoreException extends RuntimeException {
    private static final long serialVersionUID = -7825981788826014265L;

    /**
     * Constructs a <code>PersistentStoreException</code> with <code>null</code> as its detail message.
     * The cause is not initialized, and must subsequently be initialized by a call to <code>Throwable.initCause(java.lang.Throwable)</code>.
     */
    public PersistentStoreException() {
        super();
    }

    /**
     * Constructs a <code>PersistentStoreException</code> with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the <code>Throwable.getMessage()</code> method).
     * @param cause the cause (which is saved for later retrieval by the <code>Throwable.getCause()</code> method).
     */
    public PersistentStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Disallow serialization because we want this to be replaced with a spec exception class.
     * 
     * @param out stream to which to serialize
     * @throws IOException if there is an error writing to the stream
     */
    @Trivial
    private void writeObject(ObjectOutputStream out) throws IOException {
    	throw new NotSerializableException();
    }
}
