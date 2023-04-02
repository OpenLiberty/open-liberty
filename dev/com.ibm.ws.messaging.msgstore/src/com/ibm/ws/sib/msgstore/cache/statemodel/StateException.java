package com.ibm.ws.sib.msgstore.cache.statemodel;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;

/**
 * Our very own severe exception for state errors
 */
public class StateException extends SevereMessageStoreException 
{
    private static final long serialVersionUID = 8031640561007073391L;

    public StateException(String message) 
    {
        super(message);
    }
}
