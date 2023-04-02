/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Class: ConflictingCredentialsException
//------------------------------------------------------------------------------
/**
* This exception indicates that an operation has failed as the caller has attempted
* to register a service with the RecoveryLogManager class using a service name that
* is already in use.
*/
public class ConflictingCredentialsException extends Exception
{
    public ConflictingCredentialsException()
    {
        this(null);
    }
    
    protected ConflictingCredentialsException(Throwable cause)
    {
        super(cause);
    }
}

