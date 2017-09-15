/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.uow;

/**
 * This exception is thrown by UOWManager.runUnderUOW() to indicate that the
 * action being performed threw a checked exception. The exception thrown by
 * the action can be obtained by calling the getCause() method.
 * 
 * @see UOWManager#runUnderUOW(int, boolean, UOWAction)
 * 
 * @ibm-spi
 */
public class UOWActionException extends Exception
{
    private static final long serialVersionUID = 44035455915950660L;

    /** 
     * Constructs a new UOWActionException wrapping the given
     * Exception.
     */ 
    public UOWActionException(Exception cause)
    {
        super(cause);
    }

    /**
     * Returns the exception thrown by UOWManager.runUnderUOW() that
     * caused this exception to be thrown.
     * 
     * @see UOWManager#runUnderUOW(int, boolean, UOWAction)
     */
    public Throwable getCause()
    {
        return super.getCause();
    }
}
