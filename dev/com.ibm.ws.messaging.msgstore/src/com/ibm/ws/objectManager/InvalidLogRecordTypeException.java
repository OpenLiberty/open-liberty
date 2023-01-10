package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

/**
 * Thrown when a Log Record with an unrecognised type is read.
 */
public final class InvalidLogRecordTypeException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 258733197559887827L;

    protected InvalidLogRecordTypeException(Object source, int type)
    {
        super(source
              , InvalidLogRecordTypeException.class
              , new Integer(type));
    } // InvalidLogRecordTypeException(). 
} // End of class InvalidLogRecordTypeException.
