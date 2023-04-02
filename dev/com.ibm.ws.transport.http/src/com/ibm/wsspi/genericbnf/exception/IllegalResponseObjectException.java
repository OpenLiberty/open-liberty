/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package com.ibm.wsspi.genericbnf.exception;

/**
 * Thrown when the object set is not a valid response object
 * 
 * @ibm-private-in-use
 */
public class IllegalResponseObjectException extends Exception {

    /** Serialization ID value */
    static final private long serialVersionUID = 4074608858088631945L;

    /**
     * Constructor for this exception
     * 
     * @param message
     */
    public IllegalResponseObjectException(String message) {
        super(message);
    }
}
