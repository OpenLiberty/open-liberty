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


public class ProtocolException extends TransactionException
{
    private static final long serialVersionUID = -7192283248493760657L;

    public ProtocolException()
    {
        super();
    }

    public ProtocolException(String message)
    {
        super(message);
    }

    public ProtocolException(Throwable exception)
    {
        super(exception);
    }

    public ProtocolException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public ProtocolException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public ProtocolException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

