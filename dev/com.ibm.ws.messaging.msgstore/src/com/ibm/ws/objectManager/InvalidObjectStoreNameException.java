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
 * Thrown when an ObjectStore is constructed with an invalid name.
 * 
 * @param ObjectStore raising the exception.
 * @param String the invalid name.
 */
public final class InvalidObjectStoreNameException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5715968074536796455L;

    protected InvalidObjectStoreNameException(ObjectStore source,
                                              String storeName)
    {
        super(source,
              InvalidObjectStoreNameException.class,
              new Object[] { source,
                            storeName });

    } // InvalidObjectStoreNameException(). 
} // class InvalidObjectStoreNameException.
