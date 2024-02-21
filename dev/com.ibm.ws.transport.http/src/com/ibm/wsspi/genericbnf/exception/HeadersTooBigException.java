/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.genericbnf.exception;

/**
 * Thrown when the headers passed exceed the allowed value.
 */
public class HeadersTooBigException extends MalformedMessageException {

    /** Serialization ID value */
    // static final private long serialVersionUID = -5148185552401118734L;

    /**
     * Constructor for the headers too big exception
     * 
     * @param message
     */
    public HeadersTooBigException(String message) {
        super(message);
    }
}
