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

public class SevereMessageStoreException extends MessageStoreException
{
    private static final long serialVersionUID = -3790027338845641878L;

    public SevereMessageStoreException()
    {
        super();
    }

    public SevereMessageStoreException(String message)
    {
        super(message);
    }

    public SevereMessageStoreException(Throwable exception)
    {
        super(exception);
    }

    public SevereMessageStoreException(String message, Throwable exception)
    {
        super(message, exception);
    }

    /**
     * Provide a key and use formatted string
     * @param arg0
     * @param args
     */
    public SevereMessageStoreException(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    /**
     * Provide a key and use formatted string
     * @param arg0
     * @param args
     */
    public SevereMessageStoreException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }
}