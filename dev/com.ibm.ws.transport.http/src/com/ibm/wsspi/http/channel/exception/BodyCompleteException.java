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
package com.ibm.wsspi.http.channel.exception;

/**
 * There is no more body to be processed.
 * 
 * @ibm-private-in-use
 */
public class BodyCompleteException extends Exception {

    /** Serialization ID value */
    static final private long serialVersionUID = 9133046536096026337L;

    /**
     * Constructor for BodyCompleteException.
     * 
     * @param message
     */
    public BodyCompleteException(String message) {
        super(message);
    }
}
