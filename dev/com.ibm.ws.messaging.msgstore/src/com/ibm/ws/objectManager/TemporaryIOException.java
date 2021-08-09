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
 * Thrown when the object manager detects an IO error which can safely be retried
 * without the need to restart the ObjectManager.
 */
public final class TemporaryIOException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 819675519281478532L;

    /**
     * Temporary IO Exception.
     * 
     * @param Object which is throwing this TemporaryIOException.
     * @param java.io.IOException which was caught.
     */
    protected TemporaryIOException(Object source,
                                   java.io.IOException ioException)
    {
        super(source,
              TemporaryIOException.class,
              ioException,
              ioException);

    } // TemporaryIOException().

} // class TemporaryIOException.
