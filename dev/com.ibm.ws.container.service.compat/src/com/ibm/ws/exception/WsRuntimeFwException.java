/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
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
 * A <code>WsRuntimeFwException</code> and its subclasses are conditions that
 * the runtime framework might want to catch.
 * <p>
 * <b>Note:</b> Since the Application Server only supports JDK 1.4 and newer JDKs, new exceptions should extend <code>java.lang.Exception</code> which supports exception chaining.
 * 
 * @ibm-spi
 */
public class WsRuntimeFwException extends WsException {

    private static final long serialVersionUID = -8155373574187853057L;

    transient boolean reported; // D410379

    /**
     * Constructs a new <code>WsRuntimeFwException</code> with <code>null</code> as its detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause}
     */
    public WsRuntimeFwException() {
        super();
    }

    /**
     * Constructs a new <code>WsRuntimeFwException</code> with the specified
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}
     * 
     * @param s
     *            the detail message. The detail message is saved for
     *            later retrieval by the {@link #getMessage()} method.
     */
    public WsRuntimeFwException(String s) {
        super(s);
    }

    /**
     * Constructs a new <code>WsRuntimeFwException</code> with the specified
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
    public WsRuntimeFwException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new <code>WsRuntimeFwException</code> with the specified
     * detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this exception's detail message.
     * 
     * @param s
     *            the detail message (which is saved for later retrieval
     *            by the {@link #getMessage()} method).
     * @param t
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public WsRuntimeFwException(String s, Throwable t) {
        super(s, t);
    }

}
