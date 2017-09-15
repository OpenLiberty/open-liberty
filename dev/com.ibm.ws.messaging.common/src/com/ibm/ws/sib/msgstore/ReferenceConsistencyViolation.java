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
 * This class is thrown to indicate that the (admittedly convoluted) reference consistency
 * rules have been violated.  The reference consistency rules are:
 * <ul> 
 * <li>an item reference can only be added to a reference stream in the same transaction as the
 * referred item is added to its item stream.</li>
 * <li>an item reference can only be added to a reference stream owned by the item stream
 * that owns the referred item.</li>
 * </ul>
 */
public final class ReferenceConsistencyViolation extends SevereMessageStoreException
{
    private static final long serialVersionUID = -9217769294938439356L;

    public ReferenceConsistencyViolation(String message, Object[] inserts)
    {
        super(message, inserts);
    }
}
