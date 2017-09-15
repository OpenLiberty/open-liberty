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
 * Thrown when the object manager catches NIOException which cannot safely be retried.
 */
public final class PermanentNIOException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -6397836846919935377L;

    /**
     * Permanent NIO Exception.
     * 
     * @param Object which is throwing this PermanentNIOException.
     * @param Exception which was caught.
     */
    protected PermanentNIOException(Object source,
                                    Exception exception)
    {
        super(source,
              PermanentNIOException.class,
              exception);

    } // PermanentNIOException().

} // End of class PermanentNIOException.
