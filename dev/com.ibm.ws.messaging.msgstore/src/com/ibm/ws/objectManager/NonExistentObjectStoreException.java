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
 * Thrown when an attempt is made to locate an object store whose
 * objectStoreIdentifier is not registered with the ObjectManager.
 * 
 * @param ObjectManagerState throwing the exception.
 * @param int identifier of the ObjectStore.
 */
public final class NonExistentObjectStoreException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5401561769543505756L;

    protected NonExistentObjectStoreException(ObjectManagerState objectManagerState,
                                              int objectStoreIdentifier)
    {
        super(objectManagerState,
              NonExistentObjectStoreException.class,
              new Object[] { new Integer(objectStoreIdentifier) });

    } // NonExistentObjectStoreException().
} // class NonExistentObjectStoreException.
