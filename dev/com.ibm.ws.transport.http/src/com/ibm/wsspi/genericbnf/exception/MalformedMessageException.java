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
 * Thrown when a malformed message is detected during message parsing.
 * 
 * @ibm-private-in-use
 */
public class MalformedMessageException extends Exception {

    /** Serialization ID value */
    static final private long serialVersionUID = -5170605634089879689L;

    /**
     * Constructor for the malformed message exception
     * 
     * @param message
     */
    public MalformedMessageException(String message) {
        super(message);
    }
}
