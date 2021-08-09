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
 * This exception will be thrown if the transaction being used
 * to add an Item (or ItemStream) to the MessageStore originates
 * from a different MS to that where the ItemStream being added
 * to resides.
 */
public class MismatchedMessageStoreException extends TransactionException
{
    private static final long serialVersionUID = -3504065490008776065L;

    public MismatchedMessageStoreException()
    {
        super();
    }

    public MismatchedMessageStoreException(String message)
    {
        super(message);
    }

    public MismatchedMessageStoreException(Throwable exception)
    {
        super(exception);
    }

    public MismatchedMessageStoreException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public MismatchedMessageStoreException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public MismatchedMessageStoreException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

