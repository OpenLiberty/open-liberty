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
 * Thrown when we attempt to store a named ManagedObject and there are no restartable
 * ObjectStores available.
 */
public final class NoRestartableObjectStoresAvailableException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 939643560396649135L;

    protected NoRestartableObjectStoresAvailableException(ObjectManager objectManager)
    {
        super(objectManager,
              NoRestartableObjectStoresAvailableException.class,
              objectManager);
    } // NoRestartableObjectStoresAvailableException().
} // class NoRestartableObjectStoresAvailableException.