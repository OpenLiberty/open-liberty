/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.htod;

/**
 * Thrown to indicate that some component of an ODG does not exist.
 *
 */
public class HashtableOnDiskException extends RuntimeException {
    
    private static final long serialVersionUID = -5948997263475181113L;
    
    /**
     * Constructs an NoSuchObjectException with the specified
     * detail message.
     */
    public  HashtableOnDiskException(String message) {
        super(message);
    }

    public  HashtableOnDiskException(String message, Exception e) {
        super(message);
        wrapped_exception = e;
    }

    private Exception wrapped_exception;
    public Exception getWrappedException() { return wrapped_exception; }
}


