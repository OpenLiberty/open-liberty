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

public final class InvalidAddOperation extends SevereMessageStoreException
{
    private static final long serialVersionUID = 8943647275303324403L;

    public InvalidAddOperation(String message, Object[] inserts)
    {
        super(message, inserts);
    }

    public InvalidAddOperation(String message, Object insert)
    {
        super(message, new Object[]{insert});
    }
}
