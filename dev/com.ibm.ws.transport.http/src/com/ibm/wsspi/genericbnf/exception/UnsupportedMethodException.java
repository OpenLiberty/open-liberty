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
 * Thrown when the method given is not supported or enabled.
 * 
 * @ibm-private-in-use
 */
public class UnsupportedMethodException extends MalformedMessageException {

    /** Serialization ID value */
    static final private long serialVersionUID = -5148185552401118734L;

    /**
     * Constructor for the unsupported method exception
     * 
     * @param message
     */
    public UnsupportedMethodException(String message) {
        super(message);
    }
}
