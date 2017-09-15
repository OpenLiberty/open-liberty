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


public class MessageStoreUnavailableException extends PersistenceException
{
    private static final long serialVersionUID = 8140943749856118198L;

    public MessageStoreUnavailableException()
    {
        super();
    }

    public MessageStoreUnavailableException(String message)
    {
        super(message);
    }

    public MessageStoreUnavailableException(Throwable exception)
    {
        super(exception);
    }

    public MessageStoreUnavailableException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public MessageStoreUnavailableException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public MessageStoreUnavailableException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

