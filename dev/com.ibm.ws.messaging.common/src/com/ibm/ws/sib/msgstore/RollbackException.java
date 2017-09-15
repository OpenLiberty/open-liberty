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
 * This exception is a result of work associated with a transaction being rolled-back 
 * despite the completion direction of the transaction being commit. This is 
 * usually caused by an unrecoverable error occuring during completion of the
 * transaction.
 */
public class RollbackException extends TransactionException
{
    private static final long serialVersionUID = 8183924275251354462L;

    public RollbackException()
    {
        super();
    }

    public RollbackException(String message)
    {
        super(message);
    }

    public RollbackException(Throwable exception)
    {
        super(exception);
    }

    public RollbackException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public RollbackException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public RollbackException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

