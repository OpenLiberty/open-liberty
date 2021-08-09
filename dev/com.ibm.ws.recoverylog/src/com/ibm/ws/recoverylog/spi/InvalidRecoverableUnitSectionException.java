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
// Class: InvalidRecoverableUnitSectionException
//------------------------------------------------------------------------------
/**
* This exception indicates that an operation has failed as the caller has
* referenced a RecoverableUnitSection that is not recognised by the recovery log 
* service
*/
public class InvalidRecoverableUnitSectionException extends Exception
{
    public InvalidRecoverableUnitSectionException()
    {
        this(null);
    }
    
    protected InvalidRecoverableUnitSectionException(Throwable cause)
    {
        super(cause);
    }
}

