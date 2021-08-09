package com.ibm.ws.sib.msgstore.task;
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

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;

public class TaskListException extends SevereMessageStoreException
{
    private static final long serialVersionUID = 6923446797671545643L;

    public TaskListException()
    {
        super();
    }

    public TaskListException(String message)
    {
        super(message);
    }

    public TaskListException(Throwable exception)
    {
        super(exception);
    }

    public TaskListException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public TaskListException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public TaskListException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}
