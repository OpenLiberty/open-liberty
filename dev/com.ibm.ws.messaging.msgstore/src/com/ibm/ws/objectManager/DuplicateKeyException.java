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
 * Thrown when an attempt is made to create a duplicate key in a map.
 * 
 * @param Map throwing the exception.
 * @param Object key which is a duplicate.
 * @param Map.Entry that already exists in the map.
 * @param Internal Transaction locking the existing antry or null.
 */
public final class DuplicateKeyException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -8673649483448257805L;

    protected DuplicateKeyException(Map source,
                                    Object key,
                                    Map.Entry entry,
                                    InternalTransaction lockingTransaction)
    {
        super(source,
              DuplicateKeyException.class,
              new Object[] { key,
                            entry,
                            lockingTransaction });
    } // DuplicateKeyException().
} // class DuplicateKeyException.
