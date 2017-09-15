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
 * Thrown when an object store finds a sequence number is not unique.
 * 
 * @param source reporting the problem.
 * @param sequenceNumber the non unique sequence number.
 * @param existingToken already using the sequence number.
 */
public final class StoreSequenceException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 4022708165277331465L;

    protected StoreSequenceException(ObjectStore source,
                                     long sequenceNumber,
                                     Token existingToken)
    {
        super(source,
              StoreSequenceException.class,
              new Object[] { source,
                            new Long(sequenceNumber),
                            existingToken });
    } // StoreSequenceException().
} // class StoreSequenceException.
