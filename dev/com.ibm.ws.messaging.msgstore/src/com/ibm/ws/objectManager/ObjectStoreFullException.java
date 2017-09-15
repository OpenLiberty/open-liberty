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
 * Thrown when the ObjectStore is too full to hold a new ManagedObject.
 * 
 * @param ObjectStore requested to make the allocate() request.
 * @param ManagedObject requesting the allocate().
 */
public final class ObjectStoreFullException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -8191673442607535682L;

    protected ObjectStoreFullException(ObjectStore source,
                                       ManagedObject managedObjectToStore)
    {
        super(source,
              ObjectStoreFullException.class,
              new Object[] { source, managedObjectToStore });

    } // ObjectTsoreFulleexception(). 
} // End of class ObjectStoreFullException.