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


public class PersistenceFullException extends PersistenceException
{
    private static final long serialVersionUID = -2879621386417154464L;

    public PersistenceFullException()
    {
        super();
    }

    public PersistenceFullException(String message)
    {
        super(message);
    }

    public PersistenceFullException(Throwable exception)
    {
        super(exception);
    }

    public PersistenceFullException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public PersistenceFullException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public PersistenceFullException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}


