/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
// Class: InternalLogException
//------------------------------------------------------------------------------
/**
* This exception indicates that an operation has failed due to an unexpected
* error condition. The recovery log service is in an undefined state and continued
* use may be impossible.
*/
public class InternalLogException extends Exception
{
    String reason = null;
    public InternalLogException()
    {
        this(null);  
    }
    public InternalLogException(Throwable cause)
    {
        super(cause);
    }
    public InternalLogException(String s,Throwable cause)
    {
        super(s, cause);
        reason = s;
    }  
    public String toString()
    {
        if(reason != null)
            return reason + ", " + super.toString();
        else
            return super.toString();
    }
}

