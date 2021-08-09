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
 * A Message Store sub-exception that signifies an attempt was made to
 * carry out some unsupported processing.
 */
public class NotSupportedException extends MessageStoreException
{
    private static final long serialVersionUID = 1876240915914565653L;

    public NotSupportedException()
    {
        super();
    }

    public NotSupportedException(String message)
    {
        super(message);
    }

    public NotSupportedException(Throwable exception)
    {
        super(exception);
    }

    public NotSupportedException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public NotSupportedException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public NotSupportedException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

