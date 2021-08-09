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
package com.ibm.ws.sib.msgstore;

/**
 * Exception to indicate that the owning stream has exceeded its storage
 * limit.
 * 
 * @author DrPhill
 *
 */
public final class StreamIsFull extends MessageStoreException
{
    private static final long serialVersionUID = -2236937663160002456L;

    public StreamIsFull()
    {
        super();
    }
}
