/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.exception;

/**
 * Chain name is equal to that of anothers.
 */
public class InvalidChainNameException extends ChainException {

    /** Serialization ID string */
    private static final long serialVersionUID = -2233425810071024316L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public InvalidChainNameException(String message) {
        super(message);
    }
}
