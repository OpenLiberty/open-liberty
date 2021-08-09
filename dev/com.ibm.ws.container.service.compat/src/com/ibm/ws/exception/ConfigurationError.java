/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.exception;

/**
 * A <code>ConfigurationError</code> is used to signal to the runtime
 * framework that a serious initialization error has occurred. The
 * server will shut down immediately.
 * 
 * @see com.ibm.wsspi.runtime.component.WsComponent#initialize(Object)
 * 
 * @ibm-spi
 */

public class ConfigurationError extends WsRuntimeFwException { // LIDB1966

    private static final long serialVersionUID = -7546557410643519975L;

    /**
     * Constructs a new <code>ConfigurationError</code> with the specified
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}
     * 
     * @param msg
     *            the detail message. The detail message is saved for
     *            later retrieval by the {@link #getMessage()} method.
     */
    public ConfigurationError(String msg) {
        super(msg);
    }

    /**
     * Constructs a new <code>ConfigurationError</code> with the specified
     * cause and a detail message of <tt>(cause==null ? null : cause.toString())</tt> (which typically
     * contains the class and detail message of <tt>cause</tt>).
     * 
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables.
     * 
     * @param t
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public ConfigurationError(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new <code>ConfigurationError</code> with the specified
     * detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this exception's detail message.
     * 
     * @param msg
     *            the detail message (which is saved for later retrieval
     *            by the {@link #getMessage()} method).
     * @param t
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public ConfigurationError(String msg, Throwable t) {
        super(msg, t);
    }
}