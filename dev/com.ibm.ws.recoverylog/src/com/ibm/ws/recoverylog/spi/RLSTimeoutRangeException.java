/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
// Class: RLSSuspendTimeoutException
//------------------------------------------------------------------------------
public class RLSTimeoutRangeException extends Exception
{
    public RLSTimeoutRangeException() 
    {
    }

    public RLSTimeoutRangeException(String msg) 
    {
        super(msg);
    }

    public RLSTimeoutRangeException(Throwable throwable) 
    {
        super(throwable);
    }

    public RLSTimeoutRangeException(String msg, Throwable throwable) 
    {
        super(msg, throwable);
    }
}

