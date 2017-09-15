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
// Class: InvalidFailureScopeException
//------------------------------------------------------------------------------
/**
* This exception indicates that an invalid FailureScope has been detected.
*/
public class InvalidFailureScopeException extends Exception
{
    public InvalidFailureScopeException()
    {
        this(null);
    }
    
    public InvalidFailureScopeException(Throwable cause)
    {
        super(cause);
    }
}

