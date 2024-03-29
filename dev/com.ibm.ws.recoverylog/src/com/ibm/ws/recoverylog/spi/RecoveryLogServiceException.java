/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
// Class: RecoveryLogServiceException
//------------------------------------------------------------------------------
public class RecoveryLogServiceException extends Exception
{
    public RecoveryLogServiceException() 
    {
    }

    public RecoveryLogServiceException(String msg) 
    {
        super(msg);
    }

    public RecoveryLogServiceException(Throwable throwable) 
    {
        super(throwable);
    }

    public RecoveryLogServiceException(String msg, Throwable throwable) 
    {
        super(msg, throwable);
    }
}

