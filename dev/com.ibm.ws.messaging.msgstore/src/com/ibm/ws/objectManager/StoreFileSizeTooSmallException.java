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
 * Thrown when an Objectstore is requested to reduce its file size below that which will
 * allow it to store the existing contents of the store.
 * 
 * @param ObjectStore which has been requested to reduce its size.
 * @param long the new maximum size of the store file.
 * @param long the surrent size of the store file.
 * @param long the ammount of space currently used in the store file.
 */
public final class StoreFileSizeTooSmallException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8783023996392115290L;

    protected StoreFileSizeTooSmallException(ObjectStore source
                                             , long maximumStoreFileSize
                                             , long storeFileSizeAllocated
                                             , long storeFileSizeUsed)
    {
        super(source,
              StoreFileSizeTooSmallException.class,
              new Object[] { new Long(maximumStoreFileSize)
                            , new Long(storeFileSizeAllocated)
                            , new Long(storeFileSizeUsed) });
    } // Constructor.
} // End of class StoreFileSizeTooSmallException.
