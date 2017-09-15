/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others.
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
 * The <code>WsNestedException</code> interface defines the interface that a
 * WebSphere
 * exception class must implement to obtain the exception chaining semantics
 * that will
 * be available in the JDK 1.4 timeframe. It is intended for usage by Exception
 * subclasses
 * that are constrained to extend an architected exception that does not provide
 * these
 * semantics. Exception classes that are free to extend java.lang.Exception or
 * java.lang.Runtime exceptions should extend the
 * com.ibm.ws.exception.WsException and
 * com.ibm.ws.exception.WsRuntimeException instead.
 * <p>
 * Exceptions that implement the WsNestedException interface and do not already contain a legacy exception chaining mechanism must do the following.
 * <ul>
 * <li>contain an attribute to maintain a reference to the causing or nested exception. For example private Throwable ivCause; See WsException for a sample.
 * <li>provide the appropriate constructors to allow the specification of a nested exception during construction. This is not absolutely required, but is good practice.
 * <li>provide implementations for the methods defined in this interface. In most cases, the method implementations can be copied directly from WsException, if desired.
 * <li>provide appropriate implementations for the printStackTrace(PrintStream) and printStackTrace(PrintWriter) methods. These implementations can be copied directly from the
 * WsException class.
 * </ul>
 * Exceptions that already implement a legacy exception chaining mechanism must perform the moral equivalent of the above operations. However, the implementations may vary based on
 * the form of the legacy chaining mechanism.
 * 
 * @deprecated - We are now using JDK 1.4 - D200273.1
 * @ibm-private-in-use
 */
public interface WsNestedException {

    /**
     * Return the Throwable that is considered the root cause of this WsException.
     * Null is returned if the root cause is nonexistent or unknown. The root
     * cause is
     * the throwable that caused this WsNestedException to get thrown.
     * <p>
     * The Throwable that is returned is either the Throwable supplied via one of the appropriate constructors, that set via the {@link #initCause(Throwable)} method, or that
     * obtained from the legacy exception chaining mechanism if the class already provides one.
     * <p>
     * 
     * @return the Throwable that is the cause of this WsException, or null if the
     *         cause
     *         is nonexistent or unknown.
     */
    public Throwable getCause();

    /**
     * Initialize the cause field for this WsNestedException to the specified
     * value. The
     * cause is the Throwable that caused this WsNestedException to get thrown.
     * <p>
     * For classes that do not implement an exception chaining mechanism, this method can be called at most once. In such a case, it is generally called from within a constructor
     * that takes a Throwable, or immediately after constructing the exception object with a constructor that does not accept a Throwable. Thus, if a constructor that takes
     * Throwable
     * as a parameter is used to construct this object, it cannot be called at all.
     * <p>
     * For classes that already implement an exception chaining mechanism, the implementation of this method must provide moral equivalence to the above restrictions. Essentially,
     * a
     * cause is allowed to be specified only once. Once specified it is not allowed to change. The cause cannot be the exception itself.
     * <p>
     * 
     * @param cause
     *            the Throwable which caused this WsException to be thrown. Null is
     *            tolerated.
     * @return a reference to this <code>Throwable</code> instance.
     * @exception IllegalArgumentException
     *                if the specified cause is the same exception as
     *                the object this method is being called on. An exception cannot
     *                be its own cause.
     * @exception IllegalStateException
     *                if this WsNestedException was created with a constructor
     *                that specified a cause, or this method has already been called
     *                on this object.
     */
    public Throwable initCause(Throwable cause) throws IllegalArgumentException, IllegalStateException;

}
