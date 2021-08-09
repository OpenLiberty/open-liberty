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
 * Thrown when an attempt is made to replace a Token with
 * a different Token, rather than update in place the same object.
 * 
 * @param The ObjectStore throwing the ReplacementException.
 * @param ManagedObject trying to replace the different token.
 * @param Token the new Token trying to replace the existing one.
 * @param Token already in the store.
 */
public final class ReplacementException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 4367412760461258433L;

    protected ReplacementException(ObjectStore source
                                   , ManagedObject managedObject
                                   , Token managedObjectOwningToken
                                   , Token existingToken)
    {
        super(source,
              ReplacementException.class,
              new Object[] { source, managedObject, managedObjectOwningToken, existingToken });
    } // ReplacementException(). 
} // class ReplacementException.
