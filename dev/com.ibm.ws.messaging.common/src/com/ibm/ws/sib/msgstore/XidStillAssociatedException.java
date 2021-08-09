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
public class XidStillAssociatedException extends TransactionException
{
    private static final long serialVersionUID = -4504349232943302987L;

    public XidStillAssociatedException()
    {
        super();
    }

    public XidStillAssociatedException(String message)
    {
        super(message);
    }

    public XidStillAssociatedException(Throwable exception)
    {
        super(exception);
    }

    public XidStillAssociatedException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public XidStillAssociatedException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public XidStillAssociatedException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

