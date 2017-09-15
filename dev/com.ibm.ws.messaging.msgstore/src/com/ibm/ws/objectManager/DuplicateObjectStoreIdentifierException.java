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
 * Thrown when an attempt is made to construct an ObjectStore with an identifier
 * that has already been used.
 * 
 * @param ObjectStore
 *            which already uses the name.
 * @param String
 *            name attempted to be reused.
 * @param int the objectStoreIdentifier which is already in use.
 * @param ObjectStore which is already using the idetifier.
 */
public final class DuplicateObjectStoreIdentifierException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -896508052084689528L;

    protected DuplicateObjectStoreIdentifierException(Object source,
                                                      String objectStoreName,
                                                      int objectStoreIdentifier,
                                                      ObjectStore existingObjectStore)
    {
        super(source,
              DuplicateObjectStoreIdentifierException.class,
              new Object[] { objectStoreName,
                            new Integer(objectStoreIdentifier),
                            existingObjectStore });

    } // DuplicateObjectStoreIdentifierException().
} // class DuplicateObjectStoreIdentifierException.