/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * This subclass of java.lang.Exception is intended to be used by the methods
 * in InjectionEngine interfaces.
 */
public class InjectionException extends Exception
{
    private static final long serialVersionUID = 4288178466335454550L;

    /**
     * Constructs a new <code>InjectionException</code> with null
     * as its detail message and cause.
     */
    public InjectionException()
    {
        super();
    }

    /**
     * Constructs a new <code>InjectionException</code> with a
     * specified detail message and null as the cause.
     *
     * @param message - the detail message (which is saved for later
     *            retrieval by the getMessage() method).
     */
    public InjectionException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new <code>InjectionException</code> with the
     * specified detail message and cause.
     *
     * @param message - the detail message (which is saved for later
     *            retrieval by the getMessage() method).
     * @param cause - the cause (which is saved for later retrieval by
     *            the getCause() method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public InjectionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new <code>InjectionException</code> with the
     * specified detail message and cause.
     *
     * @param cause - the cause (which is saved for later retrieval by
     *            the getCause() method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public InjectionException(Throwable cause)
    {
        super(cause);
    }
}
