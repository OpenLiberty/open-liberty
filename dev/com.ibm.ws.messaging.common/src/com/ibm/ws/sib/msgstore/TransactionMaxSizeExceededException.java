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
 * This exception is thrown when the administered size limit for 
 * a transaction is reached upon calling Transaction.incrementCurrentSize()
 */
public class TransactionMaxSizeExceededException extends TransactionException
{
    private static final long serialVersionUID = -2900534940556773209L;

    public TransactionMaxSizeExceededException()
    {
        super();
    }

    public TransactionMaxSizeExceededException(String message)
    {
        super(message);
    }

    public TransactionMaxSizeExceededException(Throwable exception)
    {
        super(exception);
    }

    public TransactionMaxSizeExceededException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public TransactionMaxSizeExceededException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public TransactionMaxSizeExceededException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

