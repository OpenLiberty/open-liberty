/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
// Class: RecoverableUnitSectionExistsException
//------------------------------------------------------------------------------
/**
* This exception is generated if an attempt is made to create a new 
* RecoverableUnitSection with an identity that is already in use.
*/
public class RecoverableUnitSectionExistsException extends Exception
{
    public RecoverableUnitSectionExistsException()
    {
        this(null);
    }
    
    public RecoverableUnitSectionExistsException(Throwable cause)
    {
        super(cause);    
    }
}

