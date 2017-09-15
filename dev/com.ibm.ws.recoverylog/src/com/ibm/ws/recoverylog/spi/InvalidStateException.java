/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Class: InvalidStateException
//------------------------------------------------------------------------------
/**
* Generic exception indicating that an operation has been attempted whilst the
* system is not in the correct state to handle it.
*/
public class InvalidStateException extends Exception
{
    protected InvalidStateException(Throwable cause)
    {
        super(cause);
    }
    
    public InvalidStateException()
    {
        this(null);
    }
}

