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
 * Thrown to indicate that an add operation was performed with an item that does not
 * have a valid constructor.  A valid constructor for the message store purposes
 * must be public, accessible, and take no arguments.  This exception will also be
 * thrown if the item being added is a non-static inner class.
 */
public final class InvalidConstructor extends SevereMessageStoreException
{
    private static final long serialVersionUID = -8577657583093944999L;

    public InvalidConstructor(String message, Throwable exception)
    {
        super(message, exception);
    }
}
