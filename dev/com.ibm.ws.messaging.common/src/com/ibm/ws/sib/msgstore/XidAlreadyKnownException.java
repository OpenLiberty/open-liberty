package com.ibm.ws.sib.msgstore;
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


/**
 * A Message Store sub-exception that defines exceptions that occur during
 * transaction processing.
 */
public class XidAlreadyKnownException extends TransactionException
{
    private static final long serialVersionUID = -7548775616524029862L;

    public XidAlreadyKnownException()
    {
        super();
    }

    public XidAlreadyKnownException(String message)
    {
        super(message);
    }

    public XidAlreadyKnownException(Throwable exception)
    {
        super(exception);
    }

    public XidAlreadyKnownException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public XidAlreadyKnownException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public XidAlreadyKnownException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

