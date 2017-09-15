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
 * Exception indicating that the item is not in the message store, and therefore
 * the requested operation is invalid.
 */
public final class NotInMessageStore extends SevereMessageStoreException
{
    private static final long serialVersionUID = -1595749431001619585L;
}
