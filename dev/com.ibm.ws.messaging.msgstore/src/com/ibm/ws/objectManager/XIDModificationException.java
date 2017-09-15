package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Thrown when an attempt is made to set another XID for a transaction where it is already set.
 * 
 * @param Object throwing the exception.
 * @param byte[] the existing XID.
 * @para, byte[] the rejected XID.
 */
public final class XIDModificationException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 154584820534086970L;

    protected XIDModificationException(Object source,
                                       byte[] existingXID,
                                       byte[] rejectedXID)
    {
        super(source,
              XIDModificationException.class,
              new Object[] { existingXID,
                            rejectedXID });
    } // XIDModificationException().
} // class XIDModificationException.
