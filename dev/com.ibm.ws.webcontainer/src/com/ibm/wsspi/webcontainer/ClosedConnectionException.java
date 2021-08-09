/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

//  PK04668    IF THE CLIENT THAT MADE THE SERVLET REQUEST GOES DOWN,THERE IS    WAS.webcontainer

/**
 * This class is a subclass of IOException used to differentiate when a client
 * prematurely terminates a connection to the server from other IOExceptions.
 */
public class ClosedConnectionException extends java.io.IOException
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256441387271468600L;

	/**
     * Creates a new ClosedConnectionException object and invokes the 
     * super constructor.
     */
    public ClosedConnectionException()
    {
        super();
    }

    /**
     * Creates a new ClosedConnectionException object with an exception string
     * and invokes the super constructor with the string.
     *
     * @param s The message string to set in this exception
     */
    public ClosedConnectionException(String s)
    {
        super(s);
    }

    /**
     * Creates a new ClosedConnectionException object with an exception string
     * and root cause, invokes the super constructor with the string, and
     * sets initCause to the root cause.
     *
     * @param s The message string to set in this exception.
     * @param writeStatusCode The write status code.
     */

    public ClosedConnectionException(String s, Throwable t)
    {
        super(s);
        this.initCause(t);
    }

}
