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
 * Thrown when an attempt is made to construct an ObjectStore with a name that has already been used.
 * 
 * @param ObjectStore
 *            which already uses the name.
 * @param String
 *            name attempted to be reused.
 * @param ObjectStore which is already using the name.
 */
public final class DuplicateObjectStoreNameException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -8851457471856201460L;

    protected DuplicateObjectStoreNameException(Object source,
                                                String objectStoreName,
                                                ObjectStore existingObjectStore)
    {
        super(source,
              DuplicateObjectStoreNameException.class,
              new Object[] { objectStoreName,
                            existingObjectStore });

    } // DuplicateObjectStoreNameException().
} // class DuplicateObjectStoreNameException.