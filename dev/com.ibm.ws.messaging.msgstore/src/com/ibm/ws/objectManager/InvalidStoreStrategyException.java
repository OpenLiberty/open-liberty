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
 * Thrown when an ObjectStore is constructed using an invalid store strategy.
 */
public final class InvalidStoreStrategyException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 1161447988576386799L;

    /**
     * @param ObjectStore
     *            being constructed.
     * @param int
     *        The invalid store strategy value.
     */
    protected InvalidStoreStrategyException(ObjectStore source,
                                            int invalidStoreStrategy)
    {
        super(source,
              InvalidStoreStrategyException.class,
              new Object[] { source,
                            new Integer(invalidStoreStrategy) });

    } //InvalidTransactionException().

} // class InvalidTransactionException.
