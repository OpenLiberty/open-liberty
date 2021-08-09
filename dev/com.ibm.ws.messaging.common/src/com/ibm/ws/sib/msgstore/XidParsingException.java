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
 * This exception represents a failure to restore an XID from 
 * our datastore so it is a SevereException as we need to try 
 * to ensure data integrity.
 */
public class XidParsingException extends SevereMessageStoreException
{
    private static final long serialVersionUID = -8722325695588636013L;

    public XidParsingException()
    {
        super();
    }

    public XidParsingException(String message)
    {
        super(message);
    }

    public XidParsingException(Throwable exception)
    {
        super(exception);
    }

    public XidParsingException(String message, Throwable exception)
    {
        super(message, exception);
    }

    public XidParsingException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public XidParsingException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}