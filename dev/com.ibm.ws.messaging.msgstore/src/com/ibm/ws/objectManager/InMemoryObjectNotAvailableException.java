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
 * Thrown when a MemoryObjectStore was asked to retieve an managedObject that was not already in memory.
 * 
 */
public final class InMemoryObjectNotAvailableException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 2014135726065224050L;

    /*
     * Constructor.
     * 
     * @param Objectstore throwing the exception.
     * 
     * @param Token trying to get its ManagedObject.
     */
    protected InMemoryObjectNotAvailableException(ObjectStore source,
                                                  Token token)
    {
        super(source,
              InMemoryObjectNotAvailableException.class,
              new Object[] { source,
                            token });

    } // InMemoryObjectNotAvailableException().
} // class InMemoryObjectNotAvailableException.
