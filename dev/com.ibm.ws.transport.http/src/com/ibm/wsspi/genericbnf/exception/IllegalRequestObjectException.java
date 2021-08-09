/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.genericbnf.exception;

/**
 * Thrown when the object set is not a valid request object
 * 
 * @ibm-private-in-use
 */
public class IllegalRequestObjectException extends Exception {

    /** Serialization ID value */
    static final private long serialVersionUID = -6834366662772796503L;

    /**
     * Constructor for this exception
     * 
     * @param message
     */
    public IllegalRequestObjectException(String message) {
        super(message);
    }
}
