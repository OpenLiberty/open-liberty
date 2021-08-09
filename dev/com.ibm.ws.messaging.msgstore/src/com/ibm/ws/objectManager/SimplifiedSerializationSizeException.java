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
 * Thrown when the maximum size of a SimpleSerialization object exceeds its maximum size.
 * 
 * @param Object throwing the exception.
 * @param long the expected maximum size of the serialized ManagedObject.
 * @param long the actual size of the serialized ManagedObject.
 */
public final class SimplifiedSerializationSizeException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 1261822491101875639L;

    protected SimplifiedSerializationSizeException(Object source,
                                                   long maximumSize,
                                                   long actualSize)
    {
        super(source,
              SimplifiedSerializationSizeException.class,
              new Object[] { new Long(maximumSize),
                            new Long(actualSize) });
    } // End of Constructor.
} // End of class InvalidStateException.
