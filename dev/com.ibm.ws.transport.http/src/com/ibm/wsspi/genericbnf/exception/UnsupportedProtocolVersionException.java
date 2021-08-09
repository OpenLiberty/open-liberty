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
 * Thrown when the protocol matched is not supported.
 * 
 * @ibm-private-in-use
 */
public class UnsupportedProtocolVersionException extends MalformedMessageException {

    /** Serialization ID value */
    static final private long serialVersionUID = 7979141254464450816L;

    /**
     * Constructor for an unsupported protocol version exception
     * 
     * @param message
     */
    public UnsupportedProtocolVersionException(String message) {
        super(message);
    }
}
