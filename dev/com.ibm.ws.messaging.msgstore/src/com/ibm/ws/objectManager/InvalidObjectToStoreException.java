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
 * Thrown when an attempt is made to store an invalid object.
 * 
 * @param ObjectStore throwing the exception.
 * @param ManagedObject which is invalid.
 */
public final class InvalidObjectToStoreException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 1623366100827054579L;

    protected InvalidObjectToStoreException(ObjectStore source,
                                            ManagedObject objectToStore)
    {
        super(source,
              InvalidObjectToStoreException.class,
              new Object[] { source,
                            objectToStore });
    } // InvalidObjectToStoreException(). 
} // class InvalidObjectToStoreException.
