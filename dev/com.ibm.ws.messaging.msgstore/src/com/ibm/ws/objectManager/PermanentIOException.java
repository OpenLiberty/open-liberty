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
 * Thrown when the object manager catches IOException which cannot safely be retried.
 */
public final class PermanentIOException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -3084699571860961761L;

    /**
     * Permanent IO Exception.
     * 
     * @param Class of static which throws this PermanentIOException.
     * @param java.io.IOException which was caught.
     */
    protected PermanentIOException(Class sourceClass,
                                   java.io.IOException ioException)
    {
        super(sourceClass,
              PermanentIOException.class,
              ioException,
              ioException);

    } // PermanentIOException().

    /**
     * Permanent IO Exception.
     * 
     * @param Object which is throwing this PermanentIOException.
     * @param java.io.IOException which was caught.
     */
    protected PermanentIOException(Object source,
                                   java.io.IOException ioException)
    {
        super(source,
              PermanentIOException.class,
              ioException,
              ioException);

    } // PermanentIOException().

} // End of class PermanentIOException.
