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
 * This exception is a result of a rollback being requested on a
 * separate thread to the prepare/commit thread that is 
 * currently in the middle of tran completion.
 */
public class DeferredRollbackException extends TransactionException
{
    private static final long serialVersionUID = 335280537236623108L;

    public DeferredRollbackException()
    {
        super();
    }

    public DeferredRollbackException(String message)
    {
        super(message);
    }

    public DeferredRollbackException(Throwable exception)
    {
        super(exception);
    }

    public DeferredRollbackException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public DeferredRollbackException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public DeferredRollbackException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}

