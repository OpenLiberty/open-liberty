/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * This exception is thrown to indicate an attempt has been made to use
 * an EJB that is not known to the EJB Container. Most likely, the application
 * is either not currently installed, or has not been started.
 */
public class EJBNotFoundException
                extends ClassNotFoundException
{
    private static final long serialVersionUID = -7732975938299597918L;

    /**
     * Create a new <code>EJBNotFoundException</code> instance. <p>
     * 
     * @param j2eeName unique Java EE name representing the EJB.
     */
    public EJBNotFoundException(String j2eeName)
    {
        super(j2eeName);
    }

    /**
     * Create a new <code>EJBNotFoundException</code> with the
     * specified detail message and cause.
     * 
     * @param message - the detail message (which is saved for later
     *            retrieval by the getMessage() method).
     * @param cause - the cause (which is saved for later retrieval by
     *            the getCause() method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public EJBNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
