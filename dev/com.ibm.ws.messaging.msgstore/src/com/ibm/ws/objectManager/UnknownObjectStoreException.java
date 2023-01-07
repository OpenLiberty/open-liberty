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
 * Thrown when an attempt is made to locate an object store whose name is not registered with th object manager.
 * 
 * @param Object throwing the exception.
 * @param String the name of the ObjectStore.
 */
public final class UnknownObjectStoreException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -6059445638681247169L;

    protected UnknownObjectStoreException(Object source,
                                          String objectStoreName)
    {
        super(source,
              UnknownObjectStoreException.class,
              new Object[] { objectStoreName });
    } // UnknownObjectStoreException().
} // class NonExistentObjectStoreException.
