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
 * A <code>ComponentDisabledException</code> is used to signal to the runtime
 * framework that a component exists in the runtime framework but is currently
 * disabled (for reasons unspecified). The component which throws this event
 * will be exempt from further lifecycle events.
 * 
 * <p>
 * <b>Note:</b> Configuration analyzers or a server activation plan are preferential ways to avoid using this exception. For example, it is inappropriate to throw this exception to
 * prevent component startup based on server type.
 * 
 * @see com.ibm.wsspi.runtime.component.WsComponent#initialize(Object)
 * 
 * @ibm-spi
 */
public class ComponentDisabledException extends WsRuntimeFwException { // LIDB1966

    private static final long serialVersionUID = -6923053474821316164L;

    /**
     * Constructs a new <code>ComponentDisabledException</code> with <code>null</code> as its detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause}
     */
    public ComponentDisabledException() {}

    /**
     * Constructs a new <code>ComponentDisabledException</code> with the specified
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}
     * 
     * @param msg
     *            the detail message. The detail message is saved for
     *            later retrieval by the {@link #getMessage()} method.
     */
    public ComponentDisabledException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new <code>ComponentDisabledException</code> with the specified
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
    public ComponentDisabledException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new <code>ComponentDisabledException</code> with the specified
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
    public ComponentDisabledException(String msg, Throwable t) {
        super(msg, t);
    }
}