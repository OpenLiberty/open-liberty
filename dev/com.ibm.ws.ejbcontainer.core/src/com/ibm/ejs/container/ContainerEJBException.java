/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import javax.ejb.EJBException;

import com.ibm.ejs.container.util.ExceptionUtil;

/**
 * This subclass of EJBException is intended to be used by the
 * ejb container when it needs to throw an exception from a method that
 * implements a SUN architected Java EE interface and the interface does not
 * permit an appropriate checked exception to be thrown
 * (e.g. ContainerInternalError exception). Since it is a RuntimeException,
 * it can be thrown by any method. However, please only use this
 * class when interface does not allow the appropriate checked exception
 * to be thrown.
 */
public class ContainerEJBException extends EJBException //p118356
{
    private static final long serialVersionUID = -8150478492856479226L;

    /**
     * Constructs a new <code>ContainerEJBException</code> with null
     * as its detail message and cause.
     */
    public ContainerEJBException()
    {
        super();
    }

    /**
     * Constructs a new <code>ContainerEJBException</code> with a
     * specified detail message and null as the cause.
     * 
     * @param message - the detail message (which is saved for later
     *            retrieval by the getMessage() method).
     */
    public ContainerEJBException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new <code>ContainerEJBException</code> with the
     * specified detail message and cause.
     * 
     * @param message - the detail message (which is saved for later
     *            retrieval by the getMessage() method).
     * @param cause - the cause (which is saved for later retrieval by
     *            the getCause() method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public ContainerEJBException(String message, Throwable cause)
    {
        super(message, ExceptionUtil.Exception(cause));
    }

    /**
     * Construct a new WsException with a null detail message and a cause field
     * set to the specified Throwable. Subsequent calls to the {@link #initCause} method on this instance will result in an exception. The value of the cause
     * field may be retrieved at any time via the {@link #getCause()} method.
     * <p>
     * 
     * @param cause the Throwable that was caught and is considered the root cause
     *            of this exception. Null is tolerated.
     */
    public ContainerEJBException(Throwable cause)
    {
        super(ExceptionUtil.Exception(cause)); //150727
    }

} // end of class
