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
 * Thrown when a Log Record part with an unrecognised type is read.
 * 
 * @param LogInput throwing the exception.
 * @param byte the invalid part type.
 */
public final class InvalidLogRecordPartTypeException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 3919536104502610744L;

    protected InvalidLogRecordPartTypeException(LogInput source,
                                                byte partType)
    {
        super(source,
              InvalidLogRecordPartTypeException.class,
              new Byte(partType));

    } // InvalidLogRecordPartTypeException(). 
} // class InvalidLogRecordTypeException.
