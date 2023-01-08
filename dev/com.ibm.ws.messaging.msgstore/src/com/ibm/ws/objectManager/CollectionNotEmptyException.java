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
 * Thrown when we try to delete a Collection which is not empty.
 * 
 * @param Collection the non empty collection.
 * @param long the dirty size of the collection.
 * @param Transaction controlling the visibility.
 */
public final class CollectionNotEmptyException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 6421287229418387453L;

    protected CollectionNotEmptyException(Object sourceCollection,
                                          long size,
                                          Transaction transaction)
    {
        super(sourceCollection,
              CollectionNotEmptyException.class,
              new Object[] { sourceCollection,
                            new Long(size),
                            transaction });
    } // CollectionNotEmptyException().
} // class CollectionNotEmptyException.
