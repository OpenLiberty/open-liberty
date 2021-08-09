package com.ibm.ws.LocalTransaction;
/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase 
 * is not supported.
 *
 */

/**
 * Thrown when a LocalTransactionCoordinator is completed with EndModeCommit
 * after the LTC has been marked RollbackOnly. Any enlisted resources are rolled back.
 *
 */
public final class RolledbackException extends Exception
{

    private static final long serialVersionUID = 6433647289443709613L;

    protected Throwable cause = null;

    public RolledbackException()
    {
        super();
    }

    public RolledbackException(String msg)
    {
        super(msg);
    }

    public RolledbackException(String msg, Throwable t)
    {
        super(msg);
        cause = t;
    }

    public Throwable getNestedException()
    {
        return cause;
    }
}
